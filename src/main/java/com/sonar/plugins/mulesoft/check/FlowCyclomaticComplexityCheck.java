package com.sonar.plugins.mulesoft.check;

import com.sonar.plugins.mulesoft.metrics.ComplexityComputer;
import com.sonar.plugins.mulesoft.util.RuleScope;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.List;

public class FlowCyclomaticComplexityCheck implements MuleCheck {

    private static final Namespace MULE = Namespace.getNamespace("mule", "http://www.mulesoft.org/schema/mule/core");
    private static final int DEFAULT_THRESHOLD = 10;

    @Override
    public String ruleKey() { return "flow-cyclomatic-complexity"; }

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
            if (c.cyclomatic() > threshold) {
                ctx.reportFileIssue(String.format(
                    "Flow '%s' has cyclomatic complexity of %d (threshold: %d). Simplify by extracting sub-flows.",
                    flow.getAttributeValue("name"), c.cyclomatic(), threshold));
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
