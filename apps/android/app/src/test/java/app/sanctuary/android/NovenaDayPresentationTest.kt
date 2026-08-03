package app.sanctuary.android

import app.sanctuary.android.data.NovenaDayDetail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovenaDayPresentationTest {
    @Test
    fun `combined body is hidden when structured content is available`() {
        val day = novenaDay(
            scripture = "Matthew 1:20-21",
            prayer = "Saint Joseph, pray for us.",
            reflection = "Ask for quiet trust.",
            body = "Matthew 1:20-21\n\nSaint Joseph, pray for us.\n\nAsk for quiet trust."
        )

        assertTrue(day.hasVisibleDayContent())
        assertTrue(day.hasStructuredContent())
        assertFalse(day.hasFallbackBodyContent())
    }

    @Test
    fun `combined body is shown when structured content is absent`() {
        val day = novenaDay(body = "Legacy combined novena content")

        assertTrue(day.hasVisibleDayContent())
        assertFalse(day.hasStructuredContent())
        assertTrue(day.hasFallbackBodyContent())
    }

    @Test
    fun `title alone does not count as day content`() {
        val day = novenaDay(title = "Day One")

        assertFalse(day.hasVisibleDayContent())
        assertFalse(day.hasFallbackBodyContent())
    }

    @Test
    fun `blank fields do not count as day content`() {
        val day = novenaDay(
            scripture = "  ",
            prayer = "\n",
            reflection = "",
            body = "   "
        )

        assertFalse(day.hasVisibleDayContent())
        assertFalse(day.hasFallbackBodyContent())
    }

    private fun novenaDay(
        title: String? = null,
        scripture: String? = null,
        prayer: String? = null,
        reflection: String? = null,
        body: String? = null
    ) = NovenaDayDetail(
        dayNumber = 1,
        title = title,
        openingPrayer = null,
        meditation = null,
        closingPrayer = null,
        scripture = scripture,
        prayer = prayer,
        reflection = reflection,
        body = body
    )
}
