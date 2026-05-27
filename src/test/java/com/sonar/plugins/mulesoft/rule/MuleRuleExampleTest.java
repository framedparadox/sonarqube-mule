package com.sonar.plugins.mulesoft.rule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sonar.plugins.mulesoft.rule.model.MuleRuleExample;

/**
 * Tests for MuleRuleExample - code examples in rules.
 */
class MuleRuleExampleTest {

	@Test
	void testIsCompliant_WhenTypeIsCompliant() {
		MuleRuleExample example = new MuleRuleExample();
		example.setType("compliant");
		
		assertThat(example.isCompliant()).isTrue();
		assertThat(example.isNoncompliant()).isFalse();
	}

	@Test
	void testIsNoncompliant_WhenTypeIsNoncompliant() {
		MuleRuleExample example = new MuleRuleExample();
		example.setType("noncompliant");
		
		assertThat(example.isNoncompliant()).isTrue();
		assertThat(example.isCompliant()).isFalse();
	}

	@Test
	void testIsCompliant_CaseInsensitive() {
		MuleRuleExample example = new MuleRuleExample();
		
		example.setType("COMPLIANT");
		assertThat(example.isCompliant()).isTrue();
		
		example.setType("Compliant");
		assertThat(example.isCompliant()).isTrue();
	}

	@Test
	void testIsNoncompliant_CaseInsensitive() {
		MuleRuleExample example = new MuleRuleExample();
		
		example.setType("NONCOMPLIANT");
		assertThat(example.isNoncompliant()).isTrue();
		
		example.setType("NonCompliant");
		assertThat(example.isNoncompliant()).isTrue();
	}

	@Test
	void testSettersAndGetters() {
		MuleRuleExample example = new MuleRuleExample();
		
		example.setType("compliant");
		example.setCode("<flow name=\"test\"/>");
		example.setDescription("This is a good example");
		
		assertThat(example.getType()).isEqualTo("compliant");
		assertThat(example.getCode()).isEqualTo("<flow name=\"test\"/>");
		assertThat(example.getDescription()).isEqualTo("This is a good example");
	}

	@Test
	void testDefaultValues() {
		MuleRuleExample example = new MuleRuleExample();
		
		assertThat(example.getType()).isNull();
		assertThat(example.getCode()).isNull();
		assertThat(example.getDescription()).isNull();
	}

	@Test
	void testIsCompliant_WhenTypeIsNull() {
		MuleRuleExample example = new MuleRuleExample();
		example.setType(null);
		
		assertThat(example.isCompliant()).isFalse();
		assertThat(example.isNoncompliant()).isFalse();
	}

	@Test
	void testIsCompliant_WhenTypeIsEmpty() {
		MuleRuleExample example = new MuleRuleExample();
		example.setType("");
		
		assertThat(example.isCompliant()).isFalse();
		assertThat(example.isNoncompliant()).isFalse();
	}

	@Test
	void testIsCompliant_WhenTypeIsUnknown() {
		MuleRuleExample example = new MuleRuleExample();
		example.setType("unknown");
		
		assertThat(example.isCompliant()).isFalse();
		assertThat(example.isNoncompliant()).isFalse();
	}
}
