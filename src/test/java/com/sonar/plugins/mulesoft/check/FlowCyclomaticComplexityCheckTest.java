package com.sonar.plugins.mulesoft.check;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowCyclomaticComplexityCheckTest {

    private static final Namespace MULE = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");

    private Document buildDoc(int choiceCount) {
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "test-flow");
        for (int i = 0; i < choiceCount; i++) {
            flow.addContent(new Element("choice", MULE));
        }
        root.addContent(flow);
        return new Document(root);
    }

    @Test
    void rule_key_is_correct() {
        assertEquals("flow-cyclomatic-complexity", new FlowCyclomaticComplexityCheck().ruleKey());
    }

    @Test
    void scope_is_file() {
        assertEquals(com.sonar.plugins.mulesoft.util.RuleScope.FILE_SCOPE, new FlowCyclomaticComplexityCheck().scope());
    }

    @Test
    void no_issue_when_complexity_below_threshold() {
        Document doc = buildDoc(3); // cyclomatic = 1 + 0 branching children = 1 (choice is CONTAINER)
        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "10"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowCyclomaticComplexityCheck().check(ctx);
        assertEquals(0, count.get());
    }

    @Test
    void issue_raised_when_complexity_exceeds_threshold() {
        // Add 11 "when" elements (branching) to get cyclomatic = 1 + 11 = 12 > 10
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "test-flow");
        for (int i = 0; i < 11; i++) {
            flow.addContent(new Element("when", MULE));
        }
        root.addContent(flow);
        Document doc = new Document(root);

        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "10"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowCyclomaticComplexityCheck().check(ctx);
        assertEquals(1, count.get());
    }
}
