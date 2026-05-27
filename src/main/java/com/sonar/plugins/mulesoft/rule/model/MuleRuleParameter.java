package com.sonar.plugins.mulesoft.rule.model;

/**
 * Represents a configurable parameter for a MuleRule.
 *
 * <p>Parameters allow rules to have user-tunable values (e.g., thresholds),
 * making rules reusable across different organizational requirements.</p>
 *
 * <h3>Supported Types:</h3>
 * <ul>
 *   <li>STRING — text value (default)</li>
 *   <li>INTEGER — numeric threshold</li>
 *   <li>BOOLEAN — flag</li>
 *   <li>FLOAT — decimal value</li>
 *   <li>TEXT — multi-line text</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class MuleRuleParameter {
    private String key;
    private String name;
    private String type = "STRING";
    private String defaultValue;
    private String description;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
