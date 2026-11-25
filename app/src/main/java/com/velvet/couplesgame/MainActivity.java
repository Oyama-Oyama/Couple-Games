package com.velvet.couplesgame;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.databinding.ActivityMainBinding;
import com.velvet.couplesgame.ui.common.AftercareDialogHelper;
import com.velvet.couplesgame.ui.common.HapticHelper;
import com.velvet.couplesgame.ui.onboarding.AgeGateDialogFragment;
import com.velvet.couplesgame.ui.onboarding.OnboardingDialogFragment;
import com.velvet.couplesgame.ui.security.AppLockDialogFragment;

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

        updatePrivacyWindowFlags();

        container.billingBridge.startConnectionAndSync();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean appLockRequired = container.appLockManager.isAuthenticationRequired();
        if (appLockRequired) {
            binding.rootMain.setVisibility(View.INVISIBLE);
        }

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
            navigateUpWithOptionalAftercare();
        });
        setupAftercareBackHandler();

        binding.btnSafePause.setOnClickListener(v -> {
            HapticHelper.tick(v);
            showSafePauseDialog();
        });

        getSupportFragmentManager().setFragmentResultListener(
                AgeGateDialogFragment.RESULT_KEY,
                this,
                (requestKey, result) ->
                        getWindow().getDecorView().post(this::showOnboardingIfNeeded)
        );

        getSupportFragmentManager().setFragmentResultListener(
                AppLockDialogFragment.RESULT_KEY,
                this,
                (requestKey, result) -> onAppLockAuthenticated()
        );

        if (appLockRequired) {
            showAppLockIfNeeded();
        } else {
            showInitialDialogsIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (container != null && binding != null) {
            if (container.appLockManager.isAuthenticationRequired()) {
                binding.rootMain.setVisibility(View.INVISIBLE);
                showAppLockIfNeeded();
            } else if (container.appLockManager.isEnabled()) {
                binding.rootMain.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onPause() {
        if (container != null && binding != null && !isChangingConfigurations()
                && container.appLockManager.isEnabled()) {
            binding.rootMain.setVisibility(View.INVISIBLE);
            container.appLockManager.markBackgrounded();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (container != null && !isChangingConfigurations()) {
            container.appLockManager.markBackgrounded();
        }
        super.onStop();
    }

    private void showAppLockIfNeeded() {
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().isStateSaved()) return;
        if (getSupportFragmentManager()
                .findFragmentByTag(AppLockDialogFragment.TAG) == null) {
            AppLockDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), AppLockDialogFragment.TAG);
        }
    }

    private void onAppLockAuthenticated() {
        if (!container.appLockManager.clearPendingAuthentication()) {
            Toast.makeText(this, R.string.app_lock_state_save_failed, Toast.LENGTH_LONG).show();
            binding.rootMain.post(this::showAppLockIfNeeded);
            return;
        }
        updatePrivacyWindowFlags();
        binding.rootMain.setVisibility(View.VISIBLE);
        showInitialDialogsIfNeeded();
    }

    private void updatePrivacyWindowFlags() {
        if (container.entitlementManager.isScreenShieldEnabled()
                || container.appLockManager.isEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void showInitialDialogsIfNeeded() {
        // 软门禁：未确认或拒绝时只允许基础强度；先完成年龄确认，再显示新手引导。
        if (!container.ageGateManager.isConfirmed()) {
            binding.navHostFragment.setVisibility(View.INVISIBLE);
            if (getSupportFragmentManager().findFragmentByTag(AgeGateDialogFragment.TAG) == null) {
                AgeGateDialogFragment.newInstance()
                        .show(getSupportFragmentManager(), AgeGateDialogFragment.TAG);
            }
        } else if (getSupportFragmentManager()
                .findFragmentByTag(AgeGateDialogFragment.TAG) == null) {
            showOnboardingIfNeeded();
        }
    }

    private void showOnboardingIfNeeded() {
        if (container.ageGateManager.isConfirmed()
                && !container.profileManager.isOnboardingCompleted()
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

            if (!isTopLevel) {
                // Social games share one destination; resolve the real title from the mode argument.
                CharSequence title = destination.getLabel();
                if (id == R.id.nav_social_game && arguments != null) {
                    String modeId = arguments.getString(
                            com.velvet.couplesgame.ui.social.SocialGameFragment.ARG_MODE_ID);
                    com.velvet.couplesgame.core.model.GameModeCatalog mode =
                            com.velvet.couplesgame.core.model.GameModeCatalog.fromId(modeId);
                    if (mode != null) title = getString(mode.titleRes);
                }
                if (title != null) {
                    binding.tvGameTopbarTitle.setText(title);
                }
                int activeHeat = container.getEffectiveMaxTier();
                binding.tvGameHeatBadge.setText("🔥 " + getString(TagDisplayMapper.resolveTierNameRes(activeHeat)));
            }

            // Ensure sub-games never collide with bottom gesture bar when bottomNav is hidden
            ViewCompat.requestApplyInsets(binding.rootMain);
        });
    }

    private void setupAftercareBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController != null && navController.getCurrentDestination() != null
                        && shouldOfferAftercare(navController.getCurrentDestination().getId())) {
                    navigateUpWithOptionalAftercare();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    private boolean shouldOfferAftercare(int destinationId) {
        return destinationId != R.id.nav_home
                && destinationId != R.id.nav_consent
                && destinationId != R.id.nav_deck
                && destinationId != R.id.nav_settings
                && destinationId != R.id.nav_date_planner
                && destinationId != R.id.nav_sealed_letter
                && destinationId != R.id.nav_love_graph
                && destinationId != R.id.nav_tonight_note_history;
    }

    private void navigateUpWithOptionalAftercare() {
        if (navController == null || navController.getCurrentDestination() == null) return;
        if (!shouldOfferAftercare(navController.getCurrentDestination().getId())) {
            navController.navigateUp();
            return;
        }
        Fragment navHost = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        Fragment activeFragment = navHost instanceof NavHostFragment
                ? ((NavHostFragment) navHost).getChildFragmentManager().getPrimaryNavigationFragment()
                : null;
        AftercareDialogHelper.showBeforeLeaving(activeFragment, () -> navController.navigateUp());
    }

    private void showSafePauseDialog() {
        String safeWord = container.profileManager.getSafeWord();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.safe_pause_title)
                .setMessage(getString(R.string.safe_pause_message, safeWord))
                .setPositiveButton(R.string.safe_pause_resume, null)
                .setNeutralButton(R.string.safe_pause_boundaries, (d, w) -> {
                    // 复用底部导航的标准 tab 切换逻辑，让 NavigationUI 维护一致的返回栈；
                    // 从「边界」页点击任意底栏项（含首页）即可正确回到顶层页面。
                    if (navController != null) {
                        NavigationUI.onNavDestinationSelected(
                                binding.bottomNav.getMenu().findItem(R.id.nav_consent), navController);
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
