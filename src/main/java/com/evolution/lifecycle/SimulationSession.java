package com.evolution.lifecycle;

import com.evolution.model.AppModule;
import com.evolution.model.InteractionMetric;
import com.evolution.model.LifecycleState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationSession {
    private final int day;
    private final LifecycleState lifecycleState;
    private final int durationSeconds;
    private final List<AppModule> visitedModules;
    private final Map<AppModule, Integer> moduleVisitCounts;
    private InteractionMetric metrics;

    public SimulationSession(int day, LifecycleState lifecycleState, int durationSeconds) {
        this.day = day;
        this.lifecycleState = lifecycleState;
        this.durationSeconds = durationSeconds;
        this.visitedModules = new ArrayList<>();
        this.moduleVisitCounts = new HashMap<>();
    }

    public void addModuleVisit(AppModule module) {
        visitedModules.add(module);
        moduleVisitCounts.merge(module, 1, Integer::sum);
    }

    public AppModule getLastModule() {
        if (visitedModules.isEmpty()) {
            return null;
        }
        return visitedModules.get(visitedModules.size() - 1);
    }

    public void setMetrics(InteractionMetric metrics) {
        this.metrics = metrics;
    }

    public int getDay() {
        return day;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public List<AppModule> getVisitedModules() {
        return new ArrayList<>(visitedModules);
    }

    public Map<AppModule, Integer> getModuleVisitCounts() {
        return new HashMap<>(moduleVisitCounts);
    }

    public InteractionMetric getMetrics() {
        return metrics;
    }

    public int getTotalInteractions() {
        return visitedModules.size();
    }

    @Override
    public String toString() {
        return String.format("Session{day=%d, state=%s, duration=%ds, interactions=%d}",
                day, lifecycleState.name(), durationSeconds, getTotalInteractions());
    }
}
