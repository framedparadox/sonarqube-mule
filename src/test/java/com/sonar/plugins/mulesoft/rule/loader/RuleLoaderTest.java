package com.sonar.plugins.mulesoft.rule.loader;

import com.sonar.plugins.mulesoft.rule.model.MuleRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleLoaderTest {

    @Test
    void returns_null_html_description_when_no_html_file_exists() throws Exception {
        String yaml = "rulesets:\n  - category: flows\n    rules:\n      - ruleKey: no-html-for-this-key\n        name: Test Rule\n        xpath: //test\n";
        List<MuleRule> rules = new RuleLoader().loadFromYamlString(yaml);
        assertNull(rules.get(0).getHtmlDescription());
    }

    @Test
    void loads_html_description_when_html_file_exists() throws Exception {
        String yaml = "rulesets:\n  - category: flows\n    rules:\n      - ruleKey: html-rule-for-test\n        name: Test Rule\n        xpath: //test\n";
        List<MuleRule> rules = new RuleLoader().loadFromYamlString(yaml);
        assertNotNull(rules.get(0).getHtmlDescription());
        assertTrue(rules.get(0).getHtmlDescription().contains("<h2>"));
    }
}
