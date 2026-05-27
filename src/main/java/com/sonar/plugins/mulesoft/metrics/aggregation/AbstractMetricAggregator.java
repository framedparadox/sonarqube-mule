package com.sonar.plugins.mulesoft.metrics.aggregation;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for SonarQube metric aggregation across project hierarchy levels.
 *
 * <p>This class implements the Template Method pattern to provide common aggregation logic
 * for rolling up file-level metrics to higher organizational levels (directory, module,
 * project). It handles the standard SonarQube {@link MeasureComputer} lifecycle while
 * allowing subclasses to focus on metric-specific configuration.</p>
 *
 * <h3>Aggregation Strategy:</h3>
 * <p>Performs hierarchical metric rollup using SonarQube's standard aggregation:
 * <ul>
 *   <li><strong>File Level → Directory Level:</strong> Sum all metrics from files in directory</li>
 *   <li><strong>Directory Level → Module Level:</strong> Sum all metrics from subdirectories</li>
 *   <li><strong>Module Level → Project Level:</strong> Sum all metrics from modules</li>
 * </ul>
 * </p>
 *
 * <h3>Template Method Pattern:</h3>
 * <p>Concrete subclasses must implement:
 * <ul>
 *   <li>{@link #getMetric()} - Specifies which metric to aggregate</li>
 *   <li>{@link #getDisplayName()} - Provides logging/debugging identification</li>
 * </ul>
 * While this class handles:
 * <ul>
 *   <li>MeasureComputer lifecycle management</li>
 *   <li>Child component traversal and summation</li>
 *   <li>Error handling and logging</li>
 *   <li>Measure output creation</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Concrete implementation for flow aggregation
 * public class FlowCountAggregator extends AbstractMetricAggregator {
 *     {@code @Override}
 *     protected Metric&lt;Integer&gt; getMetric() {
 *         return MuleMetrics.FLOWS;
 *     }
 *     
 *     {@code @Override}
 *     protected String getDisplayName() {
 *         return "Flow Count Aggregator";
 *     }
 * }
 * </pre>
 *
 * <h3>SonarQube Integration:</h3>
 * <p>Aggregators are automatically executed during SonarQube's Compute Engine phase:
 * <ol>
 *   <li>Sensors collect file-level metrics during analysis</li>
 *   <li>Compute Engine runs aggregators to roll up metrics</li>
 *   <li>Aggregated metrics appear in project dashboard and reports</li>
 * </ol>
 * </p>
 *
 * <h3>Error Handling:</h3>
 * <p>Gracefully handles missing or null measures, treating them as zero values
 * for aggregation purposes. Logs aggregation progress for debugging.</p>
 *
 * @see MeasureComputer for SonarQube compute engine interface
 * @see MuleMetrics for available metric definitions
 * @see FlowCountAggregator for concrete implementation example
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public abstract class AbstractMetricAggregator implements MeasureComputer {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Gets the metric to aggregate.
	 * 
	 * @return The metric to aggregate
	 */
	protected abstract Metric<Integer> getMetric();

	/**
	 * Gets the display name for logging purposes.
	 * 
	 * @return A human-readable name for this aggregator
	 */
	protected abstract String getDisplayName();

	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
		return defContext.newDefinitionBuilder()
				.setOutputMetrics(getMetric().key())
				.build();
	}

	@Override
	public void compute(MeasureComputerContext context) {
		logger.info("Computing {}", getDisplayName());

		// Only aggregate for non-file components (directories, modules, projects)
		if (context.getComponent().getType() != Component.Type.FILE) {
			int sum = 0;
			String metricKey = getMetric().key();
			
			for (Measure child : context.getChildrenMeasures(metricKey)) {
				sum += child.getIntValue();
			}
			
			context.addMeasure(metricKey, sum);
		}
	}
}
