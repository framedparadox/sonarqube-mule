package com.sonar.plugins.mulesoft.metrics;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

/**
 * Defines custom metrics for Mule application analysis.
 * 
 * <p>This class registers custom metrics with SonarQube for tracking:
 * <ul>
 *   <li>Structural metrics: flows, subflows, transformations</li>
 *   <li>Complexity metrics: configuration file complexity rating</li>
 *   <li>Size metrics: number of configuration files</li>
 * </ul>
 * 
 * <p>All metrics are automatically aggregated from file level to higher levels
 * (directory, module, project) by the corresponding aggregator classes.</p>
 * 
 * @see com.sonar.plugins.mulesoft.metrics.aggregation
 * @since 1.0.0
 */
public class MuleMetrics implements Metrics {

	/**
	 * Number of flows in Mule configuration files.
	 * 
	 * <p>Flows are the primary processing units in Mule applications.
	 * Higher flow counts may indicate increased complexity.</p>
	 */
	public static final Metric<Integer> FLOWS = new Metric.Builder("mule_flows", "Flows", Metric.ValueType.INT)
			.setDescription("Number of flows")
			.setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false)
			.setDomain(CoreMetrics.DOMAIN_SIZE)
			.create();

	/**
	 * Number of subflows in Mule configuration files.
	 * 
	 * <p>Subflows are reusable processing units called from flows.
	 * Good subflow usage indicates proper modularization.</p>
	 */
	public static final Metric<Integer> SUBFLOWS = new Metric.Builder("mule_subflows", "SubFlows", Metric.ValueType.INT)
			.setDescription("Number of subflows")
			.setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false)
			.setDomain(CoreMetrics.DOMAIN_SIZE)
			.create();

	/**
	 * Number of DataWeave transformations in Mule configuration files.
	 * 
	 * <p>Counts both payload transformations and variable assignments using DataWeave.
	 * High transformation counts may indicate data-intensive applications.</p>
	 */
	public static final Metric<Integer> TRANSFORMATIONS = new Metric.Builder("mule_dataweave_transformations", "DataWeave Transformations",
			Metric.ValueType.INT)
			.setDescription("Number of DataWeave transformations")
			.setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false)
			.setDomain(CoreMetrics.DOMAIN_SIZE)
			.create();

	/**
	 * Configuration file complexity rating (A/B/C).
	 * 
	 * <p>Calculated based on total flow and subflow count:
	 * <ul>
	 *   <li>A (1): Simple complexity (≤7 flows)</li>
	 *   <li>B (2): Medium complexity (8-15 flows)</li>
	 *   <li>C (3): High complexity (>15 flows)</li>
	 * </ul></p>
	 */
	public static final Metric<Integer> CONFIGURATION_FILES_COMP_RATING = new Metric.Builder(
			"mule_complexity_rating", "Mule Complexity Rating", Metric.ValueType.RATING)
			.setDescription("Rating based on complexity of configuration file")
			.setDirection(Metric.DIRECTION_BETTER)
			.setQualitative(true)
			.setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
			.create();

	/**
	 * Number of Mule configuration files in the project.
	 * 
	 * <p>Counts all XML files with Mule namespace declarations.</p>
	 */
	public static final Metric<Integer> CONFIGURATION_FILES = new Metric.Builder("mule_configuration_files",
			"Configuration Files", Metric.ValueType.INT)
			.setDescription("Number of configuration files")
			.setDirection(Metric.DIRECTION_WORST)
			.setQualitative(false)
			.setDomain(CoreMetrics.DOMAIN_SIZE)
			.create();

	public static final Metric<Integer> ERROR_HANDLERS = new Metric.Builder(
			"mule_error_handlers", "Error Handlers", Metric.ValueType.INT)
			.setDescription("Number of error handlers")
			.setDomain("Reliability").create();

	public static final Metric<Integer> HTTP_LISTENERS = new Metric.Builder(
			"mule_http_listeners", "HTTP Listeners", Metric.ValueType.INT)
			.setDescription("Number of HTTP listener elements")
			.setDomain(CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> CONNECTOR_CONFIGS = new Metric.Builder(
			"mule_connector_configs", "Connector Configs", Metric.ValueType.INT)
			.setDescription("Number of connector configuration elements")
			.setDomain(CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> MUNIT_TESTS = new Metric.Builder(
			"mule_munit_tests", "MUnit Tests", Metric.ValueType.INT)
			.setDescription("Number of MUnit test elements")
			.setDomain(CoreMetrics.DOMAIN_COVERAGE).create();

	public static final Metric<Integer> MUNIT_ASSERTIONS = new Metric.Builder(
			"mule_munit_assertions", "MUnit Assertions", Metric.ValueType.INT)
			.setDescription("Number of MUnit assertion elements")
			.setDomain(CoreMetrics.DOMAIN_COVERAGE).create();

	public static final Metric<Integer> DW_INLINE_COUNT = new Metric.Builder(
			"mule_dataweave_inline_count", "Inline DataWeave Scripts", Metric.ValueType.INT)
			.setDescription("DataWeave transforms embedded inline in XML")
			.setDomain(CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Integer> DW_EXTERNAL_COUNT = new Metric.Builder(
			"mule_dataweave_external_count", "External DataWeave Scripts", Metric.ValueType.INT)
			.setDescription("DataWeave transforms that reference external .dwl files")
			.setDomain(CoreMetrics.DOMAIN_SIZE).create();

	public static final Metric<Double> DW_EXTERNAL_RATIO = new Metric.Builder(
			"mule_dataweave_external_ratio", "External DataWeave Ratio", Metric.ValueType.PERCENT)
			.setDescription("Percentage of DataWeave transforms that use external files")
			.setDomain(CoreMetrics.DOMAIN_SIZE)
			.setDirection(Metric.DIRECTION_BETTER).setQualitative(true).create();

	public static final Metric<Integer> FLOW_MAX_COMPLEXITY = new Metric.Builder(
			"mule_flow_max_complexity", "Max Flow Cyclomatic Complexity", Metric.ValueType.INT)
			.setDescription("Highest cyclomatic complexity across flows in a file")
			.setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
			.setDirection(Metric.DIRECTION_WORST).create();

	public static final Metric<Integer> FLOW_MAX_NESTING_DEPTH = new Metric.Builder(
			"mule_flow_max_nesting_depth", "Max Flow Nesting Depth", Metric.ValueType.INT)
			.setDescription("Deepest scope nesting across flows in a file")
			.setDomain(CoreMetrics.DOMAIN_COMPLEXITY)
			.setDirection(Metric.DIRECTION_WORST).create();

	/**
	 * Returns all custom Mule metrics for registration with SonarQube.
	 * 
	 * <p>This method is called by SonarQube during plugin initialization to
	 * register all custom metrics. The metrics will then be available for
	 * collection by sensors and display in the SonarQube UI.</p>
	 * 
	 * @return List of all Mule-specific metrics
	 * @see org.sonar.api.measures.Metrics#getMetrics()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public List<Metric> getMetrics() {
		return Arrays.asList(FLOWS, SUBFLOWS, CONFIGURATION_FILES_COMP_RATING, TRANSFORMATIONS, CONFIGURATION_FILES,
				ERROR_HANDLERS, HTTP_LISTENERS, CONNECTOR_CONFIGS, MUNIT_TESTS, MUNIT_ASSERTIONS,
				DW_INLINE_COUNT, DW_EXTERNAL_COUNT, DW_EXTERNAL_RATIO, FLOW_MAX_COMPLEXITY, FLOW_MAX_NESTING_DEPTH);
	}
}
