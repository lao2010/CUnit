package com.cunit.app.store

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ApiSettings(
    val baseUrl: String = "https://open.bigmodel.cn/api/paas/v4",
    val apiKey: String = "",
    val chatModel: String = "glm-4-flash",
    val embeddingModel: String = "embedding-3"
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

class SettingsStore(context: Context) {
    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: ApiSettings = load()

    val current: ApiSettings get() = cache

    private fun load(): ApiSettings =
        if (file.exists()) {
            try {
                json.decodeFromString(ApiSettings.serializer(), file.readText())
            } catch (_: Exception) {
                ApiSettings()
            }
        } else {
            ApiSettings()
        }

    @Synchronized
    fun update(settings: ApiSettings) {
        cache = settings
        file.writeText(json.encodeToString(ApiSettings.serializer(), settings))
    }
}
