package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObservabilityRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\"";

    @Test
    void logger_category_required_passes_when_category_set() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger category='com.example.api' message='Done'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[not(@category) or @category=''])=0"));
    }

    @Test
    void logger_category_required_fails_when_no_category() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger message='Done'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//mule:logger[not(@category) or @category=''])=0"));
    }

    @Test
    void doc_name_non_empty_passes_when_all_have_names() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger doc:name='Log Request' message='x'/></flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//*[@doc:name=''])=0"));
    }

    @Test
    void doc_name_non_empty_fails_when_empty_doc_name() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<logger doc:name='' message='x'/></flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule//*[@doc:name=''])=0"));
    }
}
