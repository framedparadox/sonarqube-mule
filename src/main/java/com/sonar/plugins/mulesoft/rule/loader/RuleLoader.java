package com.sonar.plugins.mulesoft.rule.loader;

import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flexible rule loader supporting both YAML (preferred) and XML (legacy) rule formats.
 *
 * <p>This loader enables dynamic rule customization by reading validation rules from
 * external configuration files placed in the SonarQube extensions directory. It supports
 * a fallback strategy prioritizing modern YAML format while maintaining compatibility
 * with legacy XML configurations.</p>
 *
 * <h3>Loading Strategy:</h3>
 * <ol>
 *   <li><strong>Primary:</strong> Look for {@code extensions/plugins/mulesoft-ruleset.yaml}</li>
 *   <li><strong>Fallback 1:</strong> Look for {@code extensions/plugins/mulesoft-ruleset.xml}</li>
 *   <li><strong>Fallback 2:</strong> Load bundled YAML/XML resource from classpath</li>
 *   <li><strong>Default:</strong> If nothing found, return empty rule set</li>
 * </ol>
 *
 * <h3>YAML Format (Recommended):</h3>
 * <pre>
 * # mulesoft-ruleset.yaml
 * rules:
 *   - key: "avoid-hardcoded-credentials"
 *     name: "Avoid Hardcoded Credentials"
 *     description: "Credentials should be externalized using property placeholders"
 *     xpath: "//db:config/@password[not(matches(., '^\\$\\{.*\\}$'))]"
 *     scope: "FILE"
 *     severity: "CRITICAL"
 *     type: "VULNERABILITY"
 *     message: "Database password should use property placeholder: ${db.password}"
 *     examples:
 *       - type: "noncompliant"
 *         description: "Hardcoded password"
 *         code: '&lt;db:config password="mypassword"/&gt;'
 *       - type: "compliant"
 *         description: "Configurable password"
 *         code: '&lt;db:config password="${db.password}"/&gt;'
 * </pre>
 *
 * <h3>XML Format (Legacy):</h3>
 * <pre>
 * &lt;!-- mulesoft-ruleset.xml --&gt;
 * &lt;rulestore type="mulesoft-ruleset"&gt;
 *   &lt;ruleset category="security"&gt;
 *     &lt;rule id="1" ruleKey="avoid-hardcoded-credentials" 
 *           name="Avoid Hardcoded Credentials"
 *           description="..."
 *           severity="CRITICAL"
 *           applies="file"
 *           type="vulnerability"&gt;
 *       //db:config/@password[not(matches(., '^\\$\\{.*\\}$'))]
 *     &lt;/rule&gt;
 *   &lt;/ruleset&gt;
 * &lt;/rulestore&gt;
 * </pre>
 *
 * <h3>Dynamic Rule Management:</h3>
 * <p>This approach enables:
 * <ul>
 *   <li><strong>No Plugin Rebuilds:</strong> Update rules without recompiling the plugin</li>
 *   <li><strong>Environment-Specific Rules:</strong> Different rules for different SonarQube instances</li>
 *   <li><strong>Gradual Migration:</strong> Add rules incrementally as teams adopt standards</li>
 *   <li><strong>Custom Organizational Rules:</strong> Implement company-specific validation requirements</li>
 * </ul>
 * </p>
 *
 * <h3>Error Handling:</h3>
 * <p>Robust error handling ensures plugin stability:
 * <ul>
 *   <li><strong>Missing Files:</strong> Gracefully handle missing rule configuration files</li>
 *   <li><strong>Parse Errors:</strong> Log and skip malformed rules while continuing with valid ones</li>
 *   <li><strong>Invalid XPath:</strong> Validate XPath expressions and report errors</li>
 *   <li><strong>Schema Validation:</strong> Verify rule structure meets expected format</li>
 * </ul>
 * </p>
 *
 * <h3>Integration Points:</h3>
 * <p>The rule loader integrates with:
 * <ul>
 *   <li><strong>{@link MuleRulesDefinition}:</strong> Registers loaded rules with SonarQube</li>
 *   <li><strong>{@link MuleQualityProfile}:</strong> Activates rules in default quality profile</li>
 *   <li><strong>{@link XmlRuleParser}:</strong> Handles XML format parsing (legacy support)</li>
 * </ul>
 * </p>
 *
 * @see MuleRule for rule data model
 * @see XmlRuleParser for XML parsing implementation
 * @see MuleRulesDefinition for SonarQube integration
 * @see MuleQualityProfile for rule activation
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
@ScannerSide
@ServerSide
public class RuleLoader {

    private static final Logger LOG = LoggerFactory.getLogger(RuleLoader.class);
    private static final String YAML_FILENAME = "mulesoft-ruleset.yaml";
    private static final String XML_FILENAME = "mulesoft-ruleset.xml";
    private static final String EXTENSIONS_SUBPATH = "extensions/plugins";

    private final ObjectMapper yamlMapper;
    private final XmlRuleParser xmlParser;

    public RuleLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.xmlParser = new XmlRuleParser();
    }

    /**
     * Loads rules from external files using YAML-first strategy.
     *
     * <p>This method searches for ruleset files in the SonarQube extensions/plugins/
     * directory first. If not found externally, it falls back to bundled resources.</p>
     *
     * @return List of MuleRule objects loaded from external files or bundled resources
     */
    public List<MuleRule> loadRules() {
        LOG.info("Loading Mule validation rules...");

        // Try all candidate extensions paths in priority order
        List<Path> candidatePaths = resolveExtensionPaths();

        for (Path extensionsPath : candidatePaths) {
            LOG.debug("Looking for external ruleset in: {}", extensionsPath.toAbsolutePath());

            List<MuleRule> rules = loadFromExternalYaml(extensionsPath);
            if (!rules.isEmpty()) {
                LOG.info("Successfully loaded {} rules from external YAML: {}/{}",
                         rules.size(), extensionsPath, YAML_FILENAME);
                return rules;
            }

            rules = loadFromExternalXml(extensionsPath);
            if (!rules.isEmpty()) {
                LOG.info("Successfully loaded {} rules from external XML: {}/{}",
                         rules.size(), extensionsPath, XML_FILENAME);
                return rules;
            }
        }

        // Fallback to bundled YAML resource (default ruleset)
        LOG.debug("No external ruleset found in any candidate path, trying bundled YAML resource");
        List<MuleRule> rules = loadFromBundledYaml();
        if (!rules.isEmpty()) {
            LOG.info("Successfully loaded {} rules from bundled YAML resource: {}",
                     rules.size(), YAML_FILENAME);
            return rules;
        }

        // Fallback to bundled XML resource (legacy default)
        LOG.debug("No bundled YAML found, trying bundled XML resource");
        rules = loadFromBundledXml();
        if (!rules.isEmpty()) {
            LOG.info("Successfully loaded {} rules from bundled XML resource: {}",
                     rules.size(), XML_FILENAME);
            return rules;
        }

        // No rules found anywhere
        LOG.warn("No ruleset file found. No rules will be registered.");
        LOG.warn("Searched external paths: {}",
                 candidatePaths.stream().map(p -> p.toAbsolutePath().toString()).collect(Collectors.joining(", ")));

        return Collections.emptyList();
    }

    /**
     * Loads and parses rules from a YAML string. Used by tests.
     *
     * @param yamlContent YAML string containing rule definitions
     * @return List of parsed MuleRule objects
     */
    public List<MuleRule> loadFromYamlString(String yamlContent) throws IOException {
        List<MuleRule> rules = parseYamlFile(new java.io.ByteArrayInputStream(yamlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        enrichWithHtmlDescriptions(rules);
        return rules;
    }

    /**
     * Enriches each rule with an HTML description loaded from the classpath resource
     * {@code rules/<ruleKey>.html}. If no such resource exists, the rule is left unchanged.
     */
    private void enrichWithHtmlDescriptions(List<MuleRule> rules) {
        for (MuleRule rule : rules) {
            if (rule.getKey() == null) continue;
            String resourcePath = "rules/" + rule.getKey() + ".html";
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    LOG.debug("No HTML description found for rule '{}' at classpath:{}", rule.getKey(), resourcePath);
                    continue;
                }
                String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                rule.setHtmlDescription(html);
                LOG.debug("Loaded HTML description for rule '{}' from classpath:{}", rule.getKey(), resourcePath);
            } catch (java.io.IOException e) {
                LOG.debug("Failed to read HTML description for rule '{}': {}", rule.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Resolves all candidate extensions/plugins directories in priority order.
     *
     * <p>Checks the following sources in order:
     * <ol>
     *   <li>{@code sonar.path.home} system property (set by SonarQube server at startup)</li>
     *   <li>{@code SONARQUBE_HOME} environment variable</li>
     *   <li>Common installation path {@code /opt/sonarqube} (Docker default)</li>
     *   <li>Common installation path {@code /usr/local/sonarqube}</li>
     *   <li>Relative path from JVM working directory (last resort)</li>
     * </ol>
     * </p>
     */
    private List<Path> resolveExtensionPaths() {
        List<Path> candidates = new ArrayList<>();

        // 1. sonar.path.home system property (authoritative — set by SonarQube server at startup)
        String sonarHome = System.getProperty("sonar.path.home");
        if (sonarHome != null && !sonarHome.isEmpty()) {
            LOG.debug("Adding candidate from system property sonar.path.home: {}", sonarHome);
            candidates.add(Paths.get(sonarHome, EXTENSIONS_SUBPATH));
        }

        // 2. SONARQUBE_HOME environment variable
        String sonarHomeEnv = System.getenv("SONARQUBE_HOME");
        if (sonarHomeEnv != null && !sonarHomeEnv.isEmpty()) {
            LOG.debug("Adding candidate from env SONARQUBE_HOME: {}", sonarHomeEnv);
            candidates.add(Paths.get(sonarHomeEnv, EXTENSIONS_SUBPATH));
        }

        // 3. Common installation paths (Docker/Linux defaults)
        candidates.add(Paths.get("/opt/sonarqube", EXTENSIONS_SUBPATH));
        candidates.add(Paths.get("/usr/local/sonarqube", EXTENSIONS_SUBPATH));

        // 4. Relative path fallback (only works when CWD happens to be SonarQube home)
        candidates.add(Paths.get(EXTENSIONS_SUBPATH));

        return candidates;
    }

    /**
     * Loads rules from an external YAML file in the given directory.
     */
    private List<MuleRule> loadFromExternalYaml(Path basePath) {
        Path yamlPath = basePath.resolve(YAML_FILENAME);

        if (!Files.exists(yamlPath)) {
            LOG.debug("YAML ruleset not found at: {}", yamlPath.toAbsolutePath());
            return Collections.emptyList();
        }

        try (InputStream is = Files.newInputStream(yamlPath)) {
            LOG.debug("Reading YAML ruleset from: {}", yamlPath.toAbsolutePath());
            return parseYamlFile(is);
        } catch (IOException e) {
            LOG.error("Failed to read YAML ruleset from {}: {}", yamlPath, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Loads rules from an external XML file in the given directory.
     */
    private List<MuleRule> loadFromExternalXml(Path basePath) {
        Path xmlPath = basePath.resolve(XML_FILENAME);

        if (!Files.exists(xmlPath)) {
            LOG.debug("XML ruleset not found at: {}", xmlPath.toAbsolutePath());
            return Collections.emptyList();
        }

        try (InputStream is = Files.newInputStream(xmlPath)) {
            LOG.debug("Reading XML ruleset from: {}", xmlPath.toAbsolutePath());
            return xmlParser.parse(is);
        } catch (Exception e) {
            LOG.error("Failed to read XML ruleset from {}: {}", xmlPath, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Loads rules from bundled YAML resource (classpath).
     * This provides a default ruleset bundled with the plugin.
     */
    private List<MuleRule> loadFromBundledYaml() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(YAML_FILENAME)) {
            if (is == null) {
                LOG.debug("Bundled YAML resource not found: {}", YAML_FILENAME);
                return Collections.emptyList();
            }
            LOG.debug("Reading bundled YAML resource: {}", YAML_FILENAME);
            return parseYamlFile(is);
        } catch (IOException e) {
            LOG.error("Failed to read bundled YAML resource {}: {}", YAML_FILENAME, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Loads rules from bundled XML resource (classpath).
     * This provides legacy default ruleset support.
     */
    private List<MuleRule> loadFromBundledXml() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(XML_FILENAME)) {
            if (is == null) {
                LOG.debug("Bundled XML resource not found: {}", XML_FILENAME);
                return Collections.emptyList();
            }
            LOG.debug("Reading bundled XML resource: {}", XML_FILENAME);
            return xmlParser.parse(is);
        } catch (Exception e) {
            LOG.error("Failed to read bundled XML resource {}: {}", XML_FILENAME, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses YAML input stream into list of MuleRule objects.
     *
     * <p>Expected YAML structure:</p>
     * <pre>
     * rulesets:
     *   - category: flows
     *     rules:
     *       - key: rule-key
     *         name: Rule Name
     *         description: Rule description
     *         severity: MAJOR
     *         type: CODE_SMELL
     *         scope: file
     *         xpath: //xpath/expression
     *         xpathLocationHint: //hint/expression
     *         tags: [tag1, tag2]
     * </pre>
     */
    @SuppressWarnings("unchecked")
    private List<MuleRule> parseYamlFile(InputStream inputStream) throws IOException {
        Map<String, Object> yamlData = yamlMapper.readValue(inputStream, Map.class);

        List<MuleRule> allRules = new ArrayList<>();

        // Parse rulesets
        Object rulesetsObj = yamlData.get("rulesets");
        if (rulesetsObj instanceof List) {
            List<Map<String, Object>> rulesets = (List<Map<String, Object>>) rulesetsObj;

            for (Map<String, Object> ruleset : rulesets) {
                String category = getOrDefault(ruleset, "category", null);
                Object rulesObj = ruleset.get("rules");

                if (rulesObj instanceof List) {
                    List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesObj;

                    for (Map<String, Object> ruleData : rules) {
                        MuleRule rule = parseYamlRule(ruleData, category);
                        if (rule != null) {
                            allRules.add(rule);
                        }
                    }
                }
            }
        }

        LOG.debug("Parsed {} rules from YAML", allRules.size());
        return allRules;
    }

    /**
     * Parses a single rule from YAML data structure.
     * Supports both modern field names (key, scope) and legacy field names (ruleKey, applies).
     */
    @SuppressWarnings("unchecked")
    private MuleRule parseYamlRule(Map<String, Object> ruleData, String defaultCategory) {
        try {
            MuleRule rule = new MuleRule();

            // Support both 'key' and 'ruleKey' (legacy XML format compatibility)
            String key = getOrDefault(ruleData, "key", null);
            if (key == null) {
                key = getOrDefault(ruleData, "ruleKey", null);
            }
            rule.setKey(key);

            rule.setName(getOrDefault(ruleData, "name", null));
            rule.setDescription(getOrDefault(ruleData, "description", null));
            rule.setSeverity(getOrDefault(ruleData, "severity", "MAJOR"));
            rule.setType(getOrDefault(ruleData, "type", "CODE_SMELL"));
            rule.setCategory(getOrDefault(ruleData, "category", defaultCategory));

            // Support both 'scope' and 'applies' (legacy XML format compatibility)
            String scope = getOrDefault(ruleData, "scope", null);
            if (scope == null) {
                scope = getOrDefault(ruleData, "applies", "file");
            }
            rule.setScope(scope);

            rule.setXpath(getOrDefault(ruleData, "xpath", null));
            String locationHint = getOrDefault(ruleData, "locationHint", null);
            if (locationHint == null) {
                locationHint = getOrDefault(ruleData, "xpathLocationHint", null);
            }
            rule.setXpathLocationHint(locationHint);
            rule.setHtmlDescription(getOrDefault(ruleData, "htmlDescription", null));
            rule.setRemediationEffort(getOrDefault(ruleData, "remediationEffort", null));

            // Parse tags
            Object tagsObj = ruleData.get("tags");
            if (tagsObj instanceof List) {
                List<String> tags = new ArrayList<>();
                for (Object tag : (List<?>) tagsObj) {
                    if (tag != null) {
                        tags.add(tag.toString());
                    }
                }
                rule.setTags(tags);
            }

            // Parse configurable parameters (YAML 'parameters' section)
            Object paramsObj = ruleData.get("parameters");
            if (paramsObj instanceof List) {
                for (Object p : (List<?>) paramsObj) {
                    if (p instanceof Map) {
                        Map<String, Object> pm = (Map<String, Object>) p;
                        MuleRuleParameter param = new MuleRuleParameter();
                        param.setKey(getOrDefault(pm, "key", null));
                        param.setName(getOrDefault(pm, "name", null));
                        param.setType(getOrDefault(pm, "type", "STRING"));
                        param.setDefaultValue(getOrDefault(pm, "defaultValue", null));
                        param.setDescription(getOrDefault(pm, "description", null));
                        rule.getParameters().add(param);
                    }
                }
            }

            // Parse requires (capabilities this rule depends on)
            Object requiresObj = ruleData.get("requires");
            if (requiresObj instanceof List) {
                for (Object r : (List<?>) requiresObj) {
                    if (r != null) {
                        rule.getRequires().add(r.toString());
                    }
                }
            }

            // Validate required fields
            if (rule.getKey() == null || rule.getKey().isEmpty()) {
                LOG.warn("Skipping rule with missing key in category: {}", defaultCategory);
                return null;
            }

            if (rule.getXpath() == null || rule.getXpath().isEmpty()) {
                LOG.warn("Skipping rule {} with missing XPath expression", rule.getKey());
                return null;
            }

            return rule;
        } catch (Exception e) {
            LOG.warn("Failed to parse YAML rule (key={}): {}", ruleData.get("key"), e.getMessage());
            return null;
        }
    }

    /**
     * Gets a value from the map or returns a default value if not present.
     */
    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
