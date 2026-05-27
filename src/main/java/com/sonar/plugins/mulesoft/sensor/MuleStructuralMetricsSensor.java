package com.sonar.plugins.mulesoft.sensor;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.metrics.ComplexityComputer;
import com.sonar.plugins.mulesoft.metrics.CoreMetricsFeeder;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;
import com.sonar.plugins.mulesoft.parse.ParsedDocumentCache;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

/**
 * Sensor that performs a single DOM walk per file to collect all structural
 * metrics for Mule 4 XML configuration files.
 *
 * <p>Uses {@link ParsedDocumentCache} so that each file is parsed only once,
 * even when multiple sensors are active in the same analysis.</p>
 *
 * <h3>Metrics collected per file:</h3>
 * <ul>
 *   <li>Flows, sub-flows, error handlers, connector configs</li>
 *   <li>HTTP listener count</li>
 *   <li>MUnit tests and assertions</li>
 *   <li>DataWeave inline / external transform counts</li>
 *   <li>Max cyclomatic complexity and max nesting depth across flows</li>
 *   <li>SonarQube core metrics: NCLOC, COMMENT_LINES, COMPLEXITY, COGNITIVE_COMPLEXITY</li>
 * </ul>
 */
public class MuleStructuralMetricsSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(MuleStructuralMetricsSensor.class);

    private final ParsedDocumentCache cache;

    /** No-arg constructor used by SonarQube IoC. */
    public MuleStructuralMetricsSensor() {
        this(new ParsedDocumentCache());
    }

    public MuleStructuralMetricsSensor(ParsedDocumentCache cache) {
        this.cache = cache;
    }

    @Override
    public void describe(SensorDescriptor d) {
        d.name("Mule Structural Metrics").onlyOnLanguage(MuleLanguage.LANGUAGE_KEY);
    }

    @Override
    public void execute(SensorContext ctx) {
        for (InputFile file : ctx.fileSystem().inputFiles(
                ctx.fileSystem().predicates().hasLanguage(MuleLanguage.LANGUAGE_KEY))) {
            analyze(ctx, file);
        }
    }

    // -------------------------------------------------------------------------
    // Internal analysis
    // -------------------------------------------------------------------------

    private void analyze(SensorContext ctx, InputFile file) {
        Optional<Document> parsed = cache.get(file);
        if (parsed.isEmpty()) return;
        Element root = parsed.get().getRootElement();

        int flows = 0, subflows = 0, errorHandlers = 0, connectorConfigs = 0;
        int munitTests = 0, munitAssertions = 0, dwInline = 0, dwExternal = 0;
        int httpListeners = 0;
        int maxComplexity = 0, maxNesting = 0;
        int totalCyclomatic = 0, totalCognitive = 0;

        // --- Top-level children (flow, sub-flow, error-handler, *-config) ---
        for (Element el : root.getChildren()) {
            String name = el.getName();
            if ("flow".equals(name)) {
                flows++;
                ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(el);
                totalCyclomatic += c.cyclomatic();
                totalCognitive  += c.cognitive();
                if (c.cyclomatic()      > maxComplexity) maxComplexity = c.cyclomatic();
                if (c.maxNestingDepth() > maxNesting)    maxNesting    = c.maxNestingDepth();
            } else if ("sub-flow".equals(name)) {
                subflows++;
                ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(el);
                totalCyclomatic += c.cyclomatic();
                totalCognitive  += c.cognitive();
            } else if ("error-handler".equals(name)) {
                errorHandlers++;
            } else if (name.endsWith("-config")) {
                connectorConfigs++;
            }
        }

        // --- All descendants (elements that may appear anywhere) ---
        Iterator<Element> it = root.getDescendants(Filters.element());
        while (it.hasNext()) {
            Element el = it.next();
            String ln = el.getName();
            String ns = el.getNamespace().getURI();
            if ("listener".equals(ln) && ns.contains("/mule/http"))   httpListeners++;
            if ("test".equals(ln)     && ns.contains("/mule/munit"))  munitTests++;
            if (ln.startsWith("assert-") && ns.contains("/mule/munit")) munitAssertions++;
            if ("transform".equals(ln) && ns.contains("/mule/ee/core")) {
                if (hasExternalResource(el)) dwExternal++; else dwInline++;
            }
        }

        // --- Save structural metrics ---
        saveInt(ctx, file, MuleMetrics.FLOWS,              flows);
        saveInt(ctx, file, MuleMetrics.SUBFLOWS,           subflows);
        saveInt(ctx, file, MuleMetrics.ERROR_HANDLERS,     errorHandlers);
        saveInt(ctx, file, MuleMetrics.HTTP_LISTENERS,     httpListeners);
        saveInt(ctx, file, MuleMetrics.CONNECTOR_CONFIGS,  connectorConfigs);
        saveInt(ctx, file, MuleMetrics.MUNIT_TESTS,        munitTests);
        saveInt(ctx, file, MuleMetrics.MUNIT_ASSERTIONS,   munitAssertions);
        saveInt(ctx, file, MuleMetrics.DW_INLINE_COUNT,    dwInline);
        saveInt(ctx, file, MuleMetrics.DW_EXTERNAL_COUNT,  dwExternal);
        saveInt(ctx, file, MuleMetrics.TRANSFORMATIONS,    dwInline + dwExternal);
        saveInt(ctx, file, MuleMetrics.FLOW_MAX_COMPLEXITY,    maxComplexity);
        saveInt(ctx, file, MuleMetrics.FLOW_MAX_NESTING_DEPTH, maxNesting);
        saveInt(ctx, file, CoreMetrics.COMPLEXITY,             totalCyclomatic);
        saveInt(ctx, file, CoreMetrics.COGNITIVE_COMPLEXITY,   totalCognitive);

        // --- Core NCLOC / COMMENT_LINES ---
        try (java.io.InputStream is = file.inputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            CoreMetricsFeeder.Counts counts = CoreMetricsFeeder.countXml(content);
            saveInt(ctx, file, CoreMetrics.NCLOC,          counts.ncloc());
            saveInt(ctx, file, CoreMetrics.COMMENT_LINES,  counts.commentLines());
        } catch (IOException e) {
            LOG.warn("Could not count lines for {}: {}", file, e.getMessage());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void saveInt(SensorContext ctx, InputFile file, Metric metric, int value) {
        ctx.newMeasure().forMetric(metric).on(file).withValue(value).save();
    }

    /**
     * Returns true if any descendant of the given transform element has a non-empty
     * {@code resource} attribute, indicating an external DataWeave script reference.
     */
    private static boolean hasExternalResource(Element transform) {
        for (Element child : transform.getDescendants(Filters.element())) {
            String resource = child.getAttributeValue("resource");
            if (resource != null && !resource.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
