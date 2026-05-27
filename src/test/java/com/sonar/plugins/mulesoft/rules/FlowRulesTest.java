package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XPath rule validation tests for the 'flows' category rules.
 * Each test verifies the exact XPath expression from mulesoft-ruleset.yaml.
 */
class FlowRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\"";

    // ── count-limit ─────────────────────────────────────────────────────────

    @Test
    void count_limit_passes_when_fewer_than_10_flows() throws Exception {
        StringBuilder xml = new StringBuilder("<mule" + MULE_NS + ">");
        for (int i = 1; i <= 9; i++) {
            xml.append("<flow name='flow-").append(i).append("'/>");
        }
        xml.append("</mule>");
        assertTrue(RuleTestHarness.evaluate(xml.toString(),
            "not(count(//mule:mule/mule:flow)>=10)"));
    }

    @Test
    void count_limit_fails_when_10_or_more_flows() throws Exception {
        StringBuilder xml = new StringBuilder("<mule" + MULE_NS + ">");
        for (int i = 1; i <= 10; i++) {
            xml.append("<flow name='flow-").append(i).append("'/>");
        }
        xml.append("</mule>");
        assertFalse(RuleTestHarness.evaluate(xml.toString(),
            "not(count(//mule:mule/mule:flow)>=10)"));
    }

    @Test
    void count_limit_passes_when_no_flows() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "not(count(//mule:mule/mule:flow)>=10)"));
    }

    // ── subflow-count-limit ─────────────────────────────────────────────────

    @Test
    void subflow_count_limit_passes_when_fewer_than_5_subflows() throws Exception {
        StringBuilder xml = new StringBuilder("<mule" + MULE_NS + ">");
        for (int i = 1; i <= 4; i++) {
            xml.append("<sub-flow name='sub-flow-").append(i).append("'/>");
        }
        xml.append("</mule>");
        assertTrue(RuleTestHarness.evaluate(xml.toString(),
            "not(count(//mule:mule/mule:sub-flow)>=5)"));
    }

    @Test
    void subflow_count_limit_fails_when_5_or_more_subflows() throws Exception {
        StringBuilder xml = new StringBuilder("<mule" + MULE_NS + ">");
        for (int i = 1; i <= 5; i++) {
            xml.append("<sub-flow name='sub-flow-").append(i).append("'/>");
        }
        xml.append("</mule>");
        assertFalse(RuleTestHarness.evaluate(xml.toString(),
            "not(count(//mule:mule/mule:sub-flow)>=5)"));
    }

    // ── naming-convention ───────────────────────────────────────────────────

    @Test
    void naming_convention_passes_when_no_flows() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0 or\n" +
            "matches(//mule:mule/mule:flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    @Test
    void naming_convention_passes_with_valid_kebab_case_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='get-customer-details'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0 or\n" +
            "matches(//mule:mule/mule:flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    @Test
    void naming_convention_fails_with_uppercase_flow_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='GetCustomerDetails'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0 or\n" +
            "matches(//mule:mule/mule:flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    @Test
    void naming_convention_fails_with_underscore_flow_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='get_customer_details'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0 or\n" +
            "matches(//mule:mule/mule:flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    // ── subflow-naming-convention ────────────────────────────────────────────

    @Test
    void subflow_naming_convention_passes_when_no_subflows() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:sub-flow)=0 or\n" +
            "matches(//mule:mule/mule:sub-flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    @Test
    void subflow_naming_convention_passes_with_valid_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><sub-flow name='validate-input'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:sub-flow)=0 or\n" +
            "matches(//mule:mule/mule:sub-flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    @Test
    void subflow_naming_convention_fails_with_camel_case_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><sub-flow name='validateInput'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:sub-flow)=0 or\n" +
            "matches(//mule:mule/mule:sub-flow/@name, '^[a-z:\\{}]+(-[a-z]+)*$')"));
    }

    // ── encryption-key-logging-forbidden ────────────────────────────────────

    @Test
    void encryption_key_logging_forbidden_passes_when_no_key_in_logger() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger message='Processing request'/>" +
            "</flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:logger[contains(@message,'${mule.key}')])" +
            "=0 and count(//mule:mule/mule:sub-flow/mule:logger[contains(@message,'${mule.key}')])=0"));
    }

    @Test
    void encryption_key_logging_forbidden_fails_when_key_logged_in_flow() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger message='Key: ${mule.key}'/>" +
            "</flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:logger[contains(@message,'${mule.key}')])" +
            "=0 and count(//mule:mule/mule:sub-flow/mule:logger[contains(@message,'${mule.key}')])=0"));
    }

    @Test
    void encryption_key_logging_forbidden_fails_when_key_logged_in_subflow() throws Exception {
        String xml = "<mule" + MULE_NS + "><sub-flow name='s'>" +
            "<logger message='Key is ${mule.key}'/>" +
            "</sub-flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:logger[contains(@message,'${mule.key}')])" +
            "=0 and count(//mule:mule/mule:sub-flow/mule:logger[contains(@message,'${mule.key}')])=0"));
    }

    // ── set-variable-doc-name ────────────────────────────────────────────────

    @Test
    void set_variable_doc_name_passes_when_no_set_variables() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:set-variable)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow/mule:set-variable/@doc:name = //mule:mule/mule:flow/mule:set-variable/@variableName"));
    }

    @Test
    void set_variable_doc_name_passes_when_doc_name_matches_variable_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<set-variable doc:name='customerId' variableName='customerId' value='123'/>" +
            "</flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:set-variable)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow/mule:set-variable/@doc:name = //mule:mule/mule:flow/mule:set-variable/@variableName"));
    }

    @Test
    void set_variable_doc_name_fails_when_doc_name_differs_from_variable_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<set-variable doc:name='Set Customer ID' variableName='customerId' value='123'/>" +
            "</flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/mule:set-variable)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow/mule:set-variable/@doc:name = //mule:mule/mule:flow/mule:set-variable/@variableName"));
    }

    // ── healthcheck-flow-required ────────────────────────────────────────────

    @Test
    void healthcheck_flow_required_passes_when_no_flows() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow[contains(@name,'healthcheck')]"));
    }

    @Test
    void healthcheck_flow_required_passes_when_healthcheck_flow_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<flow name='get-healthcheck'/>" +
            "<flow name='get-customers'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow[contains(@name,'healthcheck')]"));
    }

    @Test
    void healthcheck_flow_required_fails_when_no_healthcheck_flow() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<flow name='get-customers'/>" +
            "<flow name='post-orders'/>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow)=0\n" +
            "or\n" +
            "//mule:mule/mule:flow[contains(@name,'healthcheck')]"));
    }
}
