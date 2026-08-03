package app.sanctuary.android

internal enum class CalendarSwipeDirection {
    Previous,
    Next,
    None
}

internal fun calendarSwipeDirection(
    dragAmount: Float,
    threshold: Float
): CalendarSwipeDirection = when {
    dragAmount > threshold -> CalendarSwipeDirection.Previous
    dragAmount < -threshold -> CalendarSwipeDirection.Next
    else -> CalendarSwipeDirection.None
}
