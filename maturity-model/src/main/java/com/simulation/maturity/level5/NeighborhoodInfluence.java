package com.simulation.maturity.level5;

import java.util.*;

/**
 * Social Proofing / Herd Behavior - Neighborhood Influence Model
 * 
 * Models how individual agent behavior is influenced by peer activity:
 * - Users observe peer actions through social graph
 * - Action probability weighted by neighbor activity
 * - Implements "social validation" effect
 * 
 * Mathematical Model:
 * P(action_i) = softmax(α * BaseP_i + β * NeighborActivity_i)
 * 
 * Where:
 * - α = Individual baseline weight
 * - β = Neighborhood influence coefficient
 * - NeighborActivity = sum of peer actions weighted by graph edges
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class NeighborhoodInfluence {
    
    // Social graph: agent -> list of neighbors
    private Map<String, Set<String>> socialGraph;
    
    // Activity history: agent -> action counts
    private Map<String, Map<String, Integer>> agentActivities;
    
    // Influence weights
    private double individualWeight;      // Alpha
    private double neighborhoodWeight;   // Beta
    
    // Time decay parameters
    private double activityDecayRate;    // How fast past activity becomes irrelevant
    private long timeWindowMs;           // Activity window to consider
    
    private Random random;
    
    public NeighborhoodInfluence() {
        this.socialGraph = new HashMap<>();
        this.agentActivities = new HashMap<>();
        
        this.individualWeight = 0.3;     // 30% individual baseline
        this.neighborhoodWeight = 0.7;   // 70% neighborhood influence
        this.activityDecayRate = 0.1;    // 10% decay per hour
        this.timeWindowMs = 3600000;     // 1 hour window
        
        this.random = new Random();
    }
    
    /**
     * Add agent to social graph
     * 
     * @param agentId Agent identifier
     * @param neighbors List of neighbor agent IDs
     */
    public void addAgent(String agentId, List<String> neighbors) {
        socialGraph.put(agentId, new HashSet<>(neighbors));
        agentActivities.put(agentId, new HashMap<>());
        
        // Add reverse edges (undirected graph)
        for (String neighbor : neighbors) {
            socialGraph.computeIfAbsent(neighbor, k -> new HashSet<>()).add(agentId);
            agentActivities.computeIfAbsent(neighbor, k -> new HashMap<>());
        }
    }
    
    /**
     * Record an action performed by an agent
     * 
     * @param agentId Agent who performed action
     * @param actionType Type of action (e.g., "view", "like", "purchase")
     */
    public void recordAction(String agentId, String actionType) {
        Map<String, Integer> actions = agentActivities.get(agentId);
        if (actions == null) {
            actions = new HashMap<>();
            agentActivities.put(agentId, actions);
        }
        
        actions.merge(actionType, 1, Integer::sum);
    }
    
    /**
     * Calculate weighted influence score for an agent-action pair
     * 
     * @param agentId Agent to evaluate
     * @param actionType Action type
     * @return Weighted influence score
     */
    public double calculateInfluenceScore(String agentId, String actionType) {
        // Individual baseline score (randomized to add variance)
        double individualScore = individualWeight * (0.5 + random.nextDouble() * 0.5);
        
        // Calculate neighborhood influence
        Set<String> neighbors = socialGraph.get(agentId);
        if (neighbors == null || neighbors.isEmpty()) {
            return individualScore;
        }
        
        double neighborActivity = 0;
        int activeNeighbors = 0;
        
        for (String neighbor : neighbors) {
            Map<String, Integer> neighborActions = agentActivities.get(neighbor);
            if (neighborActions != null) {
                Integer count = neighborActions.get(actionType);
                if (count != null && count > 0) {
                    // Apply time decay
                    double decay = Math.exp(-activityDecayRate);
                    neighborActivity += count * decay;
                    activeNeighbors++;
                }
            }
        }
        
        // Normalize by neighbor count
        if (activeNeighbors > 0) {
            neighborActivity /= activeNeighbors;
        }
        
        // Combine scores
        double neighborhoodScore = neighborhoodWeight * Math.min(1.0, neighborActivity / 10.0);
        
        return individualScore + neighborhoodScore;
    }
    
    /**
     * Calculate probability of agent performing action
     * 
     * @param agentId Agent to evaluate
     * @param actionType Action type
     * @param availableActions List of possible actions
     * @return Probability distribution
     */
    public Map<String, Double> getActionProbabilities(String agentId, 
                                                        String[] availableActions) {
        Map<String, Double> scores = new HashMap<>();
        double totalScore = 0;
        
        for (String action : availableActions) {
            double score = calculateInfluenceScore(agentId, action);
            scores.put(action, score);
            totalScore += score;
        }
        
        // Normalize to probabilities
        Map<String, Double> probabilities = new HashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            probabilities.put(entry.getKey(), entry.getValue() / totalScore);
        }
        
        return probabilities;
    }
    
    /**
     * Sample action based on probabilities
     * 
     * @param agentId Agent performing action
     * @param availableActions Possible actions
     * @return Selected action
     */
    public String sampleAction(String agentId, String[] availableActions) {
        Map<String, Double> probs = getActionProbabilities(agentId, availableActions);
        
        double roll = random.nextDouble();
        double cumulative = 0;
        
        for (String action : availableActions) {
            cumulative += probs.get(action);
            if (roll <= cumulative) {
                return action;
            }
        }
        
        return availableActions[availableActions.length - 1];
    }
    
    /**
     * Check if herd effect should trigger
     * Returns true when neighborhood activity is high enough to influence
     * 
     * @param agentId Agent to check
     * @param actionType Action type
     * @return true if herd effect significant
     */
    public boolean hasHerdInfluence(String agentId, String actionType) {
        double score = calculateInfluenceScore(agentId, actionType);
        return score > (individualWeight + neighborhoodWeight * 0.5);
    }
    
    /**
     * Get degree centrality (number of connections)
     */
    public int getDegreeCentrality(String agentId) {
        Set<String> neighbors = socialGraph.get(agentId);
        return neighbors != null ? neighbors.size() : 0;
    }
    
    /**
     * Get local clustering coefficient
     * Measures how connected the agent's neighbors are to each other
     */
    public double getClusteringCoefficient(String agentId) {
        Set<String> neighbors = socialGraph.get(agentId);
        if (neighbors == null || neighbors.size() < 2) {
            return 0;
        }
        
        int edgesBetweenNeighbors = 0;
        int possibleEdges = neighbors.size() * (neighbors.size() - 1) / 2;
        
        for (String neighbor : neighbors) {
            Set<String> neighborNeighbors = socialGraph.get(neighbor);
            if (neighborNeighbors != null) {
                for (String otherNeighbor : neighbors) {
                    if (!otherNeighbor.equals(neighbor) && 
                        neighborNeighbors.contains(otherNeighbor)) {
                        edgesBetweenNeighbors++;
                    }
                }
            }
        }
        
        return (double) edgesBetweenNeighbors / possibleEdges;
    }
    
    /**
     * Apply time decay to all activities
     */
    public void applyTimeDecay() {
        for (Map<String, Integer> actions : agentActivities.values()) {
            // Decay all counts
            actions.replaceAll((k, v) -> (int) (v * (1 - activityDecayRate)));
            
            // Remove zero entries
            actions.entrySet().removeIf(e -> e.getValue() == 0);
        }
    }
    
    /**
     * Set influence weights
     */
    public void setWeights(double individual, double neighborhood) {
        this.individualWeight = individual;
        this.neighborhoodWeight = neighborhood;
    }
    
    /**
     * Get summary for agent
     */
    public InfluenceSummary getSummary(String agentId) {
        return new InfluenceSummary(
            agentId,
            getDegreeCentrality(agentId),
            getClusteringCoefficient(agentId),
            agentActivities.getOrDefault(agentId, new HashMap<>()).values().stream()
                .mapToInt(Integer::intValue).sum(),
            hasHerdInfluence(agentId, "view")
        );
    }
    
    /**
     * Influence summary
     */
    public static class InfluenceSummary {
        public final String agentId;
        public final int connections;
        public final double clusteringCoeff;
        public final int totalActions;
        public final boolean herdInfluenced;
        
        public InfluenceSummary(String agentId, int connections, double clusteringCoeff,
                                int totalActions, boolean herdInfluenced) {
            this.agentId = agentId;
            this.connections = connections;
            this.clusteringCoeff = clusteringCoeff;
            this.totalActions = totalActions;
            this.herdInfluenced = herdInfluenced;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Influence{agent=%s, connections=%d, clustering=%.2f, " +
                "actions=%d, herdInfluenced=%s}",
                agentId, connections, clusteringCoeff, totalActions, herdInfluenced
            );
        }
    }
}
