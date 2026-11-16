package com.cunit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cunit.app.store.ApiSettings
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch

private val ErrorColor = Color(0xFFFF8A9B)
private val OkColor = Color(0xFF9BE7C4)

@Composable
fun SettingsPage(vm: MainViewModel, backdrop: LayerBackdrop) {
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(vm.settings.baseUrl) }
    var apiKey by remember { mutableStateOf(vm.settings.apiKey) }
    var chatModel by remember { mutableStateOf(vm.settings.chatModel) }
    var embeddingModel by remember { mutableStateOf(vm.settings.embeddingModel) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 96.dp)
    ) {
        item {
            Text("设置", color = Inverse, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "兼容 OpenAI 接口（智谱 GLM / OpenAI / DeepSeek 等）",
                color = InverseMuted, fontSize = 13.sp
            )
        }
        item {
            GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel("API Base URL")
                    GlassTextField(baseUrl, { baseUrl = it }, "https://open.bigmodel.cn/api/paas/v4", backdrop, singleLine = true)
                    FieldLabel("API Key")
                    GlassTextField(apiKey, { apiKey = it }, "sk-…", backdrop, singleLine = true)
                    FieldLabel("关键词提取模型（Chat）")
                    GlassTextField(chatModel, { chatModel = it }, "glm-4-flash", backdrop, singleLine = true)
                    FieldLabel("向量化模型（Embedding）")
                    GlassTextField(embeddingModel, { embeddingModel = it }, "embedding-3", backdrop, singleLine = true)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    "保存设置", backdrop,
                    onClick = {
                        vm.updateSettings(
                            ApiSettings(
                                baseUrl = baseUrl.trim(),
                                apiKey = apiKey.trim(),
                                chatModel = chatModel.trim(),
                                embeddingModel = embeddingModel.trim()
                            )
                        )
                        vm.apiTestResult = "✓ 已保存"
                    },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                GlassButton(
                    if (vm.apiTestRunning) "测试中…" else "测试连接",
                    backdrop,
                    onClick = { scope.launch { vm.testApi() } },
                    modifier = Modifier.weight(1f),
                    enabled = !vm.apiTestRunning,
                    height = 46.dp
                )
            }
        }
        vm.apiTestResult?.let { result ->
            item {
                GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                    Text(
                        result,
                        color = if (result.startsWith("✓") || result.startsWith("连接成功")) OkColor else ErrorColor,
                        fontSize = 13.sp
                    )
                }
            }
        }
        item {
            GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("工作原理", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "保存 C 单位时：\n1. 注释+文字内容发给 Chat 模型提取关键词；\n2. 注释+文字+关键词发给 Embedding 模型生成向量；\n3. 媒体原件、关键词、向量一并持久化。\n\n检索时：查询文字被向量化，与所有 C 单位计算余弦相似度。",
                        color = InverseFaint, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}
