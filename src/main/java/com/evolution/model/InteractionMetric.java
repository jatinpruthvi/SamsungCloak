package com.evolution.model;

public class InteractionMetric {
    private double navigationLatencyMs;
    private double errorRate;
    private int sessionCount;
    private long totalInteractions;
    private double avgSessionDurationSeconds;

    public InteractionMetric(double initialLatencyMs, double initialErrorRate) {
        this.navigationLatencyMs = initialLatencyMs;
        this.errorRate = initialErrorRate;
        this.sessionCount = 0;
        this.totalInteractions = 0;
        this.avgSessionDurationSeconds = 60.0;
    }

    public double getNavigationLatencyMs() {
        return navigationLatencyMs;
    }

    public void setNavigationLatencyMs(double navigationLatencyMs) {
        this.navigationLatencyMs = navigationLatencyMs;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void incrementSessionCount() {
        this.sessionCount++;
    }

    public long getTotalInteractions() {
        return totalInteractions;
    }

    public void incrementInteractions(int count) {
        this.totalInteractions += count;
    }

    public double getAvgSessionDurationSeconds() {
        return avgSessionDurationSeconds;
    }

    public void setAvgSessionDurationSeconds(double avgSessionDurationSeconds) {
        this.avgSessionDurationSeconds = avgSessionDurationSeconds;
    }

    @Override
    public String toString() {
        return String.format("InteractionMetric{latency=%.2fms, errorRate=%.4f, sessions=%d, interactions=%d}",
                navigationLatencyMs, errorRate, sessionCount, totalInteractions);
    }
}
