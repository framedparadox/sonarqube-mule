package com.sonar.plugins.mulesoft.profile;

import java.util.List;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.rule.MuleRulesDefinition;

/**
 * "Mule Recommended" quality profile — activates ALL known rule keys.
 * This is the default profile applied to Mule projects.
 */
public class MuleRecommendedProfile implements BuiltInQualityProfilesDefinition {

    static final List<String> RULE_KEYS = List.of(
        "apikit-config-required",
        "apikit-exception-strategy",
        "count-limit",
        "subflow-count-limit",
        "naming-convention",
        "subflow-naming-convention",
        "encryption-key-logging-forbidden",
        "set-variable-doc-name",
        "healthcheck-flow-required",
        "config-properties-required",
        "credentials-vault-no-hardcoded-key",
        "autodiscovery-required",
        "apikit-http-status-codes",
        "dwl-external-payload",
        "dwl-external-variable",
        "http-listener-https-protocol",
        "http-listener-port-property",
        "autodiscovery-api-id-no-hardcode",
        "mssql-host-no-hardcode",
        "db-generic-url-no-hardcode",
        "jms-credentials-no-hardcode",
        "email-smtp-no-hardcode",
        "http-requester-https-protocol",
        "http-requester-port-property",
        "http-requester-basic-auth",
        "http-requester-basic-auth-no-hardcode",
        "http-requester-tls-context",
        "http-listener-tls-context",
        "munit-test-required",
        "set-payload-no-inline-secret",
        "logger-no-authorization-header",
        "http-listener-no-wildcard-path",
        "logger-no-payload-at-info-level",
        "tls-context-revocation-check",
        "logger-category-required",
        "doc-name-non-empty",
        "flow-cyclomatic-complexity",
        "flow-cognitive-complexity",
        "flow-nesting-depth"
    );

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context
                .createBuiltInQualityProfile("Mule Recommended", MuleLanguage.LANGUAGE_KEY);
        profile.setDefault(true);
        for (String key : RULE_KEYS) {
            profile.activateRule(MuleRulesDefinition.MULE_REPOSITORY_KEY, key);
        }
        profile.done();
    }
}
