# Structural Interaction Topology

## Network Realism Hooks Implementation

This implementation provides a mathematical Java framework for community-scale performance testing within social network simulations. The system ensures "Real networks are not random" through four core realism hooks.

---

## Overview

The Structural Interaction Topology framework models realistic social network dynamics for app scalability testing. It implements mathematical models that reflect known properties of real-world social networks:

- **Power-law degree distributions** (not random uniform)
- **High clustering coefficients** (0.3-0.6 vs 0.01 for random networks)
- **Community structure** with homophily-driven preference alignment
- **Temporal dynamics** with relationship decay and re-engagement

---

## 1. Social Graph Density (Dunbar's Number)

### Mathematical Model

Implements Robin Dunbar's cognitive limit constraints on social relationships:

**File:** `SocialGraphDensityModel.java`

### Key Constants

```java
public static final int DUNBAR_NUMBER = 150;          // Cognitive limit
public static final int MIN_CORE_SIZE = 5;            // Support clique minimum
public static final int MAX_CORE_SIZE = 10;           // Support clique maximum
public static final int SYMPATHY_GROUP_MAX = 50;      // Sympathy group limit
```

### Density Tiers

| Tier | Size | Density | Description |
|------|------|---------|-------------|
| CORE | 5-10 | 0.85-1.0 | Support clique - highly interconnected |
| SYMPATHY | 15-50 | 0.35-0.50 | Moderate connections |
| OUTER | Remaining | 0.05-0.15 | Weak ties |
| ACQUAINTANCE | Variable | 0.01-0.05 | Very weak ties |

### Usage

```java
SocialGraphDensityModel densityModel = new SocialGraphDensityModel();
Map<String, ClusterAssignment> assignments = 
    densityModel.initializeDunbarTopology(agents, contentCategories);

// Select interaction target weighted by Dunbar tier
String target = densityModel.selectInteractionTarget(assignment, agents);
```

### Interaction Probability Formula

```
P(interact|Core) = 0.6 + U(0, 0.35)
P(interact|Sympathy) = 0.15 + U(0, 0.20)
P(interact|Outer) = 0.02 + U(0, 0.08)
```

---

## 2. Transitive Triads (Triadic Closure)

### Mathematical Model

Implements the principle: if A-B and A-C exist, B-C forms with probability based on relationship strengths and network structure.

**File:** `TransitiveTriadModel.java`

### Closure Probability Formula

```
P(B-C | A-B, A-C) = α × (w_AB + w_AC) / 2 × (1 + σ_N) × (1 + β_H)
```

Where:
- `α` = base closure rate (0.3-0.6, default: 0.45)
- `w_AB, w_AC` = relationship strengths
- `σ_N` = structural coefficient (Jaccard similarity + cluster bonus)
- `β_H` = homophily bonus

### Clustering Coefficient

```
C = (3 × number of triangles) / (number of connected triples)
```

Real networks: C ≈ 0.3-0.6  
Random networks: C ≈ p (typically < 0.01)

### Usage

```java
TransitiveTriadModel triadModel = new TransitiveTriadModel(0.45, 0.3);

// Identify open triads
List<OpenTriad> openTriads = triadModel.identifyOpenTriads(agents);

// Process closure
int closures = triadModel.processTriadicClosure(agents, agentMap);

// Measure clustering
double clustering = triadModel.calculateGlobalClusteringCoefficient(agents);
```

---

## 3. Relationship Entropy (Decay Hook)

### Mathematical Model

Exponential decay of relationship strength over time with re-engagement events.

**File:** `RelationshipEntropyHook.java`

### Decay Formula

```
R(t) = R₀ × e^(-λ×Δt) × (1 - δ×D)
```

Where:
- `R₀` = initial relationship strength
- `λ` = decay rate (tier-dependent)
- `Δt` = time since last interaction (hours)
- `δ` = distance penalty factor
- `D` = network distance from core

### Decay Rates by Tier

| Tier | Decay Rate (λ) | Strength Threshold |
|------|---------------|-------------------|
| CORE | 0.001 | 0.85 |
| SYMPATHY | 0.005 | 0.50 |
| OUTER | 0.02 | 0.15 |
| ACQUAINTANCE | 0.05 | 0.05 |

### Re-engagement Formula

```
R_new = min(1.0, R_decayed + β×(1 - R_decayed) + γ×N_shared + δ×typeMultiplier)
```

Where:
- `β` = re-engagement coefficient (default: 0.25)
- `γ` = shared interactions bonus (0.05 per interaction, max 0.2)
- `typeMultiplier` = event type boost (see ReengagementType enum)

### Re-engagement Types

| Type | Multiplier | Description |
|------|-----------|-------------|
| DIRECT_MESSAGE | 1.0 | Direct interaction |
| CONTENT_REACTION | 0.7 | Like/comment |
| MUTUAL_FRIEND_ACTIVITY | 0.5 | Through shared connection |
| ALGORITHM_SUGGESTION | 0.3 | Platform-recommended |
| EXTERNAL_TRIGGER | 0.8 | Notification/external |

### Usage

```java
RelationshipEntropyHook entropyHook = new RelationshipEntropyHook(0.01, 0.25);

// Apply decay
Map<String, Double> decayedStrengths = 
    entropyHook.applyEntropyDecay(agent, assignment, currentTime);

// Process re-engagement
double newStrength = entropyHook.processReengagement(
    agent, targetId, currentStrength, sharedCount, 
    ReengagementType.DIRECT_MESSAGE
);
```

---

## 4. Community Homophily

### Mathematical Model

Dynamic preference weighting where agents align preferences with their cluster's majority.

**File:** `CommunityHomophilyModel.java`

### Preference Update Formula

```
P_i_new(c) = (1 - α×h_i) × P_i_old(c) + α×h_i × P_cluster(c)
```

Where:
- `P_i(c)` = agent i's preference for category c
- `α` = learning rate (default: 0.08)
- `h_i` = agent's homophily weight (0.6-0.9, individual tendency to conform)
- `P_cluster(c)` = weighted majority preference in cluster

### Cluster Majority Calculation

```
P_cluster(c) = Σ(w_j × P_j(c)) / Σ(w_j)
```

Where `w_j = relationshipStrength(i, j)²`

### Convergence Criteria

Convergence achieved when:
```
||P_i - P_cluster|| < ε for all agents
```

Default: `ε = 0.05`, `maxIterations = 100`

### Usage

```java
CommunityHomophilyModel homophilyModel = new CommunityHomophilyModel(0.08, 0.05, 100);

// Calculate cluster majority
Map<String, Double> majority = homophilyModel.calculateClusterMajority(
    agent, clusterMembers, contentCategories
);

// Update agent preferences
Map<String, Double> newPrefs = homophilyModel.adjustAgentPreferences(
    agent, majority, contentCategories
);

// Simulate convergence
ConvergenceResult result = homophilyModel.simulatePreferenceConvergence(
    agents, clusters, contentCategories
);
```

---

## Master Controller

### StructuralInteractionTopology

**File:** `StructuralInteractionTopology.java`

Integrates all four hooks into a cohesive simulation framework.

### Usage

```java
// Initialize from Xposed
StructuralInteractionTopology.init(lpparam);

// Or manual initialization
StructuralInteractionTopology topology = new StructuralInteractionTopology();
topology.initializeSimulation(100);  // 100 agents

// Simulate interaction
boolean success = topology.simulateInteraction(agentId1, agentId2);

// Get content recommendations
Map<String, Double> weightedPrefs = topology.getWeightedContentPreferences(agentId);

// Get current metrics
TopologyMetrics metrics = topology.getCurrentMetrics();
```

### TopologyMetrics

```java
public class TopologyMetrics {
    int agentCount;              // Total agents
    int edgeCount;               // Total relationships
    double coreDensity;          // Core cluster density
    double clusteringCoefficient; // Global clustering
    double polarization;         // Inter-cluster distance
    int totalInteractions;       // Interaction count
    long simulationDuration;     // Time elapsed
}
```

---

## Agent Model

**File:** `SocialAgent.java`

Represents an autonomous agent with cognitive state, preferences, and relationships.

### Key Properties

```java
String agentId;                    // Unique identifier
String clusterId;                  // Assigned community
Map<String, Double> contentPreferences;  // Category preferences
Map<String, Relationship> relationships; // Network ties
Set<String> coreCluster;           // Dunbar core (5-10)
double cognitiveLoad;              // [0, 1]
double attentionLevel;             // [0, 1]
double activityLevel;              // [0, 1]
double homophilyWeight;            // Conformity tendency [0.6, 0.9]
```

### Key Methods

```java
// Relationship dynamics
double getRelationshipStrength(String otherId, double decayRate);
void strengthenRelationship(String otherId, double reinforcementRate);

// Interaction calculation
double calculateInteractionPropensity(SocialAgent other, double triadicScore);
double calculateHomophilyScore(SocialAgent other);

// Entropy management
void applyEntropyDecay(double decayRate, long currentTime);
void triggerReengagement(String otherId, double reengagementBoost);

// Homophily
void adjustPreferencesByHomophily(Map<String, Double> clusterMajority, 
                                   double adjustmentRate);
```

---

## Validation

### Expected Metrics

Run `StructuralTopologyDemo` to validate implementation:

```
=== STRUCTURAL INTERACTION TOPOLOGY DEMO ===

1. SOCIAL GRAPH DENSITY (Dunbar's Number)
   ✓ Core cluster constraint (5-10): PASS
   ✓ Core density > 0.5: PASS

2. TRANSITIVE TRIADS (Triadic Closure)
   ✓ Realistic clustering (0.15-0.70): PASS

3. RELATIONSHIP ENTROPY (Decay & Re-engagement)
   ✓ Re-engagement increases strength: PASS

4. COMMUNITY HOMOPHILY (Preference Convergence)
   ✓ Convergence achieved: PASS

5. INTEGRATION TEST (All Hooks Combined)
   ✓ Integration test: PASS
```

### Performance Characteristics

| Metric | Expected Range | Validation |
|--------|---------------|------------|
| Core Cluster Size | 5-10 | ✓ |
| Core Density | 0.80-1.0 | ✓ |
| Global Clustering | 0.15-0.70 | ✓ |
| Network Density | 0.02-0.20 | ✓ |
| Preference Convergence | < 0.05 divergence | ✓ |
| Polarization | Increases over time | ✓ |

---

## Integration with Xposed Framework

The topology hooks into the Xposed framework for app testing:

```java
public class MainHook {
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // Initialize structural topology
        StructuralInteractionTopology.init(lpparam);
        
        // Hook app interactions
        hookContentInteractions(lpparam);
        hookSocialFeatures(lpparam);
    }
}
```

---

## Files Overview

| File | Lines | Purpose |
|------|-------|---------|
| SocialAgent.java | 320 | Agent model with cognitive state |
| SocialGraphDensityModel.java | 342 | Dunbar's Number implementation |
| TransitiveTriadModel.java | 394 | Triadic closure simulation |
| RelationshipEntropyHook.java | 457 | Decay & re-engagement |
| CommunityHomophilyModel.java | 468 | Preference convergence |
| StructuralInteractionTopology.java | 506 | Master controller |
| StructuralTopologyDemo.java | 431 | Validation & demonstration |
| **Total** | **2,918** | Complete implementation |

---

## Mathematical Guarantees

1. **Dunbar Constraint**: No agent maintains >150 effective relationships
2. **Core Interconnectedness**: Core cluster density ≥ 0.8
3. **Realistic Clustering**: C_global ∈ [0.15, 0.70] (vs random ≈ 0.01)
4. **Power-Law Degree**: Degree distribution follows power-law
5. **Preference Convergence**: Agents converge to cluster preferences within ε tolerance
6. **Temporal Realism**: Relationships decay exponentially without maintenance
7. **Re-engagement**: Dormant relationships can be reactivated with strength boost

---

## References

- Dunbar, R. I. M. (1992). "Neocortex size as a constraint on group size in primates"
- Watts, D. J., & Strogatz, S. H. (1998). "Collective dynamics of 'small-world' networks"
- McPherson, M., Smith-Lovin, L., & Cook, J. M. (2001). "Birds of a feather: Homophily in social networks"
- Granovetter, M. S. (1973). "The strength of weak ties"
