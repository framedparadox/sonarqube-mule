package com.sonar.plugins.mulesoft.metrics.aggregation;

import org.sonar.api.measures.Metric;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;

/**
 * Concrete aggregator for rolling up DataWeave transformation counts across the project hierarchy.
 *
 * <p>This aggregator extends {@link AbstractMetricAggregator} to provide hierarchical
 * summation of {@link MuleMetrics#TRANSFORMATIONS} from individual Mule configuration files.
 * DataWeave transformations represent data processing complexity and are key indicators
 * of application sophistication and maintenance requirements.</p>
 *
 * <h3>DataWeave Transformation Definition:</h3>
 * <p>Counts DataWeave transformation elements including:
 * <ul>
 *   <li><strong>Set Payload Transformations:</strong> {@code <dw:transform><dw:set-payload>}</li>
 *   <li><strong>Set Variable Transformations:</strong> {@code <dw:transform><dw:set-variable>}</li>
 *   <li><strong>Inline DataWeave Scripts:</strong> Complex data mapping and processing logic</li>
 *   <li><strong>DataWeave Functions:</strong> Custom transformation functions and modules</li>
 * </ul>
 * </p>
 *
 * <h3>Complexity Indicators:</h3>
 * <p>Transformation counts provide insights into:
 * <ul>
 *   <li><strong>Data Processing Complexity:</strong> Higher counts indicate complex data manipulation</li>
 *   <li><strong>Integration Sophistication:</strong> Many transformations suggest complex system integration</li>
 *   <li><strong>Performance Considerations:</strong> High transformation density may impact performance</li>
 *   <li><strong>Skill Requirements:</strong> DataWeave expertise becomes critical with higher counts</li>
 * </ul>
 * </p>
 *
 * <h3>Quality Assessment Applications:</h3>
 * <p>Transformation metrics enable various quality assessments:
 * <ul>
 *   <li><strong>Performance Analysis:</strong> Identify transformation-heavy bottlenecks</li>
 *   <li><strong>Code Review Focus:</strong> Prioritize review of high-transformation files</li>
 *   <li><strong>Training Needs:</strong> Assess team DataWeave skill requirements</li>
 *   <li><strong>Architecture Planning:</strong> Plan for transformation complexity in designs</li>
 * </ul>
 * </p>
 *
 * <h3>Ratio Analysis:</h3>
 * <p>Transformation density analysis patterns:
 * <pre>
 * Transformation Density = Transformations / Flows
 * 
 * Low Density (0-1):     Simple pass-through or routing logic
 * Medium Density (1-3):  Standard data processing applications
 * High Density (>3):     Complex data integration or ETL scenarios
 * </pre>
 * </p>
 *
 * <h3>Performance Implications:</h3>
 * <p>High transformation counts may indicate:
 * <ul>
 *   <li><strong>Memory Usage:</strong> DataWeave processing requires heap space</li>
 *   <li><strong>CPU Utilization:</strong> Complex transformations are computationally intensive</li>
 *   <li><strong>Optimization Opportunities:</strong> Consider caching or streaming for large datasets</li>
 *   <li><strong>Monitoring Needs:</strong> Enhanced monitoring for transformation performance</li>
 * </ul>
 * </p>
 *
 * @see AbstractMetricAggregator for aggregation template implementation
 * @see MuleMetrics#TRANSFORMATIONS for the metric being aggregated  
 * @see ConfigurationMetricsSensor for transformation detection logic
 * @see FlowCountAggregator for flow count correlation analysis
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class TransformationCountAggregator extends AbstractMetricAggregator {

	@Override
	protected Metric<Integer> getMetric() {
		return MuleMetrics.TRANSFORMATIONS;
	}

	@Override
	protected String getDisplayName() {
		return "Mule Transformation Count";
	}
}
