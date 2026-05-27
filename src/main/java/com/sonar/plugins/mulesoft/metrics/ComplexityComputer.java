package com.sonar.plugins.mulesoft.metrics;

import org.jdom2.Element;
import java.util.Set;

public final class ComplexityComputer {

    private ComplexityComputer() {}

    public record FlowComplexity(int cyclomatic, int cognitive, int maxNestingDepth) {}

    private static final Set<String> BRANCHING = Set.of(
            "when", "route", "foreach", "parallel-foreach",
            "until-successful", "async", "on-error-propagate", "on-error-continue"
    );

    private static final Set<String> CONTAINER = Set.of(
            "choice", "scatter-gather", "try", "first-successful", "error-handler"
    );

    public static FlowComplexity compute(Element flow) {
        Visitor v = new Visitor();
        for (Element child : flow.getChildren()) {
            v.visit(child, 0);
        }
        return new FlowComplexity(1 + v.cyclomatic, v.cognitive, v.maxDepth);
    }

    private static class Visitor {
        int cyclomatic = 0;
        int cognitive = 0;
        int maxDepth = 0;

        void visit(Element el, int depth) {
            String name = el.getName();
            int childDepth = depth;
            if (BRANCHING.contains(name)) {
                cyclomatic++;
                cognitive += (1 + depth);
                childDepth = depth + 1;
                if (childDepth > maxDepth) maxDepth = childDepth;
            } else if (CONTAINER.contains(name)) {
                // CONTAINER groups branches but does not itself add nesting depth
                childDepth = depth;
            }
            for (Element child : el.getChildren()) {
                visit(child, childDepth);
            }
        }
    }
}
