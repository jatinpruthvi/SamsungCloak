package com.simulation.maturity.level5;

import java.util.*;

/**
 * Long-Term Drift & Lifecycle - 90-Day Interest Decay Model
 * 
 * Simulates users naturally losing interest or evolving their app usage patterns over time.
 * Implements:
 * - Interest decay (usage decreases without reinforcement)
 * - Lifecycle state transitions (new -> active -> engaged -> lapsed -> churned)
 * - Weekly/monthly pattern evolution
 * - Novelty effect (initial high usage -> normalization)
 * 
 * Mathematical Model:
 * Interest(t) = I_0 * e^(-λt) + I_min + Seasonal(t) + Drift(t)
 * 
 * Where:
 * - I_0 = Initial interest
 * - λ = Decay constant
 * - Seasonal = Periodic variation
 * - Drift = Long-term trend changes
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class InterestDecayModel {
    
    // Lifecycle states
    public enum LifecycleState {
        NEW("New user, high exploration"),
        ACTIVE("Regular user, established patterns"),
        ENGAGED("Power user, high engagement"),
        AT_RISK("Declining usage, may churn"),
        LAPSED("Stopped using, may return"),
        CHURNED("No longer using app"),
        REACTIVATED("Returned after lapse");
        
        private final String description;
        
        LifecycleState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // User lifecycle data
    private String userId;
    private LifecycleState currentState;
    private double interestLevel;        // 0.0 - 1.0
    private double engagementScore;      // 0.0 - 1.0
    private int dayOfLifecycle;          // Days since first use
    private int totalSessions;
    private long totalSessionDurationMs;
    private Date firstSeenDate;
    private Date lastActiveDate;
    private Date churnDate;
    
    // Decay parameters
    private double decayConstant;        // Lambda for interest decay
    private double noveltyDecayRate;     // Rate of novelty loss
    private int noveltyPeriodDays;       // Days before novelty wears off
    private double minInterestFloor;     // Minimum interest level
    
    // Trend parameters
    private double weeklySeasonality;    // Weekend vs weekday effect
    private double driftRate;            // Long-term interest drift
    
    private Random random;
    
    public InterestDecayModel(String userId) {
        this.userId = userId;
        this.currentState = LifecycleState.NEW;
        this.interestLevel = 1.0;
        this.engagementScore = 0.5;
        this.dayOfLifecycle = 0;
        this.totalSessions = 0;
        this.totalSessionDurationMs = 0;
        
        this.decayConstant = 0.02;       // 2% decay per day
        this.noveltyDecayRate = 0.1;     // 10% novelty loss per week
        this.noveltyPeriodDays = 14;     // 2 weeks novelty period
        this.minInterestFloor = 0.1;     // 10% minimum interest
        
        this.weeklySeasonality = 0.15;   // 15% more/less on weekends
        this.driftRate = 0.0;            // No initial drift
        
        this.firstSeenDate = new Date();
        this.lastActiveDate = new Date();
        this.random = new Random();
    }
    
    /**
     * Update model for one day
     * Call this daily for each user
     * 
     * @param sessionsToday Number of sessions today
     * @param durationTodayMs Total session time today in ms
     */
    public void updateDaily(int sessionsToday, long durationTodayMs) {
        dayOfLifecycle++;
        totalSessions += sessionsToday;
        totalSessionDurationMs += durationTodayMs;
        
        // Update interest based on usage
        updateInterest(sessionsToday, durationTodayMs);
        
        // Update engagement score
        updateEngagement(sessionsToday, durationTodayMs);
        
        // Update lifecycle state
        updateLifecycleState();
        
        lastActiveDate = new Date();
    }
    
    /**
     * Calculate interest level for current day
     * Combines decay, novelty effects, and seasonal patterns
     */
    public double getInterestLevel() {
        // Base decay: interest decreases over time
        double baseDecay = Math.exp(-decayConstant * dayOfLifecycle);
        
        // Novelty effect: higher interest in early days
        double noveltyEffect = calculateNoveltyEffect();
        
        // Weekly seasonality
        double seasonality = calculateWeeklySeasonality();
        
        // Long-term drift
        double drift = driftRate * dayOfLifecycle / 30.0; // Monthly drift
        
        // Combine factors
        double interest = minInterestFloor + 
            (1.0 - minInterestFloor) * (baseDecay * 0.3 + noveltyEffect * 0.5 + seasonality * 0.2) +
            drift;
        
        return Math.max(minInterestFloor, Math.min(1.0, interest));
    }
    
    /**
     * Calculate novelty effect (high initially, decays over time)
     */
    private double calculateNoveltyEffect() {
        if (dayOfLifecycle <= noveltyPeriodDays) {
            // Linear decay during novelty period
            return 1.0 - (dayOfLifecycle * noveltyDecayRate / 7.0); // Weekly decay
        } else {
            // After novelty period, effect is minimal
            return 0.1;
        }
    }
    
    /**
     * Calculate weekly seasonality effect
     */
    private double calculateWeeklySeasonality() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // Weekend (Saturday=7, Sunday=1) typically has more usage
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return 1.0 + weeklySeasonality;
        }
        
        return 1.0 - (weeklySeasonality * 0.5);
    }
    
    /**
     * Update interest based on activity
     */
    private void updateInterest(int sessions, long durationMs) {
        if (sessions > 0) {
            // Positive reinforcement increases interest
            double sessionBoost = Math.min(0.1, sessions * 0.02);
            interestLevel = Math.min(1.0, interestLevel + sessionBoost);
        } else {
            // No activity = decay
            interestLevel = Math.max(minInterestFloor, 
                interestLevel * (1.0 - decayConstant));
        }
    }
    
    /**
     * Update engagement score
     */
    private void updateEngagement(int sessions, long durationMs) {
        if (sessions > 0) {
            // Longer sessions and more sessions = higher engagement
            double sessionEngagement = Math.min(0.05, sessions * 0.01);
            double durationEngagement = Math.min(0.05, durationMs / 3600000.0); // Hourly
            
            engagementScore = Math.min(1.0, engagementScore + sessionEngagement + durationEngagement);
        } else {
            // Decay without activity
            engagementScore = Math.max(0, engagementScore - 0.02);
        }
    }
    
    /**
     * Update lifecycle state based on metrics
     */
    private void updateLifecycleState() {
        switch (currentState) {
            case NEW:
                if (dayOfLifecycle > 7 && engagementScore > 0.3) {
                    currentState = LifecycleState.ACTIVE;
                } else if (dayOfLifecycle > 14) {
                    currentState = LifecycleState.AT_RISK;
                }
                break;
                
            case ACTIVE:
                if (engagementScore > 0.7) {
                    currentState = LifecycleState.ENGAGED;
                } else if (interestLevel < 0.3) {
                    currentState = LifecycleState.AT_RISK;
                }
                break;
                
            case ENGAGED:
                if (interestLevel < 0.4) {
                    currentState = LifecycleState.AT_RISK;
                }
                break;
                
            case AT_RISK:
                if (totalSessions == 0 && dayOfLifecycle > 7) {
                    currentState = LifecycleState.LAPSED;
                } else if (engagementScore > 0.5) {
                    currentState = LifecycleState.ACTIVE;
                }
                break;
                
            case LAPSED:
                if (interestLevel > 0.5) {
                    currentState = LifecycleState.REACTIVATED;
                } else if (dayOfLifecycle > 30) {
                    currentState = LifecycleState.CHURNED;
                }
                break;
                
            case REACTIVATED:
                if (interestLevel < 0.3) {
                    currentState = LifecycleState.LAPSED;
                } else if (engagementScore > 0.6) {
                    currentState = LifecycleState.ACTIVE;
                }
                break;
                
            case CHURNED:
                // Final state, no transitions
                break;
        }
    }
    
    /**
     * Simulate a user session
     * Returns session duration in ms
     */
    public long simulateSession() {
        // Base duration from interest level
        double baseDuration = 300000 + random.nextInt(600000); // 5-15 min base
        
        // Adjust by interest
        double interestMultiplier = interestLevel * 0.5 + 0.5;
        
        // Adjust by state
        double stateMultiplier = 1.0;
        switch (currentState) {
            case NEW: stateMultiplier = 1.3; break;
            case ENGAGED: stateMultiplier = 1.5; break;
            case AT_RISK: stateMultiplier = 0.7; break;
            case LAPSED: stateMultiplier = 0.3; break;
        }
        
        long sessionDuration = (long) (baseDuration * interestMultiplier * stateMultiplier);
        
        // Update model
        updateDaily(1, sessionDuration);
        
        return sessionDuration;
    }
    
    /**
     * Trigger external reinforcement (e.g., notification, promotion)
     * Boosts interest temporarily
     */
    public void triggerReinforcement(double boost) {
        interestLevel = Math.min(1.0, interestLevel + boost);
        
        // Potential state recovery
        if (currentState == LifecycleState.LAPSED) {
            currentState = LifecycleState.REACTIVATED;
        } else if (currentState == LifecycleState.AT_RISK) {
            engagementScore = Math.min(1.0, engagementScore + 0.2);
        }
    }
    
    /**
     * Check if user has churned
     */
    public boolean isChurned() {
        return currentState == LifecycleState.CHURNED;
    }
    
    /**
     * Get expected sessions for next day
     */
    public int getExpectedSessionsTomorrow() {
        double interest = getInterestLevel();
        
        switch (currentState) {
            case NEW: return (int) (3 * interest) + 1;
            case ACTIVE: return (int) (2 * interest) + 1;
            case ENGAGED: return (int) (4 * interest) + 2;
            case AT_RISK: return (int) (1 * interest);
            case LAPSED: return 0;
            case REACTIVATED: return (int) (2 * interest) + 1;
            default: return 0;
        }
    }
    
    /**
     * Get current state
     */
    public LifecycleState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get engagement score
     */
    public double getEngagementScore() {
        return engagementScore;
    }
    
    /**
     * Get days since first seen
     */
    public int getDayOfLifecycle() {
        return dayOfLifecycle;
    }
    
    /**
     * Set decay constant
     */
    public void setDecayConstant(double lambda) {
        this.decayConstant = lambda;
    }
    
    /**
     * Get lifecycle summary
     */
    public LifecycleSummary getSummary() {
        return new LifecycleSummary(
            userId, currentState, dayOfLifecycle, interestLevel, engagementScore,
            totalSessions, totalSessionDurationMs
        );
    }
    
    /**
     * Lifecycle summary
     */
    public static class LifecycleSummary {
        public final String userId;
        public final LifecycleState state;
        public final int day;
        public final double interest;
        public final double engagement;
        public final int totalSessions;
        public final long totalDurationMs;
        
        public LifecycleSummary(String userId, LifecycleState state, int day,
                                double interest, double engagement,
                                int totalSessions, long totalDurationMs) {
            this.userId = userId;
            this.state = state;
            this.day = day;
            this.interest = interest;
            this.engagement = engagement;
            this.totalSessions = totalSessions;
            this.totalDurationMs = totalDurationMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Lifecycle{user=%s, state=%s, day=%d, interest=%.1f%%, " +
                "engagement=%.1f%%, sessions=%d, duration=%.1fh}",
                userId, state, day, interest*100, engagement*100,
                totalSessions, totalDurationMs/3600000.0
            );
        }
    }
}
