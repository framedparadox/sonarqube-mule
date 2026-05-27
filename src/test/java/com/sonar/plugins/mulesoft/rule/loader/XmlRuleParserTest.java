package com.sonar.plugins.mulesoft.rule.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sonar.plugins.mulesoft.rule.model.MuleRule;

/**
 * Tests for XmlRuleParser - parses legacy XML ruleset files.
 * 
 * <p>Verifies backward compatibility with XML-based ruleset configuration.</p>
 */
class XmlRuleParserTest {

	private XmlRuleParser parser;

	@BeforeEach
	void setUp() {
		parser = new XmlRuleParser();
	}

	@Test
	void testParse_ValidXmlRuleset_ReturnsRules() throws Exception {
		// Arrange - use bundled mulesoft-ruleset.xml from classpath
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");
		assertThat(xmlStream).as("Test requires mulesoft-ruleset.xml in resources").isNotNull();

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
	}

	@Test
	void testParse_ValidXml_ParsesRuleFields() throws Exception {
		// Arrange
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert - verify rule fields are populated
		for (MuleRule rule : rules) {
			assertThat(rule.getKey())
					.as("Rule key should not be null or empty")
					.isNotNull()
					.isNotEmpty();
			assertThat(rule.getName())
					.as("Rule name should not be null or empty")
					.isNotNull()
					.isNotEmpty();
			assertThat(rule.getDescription())
					.as("Rule description should not be null or empty")
					.isNotNull()
					.isNotEmpty();
		}
	}

	@Test
	void testParse_NullInputStream_ReturnsEmptyList() throws Exception {
		// Act
		List<MuleRule> rules = parser.parse(null);

		// Assert
		assertThat(rules).isNotNull();
		assertThat(rules).isEmpty();
	}

	@Test
	void testParse_ValidXml_MapsXpathCorrectly() throws Exception {
		// Arrange
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert - at least some rules should have xpath
		boolean hasRulesWithXpath = rules.stream()
				.anyMatch(rule -> rule.getXpath() != null && !rule.getXpath().isEmpty());
		assertThat(hasRulesWithXpath)
				.as("At least some rules should have xpath expressions")
				.isTrue();
	}

	@Test
	void testParse_ValidXml_MapsScopeCorrectly() throws Exception {
		// Arrange
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert - all rules should have scope (file or application)
		for (MuleRule rule : rules) {
			assertThat(rule.getScope())
					.as("Rule scope should be defined")
					.isNotNull()
					.isNotEmpty();
		}
	}

	@Test
	void testParse_ValidXml_MapsSeverityCorrectly() throws Exception {
		// Arrange
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert - all rules should have severity
		for (MuleRule rule : rules) {
			assertThat(rule.getSeverity())
					.as("Rule severity should be defined")
					.isNotNull()
					.isNotEmpty()
					.isIn("INFO", "MINOR", "MAJOR", "CRITICAL", "BLOCKER");
		}
	}

	@Test
	void testParse_ValidXml_ParsesMultipleRules() throws Exception {
		// Arrange
		InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("mulesoft-ruleset.xml");

		// Act
		List<MuleRule> rules = parser.parse(xmlStream);

		// Assert - mulesoft-ruleset.xml should have many rules
		assertThat(rules.size()).as("Should parse multiple rules from XML").isGreaterThan(5);
	}
}
