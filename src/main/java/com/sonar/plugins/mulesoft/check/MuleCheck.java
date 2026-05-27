package com.sonar.plugins.mulesoft.check;

import com.sonar.plugins.mulesoft.util.RuleScope;

public interface MuleCheck {
    String ruleKey();
    RuleScope scope();
    void check(MuleCheckContext ctx);
}
