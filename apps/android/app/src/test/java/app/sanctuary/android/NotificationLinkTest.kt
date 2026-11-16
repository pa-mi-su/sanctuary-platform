package app.sanctuary.android
import org.junit.Assert.*
import org.junit.Test
class NotificationLinkTest {
    @Test fun announcementCleanupPreservesReminderAndUnknownChannels() {
        assertTrue(isAnnouncementAlertChannel("sanctuary-announcements"))
        assertFalse(isAnnouncementAlertChannel("sanctuary-reminders"))
        assertFalse(isAnnouncementAlertChannel("sanctuary-item-reminders"))
        assertFalse(isAnnouncementAlertChannel(null))
    }
    @Test fun devAcceptsDevAndExistingCanonicalLinks() {
        assertTrue(isSanctuaryNotificationLink("https://dev.mydailysanctuary.com/prayers/our_father", "dev"))
        assertTrue(isSanctuaryNotificationLink("https://mydailysanctuary.com/novenas/saint-joseph", "dev"))
    }
    @Test fun productionRejectsDevAndUntrustedHosts() {
        assertFalse(isSanctuaryNotificationLink("https://dev.mydailysanctuary.com/", "prod"))
        assertFalse(isSanctuaryNotificationLink("http://mydailysanctuary.com/", "dev"))
        assertFalse(isSanctuaryNotificationLink("https://mydailysanctuary.com.evil.example/", "dev"))
    }
}
