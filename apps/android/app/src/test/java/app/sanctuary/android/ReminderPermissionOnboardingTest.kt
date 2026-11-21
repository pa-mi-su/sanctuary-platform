package app.sanctuary.android

import org.junit.Assert.*
import org.junit.Test

class ReminderPermissionOnboardingTest {
    private fun offer(allowed: Boolean = false, supported: Boolean = true,
                      answered: String? = null, installation: Long = 10) = shouldOfferReminderPermission(
        supported, allowed, "1.0.41:100", 10, answered, installation
    )

    @Test fun freshInstallOffersSetupWithoutAccountOrReminders() { assertTrue(offer()) }
    @Test fun upgradeOffersSetupWithoutCachedReminders() { assertTrue(offer(answered = "1.0.40:99")) }
    @Test fun newBuildOffersSetupAfterPreviousBuildWasDeclined() { assertTrue(offer(answered = "1.0.41:99")) }
    @Test fun grantedPermissionDoesNotPrompt() { assertFalse(offer(allowed = true)) }
    @Test fun olderAndroidDoesNotRequestUnsupportedPermission() { assertFalse(offer(supported = false)) }
    @Test fun declinedIntroductionDoesNotLoopWithinTheSameInstallationAndBuild() {
        assertFalse(offer(answered = "1.0.41:100"))
    }
    @Test fun restoredPreferencesOnAnotherInstallationDoNotSuppressSetup() {
        assertTrue(offer(answered = "1.0.41:100", installation = 9))
    }
}
