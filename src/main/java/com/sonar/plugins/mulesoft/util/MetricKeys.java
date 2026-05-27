package com.sonar.plugins.mulesoft.util;

/**
 * Constants for metric property keys used in MuleSoft configuration and analysis.
 *
 * <p>This utility class defines string constants for accessing configuration properties
 * that contain XPath expressions and other metric-related settings. These keys correspond
 * to properties defined in {@code mule.properties} and related configuration files.</p>
 *
 * <h3>Property File Structure:</h3>
 * <p>The keys defined here map to properties in configuration files:
 * <pre>
 * # mule.properties example
 * mule.metric.flow=//mule:flow
 * mule.metric.subflow=//mule:sub-flow
 * mule.metric.dw.payload=//dw:transform[dw:set-payload]
 * mule.metric.test=//munit:test
 * </pre>
 * </p>
 *
 * <h3>Key Categories:</h3>
 * <ul>
 *   <li><strong>Flow Metrics:</strong> METRIC_FLOW, METRIC_SUBFLOW - Structural elements</li>
 *   <li><strong>DataWeave Metrics:</strong> METRIC_DW_PAYLOAD, METRIC_DW_VARIABLE - Transformation usage</li>
 *   <li><strong>Test Metrics:</strong> METRIC_UNIT_TESTS - MUnit test counting</li>
 *   <li><strong>Coverage Properties:</strong> MUNIT_* constants - MUnit coverage configuration</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>
 * // Typical usage in sensors
 * Properties props = PropertyLoader.getProperties(language);
 * String flowXPath = props.get(MetricKeys.METRIC_FLOW).toString();
 * int flowCount = xpathProcessor.evaluate(flowXPath, rootElement, Double.class).intValue();
 * </pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class contains only static final constants and is inherently thread-safe.
 * All field values are immutable strings.</p>
 *
 * @see PropertyLoader for property loading and caching
 * @see XPathProcessor for XPath expression evaluation
 * @see CoverageData for coverage data structure
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public final class MetricKeys {

	// Flow and structure metrics
	public static final String METRIC_FLOW = "mule.metric.flow";
	public static final String METRIC_SUBFLOW = "mule.metric.subflow";
	
	// DataWeave transformation metrics
	public static final String METRIC_DW_PAYLOAD = "mule.metric.dw.payload";
	public static final String METRIC_DW_VARIABLE = "mule.metric.dw.variable";
	
	// Test metrics
	public static final String METRIC_UNIT_TESTS = "mule.metric.test";
	
	// Coverage metrics
	public static final String MUNIT_NAME = "mule.munit.properties.name";
	public static final String MUNIT_FLOWS = "mule.munit.properties.flows";
	public static final String MUNIT_FILES = "mule.munit.properties.files";
	public static final String MUNIT_LINES = "mule.munit.properties.lines";
	public static final String MUNIT_COVERAGE = "mule.munit.properties.coverage";
	public static final String MUNIT_PROCESSOR_COUNT = "mule.munit.properties.processorCount";
	public static final String MUNIT_COVERED_PROCESSOR_COUNT = "mule.munit.properties.coveredProcessorCount";
	public static final String MUNIT_LINE_NUMBER = "mule.munit.properties.lineNumber";
	public static final String MUNIT_COVERED = "mule.munit.properties.covered";

	private MetricKeys() {
		// Prevent instantiation
	}
}
