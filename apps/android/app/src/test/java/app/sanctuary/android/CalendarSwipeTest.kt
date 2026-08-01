package app.sanctuary.android

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSwipeTest {
    private val threshold = 240f

    @Test
    fun `right swipe beyond threshold navigates to previous day`() {
        assertEquals(
            CalendarSwipeDirection.Previous,
            calendarSwipeDirection(dragAmount = threshold + 1f, threshold = threshold)
        )
    }

    @Test
    fun `left swipe beyond threshold navigates to next day`() {
        assertEquals(
            CalendarSwipeDirection.Next,
            calendarSwipeDirection(dragAmount = -threshold - 1f, threshold = threshold)
        )
    }

    @Test
    fun `drag below threshold does not navigate`() {
        assertEquals(
            CalendarSwipeDirection.None,
            calendarSwipeDirection(dragAmount = threshold - 1f, threshold = threshold)
        )
        assertEquals(
            CalendarSwipeDirection.None,
            calendarSwipeDirection(dragAmount = -threshold + 1f, threshold = threshold)
        )
    }

    @Test
    fun `drag exactly at threshold does not navigate`() {
        assertEquals(
            CalendarSwipeDirection.None,
            calendarSwipeDirection(dragAmount = threshold, threshold = threshold)
        )
        assertEquals(
            CalendarSwipeDirection.None,
            calendarSwipeDirection(dragAmount = -threshold, threshold = threshold)
        )
    }

    @Test
    fun `vertical gesture with no horizontal drag does not navigate`() {
        assertEquals(
            CalendarSwipeDirection.None,
            calendarSwipeDirection(dragAmount = 0f, threshold = threshold)
        )
    }
}
