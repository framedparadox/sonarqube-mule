package com.sonar.plugins.mulesoft.profile;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.rule.MuleRulesDefinition;

/**
 * "Mule Strict" quality profile — same rule set as Recommended plus additional
 * strict-only rules, intended for projects that require zero tolerance on all violations.
 */
public class MuleStrictProfile implements BuiltInQualityProfilesDefinition {

    static final List<String> RULE_KEYS;

    static {
        List<String> keys = new ArrayList<>(MuleRecommendedProfile.RULE_KEYS);
        keys.add("correlation-id-in-logger");
        RULE_KEYS = List.copyOf(keys);
    }

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context
                .createBuiltInQualityProfile("Mule Strict", MuleLanguage.LANGUAGE_KEY);
        for (String key : RULE_KEYS) {
            profile.activateRule(MuleRulesDefinition.MULE_REPOSITORY_KEY, key);
        }
        profile.done();
    }
}
