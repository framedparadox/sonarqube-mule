package com.sonar.plugins.mulesoft.profile;

import java.util.List;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.rule.MuleRulesDefinition;

/**
 * "Mule Sanity" quality profile — activates the critical security/convention subset.
 */
public class MuleSanityProfile implements BuiltInQualityProfilesDefinition {

    static final List<String> RULE_KEYS = List.of(
        "apikit-config-required",
        "apikit-exception-strategy",
        "encryption-key-logging-forbidden",
        "config-properties-required",
        "credentials-vault-no-hardcoded-key",
        "http-listener-https-protocol",
        "http-requester-https-protocol",
        "http-listener-tls-context",
        "http-requester-tls-context",
        "jms-credentials-no-hardcode",
        "email-smtp-no-hardcode",
        "http-requester-basic-auth-no-hardcode",
        "munit-test-required",
        "autodiscovery-required",
        "apikit-http-status-codes",
        "naming-convention",
        "subflow-naming-convention",
        "mssql-host-no-hardcode",
        "db-generic-url-no-hardcode",
        "set-payload-no-inline-secret",
        "logger-no-authorization-header",
        "http-listener-no-wildcard-path"
    );

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context
                .createBuiltInQualityProfile("Mule Sanity", MuleLanguage.LANGUAGE_KEY);
        for (String key : RULE_KEYS) {
            profile.activateRule(MuleRulesDefinition.MULE_REPOSITORY_KEY, key);
        }
        profile.done();
    }
}
