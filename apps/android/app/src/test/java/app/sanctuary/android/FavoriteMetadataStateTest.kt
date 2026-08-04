package app.sanctuary.android

import app.sanctuary.android.data.FavoriteItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteMetadataStateTest {
    @Test
    fun `new novena favorite immediately retains canonical title and duration`() {
        val updated = NovenaProgressUiState().withOptimisticFavorite(
            itemType = FavoriteItemType.Novena,
            itemId = "novena_our_lady_of_lourdes",
            enabled = true,
            displayName = "Our Lady of Lourdes Novena",
            slug = "our-lady-of-lourdes-novena",
            durationDays = 9,
            createdAt = "2026-08-04T12:00:00Z"
        )

        assertTrue(updated.favorites.any { it.itemType == FavoriteItemType.Novena && it.itemId == "novena_our_lady_of_lourdes" })
        assertEquals("Our Lady of Lourdes Novena", updated.novenaTitles["novena_our_lady_of_lourdes"])
        assertEquals(9, updated.novenaDurations["novena_our_lady_of_lourdes"])
    }

    @Test
    fun `new saint favorite immediately retains canonical name and slug`() {
        val updated = NovenaProgressUiState().withOptimisticFavorite(
            itemType = FavoriteItemType.Saint,
            itemId = "saint_francis_of_assisi",
            enabled = true,
            displayName = "Saint Francis of Assisi",
            slug = "saint-francis-of-assisi",
            createdAt = "2026-08-04T12:00:00Z"
        )

        assertTrue(updated.favorites.any { it.itemType == FavoriteItemType.Saint && it.itemId == "saint_francis_of_assisi" })
        assertEquals("Saint Francis of Assisi", updated.saintNames["saint_francis_of_assisi"])
        assertEquals("saint-francis-of-assisi", updated.saintSlugs["saint_francis_of_assisi"])
    }
}
