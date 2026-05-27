package com.sonar.plugins.mulesoft.rule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.util.XmlParserFactory;

/**
 * Defines DataWeave rules for SonarQube.
 *
 * <p>This definition loads DataWeave rule metadata from {@code rules-dataweave.xml} and registers them
 * in a dedicated repository. Rules are parsed using a secure JDOM2 SAXBuilder (no JAXB).
 *
 * <p>Rules are registered under the Mule language key so {@code .dwl} files are indexed as "Mule" in SonarQube,
 * but they are executed by {@link com.sonar.plugins.mulesoft.sensor.DataWeaveSensor} (not the Mule XML/XPath sensor).
 */
public class DataWeaveRulesDefinition implements RulesDefinition {

	private static final Logger logger = LoggerFactory.getLogger(DataWeaveRulesDefinition.class);

	public static final String REPOSITORY_KEY = "mule-dataweave-repository";
	public static final String REPOSITORY_NAME = "Mule DataWeave Analyzer";

	public static final String COMMENTED_OUT_CODE_RULE_KEY = "commented-out-code";
	public static final String TOO_LARGE_FILE_RULE_KEY = "too-large-file";
	/**
	 * Rule parameter name for the maximum allowed line count of a DataWeave file.
	 */
	public static final String TOO_LARGE_FILE_MAX_LINES_PARAM = "maxLines";

	/** Immutable data transfer object for a DataWeave rule parsed from XML. */
	private record DwRule(String id, String name, String description, String severity, String type, String value) {}

	@Override
	public void define(Context context) {
		NewRepository repository = context.createRepository(REPOSITORY_KEY, MuleLanguage.LANGUAGE_KEY)
				.setName(REPOSITORY_NAME);

		for (DwRule rule : loadRules("rules-dataweave.xml")) {
			addRule(repository, rule);
		}

		repository.done();
	}

	private static List<DwRule> loadRules(String filename) {
		// Try external override at extensions/plugins/<filename> first
		Path externalPath = Paths.get("extensions", "plugins", filename);
		if (Files.exists(externalPath)) {
			try (InputStream is = Files.newInputStream(externalPath)) {
				return parseRulesXml(is);
			} catch (Exception e) {
				logger.warn("Failed to load DataWeave rules from {}, falling back to classpath", externalPath, e);
			}
		}
		// Fall back to classpath resource
		try (InputStream is = DataWeaveRulesDefinition.class.getClassLoader().getResourceAsStream(filename)) {
			if (is == null) {
				throw new IllegalStateException("DataWeave rules not found on classpath: " + filename);
			}
			return parseRulesXml(is);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load DataWeave rules from classpath: " + filename, e);
		}
	}

	private static List<DwRule> parseRulesXml(InputStream is) throws JDOMException, IOException {
		SAXBuilder builder = XmlParserFactory.createSecureBuilder();
		Document doc = builder.build(is);
		List<DwRule> result = new ArrayList<>();
		for (Element ruleset : doc.getRootElement().getChildren("ruleset")) {
			for (Element rule : ruleset.getChildren("rule")) {
				result.add(new DwRule(
						rule.getAttributeValue("id"),
						rule.getAttributeValue("name"),
						rule.getAttributeValue("description"),
						rule.getAttributeValue("severity"),
						rule.getAttributeValue("type"),
						rule.getTextTrim()
				));
			}
		}
		return result;
	}

	private static void addRule(NewRepository repository, DwRule rule) {
		NewRule sonarRule = repository.createRule(rule.id())
				.setName(rule.name())
				.setHtmlDescription(rule.description())
				.setActivatedByDefault(true)
				.setStatus(RuleStatus.READY);

		sonarRule.setSeverity(mapSeverity(rule.severity()));
		sonarRule.setType(mapType(rule.type()));
		sonarRule.addTags("dataweave", "mule");

		if (TOO_LARGE_FILE_RULE_KEY.equals(rule.id())) {
			String maxLinesDefault = parsePositiveIntOrDefault(rule.value(), "5000");
			sonarRule.createParam(TOO_LARGE_FILE_MAX_LINES_PARAM)
					.setType(RuleParamType.INTEGER)
					.setDefaultValue(maxLinesDefault)
					.setDescription("Maximum allowed line count for a DataWeave (.dwl) file.");
		}
	}

	private static String mapSeverity(String severity) {
		if (severity == null) return Severity.MAJOR;
		switch (severity.toUpperCase()) {
			case "BLOCKER":   return Severity.BLOCKER;
			case "CRITICAL":  return Severity.CRITICAL;
			case "MINOR":     return Severity.MINOR;
			case "INFO":      return Severity.INFO;
			default:          return Severity.MAJOR;
		}
	}

	private static RuleType mapType(String type) {
		if (type == null) return RuleType.CODE_SMELL;
		switch (type.toUpperCase().replace("-", "_")) {
			case "BUG":           return RuleType.BUG;
			case "VULNERABILITY": return RuleType.VULNERABILITY;
			default:              return RuleType.CODE_SMELL;
		}
	}

	private static String parsePositiveIntOrDefault(String raw, String defaultValue) {
		if (raw == null) return defaultValue;
		String t = raw.trim();
		if (t.isEmpty()) return defaultValue;
		try {
			int v = Integer.parseInt(t);
			return v > 0 ? Integer.toString(v) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
