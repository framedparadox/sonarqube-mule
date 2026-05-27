package com.sonar.plugins.mulesoft.config;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.sensor.DataWeaveSensor;

/**
 * Centralised registry of all {@code sonar.mule.*} property definitions.
 *
 * <p>Use {@link #all()} to obtain the complete list for registration in
 * {@code MulePlugin.define(Context)}.</p>
 */
public final class MulePluginProperties {

    private static final String CATEGORY     = MuleLanguage.LANGUAGE_NAME;
    private static final String SUB_GENERAL  = "General";
    private static final String SUB_LAYERS   = "API Layers";
    private static final String SUB_MUNIT    = "MUnit";
    private static final String SUB_CPD      = "CPD";

    // ── Property key constants ───────────────────────────────────────────────

    /** Mule XML file extensions scanned by the plugin. */
    public static final String FILE_SUFFIXES_KEY =
            MuleLanguage.FILE_SUFFIXES_KEY;                       // sonar.mule.file.suffixes

    /** DataWeave file extensions scanned by the plugin. */
    public static final String DATAWEAVE_FILE_SUFFIXES_KEY =
            DataWeaveSensor.DATAWEAVE_FILE_SUFFIXES_KEY;          // sonar.mule.dataweave.file.suffixes

    /** Extra namespace prefix→URI mappings for XPath evaluation. */
    public static final String NAMESPACE_PROPERTIES_KEY =
            "sonar.mule.namespace.properties";

    /** Comma-separated category filter for ruleset evaluation. */
    public static final String RULESET_CATEGORIES_KEY =
            "sonar.mule.ruleset.categories";

    /** External directory path for a custom ruleset file. */
    public static final String RULESET_EXTERNAL_PATH_KEY =
            "sonar.mule.ruleset.external.path";

    /** Override for the main Mule configuration file. */
    public static final String MAIN_CONFIG_FILE_KEY =
            "sonar.mule.mainConfigFile";

    /** Regex pattern identifying System-layer Mule config files. */
    public static final String API_LAYER_SYSTEM_PATTERN_KEY =
            "sonar.mule.apiLayer.system.pattern";

    /** Regex pattern identifying Process-layer Mule config files. */
    public static final String API_LAYER_PROCESS_PATTERN_KEY =
            "sonar.mule.apiLayer.process.pattern";

    /** Regex pattern identifying Experience-layer Mule config files. */
    public static final String API_LAYER_EXPERIENCE_PATTERN_KEY =
            "sonar.mule.apiLayer.experience.pattern";

    /** When true, uses a lightweight project model (no full DOM parsing). */
    public static final String PROJECT_MODEL_LIGHT_MODE_KEY =
            "sonar.mule.projectModel.lightMode";

    /** Path to MUnit Surefire/coverage XML report. */
    public static final String MUNIT_COVERAGE_REPORT_PATH_KEY =
            "sonar.mule.munit.coverage.reportPath";

    /** Minimum MUnit line-coverage percentage required (0–100). */
    public static final String MUNIT_COVERAGE_MINIMUM_KEY =
            "sonar.mule.munit.coverage.minimum";

    /** When true, documentation attributes are excluded from CPD analysis. */
    public static final String CPD_EXCLUDE_DOC_ATTRIBUTES_KEY =
            "sonar.mule.cpd.excludeDocAttributes";

    // ── Factory ──────────────────────────────────────────────────────────────

    private MulePluginProperties() { /* utility class */ }

    /**
     * Returns all property definitions to be registered with SonarQube.
     *
     * @return immutable list of {@link PropertyDefinition} instances
     */
    public static List<PropertyDefinition> all() {
        return Arrays.asList(

            // Mule file suffixes
            PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                .defaultValue(MuleLanguage.FILE_SUFFIXES_DEFAULT_VALUE)
                .name("File Suffixes")
                .description("List of suffixes for Mule XML files to analyse.")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .multiValues(true)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // DataWeave file suffixes
            PropertyDefinition.builder(DATAWEAVE_FILE_SUFFIXES_KEY)
                .defaultValue(DataWeaveSensor.DATAWEAVE_FILE_SUFFIXES_DEFAULT_VALUE)
                .name("DataWeave File Suffixes")
                .description("List of suffixes to scan for DataWeave source files.")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .multiValues(true)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // Extra namespace properties
            PropertyDefinition.builder(NAMESPACE_PROPERTIES_KEY)
                .name("Extra Namespace Properties")
                .description("Optional URL/file/classpath spec to a .properties file containing additional "
                        + "XML namespaces (format: prefix=namespaceURI). "
                        + "Example: file:/path/to/namespaces.properties or classpath:namespace-extra.properties")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // Ruleset category filter
            PropertyDefinition.builder(RULESET_CATEGORIES_KEY)
                .name("Ruleset Categories")
                .description("Comma-separated list of rule categories to enforce "
                        + "(e.g. flows,configuration,application). Leave empty to enforce all categories.")
                .defaultValue("")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // External ruleset directory
            PropertyDefinition.builder(RULESET_EXTERNAL_PATH_KEY)
                .name("External Ruleset Path")
                .description("Filesystem path to a directory containing a custom mulesoft-ruleset.yaml or "
                        + "mulesoft-ruleset.xml file. Overrides the bundled ruleset when present.")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // Main config file override
            PropertyDefinition.builder(MAIN_CONFIG_FILE_KEY)
                .name("Main Config File")
                .description("Override for the main Mule application configuration file name.")
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // API layer – system pattern
            PropertyDefinition.builder(API_LAYER_SYSTEM_PATTERN_KEY)
                .name("System Layer Pattern")
                .description("Regular expression that identifies System-layer Mule configuration files.")
                .defaultValue(".*system.*")
                .subCategory(SUB_LAYERS)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // API layer – process pattern
            PropertyDefinition.builder(API_LAYER_PROCESS_PATTERN_KEY)
                .name("Process Layer Pattern")
                .description("Regular expression that identifies Process-layer Mule configuration files.")
                .defaultValue(".*process.*")
                .subCategory(SUB_LAYERS)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // API layer – experience pattern
            PropertyDefinition.builder(API_LAYER_EXPERIENCE_PATTERN_KEY)
                .name("Experience Layer Pattern")
                .description("Regular expression that identifies Experience-layer Mule configuration files.")
                .defaultValue(".*experience.*")
                .subCategory(SUB_LAYERS)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // Light-mode project model
            PropertyDefinition.builder(PROJECT_MODEL_LIGHT_MODE_KEY)
                .name("Light Project Model")
                .description("When enabled, uses a lightweight project model that skips full DOM parsing "
                        + "for faster analysis of large projects.")
                .defaultValue("false")
                .type(PropertyType.BOOLEAN)
                .subCategory(SUB_GENERAL)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // MUnit coverage report path
            PropertyDefinition.builder(MUNIT_COVERAGE_REPORT_PATH_KEY)
                .name("MUnit Coverage Report Path")
                .description("Path to the MUnit Surefire/coverage XML report file.")
                .subCategory(SUB_MUNIT)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // MUnit minimum coverage
            PropertyDefinition.builder(MUNIT_COVERAGE_MINIMUM_KEY)
                .name("MUnit Minimum Coverage")
                .description("Minimum MUnit line-coverage percentage required (0–100). "
                        + "Analysis fails if coverage falls below this threshold.")
                .defaultValue("0")
                .type(PropertyType.INTEGER)
                .subCategory(SUB_MUNIT)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build(),

            // CPD exclude doc attributes
            PropertyDefinition.builder(CPD_EXCLUDE_DOC_ATTRIBUTES_KEY)
                .name("CPD Exclude Doc Attributes")
                .description("When true, documentation attributes (doc:name, doc:id) are excluded from "
                        + "Copy-Paste Detection to reduce noise.")
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .subCategory(SUB_CPD)
                .category(CATEGORY)
                .onQualifiers(Qualifiers.PROJECT)
                .build()
        );
    }
}
