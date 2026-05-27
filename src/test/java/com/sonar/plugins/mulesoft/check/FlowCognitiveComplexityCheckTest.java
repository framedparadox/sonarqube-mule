package com.sonar.plugins.mulesoft.check;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowCognitiveComplexityCheckTest {

    private static final Namespace MULE = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");

    @Test
    void rule_key_is_correct() {
        assertEquals("flow-cognitive-complexity", new FlowCognitiveComplexityCheck().ruleKey());
    }

    @Test
    void scope_is_file() {
        assertEquals(com.sonar.plugins.mulesoft.util.RuleScope.FILE_SCOPE, new FlowCognitiveComplexityCheck().scope());
    }

    @Test
    void no_issue_for_simple_flow() {
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "f");
        flow.addContent(new Element("logger", MULE));
        root.addContent(flow);
        Document doc = new Document(root);

        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "15"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowCognitiveComplexityCheck().check(ctx);
        assertEquals(0, count.get());
    }

    @Test
    void issue_raised_when_cognitive_exceeds_threshold() {
        // 16 "foreach" elements at depth 0 → cognitive = 16 * (1+0) = 16 > 15
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "f");
        for (int i = 0; i < 16; i++) {
            flow.addContent(new Element("foreach", MULE));
        }
        root.addContent(flow);
        Document doc = new Document(root);

        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "15"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowCognitiveComplexityCheck().check(ctx);
        assertEquals(1, count.get());
    }
}
