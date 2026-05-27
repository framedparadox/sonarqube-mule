package com.sonar.plugins.mulesoft.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Repository;

import com.sonar.plugins.mulesoft.rule.loader.RuleLoader;
import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleExample;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleParameter;

/**
 * Tests for MuleRulesDefinition - the rules registration component.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Repository creation with correct key and name</li>
 *   <li>Rule loading from RuleLoader</li>
 *   <li>Rule metadata mapping (key, name, description, severity, type)</li>
 *   <li>Rule parameters (scope, xpath, location hint)</li>
 *   <li>Rule examples and remediation</li>
 *   <li>Handling of empty rule lists</li>
 * </ul>
 */
class MuleRulesDefinitionTest {

	private RuleLoader mockRuleLoader;
	private MuleRulesDefinition rulesDefinition;
	private RulesDefinition.Context context;

	@BeforeEach
	void setUp() {
		mockRuleLoader = mock(RuleLoader.class);
		rulesDefinition = new MuleRulesDefinition(mockRuleLoader);
		context = new RulesDefinition.Context();
	}

	@Test
	void testDefine_CreatesRepositoryWithCorrectProperties() {
		// Arrange
		when(mockRuleLoader.loadRules()).thenReturn(Collections.emptyList());

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository).isNotNull();
		assertThat(repository.key()).isEqualTo(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.name()).isEqualTo("Mulesoft");
		assertThat(repository.language()).isEqualTo("mule");
	}

	@Test
	void testDefine_LoadsRulesFromRuleLoader() {
		// Arrange
		MuleRule rule1 = createSampleRule("rule1");
		MuleRule rule2 = createSampleRule("rule2");
		when(mockRuleLoader.loadRules()).thenReturn(Arrays.asList(rule1, rule2));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rules()).hasSize(6); // 2 loaded rules + 3 complexity rules + 1 template
		assertThat(repository.rule("rule1")).isNotNull();
		assertThat(repository.rule("rule2")).isNotNull();
		assertThat(repository.rule(MuleRulesDefinition.RULE_TEMPLATE_KEY)).isNotNull();
	}

	@Test
	void testDefine_MapsRuleKeyCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("test-rule-key");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("test-rule-key")).isNotNull();
		assertThat(repository.rule("test-rule-key").key()).isEqualTo("test-rule-key");
	}

	@Test
	void testDefine_MapsRuleNameCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setName("Test Rule Name");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").name()).isEqualTo("Test Rule Name");
	}

	@Test
	void testDefine_MapsRuleDescriptionCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setDescription("This is a detailed description of the rule.");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").htmlDescription())
				.isEqualTo("This is a detailed description of the rule.");
	}

	@Test
	void testDefine_MapsSeverityCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setSeverity("CRITICAL");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").severity()).isEqualTo(Severity.CRITICAL);
	}

	@Test
	void testDefine_DefaultsToMajorSeverity() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setSeverity(null); // No severity specified
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").severity()).isEqualTo(Severity.MAJOR);
	}

	@Test
	void testDefine_MapsRuleTypeCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setType("BUG");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").type()).isEqualTo(RuleType.BUG);
	}

	@Test
	void testDefine_DefaultsToCodeSmellType() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setType(null); // No type specified
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").type()).isEqualTo(RuleType.CODE_SMELL);
	}

	@Test
	void testDefine_CreatesParametersCorrectly() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setScope("application");
		rule.setXpath("//mule:flow");
		rule.setXpathLocationHint("//mule:flow[1]");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		RulesDefinition.Rule sonarRule = repository.rule("rule-key");

		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.SCOPE)).isNotNull();
		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.SCOPE).defaultValue()).isEqualTo("application");

		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.XPATH)).isNotNull();
		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.XPATH).defaultValue()).isEqualTo("//mule:flow");

		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT)).isNotNull();
		assertThat(sonarRule.param(MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT).defaultValue())
				.isEqualTo("//mule:flow[1]");
	}

	@Test
	void testDefine_AddsRemediationEffort() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		rule.setRemediationEffort("30min");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		RulesDefinition.Rule sonarRule = repository.rule("rule-key");
		assertThat(sonarRule.debtRemediationFunction()).isNotNull();
		assertThat(sonarRule.debtRemediationFunction().gapMultiplier()).isNull();
		assertThat(sonarRule.debtRemediationFunction().baseEffort()).isEqualTo("30min");
	}

	@Test
	void testDefine_AddsRuleExamples() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		MuleRuleExample goodExample = new MuleRuleExample();
		goodExample.setType("compliant");
		goodExample.setCode("<flow name=\"good-flow\"/>");

		MuleRuleExample badExample = new MuleRuleExample();
		badExample.setType("noncompliant");
		badExample.setCode("<flow name=\"bad-flow\"/>");

		rule.setExamples(Arrays.asList(goodExample, badExample));
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert - examples are added to description
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		RulesDefinition.Rule sonarRule = repository.rule("rule-key");
		String htmlDescription = sonarRule.htmlDescription();

		assertThat(htmlDescription).contains("Compliant Solution");
		assertThat(htmlDescription).contains("<flow name=\"good-flow\"/>");
		assertThat(htmlDescription).contains("Noncompliant Code Example");
		assertThat(htmlDescription).contains("<flow name=\"bad-flow\"/>");
	}

	@Test
	void testDefine_HandlesEmptyRuleList() {
		// Arrange
		when(mockRuleLoader.loadRules()).thenReturn(Collections.emptyList());

		// Act
		rulesDefinition.define(context);

		// Assert - repository exists but only has template rule (empty rules were ignored)
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository).isNotNull();
		assertThat(repository.rules()).hasSize(4); // 3 complexity rules + 1 template
		assertThat(repository.rule(MuleRulesDefinition.RULE_TEMPLATE_KEY)).isNotNull();
	}

	@Test
	void testDefine_HandlesNullRuleFields() {
		// Arrange
		MuleRule rule = new MuleRule();
		rule.setKey("minimal-rule");
		// All other fields are null
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act - should not throw exception
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		// Rule should be skipped due to missing name, so only template remains
		assertThat(repository.rule("minimal-rule")).isNull();
		assertThat(repository.rule(MuleRulesDefinition.RULE_TEMPLATE_KEY)).isNotNull();
	}

	@Test
	void testDefine_SetsRuleStatus() {
		// Arrange
		MuleRule rule = createSampleRule("rule-key");
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("rule-key").status()).isEqualTo(RuleStatus.READY);
	}

	@Test
	void testDefine_MultipleRulesWithDifferentSeverities() {
		// Arrange
		MuleRule rule1 = createSampleRule("critical-rule");
		rule1.setSeverity("CRITICAL");

		MuleRule rule2 = createSampleRule("info-rule");
		rule2.setSeverity("INFO");

		when(mockRuleLoader.loadRules()).thenReturn(Arrays.asList(rule1, rule2));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("critical-rule").severity()).isEqualTo(Severity.CRITICAL);
		assertThat(repository.rule("info-rule").severity()).isEqualTo(Severity.INFO);
	}

	@Test
	void testDefine_RegistersIntegerParameterWithDefaultValue() {
		// Arrange
		MuleRule rule = createSampleRule("param-rule");
		MuleRuleParameter param = new MuleRuleParameter();
		param.setKey("max");
		param.setName("Max items");
		param.setType("INTEGER");
		param.setDefaultValue("15");
		param.setDescription("Maximum allowed items");
		rule.setParameters(List.of(param));
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		RulesDefinition.Rule sonarRule = repository.rule("param-rule");
		assertThat(sonarRule).isNotNull();
		assertThat(sonarRule.param("max")).isNotNull();
		assertThat(sonarRule.param("max").defaultValue()).isEqualTo("15");
		assertThat(sonarRule.param("max").type()).isEqualTo(org.sonar.api.server.rule.RuleParamType.INTEGER);
	}

	@Test
	void testDefine_SkipsRuleRequiringProjectModel() {
		// Arrange
		MuleRule rule = createSampleRule("phase2-rule");
		rule.setRequires(Set.of("project-model"));
		when(mockRuleLoader.loadRules()).thenReturn(Collections.singletonList(rule));

		// Act
		rulesDefinition.define(context);

		// Assert — rule must be absent; only the template remains
		Repository repository = context.repository(MuleRulesDefinition.MULE_REPOSITORY_KEY);
		assertThat(repository.rule("phase2-rule")).isNull();
		assertThat(repository.rules()).hasSize(4); // 3 complexity rules + 1 template
	}

	// ========== Helper Methods ==========

	/**
	 * Creates a sample MuleRule with basic required fields.
	 */
	private MuleRule createSampleRule(String key) {
		MuleRule rule = new MuleRule();
		rule.setKey(key);
		rule.setName("Sample Rule");
		rule.setDescription("Sample description");
		rule.setSeverity("MAJOR");
		rule.setType("CODE_SMELL");
		rule.setScope("file");
		rule.setXpath("boolean(//mule:flow)");
		rule.setRemediationEffort("15min");
		return rule;
	}
}
