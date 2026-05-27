package com.sonar.plugins.mulesoft.rule.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model representing a Mule validation rule with XPath-based logic.
 * 
 * <p>This class serves as a modern POJO replacement for the legacy JAXB-generated Rule class,
 * providing a cleaner, more maintainable approach to rule definition without XML binding overhead.</p>
 * 
 * <h3>Rule Structure:</h3>
 * <ul>
 *   <li><strong>Identification:</strong> Unique key and human-readable name</li>
 *   <li><strong>Classification:</strong> Severity (MINOR/MAJOR/CRITICAL), type (CODE_SMELL/BUG/VULNERABILITY), category</li>
 *   <li><strong>Scope:</strong> FILE (per-file validation) or APPLICATION (cross-file validation)</li>
 *   <li><strong>Logic:</strong> XPath expression for rule evaluation</li>
 *   <li><strong>Location:</strong> Optional XPath hint for precise issue location</li>
 *   <li><strong>Metadata:</strong> Tags, examples, remediation effort estimation</li>
 * </ul>
 * 
 * <h3>Rule Scope Behavior:</h3>
 * <ul>
 *   <li><strong>FILE_SCOPE:</strong> Rule evaluated independently on each file. Issues reported per-file.</li>
 *   <li><strong>APPLICATION_SCOPE:</strong> Rule evaluated across all files. Issue reported only if ALL files fail.</li>
 * </ul>
 * 
 * <h3>Supported Formats:</h3>
 * <p>Rules can be loaded from YAML (preferred) or XML (legacy) files:
 * <pre>
 * # YAML Example
 * - key: no-hardcoded-passwords
 *   name: "Avoid hardcoded passwords"
 *   description: "Passwords should be externalized"
 *   severity: CRITICAL
 *   type: VULNERABILITY
 *   scope: file
 *   xpath: "//set-payload[contains(@value, 'password=')]"</pre>
 * 
 * @see com.sonar.plugins.mulesoft.rule.loader.RuleLoader
 * @see com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor
 * @since 1.0.0
 */
public class MuleRule {

	/** Unique identifier for the rule (used for rule activation/deactivation) */
	private String key;
	
	/** Human-readable rule name displayed in SonarQube UI */
	private String name;
	
	/** Detailed description explaining what the rule checks */
	private String description;
	
	/** Rule severity: MINOR, MAJOR, CRITICAL, BLOCKER, or INFO */
	private String severity;
	
	/** Rule type: CODE_SMELL, BUG, VULNERABILITY, or SECURITY_HOTSPOT */
	private String type;
	
	/** Logical grouping/category for the rule (e.g., "security", "performance") */
	private String category;
	
	/** Evaluation scope: "file" (default) or "application" */
	private String scope;
	
	/** XPath expression that defines the rule logic */
	private String xpath;
	
	/** Optional XPath expression for precise issue location highlighting */
	private String xpathLocationHint;
	
	/** Tags for rule categorization and filtering */
	private List<String> tags = new ArrayList<>();
	
	/** Rich HTML description for detailed rule documentation */
	private String htmlDescription;
	
	/** Code examples demonstrating compliant and non-compliant code */
	private List<MuleRuleExample> examples = new ArrayList<>();
	
	/** Estimated time to fix issues found by this rule (in minutes) */
	private String remediationEffort;

	/** Configurable parameters for this rule (e.g., thresholds) */
	private List<MuleRuleParameter> parameters = new ArrayList<>();

	/** Capabilities required by this rule (e.g., "project-model") */
	private java.util.Set<String> requires = new java.util.HashSet<>();

	/** Mule versions or runtimes this rule applies to */
	private List<String> appliesTo = new ArrayList<>();

	/** Version since which this rule is available */
	private String since;

	/** Rule status: ready, deprecated, beta */
	private String status = "ready";

	/**
	 * Creates a new empty MuleRule.
	 * All properties must be set using setter methods.
	 */
	public MuleRule() {
	}

/**
	 * Gets the unique rule identifier.
	 * 
	 * @return Rule key (e.g., "no-hardcoded-passwords")
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the unique rule identifier.
	 * 
	 * @param key Rule key used for activation/deactivation
	 */
	public void setKey(String key) {
		this.key = key;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

/**
	 * Gets the XPath expression that defines this rule's validation logic.
	 * 
	 * @return XPath expression (e.g., "//flow[@name='hardcoded']")
	 */
	public String getXpath() {
		return xpath;
	}

	/**
	 * Sets the XPath expression for rule validation.
	 * 
	 * @param xpath XPath expression that matches violating elements
	 */
	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

    public String getXpathLocationHint() {
        return xpathLocationHint;
    }

    public void setXpathLocationHint(String xpathLocationHint) {
        this.xpathLocationHint = xpathLocationHint;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getHtmlDescription() {
        return htmlDescription;
    }

    public void setHtmlDescription(String htmlDescription) {
        this.htmlDescription = htmlDescription;
    }

    public List<MuleRuleExample> getExamples() {
        return examples;
    }

    public void setExamples(List<MuleRuleExample> examples) {
        this.examples = examples;
    }

    public String getRemediationEffort() {
        return remediationEffort;
    }

    public void setRemediationEffort(String remediationEffort) {
        this.remediationEffort = remediationEffort;
    }

    public List<MuleRuleParameter> getParameters() { return parameters; }
    public void setParameters(List<MuleRuleParameter> parameters) { this.parameters = parameters; }
    public java.util.Set<String> getRequires() { return requires; }
    public void setRequires(java.util.Set<String> requires) { this.requires = requires; }
    public List<String> getAppliesTo() { return appliesTo; }
    public void setAppliesTo(List<String> appliesTo) { this.appliesTo = appliesTo; }
    public String getSince() { return since; }
    public void setSince(String since) { this.since = since; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
	 * Determines if this rule should be evaluated at application scope.
	 * 
	 * <p>Application-scope rules are evaluated across all files in the project.
	 * An issue is reported only if ALL files fail the rule condition.
	 * This is useful for rules that check for the presence of something across the entire application.</p>
	 * 
	 * @return true if scope is "application", false otherwise
	 */
	public boolean isApplicationScope() {
		return "application".equalsIgnoreCase(scope);
	}

	/**
	 * Determines if this rule should be evaluated at file scope.
	 * 
	 * <p>File-scope rules (the default) are evaluated independently on each file.
	 * Issues are reported on each file that violates the rule condition.
	 * This is the most common rule type.</p>
	 * 
	 * @return true if scope is null or "file", false otherwise
	 */
	public boolean isFileScope() {
		return scope == null || "file".equalsIgnoreCase(scope);
	}
/**
	 * Returns a string representation of this rule for debugging and logging.
	 * 
	 * @return Formatted string with key rule properties
	 */
	@Override
	public String toString() {
		return "MuleRule{" +
				"key='" + key + '\'' +
				", name='" + name + '\'' +
				", severity='" + severity + '\'' +
				", type='" + type + '\'' +
				", category='" + category + '\'' +
				", scope='" + scope + '\'' +
				'}';
	}
}
