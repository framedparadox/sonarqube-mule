package com.sonar.plugins.mulesoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.metrics.aggregation.FlowCountAggregator;
import com.sonar.plugins.mulesoft.metrics.rating.ComplexityRating;
import com.sonar.plugins.mulesoft.metrics.aggregation.SubFlowCountAggregator;
import com.sonar.plugins.mulesoft.metrics.aggregation.TransformationCountAggregator;
import com.sonar.plugins.mulesoft.sensors.metrics.ConfigurationMetricsSensor;
import com.sonar.plugins.mulesoft.sensors.metrics.CoverageImportSensor;
import com.sonar.plugins.mulesoft.sensors.metrics.MUnitMetricsSensor;
import com.sonar.plugins.mulesoft.metrics.MuleMetrics;
import com.sonar.plugins.mulesoft.config.MulePluginProperties;
import com.sonar.plugins.mulesoft.profile.MuleRecommendedProfile;
import com.sonar.plugins.mulesoft.profile.MuleSanityProfile;
import com.sonar.plugins.mulesoft.profile.MuleStrictProfile;
import com.sonar.plugins.mulesoft.rule.DataWeaveRulesDefinition;
import com.sonar.plugins.mulesoft.rule.MuleRulesDefinition;
import com.sonar.plugins.mulesoft.sensor.CpdTokensSensor;
import com.sonar.plugins.mulesoft.sensor.DataWeaveSensor;
import com.sonar.plugins.mulesoft.sensor.MuleStructuralMetricsSensor;
import com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor;
import com.sonar.plugins.mulesoft.xml.SecureJaxp;

/**
 * Main entry point for the MuleSoft SonarQube plugin.
 * 
 * <p>This plugin provides comprehensive static analysis capabilities for Mule 4 applications:
 * 
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Language Support:</strong> Defines Mule as a language for SonarQube with configurable file extensions</li>
 *   <li><strong>Rule-based Validation:</strong> XPath-based rules for detecting anti-patterns and best practices</li>
 *   <li><strong>Metrics Collection:</strong> Structural metrics (flows, subflows, transformations) and complexity ratings</li>
 *   <li><strong>Test Analysis:</strong> MUnit test case counting and coverage import</li>
 *   <li><strong>Quality Profiles:</strong> Default quality profile with all rules activated</li>
 * </ul>
 * 
 * <h3>Architecture:</h3>
 * <ul>
 *   <li><strong>Sensors:</strong> Extract metrics and execute validation rules on Mule XML files</li>
 *   <li><strong>Aggregators:</strong> Roll up file-level metrics to project level</li>
 *   <li><strong>Rules:</strong> Loaded from external YAML/XML files in extensions/plugins/</li>
 *   <li><strong>Metrics:</strong> Custom metrics for flows, subflows, transformations, and complexity</li>
 * </ul>
 * 
 * <h3>Extension Points:</h3>
 * <p>Rules can be customized by placing {@code mulesoft-ruleset.yaml} or {@code mulesoft-ruleset.xml}
 * in the SonarQube {@code extensions/plugins/} directory.</p>
 * 
 * @see com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor
 * @see com.sonar.plugins.mulesoft.rule.loader.RuleLoader
 * @since 1.0.0
 */
public class MulePlugin implements Plugin {

	/** Logger for plugin lifecycle and configuration messages */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/** Configuration category for Mule-specific properties */
	private static final String GENERAL = "General";

	/**
	 * Defines and registers all plugin extensions with SonarQube.
	 * 
	 * <p>This method is called during SonarQube startup to register:
	 * <ul>
	 *   <li>Language definition and validation sensor</li>
	 *   <li>Rule repository and quality profile</li>
	 *   <li>Custom metrics and metric aggregators</li>
	 *   <li>Coverage import and test metric sensors</li>
	 *   <li>Configuration properties</li>
	 * </ul>
	 * 
	 * <p>All extensions are automatically discovered and integrated by SonarQube.</p>
	 * 
	 * @param context SonarQube plugin context for extension registration
	 */
	@Override
	public void define(Context context) {
		logger.debug("Configuring Mule Plugin");

		// Harden XML processing against XXE/SSRF
		SecureJaxp.harden();

		// Language
		context.addExtension(MuleLanguage.class);

		// Sensors
		context.addExtensions(
				MuleValidationSensor.class,
				DataWeaveSensor.class,
				MuleStructuralMetricsSensor.class,
				MUnitMetricsSensor.class,
				CoverageImportSensor.class,
				CpdTokensSensor.class
		);

		// Rule repositories
		context.addExtension(MuleRulesDefinition.class);
		context.addExtension(DataWeaveRulesDefinition.class);

		// Quality profiles (three tiers)
		context.addExtension(MuleSanityProfile.class);
		context.addExtension(MuleRecommendedProfile.class);
		context.addExtension(MuleStrictProfile.class);

		// Metrics and aggregators
		context.addExtensions(
				MuleMetrics.class,
				FlowCountAggregator.class,
				SubFlowCountAggregator.class,
				TransformationCountAggregator.class,
				ComplexityRating.class
		);

		// Properties (consolidated via MulePluginProperties)
		for (PropertyDefinition prop : MulePluginProperties.all()) {
			context.addExtension(prop);
		}
	}
}
