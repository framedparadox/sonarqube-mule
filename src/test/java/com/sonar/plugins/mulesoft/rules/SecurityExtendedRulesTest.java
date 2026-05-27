package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XPath rule validation tests for the 'security' category rule not covered by SecurityRulesTest:
 * tls-context-revocation-check.
 */
class SecurityExtendedRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:tls=\"http://www.mulesoft.org/schema/mule/tls\"";

    // ── tls-context-revocation-check ────────────────────────────────────────

    @Test
    void tls_revocation_check_passes_when_no_tls_context() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/tls:context)=0\n" +
            "or\n" +
            "count(//mule:mule/tls:context[not(tls:revocation-check)])=0"));
    }

    @Test
    void tls_revocation_check_passes_when_revocation_check_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<tls:context name='tls-context'>" +
            "<tls:trust-store path='${tls.truststore.path}' password='${tls.truststore.password}'/>" +
            "<tls:revocation-check>" +
            "<tls:crl-file path='${tls.crl.path}'/>" +
            "</tls:revocation-check>" +
            "</tls:context>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/tls:context)=0\n" +
            "or\n" +
            "count(//mule:mule/tls:context[not(tls:revocation-check)])=0"));
    }

    @Test
    void tls_revocation_check_fails_when_no_revocation_check() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<tls:context name='tls-context'>" +
            "<tls:trust-store path='${tls.truststore.path}' password='${tls.truststore.password}'/>" +
            "<tls:key-store path='${tls.keystore.path}' keyPassword='${tls.keystore.keyPassword}'" +
            " password='${tls.keystore.password}'/>" +
            "</tls:context>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/tls:context)=0\n" +
            "or\n" +
            "count(//mule:mule/tls:context[not(tls:revocation-check)])=0"));
    }

    @Test
    void tls_revocation_check_passes_when_all_tls_contexts_have_revocation() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<tls:context name='tls-context-1'>" +
            "<tls:revocation-check><tls:standard-revocation-check/></tls:revocation-check>" +
            "</tls:context>" +
            "<tls:context name='tls-context-2'>" +
            "<tls:revocation-check><tls:crl-file path='crl.pem'/></tls:revocation-check>" +
            "</tls:context>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/tls:context)=0\n" +
            "or\n" +
            "count(//mule:mule/tls:context[not(tls:revocation-check)])=0"));
    }

    @Test
    void tls_revocation_check_fails_when_one_context_missing_revocation() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<tls:context name='tls-context-1'>" +
            "<tls:revocation-check><tls:standard-revocation-check/></tls:revocation-check>" +
            "</tls:context>" +
            "<tls:context name='tls-context-2'>" +
            "<tls:trust-store path='ts.jks' password='${pw}'/>" +
            "</tls:context>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/tls:context)=0\n" +
            "or\n" +
            "count(//mule:mule/tls:context[not(tls:revocation-check)])=0"));
    }
}
