package com.sonar.plugins.mulesoft.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sonar.plugins.mulesoft.rule.loader.RuleLoader;
import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleParameter;

/**
 * Tests for RuleLoader to verify it correctly loads rules from bundled YAML/XML resources.
 */
class RuleLoaderTest {

	private RuleLoader ruleLoader;

	@BeforeEach
	void setUp() {
		ruleLoader = new RuleLoader();
	}

	@Test
	void testLoadRules_ReturnsListNotNull() {
		List<MuleRule> rules = ruleLoader.loadRules();

		// Should always return a list (never null)
		assertThat(rules).isNotNull();
	}

	@Test
	void testRuleLoader_CanBeInstantiated() {
		RuleLoader loader = new RuleLoader();

		assertThat(loader).isNotNull();
	}

	@Test
	void testLoadRules_LoadsBundledYamlRules() {
		// No external override files exist during the test, so RuleLoader should
		// fall back to the bundled YAML ruleset on the classpath.
		List<MuleRule> rules = ruleLoader.loadRules();

		// Bundled YAML should have rules
		assertThat(rules).isNotEmpty();
		assertThat(rules.size()).isGreaterThan(10);

		// Verify some known rules from bundled YAML
		assertThat(rules)
			.extracting(MuleRule::getKey)
			.contains(
				"apikit-config-required",
				"apikit-exception-strategy",
				"count-limit",
				"naming-convention"
			);
	}

	@Test
	void testLoadRules_ParsesRuleFieldsCorrectly() {
		List<MuleRule> rules = ruleLoader.loadRules();

		// Find specific rule to verify field parsing
		MuleRule apikitRule = rules.stream()
			.filter(r -> "apikit-config-required".equals(r.getKey()))
			.findFirst()
			.orElse(null);

		assertThat(apikitRule).isNotNull();
		assertThat(apikitRule.getName()).isNotEmpty();
		assertThat(apikitRule.getDescription()).isNotEmpty();
		assertThat(apikitRule.getSeverity()).isEqualTo("MAJOR");
		assertThat(apikitRule.getScope()).isEqualToIgnoringCase("application");
		assertThat(apikitRule.getRemediationEffort()).isEqualTo("60");
		assertThat(apikitRule.getXpath()).isNotEmpty();
	}

	@Test
	void testLoadRules_HandlesLegacyFieldNames() {
		// YAML uses 'ruleKey' and 'applies' (legacy names)
		// Verify they map correctly to 'key' and 'scope'
		List<MuleRule> rules = ruleLoader.loadRules();

		for (MuleRule rule : rules) {
			assertThat(rule.getKey())
				.as("Rule key should be populated from 'ruleKey' field")
				.isNotNull()
				.isNotEmpty();
			assertThat(rule.getScope())
				.as("Rule scope should be populated from 'applies' field")
				.isNotNull()
				.isNotEmpty();
		}
	}

	@Test
	void loads_rule_with_parameters_and_stores_them() throws Exception {
		String yaml = """
				rulesets:
				  - category: flows
				    rules:
				      - ruleKey: flow-length-limit
				        name: Flow length limit
				        description: X
				        severity: MAJOR
				        type: CODE_SMELL
				        parameters:
				          - key: max
				            type: INTEGER
				            defaultValue: "15"
				        xpath: "not(count(//flow/*)>${max})"
				        requires: []
				""";

		List<MuleRule> rules = ruleLoader.loadFromYamlString(yaml);

		assertThat(rules).hasSize(1);
		MuleRule rule = rules.get(0);
		assertThat(rule.getParameters()).hasSize(1);
		assertThat(rule.getParameters().get(0).getKey()).isEqualTo("max");
		assertThat(rule.getParameters().get(0).getDefaultValue()).isEqualTo("15");
		assertThat(rule.getXpath()).contains("${max}");
		assertThat(rule.getRequires()).isEmpty();
	}

	@Test
	void testBundledRulesets_ArePackagedInMainResources() throws Exception {
		List<String> yamlResources = Collections.list(
			getClass().getClassLoader().getResources("mulesoft-ruleset.yaml"))
			.stream()
			.map(URL::toString)
			.map(location -> location.replace('\\', '/'))
			.toList();

		List<String> xmlResources = Collections.list(
			getClass().getClassLoader().getResources("mulesoft-ruleset.xml"))
			.stream()
			.map(URL::toString)
			.map(location -> location.replace('\\', '/'))
			.toList();

		assertThat(yamlResources)
			.anyMatch(location -> location.contains("/target/classes/"));
		assertThat(xmlResources)
			.anyMatch(location -> location.contains("/target/classes/"));
	}
}
