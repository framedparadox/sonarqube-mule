package com.sonar.plugins.mulesoft.metrics;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

class MuleMetricsTest {

    @Test
    void all_expected_metrics_are_registered() {
        Set<String> keys = new MuleMetrics().getMetrics().stream()
                .map(m -> (String) m.getKey())
                .collect(Collectors.toSet());

        assertThat(keys).contains(
                "mule_flows",
                "mule_subflows",
                "mule_dataweave_transformations",
                "mule_configuration_files",
                "mule_complexity_rating",
                "mule_error_handlers",
                "mule_http_listeners",
                "mule_connector_configs",
                "mule_munit_tests",
                "mule_munit_assertions",
                "mule_dataweave_inline_count",
                "mule_dataweave_external_count",
                "mule_dataweave_external_ratio",
                "mule_flow_max_complexity",
                "mule_flow_max_nesting_depth"
        );
    }
}
