package app.sanctuary.android.data

import com.google.gson.Gson

object ApiJson {
    private val gson = Gson()

    fun parseErrorMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            gson.fromJson(raw, ApiErrorEnvelope::class.java)?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun encodeSession(session: StoredSession): String {
        return gson.toJson(session)
    }

    fun decodeSession(raw: String): StoredSession? {
        return runCatching {
            gson.fromJson(raw, StoredSession::class.java)
        }.getOrNull()
    }
}
