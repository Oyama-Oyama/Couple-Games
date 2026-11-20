package com.velvet.couplesgame;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.databinding.ActivityMainBinding;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AppContainer container;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        container = ((CouplesGameApp) getApplication()).getContainer();
        container.themeManager.applySavedTheme(this);

        // Android 15 (API 35) compliant dark Edge-to-Edge bars
        EdgeToEdge.enable(
                this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT)
        );

        super.onCreate(savedInstanceState);

        if (container.entitlementManager.isScreenShieldEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        container.billingBridge.startConnectionAndSync();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applySystemBarInsets();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
            setupDestinationChrome();
        }

        binding.btnGameBack.setOnClickListener(v -> {
            HapticHelper.tick(v);
            if (navController != null) navController.navigateUp();
        });

        binding.btnSafePause.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showSafePauseDialog();
        });

        // Show sleek 2-step first-launch onboarding once
        if (!container.profileManager.isOnboardingCompleted()
                && getSupportFragmentManager().findFragmentByTag("velvet_onboarding") == null) {
            OnboardingDialogFragment.newInstance(true)
                    .show(getSupportFragmentManager(), "velvet_onboarding");
        }
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMain, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean bottomNavVisible = binding.bottomNav.getVisibility() == View.VISIBLE;
            view.setPadding(bars.left, bars.top, bars.right, 0);
            if (bottomNavVisible) {
                binding.bottomNav.setPadding(0, 0, 0, bars.bottom);
                binding.navHostFragment.setPadding(0, 0, 0, 0);
            } else {
                binding.bottomNav.setPadding(0, 0, 0, 0);
                binding.navHostFragment.setPadding(0, 0, 0, bars.bottom);
            }
            return windowInsets;
        });
    }

    private void setupDestinationChrome() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isTopLevel = (id == R.id.nav_home
                    || id == R.id.nav_consent
                    || id == R.id.nav_deck
                    || id == R.id.nav_settings);

            binding.bottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
            binding.layoutGameTopbar.setVisibility(isTopLevel ? View.GONE : View.VISIBLE);

            if (!isTopLevel && destination.getLabel() != null) {
                binding.tvGameTopbarTitle.setText(destination.getLabel());
            }

            // Ensure sub-games never collide with bottom gesture bar when bottomNav is hidden
            ViewCompat.requestApplyInsets(binding.rootMain);
        });
    }

    private void showSafePauseDialog() {
        String safeWord = container.profileManager.getSafeWord();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.safe_pause_title)
                .setMessage(getString(R.string.safe_pause_message, safeWord))
                .setPositiveButton(R.string.safe_pause_resume, null)
                .setNeutralButton(R.string.safe_pause_boundaries, (d, w) -> {
                    if (navController != null) {
                        navController.navigate(R.id.nav_consent);
                    }
                })
                .setNegativeButton(R.string.safe_pause_exit, (d, w) -> {
                    if (navController != null) navController.navigateUp();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && container != null) {
            container.billingBridge.endConnection();
        }
    }
}
