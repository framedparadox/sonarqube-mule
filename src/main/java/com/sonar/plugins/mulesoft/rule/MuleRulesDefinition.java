package com.sonar.plugins.mulesoft.rule;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.config.PropertyLoader;
import com.sonar.plugins.mulesoft.rule.loader.RuleLoader;
import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleParameter;

/**
 * Defines and registers Mule validation rules with the SonarQube platform.
 *
 * <p>This class implements SonarQube's {@link RulesDefinition} interface to create a
 * custom rule repository for MuleSoft applications. It supports loading rules from
 * multiple formats (YAML preferred, XML fallback) and provides template-based rule
 * extensibility.</p>
 *
 * <h3>Rule Repository Configuration:</h3>
 * <ul>
 *   <li><strong>Repository Key:</strong> {@code "mule"} - identifies the rule set</li>
 *   <li><strong>Repository Name:</strong> "Mule Validation Rules"</li>
 *   <li><strong>Language:</strong> {@link MuleLanguage#LANGUAGE_KEY}</li>
 *   <li><strong>Rule Loading:</strong> External YAML first, external XML second, bundled YAML/XML fallback via {@link RuleLoader}</li>
 * </ul>
 *
 * <h3>Rule Format Support:</h3>
 * <p>Supports multiple rule definition formats:
 * <pre>
 * # YAML format (preferred) - mulesoft-ruleset.yaml
 * rules:
 *   - key: "avoid-dataweave-in-choice"
 *     name: "Avoid DataWeave in Choice Router"
 *     description: "DataWeave transformations should not be used directly in choice conditions"
 *     xpath: "//mule:choice//dw:transform"
 *     scope: "FILE"
 *     severity: "MAJOR"
 *     type: "CODE_SMELL"
 * 
 * # XML format (fallback) - mulesoft-ruleset.xml  
 * &lt;rules&gt;
 *   &lt;rule&gt;
 *     &lt;key&gt;avoid-dataweave-in-choice&lt;/key&gt;
 *     &lt;name&gt;Avoid DataWeave in Choice Router&lt;/name&gt;
 *     &lt;xpath&gt;//mule:choice//dw:transform&lt;/xpath&gt;
 *     &lt;scope&gt;FILE&lt;/scope&gt;
 *   &lt;/rule&gt;
 * &lt;/rules&gt;
 * </pre>
 * </p>
 *
 * <h3>Rule Template Support:</h3>
 * <p>Includes a rule template ({@code RULE_TEMPLATE_KEY}) that allows users to:
 * <ul>
 *   <li>Create custom rules through SonarQube UI</li>
 *   <li>Define custom XPath expressions</li>
 *   <li>Configure rule scope (FILE vs APPLICATION)</li>
 *   <li>Set custom severity levels</li>
 * </ul>
 * </p>
 *
 * <h3>Rule Registration Process:</h3>
 * <ol>
 *   <li>Load rules from YAML/XML using {@link RuleLoader}</li>
 *   <li>Create SonarQube rule definitions with metadata</li>
 *   <li>Configure rule parameters (XPath, scope, message)</li>
 *   <li>Set default severity and type classifications</li>
 *   <li>Register template rule for custom rule creation</li>
 * </ol>
 *
 * <h3>Integration with Quality Profiles:</h3>
 * <p>Rules defined here are available for activation in {@link MuleQualityProfile}
 * and can be customized per project through SonarQube's rule management interface.</p>
 *
 * @see RulesDefinition for SonarQube rule definition interface
 * @see RuleLoader for rule loading from external overrides and bundled defaults
 * @see MuleRule for rule data model
 * @see MuleQualityProfile for default rule activation
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class MuleRulesDefinition implements RulesDefinition {

	private static final Logger LOG = LoggerFactory.getLogger(MuleRulesDefinition.class);

	public static final String MULE_REPOSITORY_KEY = "mule";
	public static final String RULE_TEMPLATE_KEY = "mule-rule-template";

	/**
	 * Parameter constants for rule configuration.
	 */
	public static final class PARAMS {
		public static final String CATEGORY = "category";
		public static final String SCOPE = "scope";
		public static final String XPATH = "xpath-expression";
		public static final String XPATH_LOCATION_HINT = "xpath-location-hint";
		public static final String RULE_NAME = "rule-name";
		public static final String PLUGIN_VERSION = "plugin-version";

		private PARAMS() {
			// Prevent instantiation
		}
	}

	private final RuleLoader ruleLoader;

	public MuleRulesDefinition() {
		this(new RuleLoader());
	}

	// Constructor for dependency injection (testability)
	public MuleRulesDefinition(RuleLoader ruleLoader) {
		this.ruleLoader = ruleLoader;
	}

	@Override
	public void define(Context context) {
		LOG.debug("Defining Mule rules from external overrides or bundled defaults");

		NewRepository repository = context.createRepository(MULE_REPOSITORY_KEY, MuleLanguage.LANGUAGE_KEY)
				.setName("Mulesoft");

		// Load rules using external-first strategy with bundled fallback
		List<MuleRule> rules = ruleLoader.loadRules();

		if (rules.isEmpty()) {
			LOG.warn("No Mule validation rules loaded. The repository will be empty.");
			LOG.warn("Provide a mulesoft-ruleset.yaml or mulesoft-ruleset.xml file in extensions/plugins/, or ensure bundled default rules are present in the plugin JAR.");
		} else {
			LOG.info("Loaded {} Mule validation rules", rules.size());

			// Register each rule
			for (MuleRule rule : rules) {
				addRule(repository, rule);
			}
		}

		// Always add rule template for user-defined rules
		addRuleTemplate(repository);

		// Register Java-based complexity check rules
		addComplexityCheckRule(repository, "flow-cyclomatic-complexity",
				"Flow Cyclomatic Complexity",
				"Cyclomatic complexity of a flow must not exceed the configured threshold.",
				Severity.MAJOR, RuleType.CODE_SMELL, "10");
		addComplexityCheckRule(repository, "flow-cognitive-complexity",
				"Flow Cognitive Complexity",
				"Cognitive complexity of a flow must not exceed the configured threshold.",
				Severity.MAJOR, RuleType.CODE_SMELL, "15");
		addComplexityCheckRule(repository, "flow-nesting-depth",
				"Flow Maximum Nesting Depth",
				"Maximum nesting depth of a flow must not exceed the configured threshold.",
				Severity.MAJOR, RuleType.CODE_SMELL, "3");

		// Finalize repository
		repository.done();
	}

	/**
	 * Adds a single Mule rule to the repository.
	 */
	private void addRule(NewRepository repository, MuleRule rule) {
		try {
			// Skip rules requiring capabilities not available in Stage A
			if (rule.getRequires() != null && rule.getRequires().contains("project-model")) {
				LOG.debug("Skipping rule {} — requires project-model (Phase 2)", rule.getKey());
				return;
			}

			// Validate required fields
			if (rule.getName() == null || rule.getName().trim().isEmpty()) {
				LOG.warn("Skipping rule {} - missing name", rule.getKey());
				return;
			}
			
			NewRule newRule = repository.createRule(rule.getKey())
					.setName(rule.getName())
					.setHtmlDescription(formatHtmlDescription(rule))
					.setActivatedByDefault(true)
					.setStatus(RuleStatus.READY)
					.setSeverity(mapSeverity(rule.getSeverity()))
					.setType(mapRuleType(rule.getType()));

			// Add tags
			newRule.addTags(MuleLanguage.LANGUAGE_KEY);
			if (rule.getTags() != null && !rule.getTags().isEmpty()) {
				for (String tag : rule.getTags()) {
					newRule.addTags(tag);
				}
			}

			// Set remediation effort if provided
			if (rule.getRemediationEffort() != null && !rule.getRemediationEffort().trim().isEmpty()) {
				String effort = rule.getRemediationEffort();
				// Add "min" suffix if not already present
				if (!effort.endsWith("min") && !effort.endsWith("h")) {
					effort = effort + "min";
				}
				newRule.setDebtRemediationFunction(
					newRule.debtRemediationFunctions().constantPerIssue(effort)
				);
			}

			// Add parameters
			if (rule.getCategory() != null) {
				newRule.createParam(PARAMS.CATEGORY)
						.setDefaultValue(rule.getCategory())
						.setType(RuleParamType.STRING);
			}

			if (rule.getXpath() != null) {
				newRule.createParam(PARAMS.XPATH)
						.setDefaultValue(rule.getXpath())
						.setType(RuleParamType.STRING);
			}

			if (rule.getXpathLocationHint() != null) {
				newRule.createParam(PARAMS.XPATH_LOCATION_HINT)
						.setDefaultValue(rule.getXpathLocationHint())
						.setType(RuleParamType.STRING);
			}

			if (rule.getName() != null) {
				newRule.createParam(PARAMS.RULE_NAME)
						.setDefaultValue(rule.getName())
						.setType(RuleParamType.STRING);
			}

			newRule.createParam(PARAMS.SCOPE)
					.setDefaultValue(rule.getScope() != null ? rule.getScope() : "file")
					.setType(RuleParamType.STRING);

			// Register user-defined parameters (e.g., thresholds)
			if (rule.getParameters() != null) {
				for (MuleRuleParameter p : rule.getParameters()) {
					RulesDefinition.NewParam newParam = newRule.createParam(p.getKey());
					if (p.getName() != null) newParam.setName(p.getName());
					if (p.getDescription() != null) newParam.setDescription(p.getDescription());
					if (p.getDefaultValue() != null) newParam.setDefaultValue(p.getDefaultValue());
					newParam.setType(mapParamType(p.getType(), rule.getKey()));
				}
			}

			LOG.debug("Registered rule: {} - {}", rule.getKey(), rule.getName());
		} catch (Exception e) {
			LOG.warn("Failed to register rule {}: {}", rule.getKey(), e.getMessage());
		}
	}

	/**
	 * Registers a Java-based complexity check rule with a configurable threshold parameter.
	 */
	private void addComplexityCheckRule(NewRepository repository, String key, String name,
			String description, String severity, RuleType type, String defaultThreshold) {
		try {
			NewRule rule = repository.createRule(key)
					.setName(name)
					.setHtmlDescription("<p>" + description + "</p>")
					.setActivatedByDefault(true)
					.setStatus(RuleStatus.READY)
					.setSeverity(severity)
					.setType(type);
			rule.addTags(MuleLanguage.LANGUAGE_KEY, "complexity");
			rule.createParam("threshold")
					.setDescription("Maximum allowed value (inclusive). Flows exceeding this trigger an issue.")
					.setType(RuleParamType.INTEGER)
					.setDefaultValue(defaultThreshold);
		} catch (Exception e) {
			LOG.warn("Failed to register complexity rule {}: {}", key, e.getMessage());
		}
	}

	/**
	 * Adds the rule template that allows users to define custom XPath rules.
	 */
	private void addRuleTemplate(NewRepository repository) {
		Properties prop = PropertyLoader.getProperties(MuleLanguage.LANGUAGE_MULE4_KEY);

		NewRule template = repository.createRule(RULE_TEMPLATE_KEY)
				.setName(prop.getProperty("rule.template.name"))
				.setHtmlDescription(prop.getProperty("rule.template.description"))
				.setTemplate(true);

		template.addTags(MuleLanguage.LANGUAGE_KEY);

		template.createParam(PARAMS.CATEGORY)
				.setDescription(prop.getProperty("rule.template.parameter.category"))
				.setType(RuleParamType.STRING);

		template.createParam(PARAMS.XPATH)
				.setDescription(prop.getProperty("rule.template.parameter.xpath"))
				.setType(RuleParamType.STRING);

		template.createParam(PARAMS.XPATH_LOCATION_HINT)
				.setDescription(prop.getProperty("rule.template.parameter.xpathlocationhint"))
				.setType(RuleParamType.STRING);

		template.createParam(PARAMS.SCOPE)
				.setDescription(prop.getProperty("rule.template.parameter.scope"))
				.setType(RuleParamType.STRING);

		template.createParam(PARAMS.PLUGIN_VERSION)
				.setDescription("Plugin version that introduced this rule (e.g. 1.0 or 1.1). Controls which scope strategies are available.")
				.setType(RuleParamType.STRING)
				.setDefaultValue("1.0");

		LOG.info("Added rule template for user-defined XPath rules");
	}

	/**
	 * Formats the HTML description for a rule, optionally with examples.
	 */
	private String formatHtmlDescription(MuleRule rule) {
		StringBuilder html = new StringBuilder();

		// Main description - don't wrap in <p> tags if already HTML-like
		String description = rule.getDescription();
		if (description != null) {
			if (description.trim().startsWith("<") || rule.getHtmlDescription() != null) {
				// Already HTML or has custom HTML, use as-is
				html.append(description);
			} else {
				// Plain text, just use as-is for simple cases
				html.append(description);
			}
		}

		// Add custom HTML if provided
		if (rule.getHtmlDescription() != null) {
			html.append(rule.getHtmlDescription());
		}
		
		// Add examples if present
		if (rule.getExamples() != null && !rule.getExamples().isEmpty()) {
			for (var example : rule.getExamples()) {
				if ("compliant".equalsIgnoreCase(example.getType())) {
					html.append("<h3>Compliant Solution</h3>");
					html.append("<pre>").append(example.getCode()).append("</pre>");
				} else if ("noncompliant".equalsIgnoreCase(example.getType())) {
					html.append("<h3>Noncompliant Code Example</h3>");
					html.append("<pre>").append(example.getCode()).append("</pre>");
				}
			}
		}

		return html.toString();
	}

	/**
	 * Maps rule severity string to SonarQube Severity constant.
	 */
	private String mapSeverity(String severity) {
		if (severity == null) {
			return Severity.MAJOR;
		}

		switch (severity.toUpperCase()) {
			case "BLOCKER":
				return Severity.BLOCKER;
			case "CRITICAL":
				return Severity.CRITICAL;
			case "MAJOR":
				return Severity.MAJOR;
			case "MINOR":
				return Severity.MINOR;
			case "INFO":
				return Severity.INFO;
			default:
				return Severity.MAJOR;
		}
	}

	/**
	 * Maps rule type string to SonarQube RuleType enum.
	 */
	private RuleType mapRuleType(String type) {
		if (type == null) {
			return RuleType.CODE_SMELL;
		}

		switch (type.toUpperCase()) {
			case "BUG":
				return RuleType.BUG;
			case "VULNERABILITY":
			case "SECURITY":
				return RuleType.VULNERABILITY;
			case "CODE_SMELL":
			case "CODESMELL":
				return RuleType.CODE_SMELL;
			case "SECURITY_HOTSPOT":
				return RuleType.SECURITY_HOTSPOT;
			default:
				return RuleType.CODE_SMELL;
		}
	}

	/**
	 * Maps a parameter type string to a SonarQube RuleParamType constant.
	 */
	private RuleParamType mapParamType(String type, String ruleKey) {
		if (type == null) return RuleParamType.STRING;
		switch (type.toUpperCase()) {
			case "INTEGER": return RuleParamType.INTEGER;
			case "FLOAT":   return RuleParamType.FLOAT;
			case "BOOLEAN": return RuleParamType.BOOLEAN;
			case "TEXT":    return RuleParamType.TEXT;
			case "STRING":  return RuleParamType.STRING;
			default:
				LOG.warn("Unknown param type {} for rule {}, defaulting to STRING", type, ruleKey);
				return RuleParamType.STRING;
		}
	}

	/**
	 * Generates a full rule key from category and rule key.
	 * Kept for backward compatibility.
	 */
	public static String getRuleKey(String category, String ruleKey) {
		return category + "." + ruleKey;
	}
}
