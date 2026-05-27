package com.sonar.plugins.mulesoft.rule.model;

/**
 * Data model for code examples that demonstrate rule compliance or violations.
 *
 * <p>Rule examples provide concrete illustrations of how validation rules apply
 * to real MuleSoft code, helping developers understand rule intent and proper
 * implementation patterns. Examples are embedded in rule definitions and displayed
 * in SonarQube's rule documentation.</p>
 *
 * <h3>Example Types:</h3>
 * <ul>
 *   <li><strong>Compliant:</strong> Code that follows the rule and represents best practices</li>
 *   <li><strong>Non-compliant:</strong> Code that violates the rule and should be avoided</li>
 * </ul>
 *
 * <h3>Usage in Rule Definition:</h3>
 * <pre>
 * # YAML rule with examples
 * - key: "avoid-hardcoded-endpoints"
 *   name: "Avoid Hardcoded Endpoints"
 *   examples:
 *     - type: "noncompliant"
 *       description: "Hardcoded URL makes deployment inflexible"
 *       code: |
 *         &lt;http:request url="https://api.production.com/users"/&gt;
 *         
 *     - type: "compliant"  
 *       description: "Configurable endpoint allows environment flexibility"
 *       code: |
 *         &lt;http:request url="${api.base.url}/users"/&gt;
 * </pre>
 *
 * <h3>Integration with SonarQube UI:</h3>
 * <p>Examples are displayed in SonarQube's rule details:
 * <ul>
 *   <li><strong>Rule Description Page:</strong> Shows examples with syntax highlighting</li>
 *   <li><strong>Issue Details:</strong> References relevant examples for context</li>
 *   <li><strong>IDE Integration:</strong> Examples appear in IDE rule documentation</li>
 * </ul>
 * </p>
 *
 * <h3>Example Structure:</h3>
 * <ul>
 *   <li><strong>Type:</strong> {@code "compliant"} or {@code "noncompliant"}</li>
 *   <li><strong>Code:</strong> XML code snippet demonstrating the rule application</li>
 *   <li><strong>Description:</strong> Explanation of why the code complies or violates the rule</li>
 * </ul>
 *
 * <h3>Best Practices for Examples:</h3>
 * <ul>
 *   <li><strong>Minimal:</strong> Focus on the specific pattern being illustrated</li>
 *   <li><strong>Realistic:</strong> Use practical, real-world code scenarios</li>
 *   <li><strong>Clear:</strong> Include descriptive comments explaining the rule application</li>
 *   <li><strong>Complete:</strong> Provide both compliant and non-compliant examples when possible</li>
 * </ul>
 *
 * @see MuleRule for rule data model that contains examples
 * @see RuleLoader for loading examples from YAML/XML definitions
 * @see MuleRulesDefinition for integrating examples into SonarQube rules
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class MuleRuleExample {

    private String type; // "compliant" or "noncompliant"
    private String code;
    private String description;

    public MuleRuleExample() {
    }

    public MuleRuleExample(String type, String code, String description) {
        this.type = type;
        this.code = code;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompliant() {
        return "compliant".equalsIgnoreCase(type);
    }

    public boolean isNoncompliant() {
        return "noncompliant".equalsIgnoreCase(type);
    }
}
