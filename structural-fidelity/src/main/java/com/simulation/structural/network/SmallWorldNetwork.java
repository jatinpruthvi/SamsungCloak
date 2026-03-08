package com.simulation.structural.network;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmallWorldNetwork - Structural Network Topology Implementation
 *
 * Constructs a "Small-World Network" interaction graph ensuring that simulated
 * entity-to-entity interactions follow a non-random, clustered community structure
 * consistent with real social networks.
 *
 * Mathematical Foundation:
 *
 * Watts-Strogatz Model:
 * 1. Start with regular ring lattice: each node connected to k nearest neighbors
 * 2. Rewire edges with probability p: shortcuts create small-world property
 * 3. Result: High clustering (C ≈ C_regular) with short path lengths (L ≈ L_random)
 *
 * Key Metrics:
 * - Clustering Coefficient: C = 3 × (number of triangles) / (number of connected triples)
 * - Average Path Length: L = average shortest path between all node pairs
 * - Small-world coefficient: σ = (C/C_random) / (L/L_random) > 1
 *
 * Community Structure:
 * - Modularity: Q = Σ[ e_ii - (a_i)² ] where e_ii = intra-community edges, a_i = community degree
 * - Community detection via Louvain algorithm for realistic clustering
 *
 * Edge Formation:
 * - Homophily: P(edge) ∝ similarity(node_i, node_j)
 * - Triadic closure: P(edge|common_neighbor) = α × (shared_neighbors / max_possible)
 * - Preferential attachment: P(edge to i) ∝ degree(i)^β
 */
public class SmallWorldNetwork {

    private static final String LOG_TAG = "StructuralFidelity.SmallWorldNetwork";

    // Watts-Strogatz parameters
    private static final int DEFAULT_K = 6;
    private static final double DEFAULT_REWIRE_PROB = 0.15;

    // Community detection parameters
    private static final double MIN_MODULARITY_GAIN = 0.0001;
    private static final int MAX_COMMUNITY_ITERATIONS = 100;

    private final Map<String, NetworkNode> nodes;
    private final Map<String, Set<String>> adjacencyList;
    private final List<Edge> edges;
    private final Random random;

    // Network metrics
    private double clusteringCoefficient;
    private double averagePathLength;
    private double modularity;

    public SmallWorldNetwork() {
        this.nodes = new ConcurrentHashMap<>();
        this.adjacencyList = new ConcurrentHashMap<>();
        this.edges = new ArrayList<>();
        this.random = new Random();
        this.clusteringCoefficient = 0.0;
        this.averagePathLength = 0.0;
        this.modularity = 0.0;
    }

    /**
     * Network Node with attributes for community formation
     */
    public static class NetworkNode {
        private final String nodeId;
        private final Map<String, Double> attributes;
        private String communityId;
        private int degree;

        public NetworkNode(String nodeId) {
            this.nodeId = nodeId;
            this.attributes = new HashMap<>();
            this.communityId = null;
            this.degree = 0;
        }

        public void setAttribute(String key, double value) {
            attributes.put(key, value);
        }

        public double getAttribute(String key) {
            return attributes.getOrDefault(key, 0.0);
        }

        public void setCommunity(String communityId) {
            this.communityId = communityId;
        }

        public void incrementDegree() {
            degree++;
        }

        public String getNodeId() { return nodeId; }
        public Map<String, Double> getAttributes() { return new HashMap<>(attributes); }
        public String getCommunityId() { return communityId; }
        public int getDegree() { return degree; }
    }

    /**
     * Edge with weight and formation metadata
     */
    public static class Edge {
        private final String sourceId;
        private final String targetId;
        private final double weight;
        private final EdgeType type;
        private final long formationTime;

        public Edge(String sourceId, String targetId, double weight, EdgeType type) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.weight = weight;
            this.type = type;
            this.formationTime = System.currentTimeMillis();
        }

        public String getSourceId() { return sourceId; }
        public String getTargetId() { return targetId; }
        public double getWeight() { return weight; }
        public EdgeType getType() { return type; }
    }

    public enum EdgeType {
        REGULAR_LATTICE,    // Original ring lattice edge
        REWIRED,            // Rewired shortcut edge
        TRIADIC_CLOSURE,    // Formed via common neighbor
        PREFERENTIAL,       // Preferential attachment edge
        HOMOPHILY           // Similarity-based edge
    }

    /**
     * Add node to network
     */
    public NetworkNode addNode(String nodeId) {
        NetworkNode node = new NetworkNode(nodeId);
        nodes.put(nodeId, node);
        adjacencyList.put(nodeId, ConcurrentHashMap.newKeySet());
        return node;
    }

    /**
     * Add edge between nodes
     */
    public void addEdge(String sourceId, String targetId, double weight, EdgeType type) {
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return;
        }

        edges.add(new Edge(sourceId, targetId, weight, type));
        adjacencyList.get(sourceId).add(targetId);
        adjacencyList.get(targetId).add(sourceId);

        nodes.get(sourceId).incrementDegree();
        nodes.get(targetId).incrementDegree();
    }

    /**
     * Build Watts-Strogatz small-world network
     */
    public void buildWattsStrogatzNetwork(int n, int k, double rewireProb) {
        if (k % 2 != 0) k--;
        if (k < 2) k = 2;

        clearNetwork();

        for (int i = 0; i < n; i++) {
            addNode("node_" + i);
        }

        List<String> nodeIds = new ArrayList<>(nodes.keySet());

        for (int i = 0; i < n; i++) {
            for (int j = 1; j <= k / 2; j++) {
                int targetIndex = (i + j) % n;
                String source = nodeIds.get(i);
                String target = nodeIds.get(targetIndex);

                if (random.nextDouble() < rewireProb) {
                    target = selectRewireTarget(source, nodeIds, k);
                    addEdge(source, target, 1.0, EdgeType.REWIRED);
                } else {
                    addEdge(source, target, 1.0, EdgeType.REGULAR_LATTICE);
                }
            }
        }

        calculateNetworkMetrics();
    }

    private String selectRewireTarget(String source, List<String> allNodes, int k) {
        Set<String> existingNeighbors = adjacencyList.get(source);
        List<String> candidates = new ArrayList<>();

        for (String candidate : allNodes) {
            if (!candidate.equals(source) && !existingNeighbors.contains(candidate)) {
                candidates.add(candidate);
            }
        }

        if (candidates.isEmpty()) {
            return allNodes.get(random.nextInt(allNodes.size()));
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Build community-structured network with homophily
     */
    public void buildCommunityNetwork(int n, int numCommunities, double intraCommunityProb,
                                       double interCommunityProb) {
        clearNetwork();

        for (int i = 0; i < n; i++) {
            NetworkNode node = addNode("node_" + i);
            node.setCommunity("community_" + (i % numCommunities));
        }

        List<String> nodeIds = new ArrayList<>(nodes.keySet());

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                String nodeI = nodeIds.get(i);
                String nodeJ = nodeIds.get(j);

                String communityI = nodes.get(nodeI).getCommunityId();
                String communityJ = nodes.get(nodeJ).getCommunityId();

                double connectionProb = communityI.equals(communityJ)
                    ? intraCommunityProb
                    : interCommunityProb;

                if (random.nextDouble() < connectionProb) {
                    EdgeType type = communityI.equals(communityJ)
                        ? EdgeType.HOMOPHILY
                        : EdgeType.PREFERENTIAL;
                    addEdge(nodeI, nodeJ, 1.0, type);
                }
            }
        }

        applyTriadicClosure(0.3);
        calculateNetworkMetrics();
    }

    /**
     * Apply triadic closure: if A-B and A-C exist, form B-C with probability
     */
    public void applyTriadicClosure(double closureProbability) {
        List<Edge> newEdges = new ArrayList<>();

        for (String nodeA : nodes.keySet()) {
            Set<String> neighbors = getNeighbors(nodeA);
            List<String> neighborList = new ArrayList<>(neighbors);

            for (int i = 0; i < neighborList.size(); i++) {
                for (int j = i + 1; j < neighborList.size(); j++) {
                    String nodeB = neighborList.get(i);
                    String nodeC = neighborList.get(j);

                    if (!hasEdge(nodeB, nodeC)) {
                        double strength = calculateTriadicStrength(nodeA, nodeB, nodeC);
                        if (random.nextDouble() < closureProbability * strength) {
                            newEdges.add(new Edge(nodeB, nodeC, strength, EdgeType.TRIADIC_CLOSURE));
                        }
                    }
                }
            }
        }

        for (Edge edge : newEdges) {
            addEdge(edge.getSourceId(), edge.getTargetId(), edge.getWeight(), edge.getType());
        }
    }

    private double calculateTriadicStrength(String a, String b, String c) {
        Set<String> neighborsB = getNeighbors(b);
        Set<String> neighborsC = getNeighbors(c);

        Set<String> common = new HashSet<>(neighborsB);
        common.retainAll(neighborsC);
        common.add(a);

        int maxPossible = Math.min(neighborsB.size(), neighborsC.size());
        return maxPossible > 0 ? (double) common.size() / maxPossible : 0;
    }

    /**
     * Calculate global clustering coefficient
     * C = 3 × triangles / connected_triples
     */
    public double calculateClusteringCoefficient() {
        int triangles = 0;
        int connectedTriples = 0;

        for (String node : nodes.keySet()) {
            Set<String> neighbors = getNeighbors(node);
            List<String> neighborList = new ArrayList<>(neighbors);

            for (int i = 0; i < neighborList.size(); i++) {
                for (int j = i + 1; j < neighborList.size(); j++) {
                    connectedTriples++;
                    if (hasEdge(neighborList.get(i), neighborList.get(j))) {
                        triangles++;
                    }
                }
            }
        }

        return connectedTriples > 0 ? (3.0 * triangles) / connectedTriples : 0;
    }

    /**
     * Calculate average shortest path length using BFS
     */
    public double calculateAveragePathLength() {
        long totalPathLength = 0;
        int pathCount = 0;

        for (String source : nodes.keySet()) {
            Map<String, Integer> distances = bfsDistances(source);
            for (int dist : distances.values()) {
                if (dist > 0) {
                    totalPathLength += dist;
                    pathCount++;
                }
            }
        }

        return pathCount > 0 ? (double) totalPathLength / pathCount : 0;
    }

    private Map<String, Integer> bfsDistances(String source) {
        Map<String, Integer> distances = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        distances.put(source, 0);
        queue.offer(source);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDist = distances.get(current);

            for (String neighbor : getNeighbors(current)) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDist + 1);
                    queue.offer(neighbor);
                }
            }
        }

        for (String node : nodes.keySet()) {
            distances.putIfAbsent(node, Integer.MAX_VALUE);
        }

        return distances;
    }

    /**
     * Detect communities using Louvain algorithm
     */
    public Map<String, String> detectCommunities() {
        Map<String, String> communities = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            communities.put(nodeId, nodeId);
        }

        boolean improved = true;
        int iterations = 0;

        while (improved && iterations < MAX_COMMUNITY_ITERATIONS) {
            improved = false;
            iterations++;

            for (String nodeId : nodes.keySet()) {
                String bestCommunity = findBestCommunity(nodeId, communities);
                if (!bestCommunity.equals(communities.get(nodeId))) {
                    communities.put(nodeId, bestCommunity);
                    improved = true;
                }
            }
        }

        this.modularity = calculateModularity(communities);
        return communities;
    }

    private String findBestCommunity(String nodeId, Map<String, String> communities) {
        Map<String, Double> communityGains = new HashMap<>();
        String currentCommunity = communities.get(nodeId);

        for (String neighbor : getNeighbors(nodeId)) {
            String neighborCommunity = communities.get(neighbor);
            communityGains.merge(neighborCommunity, 1.0, Double::sum);
        }

        String bestCommunity = currentCommunity;
        double bestGain = 0;

        for (Map.Entry<String, Double> entry : communityGains.entrySet()) {
            if (entry.getValue() > bestGain) {
                bestGain = entry.getValue();
                bestCommunity = entry.getKey();
            }
        }

        return bestCommunity;
    }

    private double calculateModularity(Map<String, String> communities) {
        double q = 0;
        double m = edges.size();

        if (m == 0) return 0;

        for (Edge edge : edges) {
            String commI = communities.get(edge.getSourceId());
            String commJ = communities.get(edge.getTargetId());

            double ki = nodes.get(edge.getSourceId()).getDegree();
            double kj = nodes.get(edge.getTargetId()).getDegree();

            double delta = commI.equals(commJ) ? 1 : 0;
            q += delta - (ki * kj) / (2 * m);
        }

        return q / (2 * m);
    }

    /**
     * Calculate small-world coefficient
     * σ = (C/C_random) / (L/L_random)
     * σ > 1 indicates small-world property
     */
    public double calculateSmallWorldCoefficient() {
        int n = nodes.size();
        double k = edges.isEmpty() ? 0 : (2.0 * edges.size()) / n;

        if (n < 3 || k < 2) return 0;

        double cRandom = k / n;
        double lRandom = Math.log(n) / Math.log(k);

        double cActual = calculateClusteringCoefficient();
        double lActual = calculateAveragePathLength();

        if (cRandom == 0 || lRandom == 0) return 0;

        return (cActual / cRandom) / (lActual / lRandom);
    }

    private void calculateNetworkMetrics() {
        this.clusteringCoefficient = calculateClusteringCoefficient();
        this.averagePathLength = calculateAveragePathLength();
    }

    /**
     * Simulate interaction between two nodes with small-world routing
     */
    public boolean simulateInteraction(String sourceId, String targetId) {
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return false;
        }

        if (hasEdge(sourceId, targetId)) {
            return true;
        }

        List<String> path = findShortestPath(sourceId, targetId);
        if (path == null || path.isEmpty()) {
            return false;
        }

        double pathStrength = calculatePathStrength(path);
        return random.nextDouble() < pathStrength;
    }

    private List<String> findShortestPath(String source, String target) {
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        queue.offer(source);
        parent.put(source, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(target)) {
                List<String> path = new ArrayList<>();
                String node = target;
                while (node != null) {
                    path.add(0, node);
                    node = parent.get(node);
                }
                return path;
            }

            for (String neighbor : getNeighbors(current)) {
                if (!parent.containsKey(neighbor)) {
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return null;
    }

    private double calculatePathStrength(List<String> path) {
        double strength = 1.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Edge edge = findEdge(path.get(i), path.get(i + 1));
            if (edge != null) {
                strength *= edge.getWeight();
            }
        }
        return Math.pow(strength, 1.0 / path.size());
    }

    private Edge findEdge(String source, String target) {
        for (Edge edge : edges) {
            if ((edge.getSourceId().equals(source) && edge.getTargetId().equals(target)) ||
                (edge.getSourceId().equals(target) && edge.getTargetId().equals(source))) {
                return edge;
            }
        }
        return null;
    }

    private boolean hasEdge(String source, String target) {
        return adjacencyList.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    private Set<String> getNeighbors(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptySet());
    }

    private void clearNetwork() {
        nodes.clear();
        adjacencyList.clear();
        edges.clear();
    }

    /**
     * Get network metrics summary
     */
    public NetworkMetrics getMetrics() {
        return new NetworkMetrics(
            nodes.size(),
            edges.size(),
            clusteringCoefficient,
            averagePathLength,
            modularity,
            calculateSmallWorldCoefficient()
        );
    }

    public static class NetworkMetrics {
        public final int nodeCount;
        public final int edgeCount;
        public final double clusteringCoefficient;
        public final double averagePathLength;
        public final double modularity;
        public final double smallWorldCoefficient;

        public NetworkMetrics(int nodeCount, int edgeCount, double clusteringCoefficient,
                              double averagePathLength, double modularity,
                              double smallWorldCoefficient) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.clusteringCoefficient = clusteringCoefficient;
            this.averagePathLength = averagePathLength;
            this.modularity = modularity;
            this.smallWorldCoefficient = smallWorldCoefficient;
        }

        public boolean isSmallWorld() {
            return smallWorldCoefficient > 1.0;
        }

        @Override
        public String toString() {
            return String.format(
                "NetworkMetrics{nodes=%d, edges=%d, C=%.3f, L=%.3f, Q=%.3f, sigma=%.3f, smallWorld=%s}",
                nodeCount, edgeCount, clusteringCoefficient, averagePathLength,
                modularity, smallWorldCoefficient, isSmallWorld()
            );
        }
    }

    public Map<String, NetworkNode> getNodes() {
        return new HashMap<>(nodes);
    }

    public List<Edge> getEdges() {
        return new ArrayList<>(edges);
    }

    /**
     * Create realistic social network for population simulation
     */
    public static SmallWorldNetwork createSocialNetwork(int populationSize) {
        SmallWorldNetwork network = new SmallWorldNetwork();

        int k = Math.min(8, populationSize / 10);
        double rewireProb = 0.12;

        network.buildWattsStrogatzNetwork(populationSize, k, rewireProb);
        network.applyTriadicClosure(0.25);

        Map<String, String> communities = network.detectCommunities();
        for (Map.Entry<String, String> entry : communities.entrySet()) {
            NetworkNode node = network.nodes.get(entry.getKey());
            if (node != null) {
                node.setCommunity(entry.getValue());
            }
        }

        return network;
    }
}
