package com.evolution.model;

public enum LifecycleState {
    ONBOARDING(0, 7, "User is learning the app basics"),
    EXPLORER(7, 30, "User is discovering features and modules"),
    UTILITY(30, 60, "User uses specific features for specific goals"),
    POWER_USER(60, 90, "User has mastered the app and uses it efficiently"),
    CHURNED(-1, -1, "User has stopped using the app");

    private final int startDay;
    private final int endDay;
    private final String description;

    LifecycleState(int startDay, int endDay, String description) {
        this.startDay = startDay;
        this.endDay = endDay;
        this.description = description;
    }

    public int getStartDay() {
        return startDay;
    }

    public int getEndDay() {
        return endDay;
    }

    public String getDescription() {
        return description;
    }

    public static LifecycleState getStateForDay(int day) {
        if (day >= 0 && day < 7) return ONBOARDING;
        if (day >= 7 && day < 30) return EXPLORER;
        if (day >= 30 && day < 60) return UTILITY;
        if (day >= 60 && day < 90) return POWER_USER;
        return POWER_USER;
    }

    public boolean isTransitionDay(int day) {
        return day == 7 || day == 30 || day == 60;
    }
}
