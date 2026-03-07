package com.evolution.model;

import java.util.HashMap;
import java.util.Map;

public enum AppModule {
    HOME_SCREEN("home", 0.30, 1.0),
    SEARCH("search", 0.15, 0.8),
    PROFILE("profile", 0.10, 1.0),
    SETTINGS("settings", 0.05, 0.6),
    CONTENT_A("content_a", 0.15, 0.9),
    CONTENT_B("content_b", 0.10, 0.7),
    NOTIFICATIONS("notifications", 0.08, 0.85),
    FEATURE_X("feature_x", 0.04, 0.5),
    FEATURE_Y("feature_y", 0.03, 0.4);

    private final String moduleId;
    private final double initialVisitProbability;
    private final double baseEngagementScore;

    AppModule(String moduleId, double initialVisitProbability, double baseEngagementScore) {
        this.moduleId = moduleId;
        this.initialVisitProbability = initialVisitProbability;
        this.baseEngagementScore = baseEngagementScore;
    }

    public String getModuleId() {
        return moduleId;
    }

    public double getInitialVisitProbability() {
        return initialVisitProbability;
    }

    public double getBaseEngagementScore() {
        return baseEngagementScore;
    }

    private static final Map<String, AppModule> MODULE_MAP = new HashMap<>();

    static {
        for (AppModule module : values()) {
            MODULE_MAP.put(module.moduleId, module);
        }
    }

    public static AppModule fromId(String moduleId) {
        return MODULE_MAP.get(moduleId);
    }
}
