package com.sonar.plugins.mulesoft.metrics;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.assertj.core.api.Assertions.assertThat;

class ComplexityComputerTest {

    private static Element flow(String xml) throws Exception {
        Document doc = new SAXBuilder().build(new StringReader(
                "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\">" + xml + "</mule>"));
        return doc.getRootElement().getChildren().get(0);
    }

    @Test
    void empty_flow_has_complexity_1() throws Exception {
        Element f = flow("<flow name=\"f\"/>");
        ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(f);
        assertThat(c.cyclomatic()).isEqualTo(1);
        assertThat(c.cognitive()).isZero();
        assertThat(c.maxNestingDepth()).isZero();
    }

    @Test
    void choice_with_two_branches_adds_two_cyclomatic() throws Exception {
        Element f = flow("""
                <flow name="f">
                  <choice>
                    <when expression="#[true]"/>
                    <when expression="#[false]"/>
                    <otherwise/>
                  </choice>
                </flow>
                """);
        ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(f);
        assertThat(c.cyclomatic()).isEqualTo(3); // 1 base + 2 when
    }

    @Test
    void foreach_adds_one_and_nesting_depth_one() throws Exception {
        Element f = flow("""
                <flow name="f">
                  <foreach collection="#[payload]"/>
                </flow>
                """);
        ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(f);
        assertThat(c.cyclomatic()).isEqualTo(2);
        assertThat(c.maxNestingDepth()).isEqualTo(1);
    }

    @Test
    void cognitive_weights_nesting_more_heavily() throws Exception {
        Element f = flow("""
                <flow name="f">
                  <foreach collection="#[payload]">
                    <choice>
                      <when expression="#[true]"/>
                      <otherwise/>
                    </choice>
                  </foreach>
                </flow>
                """);
        ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(f);
        assertThat(c.cyclomatic()).isEqualTo(3); // 1 + foreach + when
        assertThat(c.cognitive()).isEqualTo(3);  // foreach at depth 0 = 1, when at depth 1 = 2
        assertThat(c.maxNestingDepth()).isEqualTo(2);
    }

    @Test
    void scatter_gather_routes_add_one_each() throws Exception {
        Element f = flow("""
                <flow name="f">
                  <scatter-gather>
                    <route/>
                    <route/>
                    <route/>
                  </scatter-gather>
                </flow>
                """);
        ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(f);
        assertThat(c.cyclomatic()).isEqualTo(4); // 1 + 3 routes
    }
}
