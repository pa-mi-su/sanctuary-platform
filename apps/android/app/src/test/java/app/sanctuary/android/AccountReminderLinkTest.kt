package app.sanctuary.android

import org.junit.Assert.*
import org.junit.Test

class AccountReminderLinkTest {
    @Test fun onlySupportedCanonicalContentCanBecomeAnAccountReminder() {
        assertEquals("novenas" to "st_cecilia", AccountReminderSync.contentKey("https://mydailysanctuary.com/novenas/st_cecilia"))
        assertEquals("prayers" to "apostles_creed", AccountReminderSync.contentKey("https://mydailysanctuary.com/prayers/apostles_creed"))
        for (url in listOf("https://evil.example/prayers/creed", "https://mydailysanctuary.com/saints/cecilia", "https://mydailysanctuary.com/prayers/../me", "https://mydailysanctuary.com/prayers/creed?next=evil")) {
            assertTrue(runCatching { AccountReminderSync.contentKey(url) }.isFailure)
        }
    }
}
