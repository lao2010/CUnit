package com.cunit.app.store

import android.content.Context
import com.cunit.app.model.CUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.math.sqrt

/**
 * C 单位存储：
 * - 每条 C 单位一个 JSON 文件（units/{id}.json），包含关键词与向量；
 * - 媒体原件保存在 media/ 目录，C 单位内只记录文件名。
 */
class CUnitStore(context: Context) {
    private val unitsDir = File(context.filesDir, "units").apply { mkdirs() }
    val mediaDir = File(context.filesDir, "media").apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    @Volatile
    private var cache: List<CUnit> = loadAll()

    fun all(): List<CUnit> = cache

    private fun loadAll(): List<CUnit> =
        unitsDir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.mapNotNull { f ->
                try {
                    json.decodeFromString(CUnit.serializer(), f.readText())
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()

    fun create(
        note: String,
        body: String,
        photoFile: String?,
        videoFile: String?
    ): CUnit = CUnit(
        id = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis(),
        note = note,
        body = body,
        photoFile = photoFile,
        videoFile = videoFile
    )

    suspend fun save(unit: CUnit) {
        mutex.withLock {
            val file = File(unitsDir, "${unit.id}.json")
            file.writeText(json.encodeToString(CUnit.serializer(), unit))
            cache = cache.filterNot { it.id == unit.id } + unit
            cache = cache.sortedByDescending { it.createdAt }
        }
    }

    suspend fun delete(unitId: String) {
        mutex.withLock {
            val unit = cache.firstOrNull { it.id == unitId }
            unit?.photoFile?.let { File(mediaDir, it).delete() }
            unit?.videoFile?.let { File(mediaDir, it).delete() }
            File(unitsDir, "$unitId.json").delete()
            cache = cache.filterNot { it.id == unitId }
        }
    }

    fun mediaFile(name: String): File = File(mediaDir, name)

    /** 将选中的媒体内容复制到 media/ 目录，返回存储的文件名 */
    fun saveMedia(id: String, source: InputStream, originalName: String?): String {
        val ext = originalName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: "bin"
        val name = "$id.$ext"
        File(mediaDir, name).outputStream().use { out -> source.copyTo(out) }
        return name
    }

    /**
     * 向量检索：对查询向量与所有 C 单位向量做余弦相似度，返回按相似度降序的结果。
     */
    fun search(queryEmbedding: FloatArray, topK: Int = 20): List<Pair<CUnit, Float>> {
        val qNorm = norm(queryEmbedding)
        if (qNorm == 0f) return emptyList()
        return cache.asSequence()
            .filter { it.embedding != null && it.embeddingDim == queryEmbedding.size }
            .map { u ->
                val v = u.embedding!!.toFloatArray()
                var dot = 0f
                for (i in v.indices) dot += v[i] * queryEmbedding[i]
                u to dot / (qNorm * norm(v) + 1e-8f)
            }
            .filter { (_, s) -> s > 0.2f } // 过滤明显无关
            .toList()
            .sortedByDescending { it.second }
            .take(topK)
    }

    private fun norm(v: FloatArray): Float {
        var s = 0f
        for (x in v) s += x * x
        return sqrt(s)
    }

    companion object {
        /** 关键词频次统计（用于状态页） */
        fun keywordCounts(units: List<CUnit>, topN: Int = 20): List<Pair<String, Int>> =
            units.asSequence()
                .flatMap { it.keywords }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(topN)
                .map { it.key to it.value }
    }
}
