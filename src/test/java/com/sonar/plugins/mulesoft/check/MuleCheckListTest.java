package com.sonar.plugins.mulesoft.check;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuleCheckListTest {

    @Test
    void registry_has_three_checks_after_task7() {
        assertThat(MuleCheckList.allChecks()).hasSize(3);
    }

    @Test
    void registry_is_immutable() {
        assertThat(MuleCheckList.allChecks()).isUnmodifiable();
    }

    @Test
    void lookup_by_rule_key_returns_empty_for_unknown() {
        assertThat(MuleCheckList.byRuleKey("does-not-exist")).isEmpty();
    }
}
