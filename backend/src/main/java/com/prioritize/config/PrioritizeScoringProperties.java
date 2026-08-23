package com.prioritize.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable priority-engine weights, level thresholds, and component defaults.
 * Bound from {@code prioritize.scoring.*} in application.yml.
 */
@ConfigurationProperties(prefix = "prioritize.scoring")
public class PrioritizeScoringProperties {

    private Weights weights = new Weights();
    private Thresholds thresholds = new Thresholds();
    private Defaults defaults = new Defaults();

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(Thresholds thresholds) {
        this.thresholds = thresholds;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public static class Weights {
        private double urgency = 0.35;
        private double points = 0.20;
        private double difficulty = 0.15;
        private double workload = 0.15;
        private double personalPriority = 0.15;

        public double getUrgency() {
            return urgency;
        }

        public void setUrgency(double urgency) {
            this.urgency = urgency;
        }

        public double getPoints() {
            return points;
        }

        public void setPoints(double points) {
            this.points = points;
        }

        public double getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(double difficulty) {
            this.difficulty = difficulty;
        }

        public double getWorkload() {
            return workload;
        }

        public void setWorkload(double workload) {
            this.workload = workload;
        }

        public double getPersonalPriority() {
            return personalPriority;
        }

        public void setPersonalPriority(double personalPriority) {
            this.personalPriority = personalPriority;
        }
    }

    public static class Thresholds {
        private double critical = 80.0;
        private double high = 60.0;
        private double medium = 40.0;

        public double getCritical() {
            return critical;
        }

        public void setCritical(double critical) {
            this.critical = critical;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getMedium() {
            return medium;
        }

        public void setMedium(double medium) {
            this.medium = medium;
        }
    }

    public static class Defaults {
        /** Urgency when no due date is set (0–100). */
        private double noDueDateUrgency = 35.0;
        /** Point-value component when pointsPossible is null. */
        private double missingPoints = 40.0;
        /** Difficulty component when difficulty is null/unknown. */
        private double missingDifficulty = 50.0;
        /** Workload component when estimatedHours is null. */
        private double missingWorkload = 40.0;
        /** Personal-priority component when unset. */
        private double missingPersonalPriority = 50.0;
        /** Hours at which workload component reaches 100. */
        private double maxWorkloadHours = 20.0;
        /** Absolute points at which absolute banding reaches 100. */
        private double maxAbsolutePoints = 100.0;
        /** Days over which urgency decays from 100 (at due) toward the floor. */
        private double urgencyHorizonDays = 14.0;
        /** Minimum urgency for far-future due dates. */
        private double urgencyFloor = 15.0;

        public double getNoDueDateUrgency() {
            return noDueDateUrgency;
        }

        public void setNoDueDateUrgency(double noDueDateUrgency) {
            this.noDueDateUrgency = noDueDateUrgency;
        }

        public double getMissingPoints() {
            return missingPoints;
        }

        public void setMissingPoints(double missingPoints) {
            this.missingPoints = missingPoints;
        }

        public double getMissingDifficulty() {
            return missingDifficulty;
        }

        public void setMissingDifficulty(double missingDifficulty) {
            this.missingDifficulty = missingDifficulty;
        }

        public double getMissingWorkload() {
            return missingWorkload;
        }

        public void setMissingWorkload(double missingWorkload) {
            this.missingWorkload = missingWorkload;
        }

        public double getMissingPersonalPriority() {
            return missingPersonalPriority;
        }

        public void setMissingPersonalPriority(double missingPersonalPriority) {
            this.missingPersonalPriority = missingPersonalPriority;
        }

        public double getMaxWorkloadHours() {
            return maxWorkloadHours;
        }

        public void setMaxWorkloadHours(double maxWorkloadHours) {
            this.maxWorkloadHours = maxWorkloadHours;
        }

        public double getMaxAbsolutePoints() {
            return maxAbsolutePoints;
        }

        public void setMaxAbsolutePoints(double maxAbsolutePoints) {
            this.maxAbsolutePoints = maxAbsolutePoints;
        }

        public double getUrgencyHorizonDays() {
            return urgencyHorizonDays;
        }

        public void setUrgencyHorizonDays(double urgencyHorizonDays) {
            this.urgencyHorizonDays = urgencyHorizonDays;
        }

        public double getUrgencyFloor() {
            return urgencyFloor;
        }

        public void setUrgencyFloor(double urgencyFloor) {
            this.urgencyFloor = urgencyFloor;
        }
    }
}
