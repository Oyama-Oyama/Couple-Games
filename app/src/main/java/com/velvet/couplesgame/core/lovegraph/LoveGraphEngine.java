package com.velvet.couplesgame.core.lovegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the "Love Map" of a relationship from purely local data.
 *
 * <p>The map is an abstract constellation, not a geographic one: every node is
 * a shared ritual or milestone the couple has accumulated (games played, mood
 * check-ins, date plans, sealed letters, badges, days together), and the links
 * express how strongly those parts of the relationship are connected.
 *
 * <p>No Android dependency and no network access, so the whole derivation is
 * unit-testable.
 */
public final class LoveGraphEngine {

    /** The kind of shared activity a node represents. */
    public enum NodeKind {
        /** Days spent together. */
        TOGETHER,
        /** Games and modes played. */
        PLAY,
        /** Daily mood check-ins. */
        MOOD,
        /** Date plans scheduled. */
        DATE,
        /** Sealed love letters exchanged. */
        LETTER,
        /** Milestone badges unlocked. */
        BADGE,
        /** Tasks marked as favourites. */
        FAVORITE
    }

    /** One point on the map. */
    public static final class Node {
        public final String id;
        public final NodeKind kind;
        /** Localised label resolved by the UI layer. */
        public final String label;
        /** Raw magnitude behind this node, e.g. number of days together. */
        public final int value;
        /** Normalised weight in 0..1 driving the node radius. */
        public final float weight;
        /** Angle on the circle, in radians. */
        public final float angle;
        /** Index of the ring the node sits on; 0 is the innermost. */
        public final int ring;

        public Node(String id, NodeKind kind, String label, int value,
                   float weight, float angle, int ring) {
            this.id = id;
            this.kind = kind;
            this.label = label;
            this.value = value;
            this.weight = weight;
            this.angle = angle;
            this.ring = ring;
        }
    }

    /** A connection between two nodes. */
    public static final class Link {
        public final String fromId;
        public final String toId;
        /** Strength in 0..1 driving the stroke width. */
        public final float strength;

        public Link(String fromId, String toId, float strength) {
            this.fromId = fromId;
            this.toId = toId;
            this.strength = strength;
        }
    }

    /** Everything the engine needs, gathered by the caller from local stores. */
    public static final class Snapshot {
        public int daysTogether = -1;
        public int anniversaryCountdown = -1;
        public int totalSessions;
        public int distinctModes;
        public int moodStreak;
        public int moodTotalDays;
        public int datePlans;
        public int sealedLetters;
        public int openedLetters;
        public int badgesUnlocked;
        public int badgesTotal;
        public int favorites;

        /** Distinct game modes played, for the "variety" bonus. */
        public final Set<String> modeIds = new LinkedHashSet<>();

        public Snapshot withDaysTogether(int days) {
            this.daysTogether = days;
            return this;
        }

        public Snapshot withAnniversary(int days) {
            this.anniversaryCountdown = days;
            return this;
        }

        public Snapshot withSessions(int total, Set<String> modes) {
            this.totalSessions = Math.max(0, total);
            this.distinctModes = modes != null ? modes.size() : 0;
            if (modes != null) this.modeIds.addAll(modes);
            return this;
        }

        public Snapshot withMood(int streak, int totalDays) {
            this.moodStreak = Math.max(0, streak);
            this.moodTotalDays = Math.max(0, totalDays);
            return this;
        }

        public Snapshot withDatePlans(int count) {
            this.datePlans = Math.max(0, count);
            return this;
        }

        public Snapshot withLetters(int sealed, int opened) {
            this.sealedLetters = Math.max(0, sealed);
            this.openedLetters = Math.max(0, opened);
            return this;
        }

        public Snapshot withBadges(int unlocked, int total) {
            this.badgesUnlocked = Math.max(0, unlocked);
            this.badgesTotal = Math.max(0, total);
            return this;
        }

        public Snapshot withFavorites(int count) {
            this.favorites = Math.max(0, count);
            return this;
        }
    }

    /** The finished map. */
    public static final class Graph {
        public final List<Node> nodes;
        public final List<Link> links;
        /** Total connection strength, used for the "bond score". */
        public final float bondScore;

        Graph(List<Node> nodes, List<Link> links, float bondScore) {
            this.nodes = Collections.unmodifiableList(nodes);
            this.links = Collections.unmodifiableList(links);
            this.bondScore = bondScore;
        }
    }

    private LoveGraphEngine() {}

    /**
     * Build the map from a snapshot.
     *
     * <p>Nodes are laid out on two rings around the couple: the inner ring holds
     * the metrics that describe the relationship itself (days together,
     * milestones), the outer ring holds the shared activities.
     */
    public static Graph build(Snapshot snapshot) {
        Snapshot s = snapshot != null ? snapshot : new Snapshot();
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();

        // Magnitudes used to normalise the node radii.
        int maxActivity = Math.max(
                Math.max(s.totalSessions, s.moodTotalDays),
                Math.max(s.datePlans, Math.max(s.sealedLetters, s.favorites)));
        maxActivity = Math.max(maxActivity, 1);

        // --- Inner ring: the relationship itself ---
        List<Node> inner = new ArrayList<>();
        inner.add(node("together", NodeKind.TOGETHER, s.daysTogether,
                s.daysTogether < 0 ? 0f : normalize(s.daysTogether, 365), 0));
        inner.add(node("anniversary", NodeKind.TOGETHER,
                s.anniversaryCountdown,
                s.anniversaryCountdown < 0 ? 0f : normalize(365 - s.anniversaryCountdown, 365), 0));
        inner.add(node("badges", NodeKind.BADGE, s.badgesUnlocked,
                s.badgesTotal <= 0 ? 0f : (float) s.badgesUnlocked / s.badgesTotal, 0));
        nodes.addAll(inner);

        // --- Outer ring: the shared activities ---
        List<Node> outer = new ArrayList<>();
        outer.add(node("play", NodeKind.PLAY, s.totalSessions,
                normalize(s.totalSessions, maxActivity), 1));
        outer.add(node("mood", NodeKind.MOOD, s.moodTotalDays,
                normalize(s.moodTotalDays, maxActivity), 1));
        outer.add(node("date", NodeKind.DATE, s.datePlans,
                normalize(s.datePlans, maxActivity), 1));
        outer.add(node("letter", NodeKind.LETTER, s.sealedLetters,
                normalize(s.sealedLetters, maxActivity), 1));
        outer.add(node("favorite", NodeKind.FAVORITE, s.favorites,
                normalize(s.favorites, maxActivity), 1));
        nodes.addAll(outer);

        // --- Links: every node connects back to the relationship core ---
        float total = 0f;
        for (Node n : nodes) {
            // The three inner nodes are peers of each other, so they link to
            // each other instead of pointing at a centre that does not exist.
            if (n.ring == 0) continue;
            String target = inner.get(0).id;
            links.add(new Link(n.id, target, 0.35f + 0.5f * n.weight));
            total += 0.35f + 0.5f * n.weight;
        }
        for (int i = 0; i + 1 < inner.size(); i++) {
            float strength = 0.4f + 0.4f * Math.min(inner.get(i).weight, inner.get(i + 1).weight);
            links.add(new Link(inner.get(i).id, inner.get(i + 1).id, strength));
            total += strength;
        }
        // Play and date plans reinforce each other.
        if (!outer.isEmpty()) {
            links.add(new Link("play", "date", 0.3f + 0.4f * Math.min(
                    weightOf(outer, "play"), weightOf(outer, "date"))));
            total += 0.3f;
        }

        float bondScore = normalizeBondScore(s);

        // Lay the nodes out on their rings.
        List<Node> laid = new ArrayList<>();
        for (Node n : nodes) {
            laid.add(withAngle(n, nodes));
        }
        return new Graph(laid, links, bondScore);
    }

    /**
     * Bond score in 0..100 blending the three things that actually keep a
     * couple going: time together, consistency, and shared rituals.
     */
    public static float normalizeBondScore(Snapshot s) {
        float time = s.daysTogether < 0 ? 0f : Math.min(1f, s.daysTogether / 365f);
        float consistency = Math.min(1f, s.moodStreak / 30f);
        float rituals = Math.min(1f,
                (s.totalSessions * 0.5f + s.datePlans * 1.5f + s.sealedLetters * 2f) / 40f);
        float raw = time * 0.4f + consistency * 0.25f + rituals * 0.35f;
        return Math.round(Math.max(0f, Math.min(1f, raw)) * 100f);
    }

    private static float weightOf(List<Node> outer, String id) {
        for (Node n : outer) {
            if (n.id.equals(id)) return n.weight;
        }
        return 0f;
    }

    private static Node node(String id, NodeKind kind, int value, float weight, int ring) {
        return new Node(id, kind, "", value, Math.max(0f, Math.min(1f, weight)), 0f, ring);
    }

    /** Distributes the nodes of one ring evenly around the circle. */
    private static Node withAngle(Node candidate, List<Node> all) {
        List<Node> peers = new ArrayList<>();
        for (Node n : all) {
            if (n.ring == candidate.ring) peers.add(n);
        }
        int index = peers.indexOf(candidate);
        int count = peers.size();
        float angle = count <= 1 ? 0f : (float) (Math.PI * 2 * index / count) - (float) Math.PI / 2f;
        return new Node(candidate.id, candidate.kind, candidate.label, candidate.value,
                candidate.weight, angle, candidate.ring);
    }

    private static float normalize(int value, int max) {
        if (max <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (float) value / max));
    }
}
