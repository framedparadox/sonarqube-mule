package com.sonar.plugins.mulesoft.check;

import com.sonar.plugins.mulesoft.metrics.ComplexityComputer;
import com.sonar.plugins.mulesoft.util.RuleScope;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.List;

public class FlowCognitiveComplexityCheck implements MuleCheck {

    private static final Namespace MULE = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");
    private static final int DEFAULT_THRESHOLD = 15;

    @Override
    public String ruleKey() { return "flow-cognitive-complexity"; }

    @Override
    public RuleScope scope() { return RuleScope.FILE_SCOPE; }

    @Override
    public void check(MuleCheckContext ctx) {
        Document doc = ctx.document();
        if (doc == null) return;
        int threshold = parseThreshold(ctx, DEFAULT_THRESHOLD);
        List<Element> flows = doc.getRootElement().getChildren("flow", MULE);
        for (Element flow : flows) {
            ComplexityComputer.FlowComplexity c = ComplexityComputer.compute(flow);
            if (c.cognitive() > threshold) {
                ctx.reportFileIssue(String.format(
                    "Flow '%s' has cognitive complexity of %d (threshold: %d). Simplify by reducing nested branching.",
                    flow.getAttributeValue("name"), c.cognitive(), threshold));
            }
        }
    }

    private int parseThreshold(MuleCheckContext ctx, int defaultValue) {
        try {
            Object t = ctx.parameters().get("threshold");
            return t != null ? Integer.parseInt(t.toString()) : defaultValue;
        } catch (NumberFormatException e) { return defaultValue; }
    }
}
