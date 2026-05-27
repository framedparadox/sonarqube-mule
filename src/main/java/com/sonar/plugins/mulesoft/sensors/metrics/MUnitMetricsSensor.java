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
import com.sonar.plugins.mulesoft.util.MetricKeys;
import com.sonar.plugins.mulesoft.util.XmlParserFactory;

/**
 * Sensor for collecting MUnit test metrics from Mule test configuration files.
 *
 * <p>This sensor specializes in analyzing MUnit test files to extract test-related metrics
 * that contribute to SonarQube's test coverage and quality assessments. It identifies
 * and counts various MUnit test constructs within XML test configurations.</p>
 *
 * <h3>Primary Metric:</h3>
 * <ul>
 *   <li><strong>Test Count (TESTS):</strong> Total number of MUnit test cases defined in the file</li>
 * </ul>
 *
 * <p>The test count is determined using XPath expressions configured in the property files,
 * typically targeting MUnit-specific XML elements such as:
 * <pre>
 * # Example XPath for MUnit tests
 * mule.metric.test=//munit:test
 * </pre>
 * </p>
 *
 * <h3>Integration with SonarQube:</h3>
 * <p>This sensor contributes to SonarQube's standard {@link CoreMetrics#TESTS} metric,
 * which is used for:
 * <ul>
 *   <li>Test coverage ratio calculations</li>
 *   <li>Quality gate evaluations</li>
 *   <li>Technical debt assessments</li>
 *   <li>Development team productivity metrics</li>
 * </ul>
 * </p>
 *
 * <h3>File Processing:</h3>
 * <p>The sensor processes MUnit test files (typically {@code *test*.xml} or {@code *Test*.xml})
 * and uses secure XML parsing via {@link XmlParserFactory}. XPath evaluation is performed
 * with namespace awareness through the configured namespace properties.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // MUnit test file structure:
 * {@code
 * <mule xmlns:munit="http://www.mulesoft.org/schema/mule/munit">
 *   <munit:config name="test-config"/>
 *   
 *   <munit:test name="test-flow-success">
 *     <!-- test implementation -->
 *   </munit:test>
 *   
 *   <munit:test name="test-flow-error-handling">
 *     <!-- test implementation -->
 *   </munit:test>
 * </mule>
 * }
 * // Results in TESTS = 2
 * </pre>
 *
 * @see BaseMetricSensor for common sensor processing patterns
 * @see CoreMetrics#TESTS for the SonarQube standard test metric
 * @see XPathProcessor for XPath evaluation with namespace support
 * @since 1.0.0
 */
public class MUnitMetricsSensor extends BaseMetricSensor {

	private final Logger logger = LoggerFactory.getLogger(MUnitMetricsSensor.class);

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute number of unit test cases");
	}

	@Override
	protected void process(SensorContext context, InputFile file, String language) {
		try {
			SAXBuilder saxBuilder = XmlParserFactory.createSecureBuilder();
			Document document = saxBuilder.build(file.inputStream());
			Element rootElement = document.getRootElement();
			XPathProcessor xpathProcessor = new XPathProcessor("mulesoft-namespace.properties");
			saveMetric(xpathProcessor, CoreMetrics.TESTS, context, file, rootElement,
					PropertyLoader.getProperties().get(MetricKeys.METRIC_UNIT_TESTS).toString());
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
		NewMeasure<Integer> metrics = context.newMeasure();
		metrics.forMetric(metric).on(file).withValue(result).save();
	}

}
