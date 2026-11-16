package com.cunit.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ErrorColor = Color(0xFFFF8A9B)

@Composable
fun SearchPage(vm: MainViewModel, backdrop: LayerBackdrop) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var selected by remember { mutableStateOf<SearchResult?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 96.dp)
        ) {
            item {
                Text("语义检索", color = Inverse, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "输入想找的内容，按向量相似度排序返回 C 单位",
                    color = InverseMuted, fontSize = 13.sp
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassTextField(
                        value = vm.searchQuery,
                        onValueChange = { vm.searchQuery = it },
                        hint = "例如：上个月爬山的那段视频",
                        backdrop = backdrop,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        label = if (vm.searching) "检索中" else "搜索",
                        backdrop = backdrop,
                        onClick = { scope.launch { vm.search() } },
                        enabled = vm.searchQuery.isNotBlank() && !vm.searching,
                        height = 46.dp
                    )
                }
            }
            if (vm.searchError != null) {
                item {
                    GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                        Text("检索失败：${vm.searchError}", color = ErrorColor, fontSize = 13.sp)
                    }
                }
            }
            if (vm.searchedOnce && vm.searchResults.isEmpty() && vm.searchError == null && !vm.searching) {
                item {
                    GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
                        Text("没有找到相似内容", color = InverseMuted, fontSize = 14.sp)
                    }
                }
            }
            for (i in vm.searchResults.indices) {
                val result = vm.searchResults[i]
                item(key = result.unit.id) {
                    ResultCard(vm, backdrop, result, dateFormat) { selected = result }
                }
            }
        }

        selected?.let { result ->
            ResultDetail(
                backdrop = backdrop,
                result = result,
                dateFormat = dateFormat,
                vm = vm,
                onOpenVideo = {
                    vm.mediaFile(result.unit.videoFile)?.let { file ->
                        val uri = FileProvider.getUriForFile(context, "com.cunit.app.files", file)
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(uri, "video/*")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    }
                },
                onClose = { selected = null }
            )
        }
    }
}

@Composable
private fun ResultCard(
    vm: MainViewModel,
    backdrop: LayerBackdrop,
    result: SearchResult,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val unit = result.unit
    GlassCard(
        backdrop,
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        contentPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val photoBm = rememberMediaThumb(vm.mediaFile(unit.photoFile))
                if (photoBm != null) {
                    MediaThumb(photoBm, modifier = Modifier.size(40.dp), corner = 10.dp)
                }
                if (unit.videoFile != null) {
                    Text("🎬", fontSize = 14.sp)
                }
                Text("相似度 ${(result.score * 100).toInt()}%", color = InverseMuted, fontSize = 12.sp)
            }
            Text(unit.note, color = Inverse, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            if (unit.body.isNotBlank()) {
                Text(unit.body, color = InverseFaint, fontSize = 13.sp, maxLines = 3)
            }
            if (unit.keywords.isNotEmpty()) {
                Text(unit.keywords.joinToString(" · "), color = InverseMuted, fontSize = 12.sp, maxLines = 2)
            }
            Text(dateFormat.format(Date(unit.createdAt)), color = InverseFaint, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ResultDetail(
    backdrop: LayerBackdrop,
    result: SearchResult,
    dateFormat: SimpleDateFormat,
    vm: MainViewModel,
    onOpenVideo: () -> Unit,
    onClose: () -> Unit
) {
    val unit = result.unit
    Box(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("相似度 ${(result.score * 100).toInt()}%", color = InverseMuted, fontSize = 13.sp)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        GlassButton(
                            "关闭", backdrop, onClose, height = 34.dp,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                vm.mediaFile(unit.photoFile)?.let { photo ->
                    rememberMediaThumb(photo)?.let { bm ->
                        MediaThumb(
                            bm,
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            corner = 16.dp
                        )
                    }
                }
                if (unit.videoFile != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎬 视频已保存", color = Inverse, fontSize = 14.sp)
                        GlassButton("播放", backdrop, onOpenVideo, height = 34.dp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("注释", color = InverseMuted, fontSize = 12.sp)
                    Text(unit.note, color = Inverse, fontSize = 15.sp)
                }
                if (unit.body.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("文字内容", color = InverseMuted, fontSize = 12.sp)
                        Text(unit.body, color = Inverse, fontSize = 14.sp)
                    }
                }
                if (unit.keywords.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI 关键词", color = InverseMuted, fontSize = 12.sp)
                        Text(unit.keywords.joinToString(" · "), color = Inverse, fontSize = 13.sp)
                    }
                }
                Text("保存于 ${dateFormat.format(Date(unit.createdAt))}", color = InverseFaint, fontSize = 12.sp)
            }
        }
    }
}
