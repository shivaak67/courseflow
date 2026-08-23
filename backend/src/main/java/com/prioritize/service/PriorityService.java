package com.prioritize.service;

import com.prioritize.config.PrioritizeScoringProperties;
import com.prioritize.dto.PriorityInput;
import com.prioritize.dto.PriorityLevel;
import com.prioritize.dto.PriorityResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pure weighted priority engine. Components are normalized to 0–100, then combined
 * with configurable weights from {@code prioritize.scoring.*}.
 */
@Service
public class PriorityService {

    private static final int REASON_LIMIT = 4;

    private final PrioritizeScoringProperties properties;
    private final Clock clock;

    @Autowired
    public PriorityService(PrioritizeScoringProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Scores a single assignment-like input relative to {@link Clock#instant()}.
     */
    public PriorityResult calculate(PriorityInput input) {
        Objects.requireNonNull(input, "input");

        if (input.completed() || input.submitted()) {
            List<String> reasons = new ArrayList<>(2);
            if (input.completed()) {
                reasons.add("Excluded: marked completed");
            }
            if (input.submitted()) {
                reasons.add("Excluded: already submitted");
            }
            return new PriorityResult(0.0, PriorityLevel.LOW, List.copyOf(reasons));
        }

        Instant now = clock.instant();
        double urgency = urgencyComponent(input.dueDate(), now);
        double points = pointsComponent(input.pointsPossible(), input.courseTotalPoints());
        double difficulty = difficultyComponent(input.difficulty());
        double workload = workloadComponent(input.estimatedHours());
        double personal = personalPriorityComponent(input.personalPriority());

        PrioritizeScoringProperties.Weights w = properties.getWeights();
        double raw =
                w.getUrgency() * urgency
                        + w.getPoints() * points
                        + w.getDifficulty() * difficulty
                        + w.getWorkload() * workload
                        + w.getPersonalPriority() * personal;
        double score = roundScore(clamp(raw, 0.0, 100.0));
        PriorityLevel level = toLevel(score);

        List<String> reasons = buildReasons(
                input,
                now,
                urgency,
                points,
                difficulty,
                workload,
                personal,
                w);

        return new PriorityResult(score, level, reasons);
    }

    double urgencyComponent(Instant dueDate, Instant now) {
        PrioritizeScoringProperties.Defaults defaults = properties.getDefaults();
        if (dueDate == null) {
            return defaults.getNoDueDateUrgency();
        }
        Duration until = Duration.between(now, dueDate);
        if (until.isNegative() || until.isZero()) {
            return 100.0;
        }
        double days = until.toMinutes() / (60.0 * 24.0);
        double horizon = Math.max(defaults.getUrgencyHorizonDays(), 0.0001);
        double floor = defaults.getUrgencyFloor();
        if (days >= horizon) {
            return floor;
        }
        // Linear: 100 at due, floor at horizon days out
        return 100.0 - (days / horizon) * (100.0 - floor);
    }

    double pointsComponent(BigDecimal pointsPossible, BigDecimal courseTotalPoints) {
        PrioritizeScoringProperties.Defaults defaults = properties.getDefaults();
        if (pointsPossible == null) {
            return defaults.getMissingPoints();
        }
        double points = Math.max(pointsPossible.doubleValue(), 0.0);
        if (courseTotalPoints != null && courseTotalPoints.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = points / courseTotalPoints.doubleValue();
            return clamp(ratio * 100.0, 0.0, 100.0);
        }
        double maxAbs = Math.max(defaults.getMaxAbsolutePoints(), 0.0001);
        return clamp((points / maxAbs) * 100.0, 0.0, 100.0);
    }

    double difficultyComponent(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return properties.getDefaults().getMissingDifficulty();
        }
        return switch (difficulty.trim().toUpperCase(Locale.ROOT)) {
            case "EASY" -> 25.0;
            case "MEDIUM" -> 50.0;
            case "HARD" -> 90.0;
            default -> properties.getDefaults().getMissingDifficulty();
        };
    }

    double workloadComponent(BigDecimal estimatedHours) {
        PrioritizeScoringProperties.Defaults defaults = properties.getDefaults();
        if (estimatedHours == null) {
            return defaults.getMissingWorkload();
        }
        double hours = Math.max(estimatedHours.doubleValue(), 0.0);
        double maxHours = Math.max(defaults.getMaxWorkloadHours(), 0.0001);
        return clamp((hours / maxHours) * 100.0, 0.0, 100.0);
    }

    double personalPriorityComponent(Integer personalPriority) {
        if (personalPriority == null) {
            return properties.getDefaults().getMissingPersonalPriority();
        }
        int clamped = Math.max(1, Math.min(5, personalPriority));
        return clamped * 20.0;
    }

    PriorityLevel toLevel(double score) {
        PrioritizeScoringProperties.Thresholds t = properties.getThresholds();
        if (score >= t.getCritical()) {
            return PriorityLevel.CRITICAL;
        }
        if (score >= t.getHigh()) {
            return PriorityLevel.HIGH;
        }
        if (score >= t.getMedium()) {
            return PriorityLevel.MEDIUM;
        }
        return PriorityLevel.LOW;
    }

    private List<String> buildReasons(
            PriorityInput input,
            Instant now,
            double urgency,
            double points,
            double difficulty,
            double workload,
            double personal,
            PrioritizeScoringProperties.Weights w) {

        record Factor(double contribution, String reason) {}

        List<Factor> factors = new ArrayList<>();

        if (input.dueDate() == null) {
            factors.add(new Factor(w.getUrgency() * urgency, "No due date set"));
        } else {
            Duration until = Duration.between(now, input.dueDate());
            if (until.isNegative()) {
                long overdueDays = Math.max(1, Math.abs(until.toDays()));
                String label = overdueDays == 1
                        ? "Overdue by 1 day"
                        : "Overdue by " + overdueDays + " days";
                factors.add(new Factor(w.getUrgency() * urgency, label));
            } else {
                long daysLeft = until.toDays();
                long hoursLeft = until.toHours();
                String label;
                if (hoursLeft < 24) {
                    label = hoursLeft <= 1 ? "Due within 1 hour" : "Due within " + hoursLeft + " hours";
                } else if (daysLeft <= 1) {
                    label = "Due tomorrow";
                } else {
                    label = "Due in " + daysLeft + " days";
                }
                if (urgency >= 60.0) {
                    factors.add(new Factor(w.getUrgency() * urgency, label));
                }
            }
        }

        if (input.pointsPossible() != null && points >= 55.0) {
            factors.add(new Factor(w.getPoints() * points, "Worth high point value"));
        }

        if (input.difficulty() != null && "HARD".equalsIgnoreCase(input.difficulty().trim())) {
            factors.add(new Factor(w.getDifficulty() * difficulty, "Marked HARD"));
        } else if (input.difficulty() != null && "EASY".equalsIgnoreCase(input.difficulty().trim())) {
            // Low contribution — still surface when easy is a distinguishing factor for transparency
            if (difficulty <= 30.0) {
                factors.add(new Factor(w.getDifficulty() * (100.0 - difficulty), "Marked EASY"));
            }
        }

        if (input.estimatedHours() != null && workload >= 50.0) {
            String hoursLabel = formatHours(input.estimatedHours());
            factors.add(new Factor(w.getWorkload() * workload, "High estimated effort (" + hoursLabel + ")"));
        }

        if (input.personalPriority() != null && input.personalPriority() >= 4) {
            factors.add(new Factor(
                    w.getPersonalPriority() * personal,
                    "Personal priority " + Math.max(1, Math.min(5, input.personalPriority())) + "/5"));
        }

        return factors.stream()
                .sorted(Comparator.comparingDouble(Factor::contribution).reversed())
                .map(Factor::reason)
                .distinct()
                .limit(REASON_LIMIT)
                .toList();
    }

    private static String formatHours(BigDecimal hours) {
        double value = hours.doubleValue();
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return ((int) Math.rint(value)) + "h";
        }
        return hours.setScale(1, RoundingMode.HALF_UP).toPlainString() + "h";
    }

    private static double roundScore(double score) {
        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
