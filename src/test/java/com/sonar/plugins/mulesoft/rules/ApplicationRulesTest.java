package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XPath rule validation tests for the 'application' category rules.
 * Each test verifies the exact XPath expression from mulesoft-ruleset.yaml.
 */
class ApplicationRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:apikit=\"http://www.mulesoft.org/schema/mule/mule-apikit\"" +
        " xmlns:api-gateway=\"http://www.mulesoft.org/schema/mule/api-gateway\"";

    // ── apikit-config-required ──────────────────────────────────────────────

    @Test
    void apikit_config_required_passes_when_apikit_config_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<apikit:config name='my-config' raml='api.raml'/>" +
            "<flow name='main-flow'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/apikit:config)>0"));
    }

    @Test
    void apikit_config_required_fails_when_no_apikit_config() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='main-flow'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/apikit:config)>0"));
    }

    // ── apikit-exception-strategy ───────────────────────────────────────────

    @Test
    void apikit_exception_strategy_passes_when_apikit_error_handler_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<error-handler name='global-error-handler'>" +
            "<on-error-propagate type='APIKIT:BAD_REQUEST'>" +
            "<set-variable variableName='httpStatus' value='400'/>" +
            "</on-error-propagate>" +
            "</error-handler>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "starts-with(//mule:on-error-propagate/@type,'APIKIT')"));
    }

    @Test
    void apikit_exception_strategy_fails_when_no_apikit_error_handler() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<error-handler name='global-error-handler'>" +
            "<on-error-propagate type='ANY'/>" +
            "</error-handler>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "starts-with(//mule:on-error-propagate/@type,'APIKIT')"));
    }

    @Test
    void apikit_exception_strategy_fails_when_no_error_handler_at_all() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "starts-with(//mule:on-error-propagate/@type,'APIKIT')"));
    }
}
