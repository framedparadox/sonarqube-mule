package com.sonar.plugins.mulesoft.sensor;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CpdXmlTokenizerTest {

    @Test
    void emits_one_token_per_element_excluding_doc_attrs() throws Exception {
        Document doc = new SAXBuilder().build(new StringReader("""
                <mule xmlns="http://www.mulesoft.org/schema/mule/core"
                      xmlns:doc="http://www.mulesoft.org/schema/mule/documentation">
                  <flow name="f" doc:id="abc-123" doc:name="My Flow">
                    <logger level="INFO" message="hi"/>
                  </flow>
                </mule>
                """));

        List<CpdXmlTokenizer.Token> tokens = new CpdXmlTokenizer(true).tokenize(doc);

        assertThat(tokens).hasSize(3); // mule, flow, logger
        assertThat(tokens.get(1).text()).contains("flow");
        assertThat(tokens.get(1).text()).contains("name=f");
        assertThat(tokens.get(1).text()).doesNotContain("doc:id");
        assertThat(tokens.get(1).text()).doesNotContain("doc:name");
        assertThat(tokens.get(2).text()).contains("logger");
        assertThat(tokens.get(2).text()).contains("level=INFO");
    }

    @Test
    void when_doc_excluded_is_false_keeps_doc_attrs() throws Exception {
        Document doc = new SAXBuilder().build(new StringReader("""
                <mule xmlns:doc="http://www.mulesoft.org/schema/mule/documentation">
                  <flow doc:id="abc"/>
                </mule>
                """));
        List<CpdXmlTokenizer.Token> tokens = new CpdXmlTokenizer(false).tokenize(doc);
        assertThat(tokens.get(1).text()).contains("doc:id=abc");
    }
}
