package com.sonar.plugins.mulesoft.testing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuleTestHarnessTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"";

    private static final String COMPLIANT_XML =
        "<mule" + MULE_NS + "><flow name='my-flow'/></mule>";

    private static final String NONCOMPLIANT_XML =
        "<mule" + MULE_NS + "><flow name='MY_FLOW'/></mule>";

    @Test
    void passes_when_xpath_returns_true() throws Exception {
        // Rule: all flow names must be lowercase-only
        boolean result = RuleTestHarness.evaluate(
            COMPLIANT_XML,
            "count(//mule:mule/mule:flow[not(matches(@name,'^[a-z-]+$'))])=0"
        );
        assertTrue(result);
    }

    @Test
    void fails_when_xpath_returns_false() throws Exception {
        boolean result = RuleTestHarness.evaluate(
            NONCOMPLIANT_XML,
            "count(//mule:mule/mule:flow[not(matches(@name,'^[a-z-]+$'))])=0"
        );
        assertFalse(result);
    }
}
