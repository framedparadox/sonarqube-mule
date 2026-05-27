package com.sonar.plugins.mulesoft.check;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class MuleCheckList {

    private static final List<MuleCheck> CHECKS = Collections.unmodifiableList(List.of(
            new FlowCyclomaticComplexityCheck(),
            new FlowCognitiveComplexityCheck(),
            new FlowNestingDepthCheck()
    ));

    private MuleCheckList() {}

    public static List<MuleCheck> allChecks() {
        return CHECKS;
    }

    public static Optional<MuleCheck> byRuleKey(String ruleKey) {
        return CHECKS.stream().filter(c -> c.ruleKey().equals(ruleKey)).findFirst();
    }
}
