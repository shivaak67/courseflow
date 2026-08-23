package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.prioritize.config.PrioritizeScoringProperties;
import com.prioritize.dto.PriorityInput;
import com.prioritize.dto.PriorityLevel;
import com.prioritize.dto.PriorityResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PriorityServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");

    private PrioritizeScoringProperties properties;
    private PriorityService service;

    @BeforeEach
    void setUp() {
        properties = new PrioritizeScoringProperties();
        service = new PriorityService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PriorityInput input(
            Instant dueDate,
            BigDecimal points,
            BigDecimal courseTotal,
            String difficulty,
            BigDecimal hours,
            Integer personal,
            boolean completed,
            boolean submitted) {
        return new PriorityInput(
                dueDate, points, courseTotal, difficulty, hours, personal, completed, submitted);
    }

    private PriorityInput typical() {
        return input(
                NOW.plus(7, ChronoUnit.DAYS),
                BigDecimal.valueOf(50),
                null,
                "MEDIUM",
                BigDecimal.valueOf(4),
                3,
                false,
                false);
    }

    @Test
    @DisplayName("null input is rejected")
    void rejectsNullInput() {
        assertThatThrownBy(() -> service.calculate(null)).isInstanceOf(NullPointerException.class);
    }

    @Nested
    @DisplayName("completed / submitted")
    class CompletedSubmitted {

        @Test
        void completedGetsBottomScore() {
            PriorityResult result = service.calculate(input(
                    NOW.plus(1, ChronoUnit.HOURS),
                    BigDecimal.valueOf(100),
                    null,
                    "HARD",
                    BigDecimal.valueOf(20),
                    5,
                    true,
                    false));

            assertThat(result.score()).isEqualTo(0.0);
            assertThat(result.level()).isEqualTo(PriorityLevel.LOW);
            assertThat(result.reasons()).containsExactly("Excluded: marked completed");
        }

        @Test
        void submittedGetsBottomScore() {
            PriorityResult result = service.calculate(input(
                    NOW.minus(1, ChronoUnit.DAYS),
                    BigDecimal.valueOf(100),
                    null,
                    "HARD",
                    BigDecimal.valueOf(10),
                    5,
                    false,
                    true));

            assertThat(result.score()).isEqualTo(0.0);
            assertThat(result.level()).isEqualTo(PriorityLevel.LOW);
            assertThat(result.reasons()).containsExactly("Excluded: already submitted");
        }

        @Test
        void completedAndSubmittedListsBothReasons() {
            PriorityResult result = service.calculate(
                    input(NOW, BigDecimal.TEN, null, "EASY", BigDecimal.ONE, 1, true, true));

            assertThat(result.score()).isZero();
            assertThat(result.reasons())
                    .containsExactly("Excluded: marked completed", "Excluded: already submitted");
        }
    }

    @Nested
    @DisplayName("urgency")
    class Urgency {

        @Test
        void overdueIsNearMaxUrgencyAndCriticalOverallWhenOtherFactorsHigh() {
            PriorityResult result = service.calculate(input(
                    NOW.minus(2, ChronoUnit.DAYS),
                    BigDecimal.valueOf(100),
                    null,
                    "HARD",
                    BigDecimal.valueOf(15),
                    5,
                    false,
                    false));

            assertThat(service.urgencyComponent(NOW.minus(2, ChronoUnit.DAYS), NOW)).isEqualTo(100.0);
            assertThat(result.score()).isGreaterThanOrEqualTo(80.0);
            assertThat(result.level()).isEqualTo(PriorityLevel.CRITICAL);
            assertThat(result.reasons()).anyMatch(r -> r.startsWith("Overdue"));
        }

        @Test
        void dueWithinHoursRaisesUrgency() {
            Instant dueSoon = NOW.plus(3, ChronoUnit.HOURS);
            double urgency = service.urgencyComponent(dueSoon, NOW);
            double weekOut = service.urgencyComponent(NOW.plus(7, ChronoUnit.DAYS), NOW);

            assertThat(urgency).isGreaterThan(weekOut);
            assertThat(urgency).isGreaterThan(90.0);

            PriorityResult result = service.calculate(input(
                    dueSoon, BigDecimal.valueOf(40), null, "MEDIUM", BigDecimal.valueOf(3), 3, false, false));
            assertThat(result.reasons()).anyMatch(r -> r.contains("Due within"));
        }

        @Test
        void dueInTwoDaysIsHighUrgency() {
            Instant due = NOW.plus(2, ChronoUnit.DAYS);
            double urgency = service.urgencyComponent(due, NOW);
            assertThat(urgency).isCloseTo(100.0 - (2.0 / 14.0) * 85.0, within(0.5));
        }

        @Test
        void farFutureApproachesUrgencyFloor() {
            Instant far = NOW.plus(30, ChronoUnit.DAYS);
            assertThat(service.urgencyComponent(far, NOW))
                    .isEqualTo(properties.getDefaults().getUrgencyFloor());
        }

        @Test
        void noDueDateUsesConfiguredDefault() {
            assertThat(service.urgencyComponent(null, NOW))
                    .isEqualTo(properties.getDefaults().getNoDueDateUrgency());

            PriorityResult result = service.calculate(input(
                    null, BigDecimal.valueOf(20), null, "EASY", BigDecimal.valueOf(2), 2, false, false));
            assertThat(result.reasons()).contains("No due date set");
            assertThat(result.score()).isLessThan(60.0);
        }
    }

    @Nested
    @DisplayName("points")
    class Points {

        @Test
        void highAbsolutePointsRaiseScore() {
            PriorityResult low = service.calculate(input(
                    NOW.plus(10, ChronoUnit.DAYS),
                    BigDecimal.valueOf(5),
                    null,
                    "MEDIUM",
                    BigDecimal.valueOf(3),
                    3,
                    false,
                    false));
            PriorityResult high = service.calculate(input(
                    NOW.plus(10, ChronoUnit.DAYS),
                    BigDecimal.valueOf(100),
                    null,
                    "MEDIUM",
                    BigDecimal.valueOf(3),
                    3,
                    false,
                    false));

            assertThat(high.score()).isGreaterThan(low.score());
            assertThat(high.reasons()).contains("Worth high point value");
        }

        @Test
        void relativeToCourseTotalWhenKnown() {
            double relative = service.pointsComponent(BigDecimal.valueOf(50), BigDecimal.valueOf(200));
            double absolute = service.pointsComponent(BigDecimal.valueOf(50), null);

            assertThat(relative).isEqualTo(25.0);
            assertThat(absolute).isEqualTo(50.0);
        }

        @Test
        void missingPointsUsesDefault() {
            assertThat(service.pointsComponent(null, null))
                    .isEqualTo(properties.getDefaults().getMissingPoints());
        }

        @Test
        void pointsCappedAt100() {
            assertThat(service.pointsComponent(BigDecimal.valueOf(500), null)).isEqualTo(100.0);
            assertThat(service.pointsComponent(BigDecimal.valueOf(150), BigDecimal.valueOf(100))).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("difficulty")
    class Difficulty {

        @ParameterizedTest
        @CsvSource({
                "EASY, 25.0",
                "easy, 25.0",
                "MEDIUM, 50.0",
                "HARD, 90.0",
                "hard, 90.0"
        })
        void mapsKnownLevels(String difficulty, double expected) {
            assertThat(service.difficultyComponent(difficulty)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  ", "UNKNOWN"})
        void unknownOrMissingUsesDefault(String difficulty) {
            assertThat(service.difficultyComponent(difficulty))
                    .isEqualTo(properties.getDefaults().getMissingDifficulty());
        }

        @Test
        void hardRaisesOverallScoreAndReason() {
            PriorityResult easy = service.calculate(input(
                    NOW.plus(5, ChronoUnit.DAYS),
                    BigDecimal.valueOf(40),
                    null,
                    "EASY",
                    BigDecimal.valueOf(4),
                    3,
                    false,
                    false));
            PriorityResult hard = service.calculate(input(
                    NOW.plus(5, ChronoUnit.DAYS),
                    BigDecimal.valueOf(40),
                    null,
                    "HARD",
                    BigDecimal.valueOf(4),
                    3,
                    false,
                    false));

            assertThat(hard.score()).isGreaterThan(easy.score());
            assertThat(hard.reasons()).contains("Marked HARD");
        }
    }

    @Nested
    @DisplayName("workload")
    class Workload {

        @Test
        void higherEstimatedHoursRaisesScore() {
            PriorityResult light = service.calculate(input(
                    NOW.plus(5, ChronoUnit.DAYS),
                    BigDecimal.valueOf(40),
                    null,
                    "MEDIUM",
                    BigDecimal.valueOf(1),
                    3,
                    false,
                    false));
            PriorityResult heavy = service.calculate(input(
                    NOW.plus(5, ChronoUnit.DAYS),
                    BigDecimal.valueOf(40),
                    null,
                    "MEDIUM",
                    BigDecimal.valueOf(16),
                    3,
                    false,
                    false));

            assertThat(heavy.score()).isGreaterThan(light.score());
            assertThat(heavy.reasons()).anyMatch(r -> r.contains("High estimated effort"));
        }

        @Test
        void workloadCapsAtConfiguredMaxHours() {
            assertThat(service.workloadComponent(BigDecimal.valueOf(20))).isEqualTo(100.0);
            assertThat(service.workloadComponent(BigDecimal.valueOf(40))).isEqualTo(100.0);
            assertThat(service.workloadComponent(BigDecimal.valueOf(10))).isEqualTo(50.0);
        }

        @Test
        void missingWorkloadUsesDefault() {
            assertThat(service.workloadComponent(null))
                    .isEqualTo(properties.getDefaults().getMissingWorkload());
        }
    }

    @Nested
    @DisplayName("personal priority")
    class PersonalPriority {

        @ParameterizedTest
        @CsvSource({"1, 20.0", "2, 40.0", "3, 60.0", "4, 80.0", "5, 100.0"})
        void mapsOneToFive(int value, double expected) {
            assertThat(service.personalPriorityComponent(value)).isEqualTo(expected);
        }

        @Test
        void clampsOutOfRange() {
            assertThat(service.personalPriorityComponent(0)).isEqualTo(20.0);
            assertThat(service.personalPriorityComponent(9)).isEqualTo(100.0);
        }

        @Test
        void missingUsesDefault() {
            assertThat(service.personalPriorityComponent(null))
                    .isEqualTo(properties.getDefaults().getMissingPersonalPriority());
        }

        @Test
        void highPersonalPrioritySurfacesReason() {
            PriorityResult result = service.calculate(input(
                    NOW.plus(8, ChronoUnit.DAYS),
                    BigDecimal.valueOf(30),
                    null,
                    "MEDIUM",
                    BigDecimal.valueOf(3),
                    5,
                    false,
                    false));
            assertThat(result.reasons()).contains("Personal priority 5/5");
        }
    }

    @Nested
    @DisplayName("levels")
    class Levels {

        @Test
        void criticalThreshold() {
            assertThat(service.toLevel(80.0)).isEqualTo(PriorityLevel.CRITICAL);
            assertThat(service.toLevel(99.0)).isEqualTo(PriorityLevel.CRITICAL);
        }

        @Test
        void highThreshold() {
            assertThat(service.toLevel(60.0)).isEqualTo(PriorityLevel.HIGH);
            assertThat(service.toLevel(79.9)).isEqualTo(PriorityLevel.HIGH);
        }

        @Test
        void mediumThreshold() {
            assertThat(service.toLevel(40.0)).isEqualTo(PriorityLevel.MEDIUM);
            assertThat(service.toLevel(59.9)).isEqualTo(PriorityLevel.MEDIUM);
        }

        @Test
        void lowBelowMedium() {
            assertThat(service.toLevel(39.9)).isEqualTo(PriorityLevel.LOW);
            assertThat(service.toLevel(0.0)).isEqualTo(PriorityLevel.LOW);
        }

        @Test
        void customThresholdsAreHonored() {
            properties.getThresholds().setCritical(90);
            properties.getThresholds().setHigh(70);
            properties.getThresholds().setMedium(50);
            service = new PriorityService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

            assertThat(service.toLevel(85.0)).isEqualTo(PriorityLevel.HIGH);
            assertThat(service.toLevel(90.0)).isEqualTo(PriorityLevel.CRITICAL);
            assertThat(service.toLevel(55.0)).isEqualTo(PriorityLevel.MEDIUM);
            assertThat(service.toLevel(49.0)).isEqualTo(PriorityLevel.LOW);
        }
    }

    @Nested
    @DisplayName("weights and formula")
    class Weights {

        @Test
        void scoreMatchesWeightedSumOfComponents() {
            PriorityInput in = typical();

            double u = service.urgencyComponent(in.dueDate(), NOW);
            double p = service.pointsComponent(in.pointsPossible(), in.courseTotalPoints());
            double d = service.difficultyComponent(in.difficulty());
            double w = service.workloadComponent(in.estimatedHours());
            double r = service.personalPriorityComponent(in.personalPriority());

            PrioritizeScoringProperties.Weights weights = properties.getWeights();
            double expected = weights.getUrgency() * u
                    + weights.getPoints() * p
                    + weights.getDifficulty() * d
                    + weights.getWorkload() * w
                    + weights.getPersonalPriority() * r;
            expected = BigDecimal.valueOf(expected).setScale(1, RoundingMode.HALF_UP).doubleValue();

            assertThat(service.calculate(in).score()).isEqualTo(expected);
        }

        @Test
        void increasingUrgencyWeightRaisesOverdueScore() {
            PriorityInput overdue = input(
                    NOW.minus(1, ChronoUnit.DAYS),
                    BigDecimal.valueOf(10),
                    null,
                    "EASY",
                    BigDecimal.ONE,
                    1,
                    false,
                    false);

            PriorityResult baseline = service.calculate(overdue);

            properties.getWeights().setUrgency(0.70);
            properties.getWeights().setPoints(0.10);
            properties.getWeights().setDifficulty(0.05);
            properties.getWeights().setWorkload(0.05);
            properties.getWeights().setPersonalPriority(0.10);
            service = new PriorityService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

            assertThat(service.calculate(overdue).score()).isGreaterThan(baseline.score());
        }
    }

    @Nested
    @DisplayName("reasons")
    class Reasons {

        @Test
        void reasonsAreCappedAndOrderedByContribution() {
            PriorityResult result = service.calculate(input(
                    NOW.minus(3, ChronoUnit.DAYS),
                    BigDecimal.valueOf(100),
                    null,
                    "HARD",
                    BigDecimal.valueOf(18),
                    5,
                    false,
                    false));

            assertThat(result.reasons()).isNotEmpty();
            assertThat(result.reasons()).hasSizeLessThanOrEqualTo(4);
            assertThat(result.reasons().getFirst()).startsWith("Overdue");
        }

        @Test
        void easyAssignmentStaysLowerThanHardCounterpart() {
            PriorityResult easy = service.calculate(input(
                    NOW.plus(10, ChronoUnit.DAYS),
                    BigDecimal.valueOf(10),
                    null,
                    "EASY",
                    BigDecimal.valueOf(1),
                    1,
                    false,
                    false));
            PriorityResult hard = service.calculate(input(
                    NOW.plus(10, ChronoUnit.DAYS),
                    BigDecimal.valueOf(10),
                    null,
                    "HARD",
                    BigDecimal.valueOf(1),
                    1,
                    false,
                    false));

            assertThat(easy.score()).isLessThan(hard.score());
            assertThat(easy.level()).isIn(PriorityLevel.LOW, PriorityLevel.MEDIUM);
        }
    }

    @Test
    @DisplayName("sparse input still produces a finite score and level")
    void sparseInputIsSafe() {
        PriorityResult result = service.calculate(input(null, null, null, null, null, null, false, false));

        assertThat(result.score()).isBetween(0.0, 100.0);
        assertThat(result.level()).isNotNull();
        assertThat(result.reasons()).isNotNull();
    }
}
