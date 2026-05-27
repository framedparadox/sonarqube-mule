package com.sonar.plugins.mulesoft.check;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowNestingDepthCheckTest {

    private static final Namespace MULE = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");

    @Test
    void rule_key_is_correct() {
        assertEquals("flow-nesting-depth", new FlowNestingDepthCheck().ruleKey());
    }

    @Test
    void scope_is_file() {
        assertEquals(com.sonar.plugins.mulesoft.util.RuleScope.FILE_SCOPE, new FlowNestingDepthCheck().scope());
    }

    @Test
    void no_issue_for_shallow_flow() {
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "f");
        // "when" at depth 0 → maxNestingDepth = 1 <= 3
        flow.addContent(new Element("when", MULE));
        root.addContent(flow);
        Document doc = new Document(root);

        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "3"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowNestingDepthCheck().check(ctx);
        assertEquals(0, count.get());
    }

    @Test
    void issue_raised_for_deep_nesting() {
        // Build 4 levels deep: foreach > foreach > foreach > foreach → maxNestingDepth = 4 > 3
        Element root = new Element("mule", MULE);
        Element flow = new Element("flow", MULE).setAttribute("name", "f");
        Element level1 = new Element("foreach", MULE);
        Element level2 = new Element("foreach", MULE);
        Element level3 = new Element("foreach", MULE);
        Element level4 = new Element("foreach", MULE);
        level3.addContent(level4);
        level2.addContent(level3);
        level1.addContent(level2);
        flow.addContent(level1);
        root.addContent(flow);
        Document doc = new Document(root);

        AtomicInteger count = new AtomicInteger();
        MuleCheckContext ctx = mock(MuleCheckContext.class);
        when(ctx.document()).thenReturn(doc);
        when(ctx.parameters()).thenReturn(Map.of("threshold", "3"));
        doAnswer(inv -> { count.incrementAndGet(); return null; }).when(ctx).reportFileIssue(anyString());
        new FlowNestingDepthCheck().check(ctx);
        assertEquals(1, count.get());
    }
}
