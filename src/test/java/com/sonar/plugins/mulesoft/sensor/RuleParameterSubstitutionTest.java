package com.sonar.plugins.mulesoft.sensor;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RuleParameterSubstitutionTest {

    @Test
    void substitutes_single_parameter() {
        String xpath = "not(count(//flow/*)>${max})";
        String result = com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor
                .substituteParameters(xpath, java.util.Map.of("max", "15"));
        assertThat(result).isEqualTo("not(count(//flow/*)>15)");
    }

    @Test
    void substitutes_multiple_parameters() {
        String xpath = "${a} or ${b}";
        String result = com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor
                .substituteParameters(xpath, java.util.Map.of("a", "1", "b", "2"));
        assertThat(result).isEqualTo("1 or 2");
    }

    @Test
    void leaves_unknown_placeholders_alone() {
        String xpath = "count(*)>${max}";
        String result = com.sonar.plugins.mulesoft.sensors.validation.MuleValidationSensor
                .substituteParameters(xpath, java.util.Map.of());
        assertThat(result).isEqualTo("count(*)>${max}");
    }
}
