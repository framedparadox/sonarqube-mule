package com.sonar.plugins.mulesoft.testing;

import com.sonar.plugins.mulesoft.util.XmlParserFactory;
import com.sonar.plugins.mulesoft.xpath.XPathProcessor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Test utility for evaluating XPath rules against XML fixture strings.
 * A rule PASSES when the XPath expression evaluates to Boolean true or
 * returns a non-empty node-set.
 */
public final class RuleTestHarness {

    private static final XPathProcessor XPATH = new XPathProcessor();

    private RuleTestHarness() {}

    /**
     * Evaluates an XPath expression against an XML string.
     *
     * @param xml   Mule XML configuration as string
     * @param xpath XPath expression (returns true when rule PASSES, false when it FAILS)
     * @return true if the rule passes
     */
    public static boolean evaluate(String xml, String xpath) throws Exception {
        SAXBuilder sax = XmlParserFactory.createSecureBuilder();
        Document doc = sax.build(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element root = doc.getRootElement();

        // Try Boolean evaluation first (for count()=0 style expressions)
        Object boolResult = XPATH.evaluate(xpath, root, Object.class);
        if (boolResult instanceof Boolean) {
            return (Boolean) boolResult;
        }

        // For node-set results, check non-empty
        List<Element> nodeResult = XPATH.evaluateList(xpath, root);
        if (!nodeResult.isEmpty()) {
            return true;
        }

        // If Object result is non-null (e.g., a string or number), treat as truthy
        return boolResult != null;
    }
}
