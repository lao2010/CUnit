package com.cunit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cunit.app.store.CUnitStore
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPage(vm: MainViewModel, backdrop: LayerBackdrop) {
    val units = vm.units
    val photoCount = units.count { it.photoFile != null }
    val videoCount = units.count { it.videoFile != null }
    val textOnly = units.count { !it.hasMedia }
    val searchable = units.count { it.embedding != null }
    val topKeywords = CUnitStore.keywordCounts(units)
    val dateFormat = rememberDateFormatter()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = bottomBarTailPadding()) // 尾距按导航条 inset 动态计算
    ) {
        item {
            Text("C 单位库", color = Inverse, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "个人知识存储 · ${if (vm.settings.isConfigured) "API 已配置" else "API 未配置"}",
                color = InverseMuted, fontSize = 13.sp
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(backdrop, Modifier.weight(1f), "${units.size}", "C 单位总数")
                StatCard(backdrop, Modifier.weight(1f), "$photoCount", "照片")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(backdrop, Modifier.weight(1f), "$videoCount", "视频")
                StatCard(backdrop, Modifier.weight(1f), "$textOnly", "纯文字")
            }
        }
        item {
            GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column {
                    Text("管线状态", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "可检索（已向量化）：$searchable / ${units.size}",
                        color = Inverse, fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            !vm.settings.isConfigured -> "未配置 API Key，存入时无法提取关键词与向量化，请到「设置」页填写"
                            else -> "关键词提取模型：${vm.settings.chatModel}\n向量化模型：${vm.settings.embeddingModel}"
                        },
                        color = InverseFaint, fontSize = 13.sp
                    )
                }
            }
        }
        if (topKeywords.isNotEmpty()) {
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column {
                        Text("高频关键词", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topKeywords.forEach { (word, count) ->
                                Text(
                                    "$word ×$count",
                                    color = Inverse,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (units.isNotEmpty()) {
            item {
                Text("最近添加", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    units.take(5).forEach { unit ->
                        GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                            Column {
                                Text(
                                    unit.note.take(40),
                                    color = Inverse, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val tags = buildList {
                                        if (unit.photoFile != null) add("📷")
                                        if (unit.videoFile != null) add("🎬")
                                        if (unit.body.isNotBlank()) add("📝")
                                        if (unit.embedding == null) add("未向量化")
                                    }
                                    Text(tags.joinToString(" "), color = InverseMuted, fontSize = 12.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        dateFormat.format(Date(unit.createdAt)),
                                        color = InverseFaint, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column {
                        Text("还没有 C 单位", color = Inverse, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "到「存入」页添加照片、视频或文字，\n写一段注释，AI 会自动提取关键词并向量化。",
                            color = InverseFaint, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    backdrop: LayerBackdrop,
    modifier: Modifier,
    value: String,
    label: String
) {
    GlassCard(backdrop, modifier, shape = RoundedCornerShape(24.dp), contentPadding = 16.dp) {
        // GlassCard 内容容器是 Box，必须用 Column 纵向排列，否则数字与标签重叠
        Column {
            Text(value, color = Inverse, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = InverseMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
