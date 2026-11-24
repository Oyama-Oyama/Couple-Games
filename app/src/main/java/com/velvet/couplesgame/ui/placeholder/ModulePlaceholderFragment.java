package com.velvet.couplesgame.ui.placeholder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.velvet.couplesgame.databinding.FragmentPlaceholderBinding;

public class ModulePlaceholderFragment extends Fragment {
    public static final String ARG_TITLE = "arg_title";
    private FragmentPlaceholderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "Module") : "Module";
        binding.tvPlaceholderTitle.setText(title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
