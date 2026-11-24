package com.velvet.couplesgame.ui.lovegraph;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.lovegraph.LoveGraphEngine;
import com.velvet.couplesgame.core.streak.BadgeCatalog;
import com.velvet.couplesgame.core.streak.StreakCalculator;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import com.velvet.couplesgame.databinding.FragmentLoveGraphBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Love Map: an abstract constellation of everything the couple has shared.
 *
 * <p>Everything is derived from data already stored on this device, so the
 * screen works with no network, no map SDK and no location permission.
 */
public class LoveGraphFragment extends Fragment implements LoveGraphView.OnNodeTapListener {

    private FragmentLoveGraphBinding binding;
    private AppContainer container;
    private LoveGraphEngine.Graph graph;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoveGraphBinding.inflate(inflater, containerView, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        binding.lgView.setOnNodeTapListener(this);
        binding.btnLgRefresh.setOnClickListener(v -> {
            HapticHelper.tick(v);
            rebuild();
        });

        rebuild();
    }

    private void rebuild() {
        if (binding == null) return;
        LoveGraphEngine.Snapshot snapshot = gatherSnapshot();
        graph = LoveGraphEngine.build(snapshot);
        binding.lgView.setGraph(graph);

        int score = Math.round(graph.bondScore);
        binding.tvLgScore.setText(getString(R.string.love_graph_score_fmt, score));
        binding.tvLgScoreSub.setText(getString(R.string.love_graph_score_sub));

        boolean empty = graph.nodes.isEmpty();
        binding.tvLgEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.lgView.setVisibility(empty ? View.GONE : View.VISIBLE);

        binding.tvLgDetail.setText(getString(R.string.love_graph_tap_hint));
    }

    /** Collect every local signal the map can show. */
    private LoveGraphEngine.Snapshot gatherSnapshot() {
        LoveGraphEngine.Snapshot snapshot = new LoveGraphEngine.Snapshot();

        snapshot.withDaysTogether(container.profileManager.getDaysTogether());
        snapshot.withAnniversary(container.profileManager.getAnniversaryCountdown());

        List<SessionStatEntity> history = container.contentRepository.observeSessionStats().getValue();
        Set<String> modes = new HashSet<>();
        if (history != null) {
            for (SessionStatEntity stat : history) {
                if (stat != null && stat.gameMode != null) modes.add(stat.gameMode);
            }
        }
        snapshot.withSessions(history != null ? history.size() : 0, modes);

        Integer moodStreak = container.moodCheckinManager.observeStreak().getValue();
        snapshot.withMood(moodStreak != null ? moodStreak : 0,
                container.moodCheckinManager.getTotalCheckInDays());

        snapshot.withDatePlans(container.datePlanManager.getPlans().size());

        snapshot.withLetters(container.letterManager.getLetters().size(),
                container.letterManager.getOpenedLetters().size());

        StreakCalculator.Result stats = StreakCalculator.compute(history);
        int unlocked = 0;
        for (BadgeCatalog.Badge badge : BadgeCatalog.all()) {
            if (badge.isUnlocked(stats, history != null ? history.size() : 0)) unlocked++;
        }
        snapshot.withBadges(unlocked, BadgeCatalog.all().size());

        Integer favorites = container.contentRepository.observeFavorites().getValue() != null
                ? container.contentRepository.observeFavorites().getValue().size() : 0;
        snapshot.withFavorites(favorites);

        return snapshot;
    }

    @Override
    public void onNodeTapped(LoveGraphEngine.Node node) {
        if (binding == null || node == null) return;
        HapticHelper.tick(binding.getRoot());
        binding.tvLgDetail.setText(describeNode(node));
    }

    private String describeNode(LoveGraphEngine.Node node) {
        switch (node.id) {
            case "together":
                return getString(R.string.love_graph_detail_together, node.value);
            case "anniversary":
                return getString(R.string.love_graph_detail_anniversary, node.value);
            case "badges":
                return getString(R.string.love_graph_detail_badges, node.value);
            case "play":
                return getString(R.string.love_graph_detail_play, node.value);
            case "mood":
                return getString(R.string.love_graph_detail_mood, node.value);
            case "date":
                return getString(R.string.love_graph_detail_date, node.value);
            case "letter":
                return getString(R.string.love_graph_detail_letter, node.value);
            case "favorite":
                return getString(R.string.love_graph_detail_favorite, node.value);
            default:
                return getString(R.string.love_graph_tap_hint);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
