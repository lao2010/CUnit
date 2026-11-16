package com.cunit.app.api

import com.cunit.app.store.ApiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * OpenAI 兼容接口客户端（智谱 GLM / OpenAI / DeepSeek 均适用）。
 *
 * - POST {baseUrl}/chat/completions  —— 关键词提取
 * - POST {baseUrl}/embeddings        —— 文本向量化
 */
class LlmClient(private val settings: () -> ApiSettings) {

    private val json = Json { ignoreUnknownKeys = true }

    class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

    /** 从文本中提取关键词，返回 5~10 个关键词 */
    suspend fun extractKeywords(text: String): List<String> = withContext(Dispatchers.IO) {
        val s = settings()
        val body = buildJsonObject {
            put("model", s.chatModel)
            put("temperature", 0.2)
            put("messages", JsonArray(listOf(
                buildJsonObject {
                    put("role", "system")
                    put(
                        "content",
                        "你是一个关键词提取助手。从用户给出的内容中提取 5 到 10 个最能代表其主题的中文关键词。" +
                            "只输出一个 JSON 数组，例如 [\"旅行\", \"攻略\", \"日本\"]，不要输出任何其他内容。"
                    )
                },
                buildJsonObject {
                    put("role", "user")
                    put("content", text)
                }
            )))
        }
        val content = postForJson(s, "/chat/completions", body.toString())
            .jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw ApiException("LLM 响应格式异常：缺少 choices[0].message.content")

        parseKeywordArray(content)
    }

    /** 将文本向量化 */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val s = settings()
        val body = buildJsonObject {
            put("model", s.embeddingModel)
            put("input", text)
        }
        val arr = postForJson(s, "/embeddings", body.toString())
            .jsonObject["data"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("embedding")?.jsonArray
            ?: throw ApiException("Embedding 响应格式异常：缺少 data[0].embedding")

        FloatArray(arr.size) { i -> arr[i].jsonPrimitive.content.toFloat() }
    }

    /** 简单连通性测试 */
    suspend fun testChat(): String = withContext(Dispatchers.IO) {
        val s = settings()
        val body = buildJsonObject {
            put("model", s.chatModel)
            put("messages", JsonArray(listOf(
                buildJsonObject {
                    put("role", "user")
                    put("content", "回复\"OK\"两个字，不要输出其他内容。")
                }
            )))
        }
        val resp = postForJson(s, "/chat/completions", body.toString())
        resp.jsonObject["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: throw ApiException("LLM 响应格式异常")
    }

    private fun parseKeywordArray(content: String): List<String> {
        // 容错解析：剥掉 markdown 围栏，取第一个 [ 到最后一个 ] 之间的内容
        val cleaned = content.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start >= 0 && end > start) {
            try {
                val arr = json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonArray
                return arr.mapNotNull { el ->
                    el.jsonPrimitive.content.trim().takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {
                // 落到兜底方案
            }
        }
        // 兜底：按标点切分
        val fallback = cleaned.split(',', '，', '、', ' ', '\n')
            .map { it.trim('\"', '\'', '[', ']', ' ').trim() }
            .filter { it.isNotBlank() }
        if (fallback.isEmpty()) throw ApiException("无法从 LLM 响应中解析关键词：$content")
        return fallback
    }

    private fun postForJson(s: ApiSettings, path: String, body: String): kotlinx.serialization.json.JsonElement {
        if (s.apiKey.isBlank()) throw ApiException("API Key 未配置，请到「设置」页填写")
        val url = URL(s.baseUrl.trimEnd('/') + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${s.apiKey}")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { InputStreamReader(it, StandardCharsets.UTF_8).readText() } ?: ""
            if (code !in 200..299) {
                throw ApiException("API $code: ${text.take(300).ifBlank { "无响应体" }}")
            }
            if (text.isBlank()) throw ApiException("API 返回空响应")
            return json.parseToJsonElement(text)
        } finally {
            conn.disconnect()
        }
    }
}
