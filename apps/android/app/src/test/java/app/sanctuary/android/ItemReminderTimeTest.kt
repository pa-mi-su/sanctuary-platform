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
    @Test fun originalReportPreserves1455And1500() {
        val now = ZonedDateTime.parse("2026-11-16T13:00:00-05:00[America/New_York]")
        assertEquals("14:55", nextItemReminderTime(now, 14, 55).toLocalTime().toString())
        assertEquals("15:00", nextItemReminderTime(now, 15, 0).toLocalTime().toString())
    }
    @Test fun travelKeepsWallClockRatherThanConvertingToUtc() {
        for (zone in listOf("America/New_York", "Europe/Warsaw", "Asia/Kolkata")) {
            val now = ZonedDateTime.now(java.time.ZoneId.of(zone))
            assertEquals(55, nextItemReminderTime(now, 14, 55).minute)
            assertEquals(14, nextItemReminderTime(now, 14, 55).hour)
        }
    }
    @Test fun springGapMatchesNativeIosByMovingForwardOneHour() {
        val next = nextItemReminderTime(ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]"),2,30)
        assertEquals(8,next.dayOfMonth)
        assertEquals(3,next.hour)
        assertEquals(30,next.minute)
    }
    @Test fun fallOverlapDoesNotScheduleSameReminderTwice() {
        val now = ZonedDateTime.parse("2026-11-01T01:45:00-04:00[America/New_York]")
        assertEquals(2,nextItemReminderTime(now,1,30).dayOfMonth)
    }
}
