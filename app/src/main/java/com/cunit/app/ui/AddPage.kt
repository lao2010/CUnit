package com.cunit.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch

@Composable
fun AddPage(vm: MainViewModel, backdrop: LayerBackdrop) {
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.onPhotoPicked(uri)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.onVideoPicked(uri)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 96.dp) // 尾部留白=栏高：滚到底最后一项不被挡；滚动过程中内容仍从栏底穿过（透字）
    ) {
        item {
            Text("存入 C 单位", color = Inverse, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                if (vm.pendingPhoto != null || vm.pendingVideo != null)
                    "已选媒体：写一段注释即可保存"
                else
                    "无媒体时请用文字内容代替（此时文字必填）",
                color = InverseMuted, fontSize = 13.sp
            )
        }
        item {
            GlassCard(backdrop, Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column {
                    Text("媒体（可选，可同时包含）", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassButton("选择照片", backdrop, {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, modifier = Modifier.weight(1f), height = 44.dp)
                        GlassButton("选择视频", backdrop, {
                            videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }, modifier = Modifier.weight(1f), height = 44.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        vm.mediaFile(vm.pendingPhoto)?.let { photo ->
                            MediaThumb(
                                rememberMediaThumb(photo),
                                modifier = Modifier.size(84.dp),
                                corner = 14.dp
                            )
                        }
                        vm.mediaFile(vm.pendingVideo)?.let { video ->
                            Box(
                                Modifier.size(84.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                MediaThumb(
                                    rememberMediaThumb(video),
                                    modifier = Modifier.size(84.dp)
                                )
                                Text("🎬", fontSize = 22.sp)
                            }
                        }
                    }
                    if (vm.pendingPhoto != null || vm.pendingVideo != null) {
                        Spacer(Modifier.height(12.dp))
                        GlassButton("移除全部媒体", backdrop, { vm.clearPendingMedia() }, height = 40.dp, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("注释（必填）", color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                GlassTextField(
                    value = vm.addNote,
                    onValueChange = { vm.addNote = it },
                    hint = "写一段注释，例如：周末爬山的视频，风景很好",
                    backdrop = backdrop,
                    minLines = 2,
                    maxLines = 5
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "文字内容" + if (vm.pendingPhoto == null && vm.pendingVideo == null) "（无媒体时必填）" else "（可选）",
                    color = InverseMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                GlassTextField(
                    value = vm.addBody,
                    onValueChange = { vm.addBody = it },
                    hint = "补充文字、想法、记录……",
                    backdrop = backdrop,
                    minLines = 3,
                    maxLines = 10
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val canSave = vm.addNote.isNotBlank() &&
                    (vm.pendingPhoto != null || vm.pendingVideo != null || vm.addBody.isNotBlank()) &&
                    // 只在流水线进行中禁用；DONE/ERROR/IDLE 都允许再次保存
                    vm.ingestPhase != IngestPhase.EXTRACTING_KEYWORDS &&
                    vm.ingestPhase != IngestPhase.EMBEDDING
                GlassButton(
                    label = "保存 C 单位",
                    backdrop = backdrop,
                    onClick = { scope.launch { vm.addUnit() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave,
                    height = 52.dp
                )
                val phaseText = when (vm.ingestPhase) {
                    IngestPhase.EXTRACTING_KEYWORDS -> "① 正在通过 LLM 提取关键词…"
                    IngestPhase.EMBEDDING -> "② 正在向量化…"
                    IngestPhase.DONE -> "✓ 已保存，可到「检索」页查询"
                    IngestPhase.ERROR -> "✗ ${vm.ingestError}"
                    else -> ""
                }
                if (phaseText.isNotEmpty()) {
                    Text(phaseText, color = if (vm.ingestPhase == IngestPhase.ERROR) androidx.compose.ui.graphics.Color(0xFFFF8A9B) else InverseMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
