package com.sonar.plugins.mulesoft.util;

/**
 * Defines the evaluation scope for validation rules in the MuleSoft SonarQube plugin.
 *
 * <p>Rule scope determines how validation rules are applied across the codebase:
 * either on individual files or across the entire application. This affects both
 * rule evaluation logic and issue reporting behavior.</p>
 *
 * <h3>Scope Behaviors:</h3>
 * <ul>
 *   <li><strong>FILE_SCOPE:</strong> Rules are evaluated independently on each file.
 *       Issues are reported per file that violates the rule.</li>
 *   <li><strong>APPLICATION_SCOPE:</strong> Rules are evaluated across all project files.
 *       Issues are reported only if ALL files fail the rule.</li>
 * </ul>
 *
 * <h3>Application-Scope Logic:</h3>
 * <p>For APPLICATION_SCOPE rules, the evaluation follows this pattern:
 * <ul>
 *   <li>If <strong>any</strong> file in the project satisfies the rule → No issue reported</li>
 *   <li>If <strong>all</strong> files in the project fail the rule → Issue reported</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // File-scope rule - checks each individual Mule config file
 * - key: "no-dataweave-in-choice"
 *   scope: "file"  # Maps to FILE_SCOPE
 *   xpath: "//mule:choice//dw:transform"
 *   
 * // Application-scope rule - ensures global configuration exists somewhere
 * - key: "global-error-handler-required"
 *   scope: "application"  # Maps to APPLICATION_SCOPE  
 *   xpath: "//mule:configuration/mule:global-error-handler"
 * </pre>
 *
 * <h3>Rule Definition Integration:</h3>
 * <p>This enum works with {@link MuleRule} to provide scope-aware validation:
 * <ul>
 *   <li>{@code MuleRule.isFileScope()} returns true for FILE_SCOPE rules</li>
 *   <li>{@code MuleRule.isApplicationScope()} returns true for APPLICATION_SCOPE rules</li>
 * </ul>
 * </p>
 *
 * @see MuleRule for rule data model with scope integration
 * @see MuleValidationSensor for scope-aware rule evaluation logic
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public enum RuleScope {
	
	/**
	 * Rule is evaluated on each file independently.
	 * If a file violates the rule, an issue is reported on that file.
	 */
	FILE_SCOPE("file"),
	
	/**
	 * Rule is evaluated across all files in the application.
	 * An issue is reported only if ALL files fail the rule.
	 * If ANY file passes the rule, no issue is reported.
	 */
	APPLICATION_SCOPE("application");

	private final String key;

	RuleScope(String key) {
		this.key = key;
	}

	/**
	 * Gets the string key for this scope.
	 * 
	 * @return The scope key ("file" or "application")
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Parses a scope string into a RuleScope enum.
	 * 
	 * @param scope The scope string to parse
	 * @return The corresponding RuleScope, or FILE_SCOPE as default
	 */
	public static RuleScope fromString(String scope) {
		if (scope == null) {
			return FILE_SCOPE;
		}
		
		for (RuleScope ruleScope : values()) {
			if (ruleScope.key.equalsIgnoreCase(scope)) {
				return ruleScope;
			}
		}
		
		return FILE_SCOPE;
	}
}
