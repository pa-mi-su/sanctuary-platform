package app.sanctuary.android

import androidx.test.platform.app.InstrumentationRegistry
import java.net.HttpURLConnection
import java.net.URL

internal object ReminderFixture {
    fun auth(status: Int, refreshStatus: Int = status, delayMs: Int = 0) {
        post("auth", """{"status":$status,"refreshStatus":$refreshStatus,"delayMs":$delayMs}""")
    }

    fun reminderMutations(status: Int) { post("reminder-mutations", """{"status":$status}""") }

    private fun post(path: String, body: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL == "http://10.0.2.2:18084")
        val connection = URL("${BuildConfig.API_BASE_URL}/fixture/$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray()) }
            check(connection.responseCode == 200)
        } finally { connection.disconnect() }
    }
}
