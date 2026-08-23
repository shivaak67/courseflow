package com.prioritize.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies {@code prioritize.scoring.*} binds from application.yml into properties.
 */
@SpringBootTest
@ActiveProfiles("test")
class PrioritizeScoringPropertiesBindingTest {

    @Autowired
    private PrioritizeScoringProperties properties;

    @Test
    void bindsDefaultWeightsAndThresholds() {
        assertThat(properties.getWeights().getUrgency()).isEqualTo(0.35);
        assertThat(properties.getWeights().getPoints()).isEqualTo(0.20);
        assertThat(properties.getWeights().getDifficulty()).isEqualTo(0.15);
        assertThat(properties.getWeights().getWorkload()).isEqualTo(0.15);
        assertThat(properties.getWeights().getPersonalPriority()).isEqualTo(0.15);

        assertThat(properties.getThresholds().getCritical()).isEqualTo(80.0);
        assertThat(properties.getThresholds().getHigh()).isEqualTo(60.0);
        assertThat(properties.getThresholds().getMedium()).isEqualTo(40.0);

        assertThat(properties.getDefaults().getNoDueDateUrgency()).isEqualTo(35.0);
        assertThat(properties.getDefaults().getMaxWorkloadHours()).isEqualTo(20.0);
        assertThat(properties.getDefaults().getUrgencyHorizonDays()).isEqualTo(14.0);
    }
}
