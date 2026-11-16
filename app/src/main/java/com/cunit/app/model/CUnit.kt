package com.cunit.app.model

import kotlinx.serialization.Serializable

/**
 * C 单位：一条知识的基本存储单元。
 *
 * 组成规则：
 * - note（注释）必填；
 * - 可以包含照片 / 视频 / 文字任意组合；
 * - 若不包含任何照片和视频，则 body（文字内容）必填。
 */
@Serializable
data class CUnit(
    val id: String,
    val createdAt: Long,
    val note: String,
    val body: String = "",
    val photoFile: String? = null,
    val videoFile: String? = null,
    val keywords: List<String> = emptyList(),
    val embedding: List<Float>? = null,
    val embeddingDim: Int = 0,
    val keywordsFailed: Boolean = false,
    val embeddingFailed: Boolean = false
) {
    val hasMedia: Boolean
        get() = photoFile != null || videoFile != null

    /** 用于向量化的文本：注释 + 正文 + 关键词 */
    val embeddableText: String
        get() = buildString {
            append(note)
            if (body.isNotBlank()) {
                append('\n')
                append(body)
            }
            if (keywords.isNotEmpty()) {
                append('\n')
                append(keywords.joinToString(" "))
            }
        }
}
