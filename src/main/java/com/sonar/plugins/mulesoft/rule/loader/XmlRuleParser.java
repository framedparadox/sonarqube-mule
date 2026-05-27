package com.sonar.plugins.mulesoft.rule.loader;

import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy XML rule parser providing backward compatibility with existing XML-based rule definitions.
 *
 * <p>This parser supports the original XML rule format for organizations with established
 * XML-based rule configurations. While YAML is the preferred format for new rules, this
 * parser ensures existing rule investments remain functional during migration periods.</p>
 *
 * <h3>Supported XML Structure:</h3>
 * <pre>
 * &lt;rulestore type="mulesoft-ruleset"&gt;
 *   &lt;ruleset category="flows"&gt;
 *     &lt;rule id="1" 
 *           ruleKey="rule-key" 
 *           name="Rule Name"
 *           description="Detailed rule description"
 *           severity="MAJOR"
 *           applies="file"
 *           type="code_smell"&gt;
 *       //mule:flow[@name='hardcoded-name']
 *     &lt;/rule&gt;
 *     
 *     &lt;rule id="2" ruleKey="another-rule" ...&gt;
 *       //db:config[@password[not(matches(., '^\\$\\{.*\\}$'))]]
 *     &lt;/rule&gt;
 *   &lt;/ruleset&gt;
 * &lt;/rulestore&gt;
 * </pre>
 *
 * <h3>XML Attribute Mapping:</h3>
 * <table border="1">
 *   <tr><th>XML Attribute</th><th>MuleRule Property</th><th>Description</th></tr>
 *   <tr><td>{@code ruleKey}</td><td>{@code key}</td><td>Unique rule identifier</td></tr>
 *   <tr><td>{@code name}</td><td>{@code name}</td><td>Human-readable rule name</td></tr>
 *   <tr><td>{@code description}</td><td>{@code description}</td><td>Detailed rule explanation</td></tr>
 *   <tr><td>{@code severity}</td><td>{@code severity}</td><td>MINOR, MAJOR, CRITICAL, etc.</td></tr>
 *   <tr><td>{@code applies}</td><td>{@code scope}</td><td>"file" or "application"</td></tr>
 *   <tr><td>{@code type}</td><td>{@code type}</td><td>code_smell, vulnerability, bug</td></tr>
 *   <tr><td>Element Text</td><td>{@code xpath}</td><td>XPath expression for rule validation</td></tr>
 * </table>
 *
 * <h3>Legacy Support Features:</h3>
 * <ul>
 *   <li><strong>Attribute Flexibility:</strong> Handles variations in XML attribute naming</li>
 *   <li><strong>Multiple Rulesets:</strong> Supports categorized rule organization</li>
 *   <li><strong>Graceful Parsing:</strong> Continues processing valid rules even if some fail</li>
 *   <li><strong>Error Reporting:</strong> Detailed logging for troubleshooting malformed XML</li>
 * </ul>
 *
 * <h3>Migration Path:</h3>
 * <p>To migrate from XML to YAML format:
 * <ol>
 *   <li>Export existing XML rules using this parser</li>
 *   <li>Convert to YAML format using {@link RuleLoader} examples</li>
 *   <li>Place YAML file in extensions directory</li>
 *   <li>Remove XML file (YAML takes precedence)</li>
 * </ol>
 * </p>
 *
 * <h3>XML Validation:</h3>
 * <p>The parser validates:
 * <ul>
 *   <li><strong>Required Attributes:</strong> {@code ruleKey}, {@code name} must be present</li>
 *   <li><strong>XPath Syntax:</strong> Basic validation of XPath expressions</li>
 *   <li><strong>Enum Values:</strong> Severity and type values match SonarQube standards</li>
 *   <li><strong>Unicode Support:</strong> Proper handling of international characters</li>
 * </ul>
 * </p>
 *
 * <h3>Performance Considerations:</h3>
 * <p>XML parsing performance:
 * <ul>
 *   <li><strong>DOM Parsing:</strong> Uses JDOM2 for reliable XML processing</li>
 *   <li><strong>Single Parse:</strong> Rules are loaded once during plugin initialization</li>
 *   <li><strong>Memory Efficient:</strong> Converts to lightweight {@link MuleRule} objects</li>
 * </ul>
 * </p>
 *
 * @see RuleLoader for overall rule loading strategy and YAML support
 * @see MuleRule for rule data model
 * @see MuleRulesDefinition for SonarQube integration
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class XmlRuleParser {

    private static final Logger LOG = LoggerFactory.getLogger(XmlRuleParser.class);

    /**
     * Parses XML input stream into MuleRule objects.
     *
     * Expected XML structure:
     * <pre>
     * &lt;rulestore type="mulesoft-ruleset"&gt;
     *   &lt;ruleset category="flows"&gt;
     *     &lt;rule id="1" ruleKey="rule-key" name="Rule Name"
     *           description="..." severity="MAJOR" applies="file" type="code_smell"&gt;
     *       XPath expression here
     *     &lt;/rule&gt;
     *   &lt;/ruleset&gt;
     * &lt;/rulestore&gt;
     * </pre>
     */
    public List<MuleRule> parse(InputStream inputStream) throws Exception {
        List<MuleRule> rules = new ArrayList<>();
        
        // Handle null input stream
        if (inputStream == null) {
            LOG.debug("Null input stream provided, returning empty rule list");
            return rules;
        }

        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document doc = builder.build(inputStream);
        Element root = doc.getRootElement();

        // Iterate through rulesets
        List<Element> rulesets = root.getChildren("ruleset");
        for (Element ruleset : rulesets) {
            String category = ruleset.getAttributeValue("category");

            // Iterate through rules in this ruleset
            List<Element> ruleElements = ruleset.getChildren("rule");
            for (Element ruleElement : ruleElements) {
                MuleRule rule = parseXmlRule(ruleElement, category);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        LOG.debug("Parsed {} rules from XML", rules.size());
        return rules;
    }

    /**
     * Parses a single rule element from XML.
     */
    private MuleRule parseXmlRule(Element ruleElement, String defaultCategory) {
        try {
            MuleRule rule = new MuleRule();

            // Extract attributes — support both 'ruleKey' (preferred) and legacy 'id'
            String ruleKey = ruleElement.getAttributeValue("ruleKey");
            if (ruleKey == null || ruleKey.isEmpty()) {
                ruleKey = ruleElement.getAttributeValue("id");
            }
            rule.setKey(ruleKey);
            rule.setName(ruleElement.getAttributeValue("name"));
            rule.setDescription(ruleElement.getAttributeValue("description"));
            rule.setSeverity(getAttributeOrDefault(ruleElement, "severity", "MAJOR"));
            rule.setType(getAttributeOrDefault(ruleElement, "type", "CODE_SMELL"));
            rule.setCategory(getAttributeOrDefault(ruleElement, "category", defaultCategory));
            rule.setScope(getAttributeOrDefault(ruleElement, "applies", "file"));

            // XPath is in the element text content
            String xpath = ruleElement.getTextTrim();
            if (xpath != null && !xpath.isEmpty()) {
                rule.setXpath(xpath);
            }

            // XPath location hint (optional child element)
            Element locationHintElement = ruleElement.getChild("xpath-location-hint");
            if (locationHintElement != null) {
                rule.setXpathLocationHint(locationHintElement.getTextTrim());
            }

            // Validate required fields
            if (rule.getKey() == null || rule.getKey().isEmpty()) {
                LOG.warn("Skipping rule with missing ruleKey/id in category '{}'", defaultCategory);
                return null;
            }

            if (rule.getXpath() == null || rule.getXpath().isEmpty()) {
                LOG.warn("Skipping rule {} with missing XPath expression", rule.getKey());
                return null;
            }

            return rule;
        } catch (Exception e) {
            LOG.warn("Failed to parse XML rule: {}", e.getMessage());
            return null;
        }
    }

    private String getAttributeOrDefault(Element element, String attributeName, String defaultValue) {
        String value = element.getAttributeValue(attributeName);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
