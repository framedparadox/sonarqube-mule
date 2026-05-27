package com.sonar.plugins.mulesoft.sensors.metrics;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;

import com.sonar.plugins.mulesoft.config.PropertyLoader;
import com.sonar.plugins.mulesoft.xpath.XPathProcessor;
import com.sonar.plugins.mulesoft.sensors.BaseMetricSensor;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;
import com.sonar.plugins.mulesoft.util.MetricKeys;
import com.sonar.plugins.mulesoft.util.XmlParserFactory;

/**
 * Sensor for collecting configuration-level metrics from Mule XML files.
 *
 * <p>This sensor extends {@link BaseMetricSensor} to provide specialized metric collection
 * focused on Mule application configuration elements. It analyzes XML structure to extract
 * quantitative measures of application complexity and composition.</p>
 *
 * <h3>Collected Metrics:</h3>
 * <ul>
 *   <li><strong>Flows:</strong> Count of {@code <flow>} elements using XPath expressions</li>
 *   <li><strong>Sub-flows:</strong> Count of {@code <sub-flow>} elements for reusable logic blocks</li>
 *   <li><strong>Lines of Code (NCLOC):</strong> Total lines in the XML file (SonarQube standard metric)</li>
 *   <li><strong>DataWeave Transformations:</strong> Count of transformation elements in payloads and variables</li>
 * </ul>
 *
 * <h3>XPath-Based Extraction:</h3>
 * <p>Metric collection is configured through property files that define XPath expressions:
 * <pre>
 * # Example from mule.properties
 * mule.metric.flow=//mule:flow
 * mule.metric.subflow=//mule:sub-flow 
 * mule.metric.dw.payload=//dw:transform[dw:set-payload]
 * mule.metric.dw.variable=//dw:transform[dw:set-variable]
 * </pre>
 * </p>
 *
 * <h3>Usage Context:</h3>
 * <p>These metrics feed into SonarQube's quality model and can be used for:
 * <ul>
 *   <li>Technical debt calculations</li>
 *   <li>Architecture complexity assessment</li>
 *   <li>Code review prioritization</li>
 *   <li>Project size estimation</li>
 * </ul>
 * </p>
 *
 * @see BaseMetricSensor for common sensor processing logic
 * @see MuleMetrics for metric definitions
 * @see XPathProcessor for XPath evaluation capabilities
 * @since 1.0.0
 */
public class ConfigurationMetricsSensor extends BaseMetricSensor {

	private final Logger logger = LoggerFactory.getLogger(ConfigurationMetricsSensor.class);

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute size of configuration file");
	}

	@Override
	protected void process(SensorContext context, InputFile file, String language) {
		try {
			SAXBuilder saxBuilder = XmlParserFactory.createSecureBuilder();
			Document document = saxBuilder.build(file.inputStream());
			Element rootElement = document.getRootElement();
			XPathProcessor xpathProcessor = new XPathProcessor("mulesoft-namespace.properties");

			saveMetric(xpathProcessor, MuleMetrics.FLOWS, context, file, rootElement,
				PropertyLoader.getProperties().get(MetricKeys.METRIC_FLOW).toString());

		saveMetric(xpathProcessor, MuleMetrics.SUBFLOWS, context, file, rootElement,
				PropertyLoader.getProperties().get(MetricKeys.METRIC_SUBFLOW).toString());
			// Lines of code = Lines in Mule
			saveMetric(file.lines(), CoreMetrics.NCLOC, context, file);

			saveMetric(xpathProcessor, MuleMetrics.TRANSFORMATIONS, context, file, rootElement,
				PropertyLoader.getProperties().get(MetricKeys.METRIC_DW_PAYLOAD).toString(),
				PropertyLoader.getProperties().get(MetricKeys.METRIC_DW_VARIABLE).toString());
		} catch (JDOMException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void saveMetric(XPathProcessor helper, Metric<Integer> metric, SensorContext context, InputFile file,
			Element rootElement, String... xpathExpressions) {
		int result = 0;
		for (int i = 0; i < xpathExpressions.length; i++) {
			Double value = helper.evaluate(xpathExpressions[i], rootElement, Double.class);
			if (value != null) {
				result += value.intValue();
			}
		}
		saveMetric(result, metric, context, file);
	}

	private void saveMetric(int result, Metric<Integer> metric, SensorContext context, InputFile file) {
		NewMeasure<Integer> metrics = context.newMeasure();
		metrics.forMetric(metric).on(file).withValue(result).save();
	}

}
