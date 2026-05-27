package com.sonar.plugins.mulesoft.metrics.rating;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ComplexityRatingTest {

    @Test
    void rating_thresholds_map_as_spec_defines() {
        assertThat(ComplexityRating.rateFor(0)).isEqualTo(1);   // A
        assertThat(ComplexityRating.rateFor(5)).isEqualTo(1);   // A
        assertThat(ComplexityRating.rateFor(6)).isEqualTo(2);   // B
        assertThat(ComplexityRating.rateFor(10)).isEqualTo(2);  // B
        assertThat(ComplexityRating.rateFor(11)).isEqualTo(3);  // C
        assertThat(ComplexityRating.rateFor(15)).isEqualTo(3);  // C
        assertThat(ComplexityRating.rateFor(16)).isEqualTo(4);  // D
        assertThat(ComplexityRating.rateFor(20)).isEqualTo(4);  // D
        assertThat(ComplexityRating.rateFor(21)).isEqualTo(5);  // E
    }
}
