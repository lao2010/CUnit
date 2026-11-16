package com.cunit.app.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cunit.app.api.LlmClient
import com.cunit.app.model.CUnit
import com.cunit.app.store.ApiSettings
import com.cunit.app.store.CUnitStore
import com.cunit.app.store.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class IngestPhase { IDLE, EXTRACTING_KEYWORDS, EMBEDDING, DONE, ERROR }

data class SearchResult(val unit: CUnit, val score: Float)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val store = CUnitStore(app)
    private val llm = LlmClient { settingsStore.current }

    var units by mutableStateOf(store.all())
        private set

    fun mediaFile(name: String?): java.io.File? = name?.let { store.mediaDir.resolve(it) }

    var settings by mutableStateOf(settingsStore.current)
        private set

    // ---- 存入页状态 ----
    var addNote by mutableStateOf("")
    var addBody by mutableStateOf("")
    var pendingPhoto by mutableStateOf<String?>(null)
        private set
    var pendingVideo by mutableStateOf<String?>(null)
        private set
    var ingestPhase by mutableStateOf(IngestPhase.IDLE)
        private set
    var ingestError by mutableStateOf<String?>(null)
        private set
    var lastSavedId by mutableStateOf<String?>(null)
        private set

    // ---- 检索页状态 ----
    var searchQuery by mutableStateOf("")
    var searching by mutableStateOf(false)
        private set
    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var searchedOnce by mutableStateOf(false)
        private set

    // ---- 设置页状态 ----
    var apiTestRunning by mutableStateOf(false)
        private set
    var apiTestResult by mutableStateOf<String?>(null)

    // ---- 媒体选取 ----

    fun onPhotoPicked(uri: Uri?) {
        if (uri == null) return
        copyToStore(uri, "photo")?.let { pendingPhoto = it }
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri == null) return
        copyToStore(uri, "video")?.let { pendingVideo = it }
    }

    fun clearPhoto() {
        pendingPhoto?.let { store.mediaFile(it).delete() }
        pendingPhoto = null
    }

    fun clearVideo() {
        pendingVideo?.let { store.mediaFile(it).delete() }
        pendingVideo = null
    }

    private fun copyToStore(uri: Uri, kind: String): String? = try {
        val resolver = getApplication<Application>().contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        val displayName = name ?: "$kind"
        val id = "pending_${kind}_${System.currentTimeMillis()}"
        resolver.openInputStream(uri)?.use { store.saveMedia(id, it, displayName) }
    } catch (e: Exception) {
        ingestError = "媒体保存失败: ${e.message}"
        null
    }

    // ---- C 单位存入流水线 ----

    fun addUnit() {
        if (ingestPhase != IngestPhase.IDLE && ingestPhase != IngestPhase.DONE
            && ingestPhase != IngestPhase.ERROR
        ) return

        viewModelScope.launch {
            val ok = doAddUnit(addNote, addBody)
            if (ok) {
                addNote = ""
                addBody = ""
            }
        }
    }

    private suspend fun doAddUnit(note: String, body: String): Boolean {

        val hasMedia = pendingPhoto != null || pendingVideo != null
        if (note.isBlank()) {
            ingestError = "注释不能为空"
            ingestPhase = IngestPhase.ERROR
            return false
        }
        if (!hasMedia && body.isBlank()) {
            ingestError = "没有照片或视频时，必须填写文字内容"
            ingestPhase = IngestPhase.ERROR
            return false
        }

        ingestError = null
        val pendingId = "pending_${System.currentTimeMillis()}"
        val unit = store.create(note.trim(), body.trim(), pendingPhoto, pendingVideo)
        try {
            // 1. LLM 关键词提取
            ingestPhase = IngestPhase.EXTRACTING_KEYWORDS
            val keywords = try {
                llm.extractKeywords(unit.note + "\n" + unit.body)
            } catch (e: Exception) {
                null
            }
            val u1 = if (keywords != null) {
                unit.copy(keywords = keywords)
            } else {
                unit.copy(keywordsFailed = true)
            }

            // 2. 向量化（注释 + 正文 + 关键词）
            ingestPhase = IngestPhase.EMBEDDING
            val embedding = try {
                llm.embed(u1.embeddableText)
            } catch (e: Exception) {
                null
            }
            val u2 = if (embedding != null) {
                u1.copy(embedding = embedding.toList(), embeddingDim = embedding.size)
            } else {
                u1.copy(embeddingFailed = true)
            }

            // 3. 落盘
            store.save(u2)
            units = store.all()
            lastSavedId = u2.id
            ingestPhase =
                if (u2.keywordsFailed || u2.embeddingFailed) IngestPhase.ERROR else IngestPhase.DONE
            ingestError = buildString {
                if (u2.keywordsFailed) append("关键词提取失败（已保存，可重试）")
                if (u2.embeddingFailed) {
                    if (isNotEmpty()) append("；")
                    append("向量化失败（已保存，暂不可检索）")
                }
            }.ifBlank { null }
            clearPendingMedia()
            return ingestPhase == IngestPhase.DONE
        } catch (e: Exception) {
            ingestError = e.message ?: e.toString()
            ingestPhase = IngestPhase.ERROR
            return false
        }
    }

    /** 对失败过关键词/向量的单元重跑流水线 */
    suspend fun retryUnit(unitId: String) {
        val unit = units.firstOrNull { it.id == unitId } ?: return
        var u = unit
        if (u.keywordsFailed || u.keywords.isEmpty()) {
            val kw = try {
                llm.extractKeywords(u.note + "\n" + u.body)
            } catch (e: Exception) {
                null
            }
            if (kw != null) u = u.copy(keywords = kw, keywordsFailed = false)
        }
        if (u.embeddingFailed || u.embedding == null) {
            val emb = try {
                llm.embed(u.embeddableText)
            } catch (e: Exception) {
                null
            }
            if (emb != null) u = u.copy(embedding = emb.toList(), embeddingDim = emb.size, embeddingFailed = false)
        }
        store.save(u)
        units = store.all()
    }

    suspend fun deleteUnit(unitId: String) {
        store.delete(unitId)
        units = store.all()
        if (lastSavedId == unitId) lastSavedId = null
    }

    fun clearPendingMedia() {
        pendingPhoto = null
        pendingVideo = null
    }

    // ---- 检索 ----

    suspend fun search() {
        val q = searchQuery.trim()
        if (q.isEmpty()) return
        searching = true
        searchError = null
        try {
            val qVec = llm.embed(q)
            searchResults = store.search(qVec).map { (u, s) -> SearchResult(u, s) }
            searchedOnce = true
        } catch (e: Exception) {
            searchError = e.message ?: e.toString()
            searchResults = emptyList()
            searchedOnce = true
        } finally {
            searching = false
        }
    }

    // ---- 设置 ----

    fun updateSettings(s: ApiSettings) {
        settingsStore.update(s)
        settings = settingsStore.current
    }

    suspend fun testApi() {
        apiTestRunning = true
        apiTestResult = null
        try {
            val reply = llm.testChat()
            apiTestResult = "连接成功：$reply"
        } catch (e: Exception) {
            apiTestResult = "连接失败：${e.message}"
        } finally {
            apiTestRunning = false
        }
    }
}
