package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:http=\"http://www.mulesoft.org/schema/mule/http\"" +
        " xmlns:tls=\"http://www.mulesoft.org/schema/mule/tls\"";

    @Test
    void set_payload_no_inline_secret_passes_when_no_secrets() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<set-payload value='Hello World'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:set-payload[contains(@value,'password') or contains(@value,'secret') or contains(@value,'key')])=0"));
    }

    @Test
    void set_payload_no_inline_secret_fails_when_secret_present() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<set-payload value='my-password-123'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:set-payload[contains(@value,'password') or contains(@value,'secret') or contains(@value,'key')])=0"));
    }

    @Test
    void logger_no_payload_at_info_passes_when_no_payload_logged() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger level='INFO' message='Done'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[@level='INFO' and contains(@message,'#[payload')])=0"));
    }

    @Test
    void logger_no_payload_at_info_fails_when_payload_logged() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger level='INFO' message='Body: #[payload]'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[@level='INFO' and contains(@message,'#[payload')])=0"));
    }

    @Test
    void logger_no_auth_header_passes_when_not_logged() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger level='INFO' message='Request received'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[contains(@message,'Authorization') or contains(@message,'authorization')])=0"));
    }

    @Test
    void logger_no_auth_header_fails_when_auth_header_logged() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger message='Token: #[attributes.headers.Authorization]'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[contains(@message,'Authorization') or contains(@message,'authorization')])=0"));
    }

    @Test
    void http_listener_no_wildcard_passes_when_specific_path() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<http:listener path='/api/v1/customers' config-ref='cfg'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//http:listener[@path='/*' or @path='*'])=0"));
    }

    @Test
    void http_listener_no_wildcard_fails_on_wildcard_path() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<http:listener path='/*' config-ref='cfg'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//http:listener[@path='/*' or @path='*'])=0"));
    }
}
