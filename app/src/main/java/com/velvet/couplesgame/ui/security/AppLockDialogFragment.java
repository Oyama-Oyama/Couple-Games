package com.velvet.couplesgame.ui.security;

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.velvet.couplesgame.CouplesGameApp;
import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.profile.AppLockManager;

import java.util.concurrent.Executor;

/** A blocking local authentication gate; biometric and credential data stay in Android. */
public final class AppLockDialogFragment extends DialogFragment {

    public static final String TAG = "velvet_app_lock";
    public static final String RESULT_KEY = "app_lock_authenticated";

    private final ActivityResultLauncher<Intent> credentialLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            completeAuthentication();
                        }
                    });

    private BiometricPrompt biometricPrompt;
    private boolean promptActive;
    private boolean initialPromptStarted;

    public static AppLockDialogFragment newInstance() {
        return new AppLockDialogFragment();
    }

    public static boolean isAuthenticationAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            return biometricManager.canAuthenticate(authenticators)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        }
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean deviceCredentialAvailable = keyguardManager != null
                && keyguardManager.isDeviceSecure();
        return deviceCredentialAvailable
                || biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setCancelable(false);
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_lock_auth_title)
                .setMessage(R.string.app_lock_auth_subtitle)
                .setPositiveButton(R.string.app_lock_unlock, null)
                .setNegativeButton(R.string.app_lock_exit, (dialog, which) ->
                        requireActivity().finish())
                .setNeutralButton(R.string.app_lock_disable, null)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        dialog.setCanceledOnTouchOutside(false);
        androidx.appcompat.app.AlertDialog alertDialog =
                (androidx.appcompat.app.AlertDialog) dialog;
        alertDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> authenticate());
        Button disableButton = alertDialog.getButton(
                android.content.DialogInterface.BUTTON_NEUTRAL);
        boolean authenticationAvailable = isAuthenticationAvailable(requireContext());
        disableButton.setVisibility(authenticationAvailable ? Button.GONE : Button.VISIBLE);
        disableButton.setOnClickListener(view -> confirmDisablingLock());
        if (!initialPromptStarted) {
            initialPromptStarted = true;
            authenticate();
        }
    }

    private void authenticate() {
        if (!isAdded() || promptActive) return;
        Context context = requireContext();
        if (!isAuthenticationAvailable(context)) {
            showError(R.string.app_lock_auth_unavailable);
            return;
        }

        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean credentialAvailable = keyguardManager != null && keyguardManager.isDeviceSecure();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                && BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            launchDeviceCredential();
            return;
        }

        BiometricPrompt.PromptInfo.Builder promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(getString(R.string.app_lock_auth_title))
                        .setSubtitle(getString(R.string.app_lock_auth_subtitle));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfo.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else {
            promptInfo.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(getString(credentialAvailable
                            ? R.string.app_lock_use_device_credential
                            : android.R.string.cancel));
        }

        Executor executor = ContextCompat.getMainExecutor(context);
        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        promptActive = false;
                        completeAuthentication();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        promptActive = false;
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                                && credentialAvailable
                                && errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_CANCELED) {
                            launchDeviceCredential();
                        } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            showError(R.string.app_lock_auth_failed);
                        } else if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                && Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                                && credentialAvailable) {
                            launchDeviceCredential();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Keep the system prompt open so the user can retry or choose the fallback.
                    }
                });
        promptActive = true;
        biometricPrompt.authenticate(promptInfo.build());
    }

    private void launchDeviceCredential() {
        if (!isAdded()) return;
        KeyguardManager keyguardManager = (KeyguardManager) requireContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isDeviceSecure()) {
            showError(R.string.app_lock_auth_unavailable);
            return;
        }
        Intent confirmIntent = keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.app_lock_auth_title),
                getString(R.string.app_lock_auth_subtitle));
        if (confirmIntent == null) {
            showError(R.string.app_lock_auth_unavailable);
            return;
        }
        credentialLauncher.launch(confirmIntent);
    }

    private void completeAuthentication() {
        if (!isAdded()) return;
        getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
        dismissAllowingStateLoss();
    }

    private void confirmDisablingLock() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_lock_disable_title)
                .setMessage(R.string.app_lock_disable_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.app_lock_disable, (dialog, which) -> {
                    AppLockManager manager = ((CouplesGameApp) requireActivity()
                            .getApplication()).getContainer().appLockManager;
                    if (!manager.setEnabled(false)) {
                        showError(R.string.app_lock_state_save_failed);
                        return;
                    }
                    completeAuthentication();
                })
                .show();
    }

    private void showError(int messageRes) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_LONG).show();
    }
}
