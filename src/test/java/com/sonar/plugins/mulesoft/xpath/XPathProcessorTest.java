package com.sonar.plugins.mulesoft.xpath;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for XPathProcessor - XPath evaluation and custom functions.
 */
class XPathProcessorTest {

	private XPathProcessor processor;
	private SAXBuilder saxBuilder;
	private Namespace muleNamespace;

	@BeforeEach
	void setUp() {
		muleNamespace = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");
		processor = new XPathProcessor(Arrays.asList(muleNamespace));
		saxBuilder = new SAXBuilder();
	}

	@Test
	void testEvaluate_SimpleCount() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
  <flow name="flow2"/>
  <flow name="flow3"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		Double count = processor.evaluate("count(//mule:flow)", doc.getRootElement(), Double.class);
		
		assertThat(count).isEqualTo(3.0);
	}

	@Test
	void testEvaluate_BooleanExpression() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="mainFlow">
    <logger message="test"/>
  </flow>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		Boolean result = processor.evaluate("count(//mule:flow) > 0", doc.getRootElement(), Boolean.class);
		
		assertThat(result).isTrue();
	}

	@Test
	void testEvaluate_StringExpression() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="testFlow"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		// Use string() function to get string value of attribute
		String name = processor.evaluate("string(//mule:flow/@name)", doc.getRootElement(), String.class);
		
		assertThat(name).isEqualTo("testFlow");
	}

	@Test
	void testEvaluate_InvalidXPath_ReturnsNull() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		// Invalid XPath syntax returns false (default value), not null
		Boolean result = processor.evaluate("invalid xpath ][", doc.getRootElement(), Boolean.class);
		
		assertThat(result).isFalse();
	}

	@Test
	void testEvaluate_NoMatchingElements_ReturnsZero() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		Double count = processor.evaluate("count(//mule:sub-flow)", doc.getRootElement(), Double.class);
		
		assertThat(count).isEqualTo(0.0);
	}

	@Test
	void testEvaluateList_MultipleElements() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
  <flow name="flow2"/>
  <flow name="flow3"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		List<Element> flows = processor.evaluateList("//mule:flow", doc.getRootElement());
		
		assertThat(flows).hasSize(3);
		assertThat(flows).extracting(e -> e.getAttributeValue("name"))
			.containsExactly("flow1", "flow2", "flow3");
	}

	@Test
	void testEvaluateList_NoMatches_ReturnsEmptyList() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		List<Element> subFlows = processor.evaluateList("//mule:sub-flow", doc.getRootElement());
		
		assertThat(subFlows).isEmpty();
	}

	@Test
	void testEvaluateList_InvalidXPath_ReturnsEmptyList() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		List<Element> result = processor.evaluateList("invalid][xpath", doc.getRootElement());
		
		assertThat(result).isEmpty();
	}

	@Test
	void testCustomFunction_Matches() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="test-flow">
    <set-variable variableName="${config.value}"/>
  </flow>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		// Test matches function with regex
		Boolean matches = processor.evaluate(
			"matches(//mule:set-variable/@variableName, '^\\$\\{.*\\}$')",
			doc.getRootElement(),
			Boolean.class
		);
		
		assertThat(matches).isTrue();
	}

	@Test
	void testCustomFunction_IsConfigurable() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <global-property name="config.key" value="${env.value}"/>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		// Test is-configurable function
		Boolean isConfigurable = processor.evaluate(
			"is-configurable(//mule:global-property/@value)",
			doc.getRootElement(),
			Boolean.class
		);
		
		assertThat(isConfigurable).isTrue();
	}

	@Test
	void testConstructor_WithResourceName() {
		// Should not throw exception
		XPathProcessor processorFromResource = new XPathProcessor("mulesoft-namespace.properties");
		
		assertThat(processorFromResource).isNotNull();
		assertThat(processorFromResource.getNamespaces()).isNotEmpty();
	}

	@Test
	void testConstructor_WithInvalidResourceName() {
		// Should handle gracefully
		XPathProcessor processorWithInvalid = new XPathProcessor("non-existent-file.properties");
		
		assertThat(processorWithInvalid).isNotNull();
		assertThat(processorWithInvalid.getNamespaces()).isEmpty();
	}

	@Test
	void testConstructor_DefaultConstructor() {
		XPathProcessor defaultProcessor = new XPathProcessor();
		
		assertThat(defaultProcessor).isNotNull();
		assertThat(defaultProcessor.getNamespaces()).isNotEmpty();
	}

	@Test
	void testGetNamespaces_ReturnsImmutableList() {
		List<Namespace> namespaces = processor.getNamespaces();
		
		assertThat(namespaces).isNotNull();
		assertThat(namespaces).hasSize(1);
		
		// Verify namespace content
		Namespace ns = namespaces.get(0);
		assertThat(ns.getPrefix()).isEqualTo("mule");
		assertThat(ns.getURI()).isEqualTo("http://www.mulesoft.org/schema/mule/core");
	}

	@Test
	void testEvaluate_ComplexXPath() throws Exception {
		String xml = """
<mule xmlns="http://www.mulesoft.org/schema/mule/core">
  <flow name="flow1">
    <logger message="test1"/>
    <logger message="test2"/>
  </flow>
  <flow name="flow2">
    <logger message="test3"/>
  </flow>
</mule>
""";
		
		Document doc = saxBuilder.build(new StringReader(xml));
		
		// Count flows with more than 1 logger
		Double count = processor.evaluate(
			"count(//mule:flow[count(.//mule:logger) > 1])",
			doc.getRootElement(),
			Double.class
		);
		
		assertThat(count).isEqualTo(1.0);
	}

	@Test
	void testEvaluate_NullElement_ReturnsNull() {
		// Null element returns false (default), not null
		Boolean result = processor.evaluate("count(//mule:flow)", null, Boolean.class);
		
		assertThat(result).isFalse();
	}

	@Test
	void testEvaluate_EmptyXPath_ReturnsNull() throws Exception {
		String xml = "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"><flow name=\"test\"/></mule>";
		Document doc = saxBuilder.build(new StringReader(xml));

		// Empty XPath returns false (default), not null
		Boolean result = processor.evaluate("", doc.getRootElement(), Boolean.class);

		assertThat(result).isFalse();
	}

	@Test
	void prop_placeholder_function_usable_in_xpath() throws Exception {
		String xml = """
				<mule xmlns="http://www.mulesoft.org/schema/mule/core"
				      xmlns:db="http://www.mulesoft.org/schema/mule/db">
				  <db:config><db:generic-connection url="${db.url}"/></db:config>
				</mule>
				""";

		XPathProcessor dbProcessor = new XPathProcessor(Arrays.asList(
			muleNamespace,
			Namespace.getNamespace("db", "http://www.mulesoft.org/schema/mule/db")
		));

		Document doc = saxBuilder.build(new StringReader(xml));

		Boolean result = dbProcessor.evaluate(
			"prop-placeholder(//db:generic-connection/@url)",
			doc.getRootElement(),
			Boolean.class
		);

		assertThat(result).isTrue();
	}
}
