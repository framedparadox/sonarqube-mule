package com.sonar.plugins.mulesoft.sensor;

import com.sonar.plugins.mulesoft.metrics.MuleMetrics;
import com.sonar.plugins.mulesoft.parse.ParsedDocumentCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MuleStructuralMetricsSensorTest {

    private static final String MULE_XML = """
            <?xml version="1.0"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                  xmlns:http="http://www.mulesoft.org/schema/mule/http">
              <!-- comment -->
              <http:listener-config name="cfg">
                <http:listener-connection host="0.0.0.0"/>
              </http:listener-config>
              <flow name="f1">
                <http:listener config-ref="cfg" path="/a"/>
                <choice>
                  <when expression="#[true]"/>
                  <otherwise/>
                </choice>
              </flow>
              <error-handler name="global"/>
            </mule>
            """;

    private SensorContext ctx;
    private FileSystem fs;
    private FilePredicates predicates;

    // Capture saved measures
    private final Map<String, Integer> savedMeasures = new HashMap<>();

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws Exception {
        ctx = mock(SensorContext.class);
        fs = mock(FileSystem.class);
        predicates = mock(FilePredicates.class);

        when(ctx.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(predicates);
        when(predicates.hasLanguage(anyString())).thenReturn(f -> true);

        InputFile file = mock(InputFile.class);
        when(file.inputStream()).thenAnswer(inv ->
                new ByteArrayInputStream(MULE_XML.getBytes(StandardCharsets.UTF_8)));
        when(file.lines()).thenReturn(20);
        when(fs.inputFiles(any())).thenReturn(List.of(file));

        // Capture measures saved via newMeasure()
        when(ctx.newMeasure()).thenAnswer(inv -> {
            NewMeasure m = mock(NewMeasure.class);
            final Metric[] metricHolder = new Metric[1];
            final int[] valueHolder = new int[]{0};
            final InputFile[] fileHolder = new InputFile[]{null};

            when(m.forMetric(any())).thenAnswer(i -> {
                metricHolder[0] = i.getArgument(0);
                return m;
            });
            when(m.on(any())).thenAnswer(i -> {
                fileHolder[0] = i.getArgument(0);
                return m;
            });
            when(m.withValue(any())).thenAnswer(i -> {
                Object val = i.getArgument(0);
                if (val instanceof Integer iv) valueHolder[0] = iv;
                return m;
            });
            doAnswer(i -> {
                if (metricHolder[0] != null) {
                    savedMeasures.put(metricHolder[0].key(), valueHolder[0]);
                }
                return null;
            }).when(m).save();
            return m;
        });
    }

    @Test
    void describe_sets_name_and_language() {
        SensorDescriptor desc = mock(SensorDescriptor.class);
        when(desc.name(anyString())).thenReturn(desc);
        when(desc.onlyOnLanguage(anyString())).thenReturn(desc);

        new MuleStructuralMetricsSensor(new ParsedDocumentCache()).describe(desc);

        verify(desc).name("Mule Structural Metrics");
        verify(desc).onlyOnLanguage("mule");
    }

    @Test
    void counts_flows_handlers_listeners_and_core_metrics() {
        new MuleStructuralMetricsSensor(new ParsedDocumentCache()).execute(ctx);

        assertThat(savedMeasures.get(MuleMetrics.FLOWS.key())).isEqualTo(1);
        assertThat(savedMeasures.get(MuleMetrics.HTTP_LISTENERS.key())).isEqualTo(1);
        assertThat(savedMeasures.get(MuleMetrics.ERROR_HANDLERS.key())).isEqualTo(1);
        assertThat(savedMeasures.get(MuleMetrics.CONNECTOR_CONFIGS.key())).isEqualTo(1);
        // flow f1: base 1 + 1 "when" branch = 2
        assertThat(savedMeasures.get(MuleMetrics.FLOW_MAX_COMPLEXITY.key())).isEqualTo(2);
        assertThat(savedMeasures.get(CoreMetrics.NCLOC.key())).isGreaterThan(0);
        assertThat(savedMeasures.get(CoreMetrics.COMMENT_LINES.key())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void zero_metrics_for_empty_mule_file() throws Exception {
        String emptyXml = "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"/>";
        FileSystem fs2 = mock(FileSystem.class);
        FilePredicates preds2 = mock(FilePredicates.class);
        when(ctx.fileSystem()).thenReturn(fs2);
        when(fs2.predicates()).thenReturn(preds2);
        when(preds2.hasLanguage(anyString())).thenReturn(f -> true);
        InputFile emptyFile = mock(InputFile.class);
        when(emptyFile.inputStream()).thenAnswer(inv ->
                new ByteArrayInputStream(emptyXml.getBytes(StandardCharsets.UTF_8)));
        when(fs2.inputFiles(any())).thenReturn(List.of(emptyFile));

        savedMeasures.clear();
        new MuleStructuralMetricsSensor(new ParsedDocumentCache()).execute(ctx);

        assertThat(savedMeasures.getOrDefault(MuleMetrics.FLOWS.key(), 0)).isEqualTo(0);
        assertThat(savedMeasures.getOrDefault(MuleMetrics.ERROR_HANDLERS.key(), 0)).isEqualTo(0);
    }
}
