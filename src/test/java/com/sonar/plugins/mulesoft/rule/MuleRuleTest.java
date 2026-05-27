package com.sonar.plugins.mulesoft.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import com.sonar.plugins.mulesoft.rule.model.MuleRuleExample;

/**
 * Tests for MuleRule - rule model and helper methods.
 */
class MuleRuleTest {

	@Test
	void testIsFileScope_WhenScopeIsFile() {
		MuleRule rule = new MuleRule();
		rule.setScope("file");

		assertThat(rule.isFileScope()).isTrue();
		assertThat(rule.isApplicationScope()).isFalse();
	}

	@Test
	void testIsApplicationScope_WhenScopeIsApplication() {
		MuleRule rule = new MuleRule();
		rule.setScope("application");

		assertThat(rule.isApplicationScope()).isTrue();
		assertThat(rule.isFileScope()).isFalse();
	}

	@Test
	void testIsFileScope_WhenScopeIsNull() {
		MuleRule rule = new MuleRule();
		rule.setScope(null);

		// Default should be file scope
		assertThat(rule.isFileScope()).isTrue();
		assertThat(rule.isApplicationScope()).isFalse();
	}

	@Test
	void testIsFileScope_WhenScopeIsEmpty() {
		MuleRule rule = new MuleRule();
		rule.setScope("");

		// Empty string is not treated as file scope (only null or "file" are)
		assertThat(rule.isFileScope()).isFalse();
		assertThat(rule.isApplicationScope()).isFalse();
	}

	@Test
	void testIsFileScope_CaseInsensitive() {
		MuleRule rule = new MuleRule();
		rule.setScope("FILE");

		assertThat(rule.isFileScope()).isTrue();

		rule.setScope("File");
		assertThat(rule.isFileScope()).isTrue();
	}

	@Test
	void testIsApplicationScope_CaseInsensitive() {
		MuleRule rule = new MuleRule();
		rule.setScope("APPLICATION");

		assertThat(rule.isApplicationScope()).isTrue();

		rule.setScope("Application");
		assertThat(rule.isApplicationScope()).isTrue();
	}

	@Test
	void testSettersAndGetters() {
		MuleRule rule = new MuleRule();

		rule.setKey("TEST-001");
		rule.setName("Test Rule");
		rule.setDescription("Test description");
		rule.setSeverity("MAJOR");
		rule.setType("CODE_SMELL");
		rule.setCategory("Best Practices");
		rule.setScope("file");
		rule.setXpath("count(//mule:flow) > 0");
		rule.setXpathLocationHint("//mule:flow[1]");
		rule.setTags(Arrays.asList("test", "mule"));
		rule.setHtmlDescription("<p>HTML description</p>");

		assertThat(rule.getKey()).isEqualTo("TEST-001");
		assertThat(rule.getName()).isEqualTo("Test Rule");
		assertThat(rule.getDescription()).isEqualTo("Test description");
		assertThat(rule.getSeverity()).isEqualTo("MAJOR");
		assertThat(rule.getType()).isEqualTo("CODE_SMELL");
		assertThat(rule.getCategory()).isEqualTo("Best Practices");
		assertThat(rule.getScope()).isEqualTo("file");
		assertThat(rule.getXpath()).isEqualTo("count(//mule:flow) > 0");
		assertThat(rule.getXpathLocationHint()).isEqualTo("//mule:flow[1]");
		assertThat(rule.getTags()).containsExactly("test", "mule");
		assertThat(rule.getHtmlDescription()).isEqualTo("<p>HTML description</p>");
	}

	@Test
	void testDefaultValues() {
		MuleRule rule = new MuleRule();

		assertThat(rule.getKey()).isNull();
		assertThat(rule.getName()).isNull();
		assertThat(rule.getDescription()).isNull();
		assertThat(rule.getSeverity()).isNull();
		assertThat(rule.getType()).isNull();
		assertThat(rule.getCategory()).isNull();
		assertThat(rule.getScope()).isNull();
		assertThat(rule.getXpath()).isNull();
		assertThat(rule.getXpathLocationHint()).isNull();
		// Tags and examples are initialized to empty lists
		assertThat(rule.getTags()).isEmpty();
		assertThat(rule.getHtmlDescription()).isNull();
	}

	@Test
	void testExamples() {
		MuleRule rule = new MuleRule();

		MuleRuleExample example1 = new MuleRuleExample();
		example1.setType("compliant");
		example1.setCode("<flow name=\"good\"/>");

		MuleRuleExample example2 = new MuleRuleExample();
		example2.setType("noncompliant");
		example2.setCode("<flow/>");

		rule.setExamples(Arrays.asList(example1, example2));

		assertThat(rule.getExamples()).hasSize(2);
		assertThat(rule.getExamples().get(0).isCompliant()).isTrue();
		assertThat(rule.getExamples().get(1).isNoncompliant()).isTrue();
	}
}
