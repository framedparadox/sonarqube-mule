package com.sonar.plugins.mulesoft.rules;

import com.sonar.plugins.mulesoft.testing.RuleTestHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XPath rule validation tests for the 'configuration' category rules.
 * Each test verifies the exact XPath expression from mulesoft-ruleset.yaml.
 */
class ConfigurationRulesTest {

    private static final String MULE_NS =
        " xmlns=\"http://www.mulesoft.org/schema/mule/core\"" +
        " xmlns:http=\"http://www.mulesoft.org/schema/mule/http\"" +
        " xmlns:tls=\"http://www.mulesoft.org/schema/mule/tls\"" +
        " xmlns:db=\"http://www.mulesoft.org/schema/mule/db\"" +
        " xmlns:jms=\"http://www.mulesoft.org/schema/mule/jms\"" +
        " xmlns:email=\"http://www.mulesoft.org/schema/mule/email\"" +
        " xmlns:apikit=\"http://www.mulesoft.org/schema/mule/mule-apikit\"" +
        " xmlns:api-gateway=\"http://www.mulesoft.org/schema/mule/api-gateway\"" +
        " xmlns:secure-properties=\"http://www.mulesoft.org/schema/mule/secure-properties\"" +
        " xmlns:munit=\"http://www.mulesoft.org/schema/mule/munit\"" +
        " xmlns:ee=\"http://www.mulesoft.org/schema/mule/ee/core\"";

    // ── config-properties-required ──────────────────────────────────────────

    @Test
    void config_properties_required_passes_with_secure_properties() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<secure-properties:config name='config' file='${env}.yaml' key='${mule.key}'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)>=1 or\n" +
            "count(//mule:mule/mule:configuration-properties)>=1"));
    }

    @Test
    void config_properties_required_passes_with_configuration_properties() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<configuration-properties file='config.properties'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)>=1 or\n" +
            "count(//mule:mule/mule:configuration-properties)>=1"));
    }

    @Test
    void config_properties_required_fails_when_no_config() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)>=1 or\n" +
            "count(//mule:mule/mule:configuration-properties)>=1"));
    }

    // ── credentials-vault-no-hardcoded-key ──────────────────────────────────

    @Test
    void credentials_vault_no_hardcoded_key_passes_when_no_secure_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)=0\n" +
            "or\n" +
            "matches(//mule:mule/secure-properties:config/@key, '^\\$\\{.*\\}$')"));
    }

    @Test
    void credentials_vault_no_hardcoded_key_passes_when_key_is_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<secure-properties:config name='c' file='f.yaml' key='${mule.key}'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)=0\n" +
            "or\n" +
            "matches(//mule:mule/secure-properties:config/@key, '^\\$\\{.*\\}$')"));
    }

    @Test
    void credentials_vault_no_hardcoded_key_fails_when_key_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<secure-properties:config name='c' file='f.yaml' key='hardcodedSecret123'/>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/secure-properties:config)=0\n" +
            "or\n" +
            "matches(//mule:mule/secure-properties:config/@key, '^\\$\\{.*\\}$')"));
    }

    // ── autodiscovery-required ───────────────────────────────────────────────

    @Test
    void autodiscovery_required_passes_when_exactly_one_autodiscovery() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<api-gateway:autodiscovery apiId='${api.id}' flowRef='main-flow'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=1"));
    }

    @Test
    void autodiscovery_required_fails_when_no_autodiscovery() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=1"));
    }

    @Test
    void autodiscovery_required_fails_when_multiple_autodiscovery_elements() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<api-gateway:autodiscovery apiId='${api.id1}' flowRef='flow1'/>" +
            "<api-gateway:autodiscovery apiId='${api.id2}' flowRef='flow2'/>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=1"));
    }

    // ── apikit-http-status-codes ─────────────────────────────────────────────

    @Test
    void apikit_http_status_codes_passes_when_all_three_error_types_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<error-handler name='global'>" +
            "<on-error-propagate type='APIKIT:BAD_REQUEST'/>" +
            "<on-error-propagate type='APIKIT:METHOD_NOT_ALLOWED'/>" +
            "<on-error-propagate type='APIKIT:NOT_FOUND'/>" +
            "</error-handler>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:on-error-propagate[@type='APIKIT:BAD_REQUEST'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:METHOD_NOT_ALLOWED'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:NOT_FOUND'])>0"));
    }

    @Test
    void apikit_http_status_codes_fails_when_bad_request_missing() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<error-handler name='global'>" +
            "<on-error-propagate type='APIKIT:METHOD_NOT_ALLOWED'/>" +
            "<on-error-propagate type='APIKIT:NOT_FOUND'/>" +
            "</error-handler>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:on-error-propagate[@type='APIKIT:BAD_REQUEST'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:METHOD_NOT_ALLOWED'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:NOT_FOUND'])>0"));
    }

    @Test
    void apikit_http_status_codes_fails_when_no_error_handlers() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:on-error-propagate[@type='APIKIT:BAD_REQUEST'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:METHOD_NOT_ALLOWED'])>0\n" +
            "and\n" +
            "count(//mule:on-error-propagate[@type='APIKIT:NOT_FOUND'])>0"));
    }

    // ── dwl-external-payload ─────────────────────────────────────────────────

    @Test
    void dwl_external_payload_passes_when_no_transforms() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload/@resource,'^.*dwl$')"));
    }

    @Test
    void dwl_external_payload_passes_when_resource_is_dwl_file() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<ee:transform><ee:message>" +
            "<ee:set-payload resource='dwl/transform-response.dwl'/>" +
            "</ee:message></ee:transform>" +
            "</flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload/@resource,'^.*dwl$')"));
    }

    @Test
    void dwl_external_payload_fails_when_no_resource_attribute() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<ee:transform><ee:message>" +
            "<ee:set-payload>%dw 2.0 output application/json --- payload</ee:set-payload>" +
            "</ee:message></ee:transform>" +
            "</flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-payload/@resource,'^.*dwl$')"));
    }

    // ── dwl-external-variable ────────────────────────────────────────────────

    @Test
    void dwl_external_variable_passes_when_no_transforms() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'/></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable/@resource,'^.*dwl$')"));
    }

    @Test
    void dwl_external_variable_passes_when_resource_is_dwl_file() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<ee:transform><ee:message>" +
            "<ee:set-variable variableName='result' resource='dwl/build-vars.dwl'/>" +
            "</ee:message></ee:transform>" +
            "</flow></mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable/@resource,'^.*dwl$')"));
    }

    @Test
    void dwl_external_variable_fails_when_no_resource_attribute() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='f'>" +
            "<ee:transform><ee:message>" +
            "<ee:set-variable variableName='result'>%dw 2.0 output application/json --- {}</ee:set-variable>" +
            "</ee:message></ee:transform>" +
            "</flow></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable)=0\n" +
            "or\n" +
            "matches(//mule:mule/mule:flow/ee:transform/ee:message/ee:set-variable/@resource,'^.*dwl$')"));
    }

    // ── http-listener-https-protocol ────────────────────────────────────────

    @Test
    void http_listener_https_protocol_passes_when_no_listener_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@protocol='HTTPS'"));
    }

    @Test
    void http_listener_https_protocol_passes_when_protocol_is_https() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='http-listener-config'>" +
            "<http:listener-connection host='0.0.0.0' port='${https.port}' protocol='HTTPS'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@protocol='HTTPS'"));
    }

    @Test
    void http_listener_https_protocol_fails_when_protocol_is_http() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='http-listener-config'>" +
            "<http:listener-connection host='0.0.0.0' port='8081' protocol='HTTP'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@protocol='HTTPS'"));
    }

    // ── http-listener-port-property ──────────────────────────────────────────

    @Test
    void http_listener_port_property_passes_when_no_listener_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.private.port}'"));
    }

    @Test
    void http_listener_port_property_passes_with_https_port_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='c'>" +
            "<http:listener-connection host='0.0.0.0' port='${https.port}' protocol='HTTPS'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.private.port}'"));
    }

    @Test
    void http_listener_port_property_passes_with_private_port_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='c'>" +
            "<http:listener-connection host='0.0.0.0' port='${https.private.port}' protocol='HTTPS'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.private.port}'"));
    }

    @Test
    void http_listener_port_property_fails_when_port_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='c'>" +
            "<http:listener-connection host='0.0.0.0' port='8081'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:listener-config/http:listener-connection/@port='${https.private.port}'"));
    }

    // ── autodiscovery-api-id-no-hardcode ────────────────────────────────────

    @Test
    void autodiscovery_api_id_no_hardcode_passes_when_no_autodiscovery() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=0\n" +
            "or\n" +
            "matches(//mule:mule/api-gateway:autodiscovery/@apiId, '^\\$\\{.*\\}$')"));
    }

    @Test
    void autodiscovery_api_id_no_hardcode_passes_when_api_id_is_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<api-gateway:autodiscovery apiId='${api.id}' flowRef='main'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=0\n" +
            "or\n" +
            "matches(//mule:mule/api-gateway:autodiscovery/@apiId, '^\\$\\{.*\\}$')"));
    }

    @Test
    void autodiscovery_api_id_no_hardcode_fails_when_api_id_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<api-gateway:autodiscovery apiId='12345678' flowRef='main'/>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/api-gateway:autodiscovery)=0\n" +
            "or\n" +
            "matches(//mule:mule/api-gateway:autodiscovery/@apiId, '^\\$\\{.*\\}$')"));
    }

    // ── mssql-host-no-hardcode ───────────────────────────────────────────────

    @Test
    void mssql_host_no_hardcode_passes_when_no_mssql_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:mssql-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:mssql-connection/@host,'\\$\\{.*\\}')"));
    }

    @Test
    void mssql_host_no_hardcode_passes_when_host_is_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<db:config name='db-config'>" +
            "<db:mssql-connection host='${db.host}' port='${db.port}' databaseName='${db.name}'/>" +
            "</db:config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:mssql-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:mssql-connection/@host,'\\$\\{.*\\}')"));
    }

    @Test
    void mssql_host_no_hardcode_fails_when_host_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<db:config name='db-config'>" +
            "<db:mssql-connection host='sql-server.internal.company.com' port='1433' databaseName='mydb'/>" +
            "</db:config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:mssql-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:mssql-connection/@host,'\\$\\{.*\\}')"));
    }

    // ── db-generic-url-no-hardcode ───────────────────────────────────────────

    @Test
    void db_generic_url_no_hardcode_passes_when_no_generic_connection() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:generic-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:generic-connection/@url,'\\$\\{.*\\}')"));
    }

    @Test
    void db_generic_url_no_hardcode_passes_when_url_is_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<db:config name='db-config'>" +
            "<db:generic-connection url='${db.url}'/>" +
            "</db:config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:generic-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:generic-connection/@url,'\\$\\{.*\\}')"));
    }

    @Test
    void db_generic_url_no_hardcode_fails_when_url_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<db:config name='db-config'>" +
            "<db:generic-connection url='jdbc:mysql://localhost:3306/mydb?user=root&amp;password=secret'/>" +
            "</db:config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/db:config/db:generic-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/db:config/db:generic-connection/@url,'\\$\\{.*\\}')"));
    }

    // ── jms-credentials-no-hardcode ──────────────────────────────────────────

    @Test
    void jms_credentials_no_hardcode_passes_when_no_jms_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/jms:config/jms:active-mq-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@username,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@password,'\\$\\{.*\\}')"));
    }

    @Test
    void jms_credentials_no_hardcode_passes_when_both_are_placeholders() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<jms:config name='jms-config'>" +
            "<jms:active-mq-connection username='${jms.username}' password='${jms.password}'/>" +
            "</jms:config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/jms:config/jms:active-mq-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@username,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@password,'\\$\\{.*\\}')"));
    }

    @Test
    void jms_credentials_no_hardcode_passes_when_only_password_is_placeholder() throws Exception {
        // The rule uses OR logic: passes if username OR password is a placeholder.
        // Both must be hardcoded for the rule to fire.
        String xml = "<mule" + MULE_NS + ">" +
            "<jms:config name='jms-config'>" +
            "<jms:active-mq-connection username='admin' password='${jms.password}'/>" +
            "</jms:config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/jms:config/jms:active-mq-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@username,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@password,'\\$\\{.*\\}')"));
    }

    @Test
    void jms_credentials_no_hardcode_fails_when_both_are_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<jms:config name='jms-config'>" +
            "<jms:active-mq-connection username='admin' password='s3cret'/>" +
            "</jms:config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/jms:config/jms:active-mq-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@username,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/jms:config/jms:active-mq-connection/@password,'\\$\\{.*\\}')"));
    }

    // ── email-smtp-no-hardcode ───────────────────────────────────────────────

    @Test
    void email_smtp_no_hardcode_passes_when_no_smtp_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/email:smtp-config/email:smtp-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@host,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@port,'\\$\\{.*\\}')"));
    }

    @Test
    void email_smtp_no_hardcode_passes_when_all_fields_are_placeholders() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<email:smtp-config name='smtp-config'>" +
            "<email:smtp-connection host='${smtp.host}' port='${smtp.port}' user='${smtp.user}' password='${smtp.password}'/>" +
            "</email:smtp-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/email:smtp-config/email:smtp-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@host,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@port,'\\$\\{.*\\}')"));
    }

    @Test
    void email_smtp_no_hardcode_fails_when_host_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<email:smtp-config name='smtp-config'>" +
            "<email:smtp-connection host='smtp.gmail.com' port='587'/>" +
            "</email:smtp-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/email:smtp-config/email:smtp-connection)=0\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@host,'\\$\\{.*\\}')\n" +
            "or\n" +
            "matches(//mule:mule/email:smtp-config/email:smtp-connection/@port,'\\$\\{.*\\}')"));
    }

    // ── http-requester-https-protocol ────────────────────────────────────────

    @Test
    void http_requester_https_protocol_passes_when_no_request_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@protocol='HTTPS'"));
    }

    @Test
    void http_requester_https_protocol_passes_when_protocol_is_https() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='http-request-config'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'/>" +
            "</http:request-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@protocol='HTTPS'"));
    }

    @Test
    void http_requester_https_protocol_fails_when_protocol_is_http() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='http-request-config'>" +
            "<http:request-connection host='partner-api.example.com' port='80' protocol='HTTP'/>" +
            "</http:request-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@protocol='HTTPS'"));
    }

    // ── http-requester-port-property ────────────────────────────────────────

    @Test
    void http_requester_port_property_passes_when_no_request_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.private.port}'"));
    }

    @Test
    void http_requester_port_property_passes_with_placeholder() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'/>" +
            "</http:request-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.private.port}'"));
    }

    @Test
    void http_requester_port_property_fails_when_port_is_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='partner.example.com' port='443'/>" +
            "</http:request-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.port}'\n" +
            "or\n" +
            "//mule:mule/http:request-config/http:request-connection/@port='${https.private.port}'"));
    }

    // ── http-requester-basic-auth ────────────────────────────────────────────

    @Test
    void http_requester_basic_auth_passes_when_no_request_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)=0"));
    }

    @Test
    void http_requester_basic_auth_passes_when_no_basic_auth_configured() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'" +
            " tlsContext='tls-context'/>" +
            "</http:request-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)=0"));
    }

    @Test
    void http_requester_basic_auth_fails_when_basic_auth_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'>" +
            "<http:authentication>" +
            "<http:basic-authentication username='${api.user}' password='${api.pass}'/>" +
            "</http:authentication>" +
            "</http:request-connection>" +
            "</http:request-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)=0"));
    }

    // ── http-requester-basic-auth-no-hardcode ────────────────────────────────

    @Test
    void http_requester_basic_auth_no_hardcode_passes_when_no_request_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "(count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)>0\n" +
            "and\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@username,'^\\$\\{.*\\}$')\n" +
            "or\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@password,'^\\$\\{.*\\}$')\n" +
            ")"));
    }

    @Test
    void http_requester_basic_auth_no_hardcode_passes_when_credentials_are_placeholders() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'>" +
            "<http:authentication>" +
            "<http:basic-authentication username='${api.username}' password='${api.password}'/>" +
            "</http:authentication>" +
            "</http:request-connection>" +
            "</http:request-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "(count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)>0\n" +
            "and\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@username,'^\\$\\{.*\\}$')\n" +
            "or\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@password,'^\\$\\{.*\\}$')\n" +
            ")"));
    }

    @Test
    void http_requester_basic_auth_no_hardcode_fails_when_username_hardcoded() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'>" +
            "<http:authentication>" +
            "<http:basic-authentication username='admin' password='secret'/>" +
            "</http:authentication>" +
            "</http:request-connection>" +
            "</http:request-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "(count(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication)>0\n" +
            "and\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@username,'^\\$\\{.*\\}$')\n" +
            "or\n" +
            "matches(//mule:mule/http:request-config/http:request-connection/http:authentication/http:basic-authentication/@password,'^\\$\\{.*\\}$')\n" +
            ")"));
    }

    // ── http-requester-tls-context ───────────────────────────────────────────

    @Test
    void http_requester_tls_context_passes_when_no_request_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/@tlsContext)>0"));
    }

    @Test
    void http_requester_tls_context_passes_when_tls_context_configured() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'" +
            " tlsContext='tls-context'/>" +
            "</http:request-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/@tlsContext)>0"));
    }

    @Test
    void http_requester_tls_context_fails_when_no_tls_context_attribute() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:request-config name='c'>" +
            "<http:request-connection host='${api.host}' port='${https.port}' protocol='HTTPS'/>" +
            "</http:request-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:request-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:request-config/http:request-connection/@tlsContext)>0"));
    }

    // ── http-listener-tls-context ────────────────────────────────────────────

    @Test
    void http_listener_tls_context_passes_when_no_listener_config() throws Exception {
        String xml = "<mule" + MULE_NS + "/>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:listener-config/http:listener-connection/@tlsContext)>0"));
    }

    @Test
    void http_listener_tls_context_passes_when_tls_context_configured() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='c'>" +
            "<http:listener-connection host='0.0.0.0' port='${https.port}' protocol='HTTPS'" +
            " tlsContext='tls-context'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:listener-config/http:listener-connection/@tlsContext)>0"));
    }

    @Test
    void http_listener_tls_context_fails_when_no_tls_context_attribute() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<http:listener-config name='c'>" +
            "<http:listener-connection host='0.0.0.0' port='${https.port}' protocol='HTTPS'/>" +
            "</http:listener-config>" +
            "</mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/http:listener-config)=0\n" +
            "or\n" +
            "count(//mule:mule/http:listener-config/http:listener-connection/@tlsContext)>0"));
    }

    // ── munit-test-required ──────────────────────────────────────────────────

    @Test
    void munit_test_required_passes_when_munit_config_present() throws Exception {
        String xml = "<mule" + MULE_NS + ">" +
            "<munit:config name='munit-config'/>" +
            "</mule>";
        assertTrue(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/munit:config)>0"));
    }

    @Test
    void munit_test_required_fails_when_no_munit_config() throws Exception {
        String xml = "<mule" + MULE_NS + "><flow name='main-flow'/></mule>";
        assertFalse(RuleTestHarness.evaluate(xml,
            "count(//mule:mule/munit:config)>0"));
    }
}
