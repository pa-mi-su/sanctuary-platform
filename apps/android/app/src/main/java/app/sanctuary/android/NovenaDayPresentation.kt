package app.sanctuary.android

import app.sanctuary.android.data.NovenaDayDetail

internal fun NovenaDayDetail.hasStructuredContent(): Boolean =
    !scripture.isNullOrBlank() || !prayer.isNullOrBlank() || !reflection.isNullOrBlank()

internal fun NovenaDayDetail.hasFallbackBodyContent(): Boolean =
    !hasStructuredContent() && !body.isNullOrBlank()

internal fun NovenaDayDetail.hasVisibleDayContent(): Boolean =
    hasStructuredContent() || !body.isNullOrBlank()
