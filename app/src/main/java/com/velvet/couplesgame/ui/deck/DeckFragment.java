package com.velvet.couplesgame.ui.deck;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.velvet.couplesgame.AppContainer;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.databinding.FragmentDeckBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;

public class DeckFragment extends Fragment {
    private FragmentDeckBinding binding;
    private boolean showingFavorites = false;
    private boolean favoritesEmpty = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeckBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppContainer container = ((CouplesGameApp) requireActivity().getApplication()).getContainer();

        CustomTaskAdapter customAdapter = new CustomTaskAdapter(container.contentRepository::deleteCustomTask);
        FavoriteTaskAdapter favoriteAdapter = new FavoriteTaskAdapter(container.contentRepository::removeFavorite);

        binding.rvCustomTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomTasks.setAdapter(customAdapter);

        binding.rvFavoriteTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFavoriteTasks.setAdapter(favoriteAdapter);

        container.contentRepository.observeCustomTasks().observe(getViewLifecycleOwner(), customAdapter::submitList);
        container.contentRepository.observeFavorites().observe(getViewLifecycleOwner(), list -> {
            favoriteAdapter.submitList(list);
            favoritesEmpty = (list == null || list.isEmpty());
            updateFavoritesEmptyVisibility();
        });

        binding.btnTabCustom.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showingFavorites = false;
            updateTabState();
        });

        binding.btnTabFavorites.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showingFavorites = true;
            updateTabState();
        });

        binding.btnAddCustom.setOnClickListener(v -> {
            String prompt = binding.etCustomPrompt.getText().toString().trim();
            if (TextUtils.isEmpty(prompt)) return;
            HapticHelper.tick(v);
            container.contentRepository.addCustomTask(prompt, 2, TaskType.DARE);
            binding.etCustomPrompt.setText("");
            Toast.makeText(requireContext(), R.string.msg_card_added, Toast.LENGTH_SHORT).show();
        });

        updateTabState();
    }

    private void updateTabState() {
        if (binding == null) return;
        binding.layoutCustomCreator.setVisibility(showingFavorites ? View.GONE : View.VISIBLE);
        binding.rvCustomTasks.setVisibility(showingFavorites ? View.GONE : View.VISIBLE);
        binding.rvFavoriteTasks.setVisibility(showingFavorites ? View.VISIBLE : View.GONE);
        binding.btnTabCustom.setAlpha(showingFavorites ? 0.55f : 1.0f);
        binding.btnTabFavorites.setAlpha(showingFavorites ? 1.0f : 0.55f);
        updateFavoritesEmptyVisibility();
    }

    private void updateFavoritesEmptyVisibility() {
        if (binding == null) return;
        binding.tvDeckEmpty.setVisibility(showingFavorites && favoritesEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
