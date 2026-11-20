package com.nagabhushansn95.financetracker.ui.common

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * The investments are hidden until the phone's own unlock (fingerprint, pattern, PIN) has been passed once. The
 * unlock lasts while the app is in use and for [GRACE_MILLIS] after it goes to the background, so a quick switch to
 * another app does not ask again. Process-wide: every screen reads the same state.
 */
object InvestmentsLock {
    private const val GRACE_MILLIS = 5L * 60 * 1000

    var unlocked by mutableStateOf(false)
        private set
    private var backgroundedAt = 0L

    /** Hides the investments again from the eye button. */
    fun lock() {
        unlocked = false
    }

    /** Called from the activity's onStop / onStart: re-locks after a long stay in the background. */
    fun onBackground() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onForeground() {
        if (unlocked && backgroundedAt > 0 && System.currentTimeMillis() - backgroundedAt > GRACE_MILLIS) unlocked =
            false
        backgroundedAt = 0
    }

    /**
     * Shows the system prompt and unlocks on success. A phone with no screen lock set up cannot authenticate at all;
     * then the investments simply open, since hiding them from the phone's own user would make no sense.
     */
    fun unlock(activity: FragmentActivity, onUnlocked: () -> Unit = {}) {
        if (unlocked) {
            onUnlocked(); return
        }
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        if (BiometricManager.from(activity)
                .canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            unlocked = true; onUnlocked(); return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                    onUnlocked()
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Show investments")
                .setSubtitle("Unlock to see your investments")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }
}
