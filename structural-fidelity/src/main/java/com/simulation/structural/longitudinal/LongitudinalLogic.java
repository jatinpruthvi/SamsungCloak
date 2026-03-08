package com.simulation.structural.longitudinal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LongitudinalLogic - Multi-Session Persistent State System
 *
 * Implementation of "Longitudinal Logic" where a session's actions are dictated
 * by the "Memory" of incomplete tasks and historical preferences from previous sessions.
 *
 * Core Concepts:
 *
 * 1. Episodic Memory: Specific interactions and outcomes from previous sessions
 *    - Retrieval probability decays over time: P(retrieve) = e^(-λ × Δt)
 *    - Emotional salience enhances retention: strength = base × (1 + |emotion|)
 *
 * 2. Semantic Memory: Generalized knowledge and preferences
 *    - Feature preferences learned across sessions
 *    - Interaction patterns abstracted from episodic traces
 *    - Preference drift with exposure to alternatives
 *
 * 3. Procedural Memory: Skill-based learning
 *    - Motor skill improvement (reduced latency)
    - Navigation efficiency gains
 *    - Error rate reduction with practice
 *
 * 4. Working Memory: Current session context
 *    - Limited capacity (7±2 chunks)
 *    - Primacy and recency effects
 *    - Interference from similar items
 *
 * 5. Task Persistence: Incomplete task resumption
 *    - Zeigarnik effect: interrupted tasks remembered better
 *    - Completion probability: P(complete) = f(time_elapsed, effort_invested, reward_value)
 *
 * Implementation Strategy:
 * - Session-to-session state persistence
 * - Decay-based memory retrieval
 * - Preference learning and drift
 * - Task queue with priority based on recency and effort
 */
public class LongitudinalLogic {

    private static final String LOG_TAG = "StructuralFidelity.LongitudinalLogic";

    // Memory decay constants
    private static final double EPISODIC_DECAY_RATE = 0.1;
    private static final double SEMANTIC_DECAY_RATE = 0.02;
    private static final double PROCEDURAL_RETENTION = 0.95;

    // Working memory limits
    private static final int WORKING_MEMORY_CAPACITY = 7;
    private static final double PRIMACY_WEIGHT = 1.3;
    private static final double RECENCY_WEIGHT = 1.5;

    private final Map<String, UserLongitudinalState> userStates;
    private final Random random;

    public LongitudinalLogic() {
        this.userStates = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    /**
     * User's complete longitudinal state across all sessions
     */
    public static class UserLongitudinalState {
        private final String userId;
        private final List<SessionRecord> sessionHistory;
        private final EpisodicMemory episodicMemory;
        private final SemanticMemory semanticMemory;
        private final ProceduralMemory proceduralMemory;
        private final WorkingMemory workingMemory;
        private final TaskPersistenceManager taskManager;

        private LocalDateTime lastSessionEnd;
        private int totalSessions;
        private long cumulativeSessionTime;

        public UserLongitudinalState(String userId) {
            this.userId = userId;
            this.sessionHistory = Collections.synchronizedList(new ArrayList<>());
            this.episodicMemory = new EpisodicMemory();
            this.semanticMemory = new SemanticMemory();
            this.proceduralMemory = new ProceduralMemory();
            this.workingMemory = new WorkingMemory();
            this.taskManager = new TaskPersistenceManager();
            this.totalSessions = 0;
            this.cumulativeSessionTime = 0;
        }

        public void recordSession(SessionRecord session) {
            sessionHistory.add(session);
            totalSessions++;
            cumulativeSessionTime += session.durationSeconds;
            lastSessionEnd = session.endTime;
        }

        public String getUserId() { return userId; }
        public List<SessionRecord> getSessionHistory() { return new ArrayList<>(sessionHistory); }
        public EpisodicMemory getEpisodicMemory() { return episodicMemory; }
        public SemanticMemory getSemanticMemory() { return semanticMemory; }
        public ProceduralMemory getProceduralMemory() { return proceduralMemory; }
        public WorkingMemory getWorkingMemory() { return workingMemory; }
        public TaskPersistenceManager getTaskManager() { return taskManager; }
        public LocalDateTime getLastSessionEnd() { return lastSessionEnd; }
        public int getTotalSessions() { return totalSessions; }
    }

    /**
     * Record of a completed session
     */
    public static class SessionRecord {
        public final String sessionId;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final long durationSeconds;
        public final int interactionCount;
        public final List<String> visitedFeatures;
        public final List<String> completedTasks;
        public final List<String> abandonedTasks;
        public final Map<String, Double> featureDwellTimes;
        public final double sessionSatisfaction;

        public SessionRecord(String sessionId, LocalDateTime startTime, LocalDateTime endTime,
                             int interactionCount, List<String> visitedFeatures,
                             List<String> completedTasks, List<String> abandonedTasks,
                             Map<String, Double> featureDwellTimes, double sessionSatisfaction) {
            this.sessionId = sessionId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = Duration.between(startTime, endTime).getSeconds();
            this.interactionCount = interactionCount;
            this.visitedFeatures = new ArrayList<>(visitedFeatures);
            this.completedTasks = new ArrayList<>(completedTasks);
            this.abandonedTasks = new ArrayList<>(abandonedTasks);
            this.featureDwellTimes = new HashMap<>(featureDwellTimes);
            this.sessionSatisfaction = sessionSatisfaction;
        }
    }

    /**
     * Episodic memory - specific event recall
     */
    public static class EpisodicMemory {
        private final List<MemoryTrace> traces;

        public EpisodicMemory() {
            this.traces = Collections.synchronizedList(new ArrayList<>());
        }

        public void encode(String event, double emotionalSalience, LocalDateTime timestamp) {
            traces.add(new MemoryTrace(event, emotionalSalience, timestamp));
        }

        public List<String> retrieveRelevant(LocalDateTime currentTime, int maxResults) {
            List<ScoredMemory> scored = new ArrayList<>();

            for (MemoryTrace trace : traces) {
                double score = calculateRetrievalProbability(trace, currentTime);
                if (score > 0.1) {
                    scored.add(new ScoredMemory(trace.event, score));
                }
            }

            scored.sort((a, b) -> Double.compare(b.score, a.score));

            List<String> results = new ArrayList<>();
            for (int i = 0; i < Math.min(maxResults, scored.size()); i++) {
                results.add(scored.get(i).event);
            }

            return results;
        }

        private double calculateRetrievalProbability(MemoryTrace trace, LocalDateTime currentTime) {
            long hoursElapsed = Duration.between(trace.timestamp, currentTime).toHours();
            double timeDecay = Math.exp(-EPISODIC_DECAY_RATE * hoursElapsed);
            double salienceBoost = 1.0 + Math.abs(trace.emotionalSalience);
            return Math.min(1.0, timeDecay * salienceBoost);
        }

        public void decay() {
            traces.removeIf(t -> t.emotionalSalience < 0.1);
        }

        private static class MemoryTrace {
            final String event;
            final double emotionalSalience;
            final LocalDateTime timestamp;

            MemoryTrace(String event, double emotionalSalience, LocalDateTime timestamp) {
                this.event = event;
                this.emotionalSalience = emotionalSalience;
                this.timestamp = timestamp;
            }
        }

        private static class ScoredMemory {
            final String event;
            final double score;

            ScoredMemory(String event, double score) {
                this.event = event;
                this.score = score;
            }
        }
    }

    /**
     * Semantic memory - generalized knowledge
     */
    public static class SemanticMemory {
        private final Map<String, FeaturePreference> featurePreferences;
        private final Map<String, CategoryKnowledge> categoryKnowledge;

        public SemanticMemory() {
            this.featurePreferences = new ConcurrentHashMap<>();
            this.categoryKnowledge = new ConcurrentHashMap<>();
        }

        public void updatePreference(String feature, double exposureTime, boolean wasSuccessful) {
            featurePreferences.computeIfAbsent(feature, FeaturePreference::new)
                .update(exposureTime, wasSuccessful);
        }

        public double getPreferenceStrength(String feature) {
            FeaturePreference pref = featurePreferences.get(feature);
            return pref != null ? pref.getStrength() : 0.5;
        }

        public List<String> getPreferredFeatures(int maxResults) {
            List<Map.Entry<String, FeaturePreference>> sorted = featurePreferences.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue().getStrength(),
                                                a.getValue().getStrength()))
                .limit(maxResults)
                .toList();

            return sorted.stream().map(Map.Entry::getKey).toList();
        }

        public void learnCategoryAssociation(String category, String feature, double strength) {
            categoryKnowledge.computeIfAbsent(category, CategoryKnowledge::new)
                .associate(feature, strength);
        }

        private static class FeaturePreference {
            final String feature;
            double totalExposure;
            int visitCount;
            int successCount;
            double recencyWeightedScore;

            FeaturePreference(String feature) {
                this.feature = feature;
            }

            void update(double exposureTime, boolean wasSuccessful) {
                totalExposure += exposureTime;
                visitCount++;
                if (wasSuccessful) successCount++;

                double successRate = (double) successCount / visitCount;
                recencyWeightedScore = (recencyWeightedScore * (1 - SEMANTIC_DECAY_RATE)) +
                                       (exposureTime * successRate * SEMANTIC_DECAY_RATE);
            }

            double getStrength() {
                if (visitCount == 0) return 0.5;
                double successRate = (double) successCount / visitCount;
                return Math.min(1.0, (recencyWeightedScore / 100) * successRate + 0.3);
            }
        }

        private static class CategoryKnowledge {
            final String category;
            final Map<String, Double> featureAssociations;

            CategoryKnowledge(String category) {
                this.category = category;
                this.featureAssociations = new HashMap<>();
            }

            void associate(String feature, double strength) {
                featureAssociations.merge(feature, strength, (a, b) -> a * 0.8 + b * 0.2);
            }
        }
    }

    /**
     * Procedural memory - skill-based learning
     */
    public static class ProceduralMemory {
        private final Map<String, Skill> skills;

        public ProceduralMemory() {
            this.skills = new ConcurrentHashMap<>();
        }

        public void practice(String skillName, double performanceQuality) {
            skills.computeIfAbsent(skillName, Skill::new).practice(performanceQuality);
        }

        public double getSkillLevel(String skillName) {
            Skill skill = skills.get(skillName);
            return skill != null ? skill.getLevel() : 0.1;
        }

        public long getExpectedLatency(String skillName, long baseLatency) {
            double skillLevel = getSkillLevel(skillName);
            double efficiency = 0.3 + (skillLevel * 0.7);
            return Math.round(baseLatency / efficiency);
        }

        public double getErrorProbability(String skillName, double baseErrorRate) {
            double skillLevel = getSkillLevel(skillName);
            return baseErrorRate * (1 - skillLevel * 0.8);
        }

        private static class Skill {
            final String name;
            double practiceCount;
            double performanceSum;
            double currentLevel;

            Skill(String name) {
                this.name = name;
                this.practiceCount = 0;
                this.performanceSum = 0;
                this.currentLevel = 0.1;
            }

            void practice(double performance) {
                practiceCount++;
                performanceSum += performance;

                double powerLawProgress = 1.0 - Math.exp(-0.3 * practiceCount);
                double recentPerformance = performanceSum / practiceCount;

                currentLevel = (currentLevel * PROCEDURAL_RETENTION) +
                               (powerLawProgress * recentPerformance * (1 - PROCEDURAL_RETENTION));
            }

            double getLevel() {
                return Math.min(0.95, currentLevel);
            }
        }
    }

    /**
     * Working memory - current session context
     */
    public static class WorkingMemory {
        private final List<String> activeItems;
        private final Map<String, Double> activationLevels;

        public WorkingMemory() {
            this.activeItems = Collections.synchronizedList(new ArrayList<>());
            this.activationLevels = new ConcurrentHashMap<>();
        }

        public void add(String item, double initialActivation) {
            if (activeItems.size() >= WORKING_MEMORY_CAPACITY) {
                removeLeastActivated();
            }

            activeItems.add(item);
            activationLevels.put(item, initialActivation);
        }

        public void activate(String item) {
            activationLevels.merge(item, RECENCY_WEIGHT, (a, b) -> Math.min(1.0, a * 0.7 + b));
        }

        public boolean isActive(String item) {
            return activeItems.contains(item);
        }

        public List<String> getActiveItems() {
            List<ScoredItem> scored = new ArrayList<>();
            for (int i = 0; i < activeItems.size(); i++) {
                String item = activeItems.get(i);
                double activation = activationLevels.getOrDefault(item, 0.5);

                double positionWeight = (i == 0) ? PRIMACY_WEIGHT :
                                       (i == activeItems.size() - 1) ? RECENCY_WEIGHT : 1.0;

                scored.add(new ScoredItem(item, activation * positionWeight));
            }

            scored.sort((a, b) -> Double.compare(b.score, a.score));
            return scored.stream().map(s -> s.item).toList();
        }

        private void removeLeastActivated() {
            String leastActivated = activeItems.stream()
                .min(Comparator.comparingDouble(a -> activationLevels.getOrDefault(a, 0.0)))
                .orElse(null);

            if (leastActivated != null) {
                activeItems.remove(leastActivated);
                activationLevels.remove(leastActivated);
            }
        }

        public void clear() {
            activeItems.clear();
            activationLevels.clear();
        }

        private static class ScoredItem {
            final String item;
            final double score;

            ScoredItem(String item, double score) {
                this.item = item;
                this.score = score;
            }
        }
    }

    /**
     * Task persistence manager - incomplete task tracking
     */
    public static class TaskPersistenceManager {
        private final List<IncompleteTask> incompleteTasks;
        private final List<CompletedTask> completedTaskHistory;

        public TaskPersistenceManager() {
            this.incompleteTasks = Collections.synchronizedList(new ArrayList<>());
            this.completedTaskHistory = Collections.synchronizedList(new ArrayList<>());
        }

        public void startTask(String taskId, String taskType, double estimatedEffort,
                              double rewardValue) {
            incompleteTasks.add(new IncompleteTask(taskId, taskType, estimatedEffort,
                rewardValue, LocalDateTime.now()));
        }

        public void updateTaskProgress(String taskId, double progressFraction) {
            for (IncompleteTask task : incompleteTasks) {
                if (task.taskId.equals(taskId)) {
                    task.progress = progressFraction;
                    task.lastAccessed = LocalDateTime.now();
                    break;
                }
            }
        }

        public void completeTask(String taskId) {
            IncompleteTask task = incompleteTasks.stream()
                .filter(t -> t.taskId.equals(taskId))
                .findFirst()
                .orElse(null);

            if (task != null) {
                incompleteTasks.remove(task);
                completedTaskHistory.add(new CompletedTask(task, LocalDateTime.now()));
            }
        }

        public void abandonTask(String taskId) {
            incompleteTasks.removeIf(t -> t.taskId.equals(taskId));
        }

        public List<IncompleteTask> getResumptionCandidates(LocalDateTime currentTime) {
            List<ScoredTask> scored = new ArrayList<>();

            for (IncompleteTask task : incompleteTasks) {
                double score = calculateResumptionProbability(task, currentTime);
                if (score > 0.2) {
                    scored.add(new ScoredTask(task, score));
                }
            }

            scored.sort((a, b) -> Double.compare(b.score, a.score));
            return scored.stream().map(s -> s.task).toList();
        }

        private double calculateResumptionProbability(IncompleteTask task,
                                                       LocalDateTime currentTime) {
            long hoursSinceLastAccess = Duration.between(task.lastAccessed, currentTime).toHours();

            double zeigarnikBoost = 1.0 + task.progress;
            double timeDecay = Math.exp(-0.05 * hoursSinceLastAccess);
            double effortSunk = task.progress * task.estimatedEffort;
            double rewardIncentive = task.rewardValue * (1 - task.progress);

            return Math.min(1.0, (zeigarnikBoost * timeDecay * (effortSunk + rewardIncentive)) / 10);
        }

        public static class IncompleteTask {
            public final String taskId;
            public final String taskType;
            public final double estimatedEffort;
            public final double rewardValue;
            public final LocalDateTime startTime;
            public LocalDateTime lastAccessed;
            public double progress;

            public IncompleteTask(String taskId, String taskType, double estimatedEffort,
                                  double rewardValue, LocalDateTime startTime) {
                this.taskId = taskId;
                this.taskType = taskType;
                this.estimatedEffort = estimatedEffort;
                this.rewardValue = rewardValue;
                this.startTime = startTime;
                this.lastAccessed = startTime;
                this.progress = 0.0;
            }
        }

        private static class CompletedTask {
            final IncompleteTask original;
            final LocalDateTime completionTime;

            CompletedTask(IncompleteTask original, LocalDateTime completionTime) {
                this.original = original;
                this.completionTime = completionTime;
            }
        }

        private static class ScoredTask {
            final IncompleteTask task;
            final double score;

            ScoredTask(IncompleteTask task, double score) {
                this.task = task;
                this.score = score;
            }
        }
    }

    /**
     * Initialize or retrieve user longitudinal state
     */
    public UserLongitudinalState getOrCreateUserState(String userId) {
        return userStates.computeIfAbsent(userId, UserLongitudinalState::new);
    }

    /**
     * Prepare new session based on longitudinal state
     */
    public SessionPreparation prepareSession(String userId) {
        UserLongitudinalState state = getOrCreateUserState(userId);
        LocalDateTime now = LocalDateTime.now();

        SessionPreparation prep = new SessionPreparation();

        if (state.getLastSessionEnd() != null) {
            prep.timeSinceLastSession = Duration.between(state.getLastSessionEnd(), now);
            prep.recalledEvents = state.getEpisodicMemory().retrieveRelevant(now, 5);
        }

        prep.recommendedFeatures = state.getSemanticMemory().getPreferredFeatures(5);
        prep.resumptionTasks = state.getTaskManager().getResumptionCandidates(now);

        Map<String, Double> skillLevels = new HashMap<>();
        skillLevels.put("navigation", state.getProceduralMemory().getSkillLevel("navigation"));
        skillLevels.put("search", state.getProceduralMemory().getSkillLevel("search"));
        skillLevels.put("form_completion", state.getProceduralMemory().getSkillLevel("form_completion"));
        prep.skillLevels = skillLevels;

        prep.predictedLatency = state.getProceduralMemory().getExpectedLatency("navigation", 800);
        prep.predictedErrorRate = state.getProceduralMemory().getErrorProbability("navigation", 0.15);

        return prep;
    }

    public static class SessionPreparation {
        public Duration timeSinceLastSession;
        public List<String> recalledEvents;
        public List<String> recommendedFeatures;
        public List<TaskPersistenceManager.IncompleteTask> resumptionTasks;
        public Map<String, Double> skillLevels;
        public long predictedLatency;
        public double predictedErrorRate;
    }

    /**
     * Record completed session and update all memory systems
     */
    public void recordSession(String userId, SessionRecord session) {
        UserLongitudinalState state = getOrCreateUserState(userId);
        state.recordSession(session);

        for (String feature : session.visitedFeatures) {
            double dwellTime = session.featureDwellTimes.getOrDefault(feature, 0.0);
            boolean success = !session.abandonedTasks.contains(feature);
            state.getSemanticMemory().updatePreference(feature, dwellTime, success);
        }

        for (String task : session.completedTasks) {
            state.getTaskManager().completeTask(task);
            state.getProceduralMemory().practice(task.split(":")[0], 0.9);
        }

        for (String task : session.abandonedTasks) {
            state.getEpisodicMemory().encode("abandoned:" + task, -0.5, session.endTime);
        }

        state.getEpisodicMemory().decay();
    }

    /**
     * Update working memory during active session
     */
    public void updateWorkingMemory(String userId, String item, boolean isNew) {
        UserLongitudinalState state = getOrCreateUserState(userId);
        if (isNew) {
            state.getWorkingMemory().add(item, 0.7);
        } else {
            state.getWorkingMemory().activate(item);
        }
    }

    /**
     * Get longitudinal statistics for user
     */
    public LongitudinalStatistics getStatistics(String userId) {
        UserLongitudinalState state = userStates.get(userId);
        if (state == null) {
            return new LongitudinalStatistics(0, 0, 0, Collections.emptyMap());
        }

        Map<String, Double> avgSkillLevels = new HashMap<>();
        avgSkillLevels.put("navigation", state.getProceduralMemory().getSkillLevel("navigation"));

        return new LongitudinalStatistics(
            state.getTotalSessions(),
            state.cumulativeSessionTime,
            state.getTaskManager().incompleteTasks.size(),
            avgSkillLevels
        );
    }

    public static class LongitudinalStatistics {
        public final int totalSessions;
        public final long cumulativeSessionTime;
        public final int incompleteTaskCount;
        public final Map<String, Double> skillLevels;

        public LongitudinalStatistics(int totalSessions, long cumulativeSessionTime,
                                      int incompleteTaskCount, Map<String, Double> skillLevels) {
            this.totalSessions = totalSessions;
            this.cumulativeSessionTime = cumulativeSessionTime;
            this.incompleteTaskCount = incompleteTaskCount;
            this.skillLevels = skillLevels;
        }
    }

    public Map<String, UserLongitudinalState> getAllUserStates() {
        return new HashMap<>(userStates);
    }
}
