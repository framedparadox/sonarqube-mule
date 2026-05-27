package com.sonar.plugins.mulesoft.metrics.aggregation;

import org.sonar.api.measures.Metric;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;

/**
 * Concrete aggregator for rolling up flow counts across the project hierarchy.
 *
 * <p>This aggregator implements the {@link AbstractMetricAggregator} template to provide
 * hierarchical summation of {@link MuleMetrics#FLOWS} from individual Mule configuration
 * files up to the project level. Flow counts are essential metrics for understanding
 * application complexity and architectural patterns.</p>
 *
 * <h3>Metric Aggregation Flow:</h3>
 * <pre>
 * File Level:      config1.xml (3 flows) + config2.xml (2 flows) = 5 flows
 *                          ↓
 * Directory Level: src/main/mule/ = 5 flows  
 *                          ↓
 * Module Level:    my-mule-app = 5 flows
 *                          ↓
 * Project Level:   entire-project = 5 flows
 * </pre>
 *
 * <h3>Flow Definition:</h3>
 * <p>Counts {@code <flow>} elements in Mule configuration files, which represent:
 * <ul>
 *   <li><strong>Main Processing Units:</strong> Primary message processing pipelines</li>
 *   <li><strong>API Endpoints:</strong> HTTP listeners and other inbound endpoints</li>
 *   <li><strong>Scheduled Processes:</strong> Timer-driven and cron-triggered flows</li>
 *   <li><strong>Event Handlers:</strong> JMS listeners and other event-driven flows</li>
 * </ul>
 * </p>
 *
 * <h3>Quality Insights:</h3>
 * <p>Aggregated flow counts provide insights into:
 * <ul>
 *   <li><strong>Application Size:</strong> Overall complexity and feature scope</li>
 *   <li><strong>Architectural Patterns:</strong> API-heavy vs batch-processing vs event-driven</li>
 *   <li><strong>Maintenance Burden:</strong> Higher flow counts typically mean more maintenance</li>
 *   <li><strong>Team Productivity:</strong> Flow count trends over time indicate development velocity</li>
 * </ul>
 * </p>
 *
 * <h3>Integration with Other Metrics:</h3>
 * <p>Flow counts work with related metrics:
 * <ul>
 *   <li><strong>Sub-flows:</strong> {@link SubFlowCountAggregator} for reusable logic</li>
 *   <li><strong>Transformations:</strong> {@link TransformationCountAggregator} for data processing complexity</li>
 *   <li><strong>Complexity Rating:</strong> {@link ComplexityRating} uses flow counts for architectural assessment</li>
 * </ul>
 * </p>
 *
 * @see AbstractMetricAggregator for aggregation template implementation
 * @see MuleMetrics#FLOWS for the metric being aggregated
 * @see SubFlowCountAggregator for sub-flow aggregation
 * @see ComplexityRating for flow-based complexity calculation
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class FlowCountAggregator extends AbstractMetricAggregator {

	@Override
	protected Metric<Integer> getMetric() {
		return MuleMetrics.FLOWS;
	}

	@Override
	protected String getDisplayName() {
		return "Mule Flow Count";
	}

}
