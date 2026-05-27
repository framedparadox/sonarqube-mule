package com.sonar.plugins.mulesoft.metrics.rating;

import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

import com.sonar.plugins.mulesoft.metrics.MuleMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes architectural complexity rating based on flow and sub-flow density.
 *
 * <p>This measure computer evaluates the complexity of Mule applications by analyzing
 * the total count of flows and sub-flows, then assigning an A/B/C rating that reflects
 * the architectural complexity and maintainability risk of the application.</p>
 *
 * <h3>Rating Scale:</h3>
 * <table border="1">
 *   <tr><th>Rating</th><th>Max Cyclomatic Complexity</th><th>Complexity Level</th><th>Maintainability</th></tr>
 *   <tr><td><strong>A (1)</strong></td><td>≤ 5</td><td>Simple</td><td>Easy to maintain</td></tr>
 *   <tr><td><strong>B (2)</strong></td><td>≤ 10</td><td>Low</td><td>Low complexity</td></tr>
 *   <tr><td><strong>C (3)</strong></td><td>≤ 15</td><td>Medium</td><td>Moderate complexity</td></tr>
 *   <tr><td><strong>D (4)</strong></td><td>≤ 20</td><td>High</td><td>High maintenance risk</td></tr>
 *   <tr><td><strong>E (5)</strong></td><td>&gt; 20</td><td>Critical</td><td>Refactoring recommended</td></tr>
 * </table>
 *
 * <h3>Calculation Logic:</h3>
 * <pre>
 * if (maxCyclomaticComplexity ≤ 5)  → Rating A (1)
 * if (maxCyclomaticComplexity ≤ 10) → Rating B (2)
 * if (maxCyclomaticComplexity ≤ 15) → Rating C (3)
 * if (maxCyclomaticComplexity ≤ 20) → Rating D (4)
 * else                               → Rating E (5)
 * </pre>
 *
 * <h3>Inputs and Outputs:</h3>
 * <ul>
 *   <li><strong>Input Metric:</strong>
 *     <ul>
 *       <li>{@link MuleMetrics#FLOW_MAX_COMPLEXITY} - Maximum cyclomatic complexity across all flows</li>
 *     </ul>
 *   </li>
 *   <li><strong>Output Metric:</strong>
 *     <ul>
 *       <li>{@link MuleMetrics#CONFIGURATION_FILES_COMP_RATING} - Complexity rating (1-5)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Quality Management Integration:</h3>
 * <p>This rating integrates with SonarQube's quality management:
 * <ul>
 *   <li><strong>Quality Gates:</strong> Can set thresholds on complexity rating</li>
 *   <li><strong>Technical Debt:</strong> Higher ratings contribute to maintainability debt</li>
 *   <li><strong>Portfolio Analysis:</strong> Enables complexity comparison across projects</li>
 *   <li><strong>Refactoring Planning:</strong> Identifies applications needing architectural review</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Context:</h3>
 * <p>This rating helps development teams and architects:
 * <ul>
 *   <li>Identify applications with excessive architectural complexity</li>
 *   <li>Plan refactoring efforts and modularization strategies</li>
 *   <li>Set complexity targets for new development</li>
 *   <li>Track architectural debt over time</li>
 * </ul>
 * </p>
 *
 * <h3>Threshold Rationale:</h3>
 * <p>Thresholds are based on cognitive complexity research:
 * <ul>
 *   <li><strong>≤7 flows:</strong> Fits in working memory, easy to understand</li>
 *   <li><strong>8-15 flows:</strong> Requires documentation, moderate cognitive load</li>
 *   <li><strong>>15 flows:</strong> High cognitive complexity, refactoring recommended</li>
 * </ul>
 * </p>
 *
 * @see MeasureComputer for SonarQube compute engine interface
 * @see MuleMetrics for input and output metric definitions
 * @see AbstractMetricAggregator for metric aggregation patterns
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class ComplexityRating implements MeasureComputer {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Maps a max cyclomatic complexity value to an A-E rating (1-5).
	 * A(≤5)=1, B(≤10)=2, C(≤15)=3, D(≤20)=4, E(>20)=5
	 */
	public static int rateFor(int maxComplexity) {
		if (maxComplexity <= 5) return 1;  // A
		if (maxComplexity <= 10) return 2; // B
		if (maxComplexity <= 15) return 3; // C
		if (maxComplexity <= 20) return 4; // D
		return 5; // E
	}

	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
		return def.newDefinitionBuilder()
				.setInputMetrics(MuleMetrics.FLOW_MAX_COMPLEXITY.key())
				.setOutputMetrics(MuleMetrics.CONFIGURATION_FILES_COMP_RATING.key()).build();
	}

	@Override
	public void compute(MeasureComputerContext context) {
		logger.info("Computing Complexity Rating");
		Measure maxComplexityMeasure = context.getMeasure(MuleMetrics.FLOW_MAX_COMPLEXITY.key());
		int maxComplexity = 0;
		if (maxComplexityMeasure != null) {
			maxComplexity = maxComplexityMeasure.getIntValue();
		}

		int rating = rateFor(maxComplexity);
		context.addMeasure(MuleMetrics.CONFIGURATION_FILES_COMP_RATING.key(), rating);
	}
}
