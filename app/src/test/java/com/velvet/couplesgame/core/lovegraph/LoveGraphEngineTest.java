package com.velvet.couplesgame.core.lovegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/** Unit tests for the Love Map derivation. */
public class LoveGraphEngineTest {

    @Test
    public void emptySnapshotStillProducesTheCoreNodes() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(new LoveGraphEngine.Snapshot());

        assertNotNull(graph);
        assertEquals(8, graph.nodes.size());
        assertTrue(graph.bondScore >= 0f);
    }

    @Test
    public void allNodesCarryIdsAndWeightsInRange() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        for (LoveGraphEngine.Node node : graph.nodes) {
            assertTrue("weight out of range: " + node.weight,
                    node.weight >= 0f && node.weight <= 1f);
            assertTrue("missing id", node.id != null && !node.id.isEmpty());
        }
    }

    @Test
    public void nodesAreSplitAcrossTwoRings() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        int inner = 0;
        int outer = 0;
        for (LoveGraphEngine.Node node : graph.nodes) {
            if (node.ring == 0) inner++;
            else outer++;
        }
        assertEquals(3, inner);
        assertEquals(5, outer);
    }

    @Test
    public void nodesOnTheSameRingGetDistinctAngles() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        java.util.Set<Float> angles = new java.util.HashSet<>();
        for (LoveGraphEngine.Node node : graph.nodes) {
            if (node.ring == 1) angles.add(node.angle);
        }
        assertEquals(5, angles.size());
    }

    @Test
    public void linksOnlyReferenceExistingNodes() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        java.util.Set<String> ids = new HashSet<>();
        for (LoveGraphEngine.Node node : graph.nodes) ids.add(node.id);

        for (LoveGraphEngine.Link link : graph.links) {
            assertTrue("dangling link: " + link.fromId, ids.contains(link.fromId));
            assertTrue("dangling link: " + link.toId, ids.contains(link.toId));
            assertTrue("strength out of range", link.strength >= 0f && link.strength <= 1f);
        }
    }

    @Test
    public void everyOuterNodeLinksBackToTheCore() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        for (LoveGraphEngine.Node node : graph.nodes) {
            if (node.ring != 1) continue;
            boolean linked = false;
            for (LoveGraphEngine.Link link : graph.links) {
                if (link.fromId.equals(node.id)) {
                    linked = true;
                    break;
                }
            }
            assertTrue("outer node not linked: " + node.id, linked);
        }
    }

    @Test
    public void bondScoreGrowsWithMoreSharedActivity() {
        float small = LoveGraphEngine.build(new LoveGraphEngine.Snapshot()
                .withDaysTogether(10)
                .withMood(1, 1)
                .withSessions(1, new HashSet<>(Arrays.asList("A")))).bondScore;

        float large = LoveGraphEngine.build(new LoveGraphEngine.Snapshot()
                .withDaysTogether(200)
                .withMood(20, 40)
                .withSessions(30, new HashSet<>(Arrays.asList("A", "B", "C")))
                .withDatePlans(4)
                .withLetters(3, 2)).bondScore;

        assertTrue("bond score should grow: " + small + " vs " + large, large > small);
    }

    @Test
    public void bondScoreStaysWithinBounds() {
        LoveGraphEngine.Snapshot max = new LoveGraphEngine.Snapshot()
                .withDaysTogether(100000)
                .withMood(9999, 9999)
                .withSessions(9999, new HashSet<>(Arrays.asList("A")))
                .withDatePlans(999)
                .withLetters(999, 999)
                .withBadges(99, 99)
                .withFavorites(999);

        float score = LoveGraphEngine.build(max).bondScore;
        assertTrue("score out of range: " + score, score >= 0f && score <= 100f);
    }

    @Test
    public void nullSnapshotIsHandled() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(null);

        assertNotNull(graph);
        assertEquals(8, graph.nodes.size());
    }

    @Test
    public void daysTogetherNodeCarriesTheValue() {
        LoveGraphEngine.Graph graph = LoveGraphEngine.build(populated());

        for (LoveGraphEngine.Node node : graph.nodes) {
            if ("together".equals(node.id)) {
                assertEquals(100, node.value);
                return;
            }
        }
        org.junit.Assert.fail("together node missing");
    }

    private LoveGraphEngine.Snapshot populated() {
        return new LoveGraphEngine.Snapshot()
                .withDaysTogether(100)
                .withAnniversary(20)
                .withSessions(12, new HashSet<>(Arrays.asList("A", "B")))
                .withMood(5, 9)
                .withDatePlans(3)
                .withLetters(4, 2)
                .withBadges(3, 6)
                .withFavorites(5);
    }
}
