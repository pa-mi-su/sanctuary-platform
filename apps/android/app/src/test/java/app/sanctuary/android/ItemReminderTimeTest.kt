package app.sanctuary.android

import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ItemReminderTimeTest {
    @Test fun passedTimeMovesToTomorrowWithoutDroppingMinutes() {
        val now = ZonedDateTime.parse("2026-11-13T18:30:00-05:00[America/New_York]")
        val next = nextItemReminderTime(now, 18, 15)
        assertEquals(14, next.dayOfMonth)
        assertEquals(18, next.hour)
        assertEquals(15, next.minute)
    }
    @Test fun daylightSavingTransitionKeepsChosenLocalTime() {
        val now = ZonedDateTime.parse("2026-03-07T21:00:00-05:00[America/New_York]")
        val next = nextItemReminderTime(now, 8, 45)
        assertEquals(8, next.hour)
        assertEquals(45, next.minute)
        assertEquals("-04:00", next.offset.toString())
        assertTrue(next.isAfter(now))
    }
    @Test fun nonexistentSpringTimeStillSchedulesInTheFuture() {
        val now = ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")
        assertTrue(nextItemReminderTime(now, 2, 30).isAfter(now))
    }
}
