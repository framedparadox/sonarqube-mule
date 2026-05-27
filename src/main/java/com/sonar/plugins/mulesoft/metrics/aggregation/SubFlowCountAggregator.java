package com.sonar.plugins.mulesoft.metrics.aggregation;

import org.sonar.api.measures.Metric;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;

/**
 * Concrete aggregator for rolling up sub-flow counts across the project hierarchy.
 *
 * <p>This aggregator extends {@link AbstractMetricAggregator} to provide hierarchical
 * summation of {@link MuleMetrics#SUBFLOWS} from individual Mule configuration files.
 * Sub-flows represent reusable logic blocks that promote code modularity and
 * maintainability in MuleSoft applications.</p>
 *
 * <h3>Sub-Flow Definition:</h3>
 * <p>Counts {@code <sub-flow>} elements, which represent:
 * <ul>
 *   <li><strong>Reusable Logic:</strong> Common processing patterns shared across flows</li>
 *   <li><strong>Error Handling:</strong> Centralized error processing routines</li>
 *   <li><strong>Data Transformation:</strong> Standardized data mapping and validation</li>
 *   <li><strong>Integration Patterns:</strong> Common connector configurations and retry logic</li>
 * </ul>
 * </p>
 *
 * <h3>Code Quality Indicators:</h3>
 * <p>Sub-flow counts provide insights into:
 * <ul>
 *   <li><strong>Code Reusability:</strong> Higher sub-flow ratios indicate better modularity</li>
 *   <li><strong>Maintainability:</strong> Well-factored applications leverage sub-flows effectively</li>
 *   <li><strong>DRY Principle:</strong> Sub-flows help eliminate code duplication</li>
 *   <li><strong>Testing Strategy:</strong> Sub-flows enable focused unit testing of components</li>
 * </ul>
 * </p>
 *
 * <h3>Architectural Analysis:</h3>
 * <p>Sub-flow metrics enable architectural assessment:
 * <pre>
 * Sub-flow Ratio = Sub-flows / (Flows + Sub-flows)
 * 
 * High Ratio (>30%):  Good modular design
 * Medium Ratio (15-30%): Acceptable structure  
 * Low Ratio (<15%):   Potential code duplication
 * </pre>
 * </p>
 *
 * <h3>Relationship with Flow Complexity:</h3>
 * <p>Sub-flows work together with flows in complexity calculations:
 * <ul>
 *   <li><strong>Total Processing Units:</strong> Flows + Sub-flows = Overall application complexity</li>
 *   <li><strong>Complexity Distribution:</strong> Balanced flow/sub-flow ratios indicate good architecture</li>
 *   <li><strong>Refactoring Opportunities:</strong> Low sub-flow counts may indicate refactoring needs</li>
 * </ul>
 * </p>
 *
 * <h3>Usage in Quality Gates:</h3>
 * <p>Sub-flow metrics can drive quality decisions:
 * <ul>
 *   <li><strong>Minimum Thresholds:</strong> Ensure adequate code modularity</li>
 *   <li><strong>Ratio Targets:</strong> Maintain healthy flow-to-sub-flow ratios</li>
 *   <li><strong>Trend Analysis:</strong> Track modularity improvements over time</li>
 * </ul>
 * </p>
 *
 * @see AbstractMetricAggregator for aggregation template implementation  
 * @see MuleMetrics#SUBFLOWS for the metric being aggregated
 * @see FlowCountAggregator for main flow aggregation
 * @see ComplexityRating for combined flow/sub-flow complexity assessment
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class SubFlowCountAggregator extends AbstractMetricAggregator {

	@Override
	protected Metric<Integer> getMetric() {
		return MuleMetrics.SUBFLOWS;
	}

	@Override
	protected String getDisplayName() {
		return "Mule SubFlow Count";
	}
}
