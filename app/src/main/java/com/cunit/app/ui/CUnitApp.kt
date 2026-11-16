package com.cunit.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Status("状态", AppIcons.Status),
    Add("存入", AppIcons.Add),
    Search("检索", AppIcons.Search),
    Settings("设置", AppIcons.Settings)
}

@Composable
fun CUnitApp(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) } // 默认进入「状态」页

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0C11))) {
        val backdrop = rememberLayerBackdrop {
            drawRect(Color(0xFF0B0C11))
            drawContent()
        }

        // 只有背景渐变被记入 backdrop。
        // 官方文档（glass-bottom-sheet 教程）：玻璃元素不能出现在 layerBackdrop 的记录范围内，
        // 否则"写 layer 的同时读 layer"形成循环，RenderThread SIGSEGV 闪退。
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            AppBackgroundGradient()
        }

        // 页面内容在记录范围之外，其中的玻璃卡片可安全读取 backdrop
        Box(Modifier.fillMaxSize()) {
            when (Tab.entries[tab]) {
                Tab.Status -> StatusPage(vm, backdrop)
                Tab.Add -> AddPage(vm, backdrop)
                Tab.Search -> SearchPage(vm, backdrop)
                Tab.Settings -> SettingsPage(vm, backdrop)
            }
        }

        // 玻璃底部导航
        GlassBottomBar(
            backdrop = backdrop,
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GlassBottomBar(
    backdrop: LayerBackdrop,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 官方教程结构：safeContentPadding + 64dp 高玻璃栏；
    // 选中项用 BlendMode.Hue 染色；按压缩放动画放在 layerBlock（backdrop 不缩放）
    Row(
        modifier
            .safeContentPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab.entries.forEachIndexed { index, tabItem ->
            val isSelected = index == selected
            val animationScope = rememberCoroutineScope()
            val progressAnimation = remember { Animatable(0f) }

            Box(
                Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(16f.dp.toPx(), 32f.dp.toPx())
                        },
                        layerBlock = {
                            // 教程要点：缩放必须放在 layerBlock，backdrop 不会跟着缩放
                            val progress = progressAnimation.value
                            val maxScale = (size.width + 16f.dp.toPx()) / size.width
                            val scale = lerp(1f, maxScale, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = {
                            if (isSelected) {
                                // 教程推荐 BlendMode.Hue：色相随背景自适应
                                val tint = Color(0xFF0088FF)
                                drawRect(tint, blendMode = BlendMode.Hue)
                                drawRect(tint.copy(alpha = 0.75f))
                            } else {
                                drawRect(Color.White.copy(alpha = 0.15f))
                            }
                        }
                    )
                    .clickable(interactionSource = null, indication = null) { onSelect(index) }
                    .pointerInput(animationScope) {
                        val animationSpec = spring(0.5f, 300f, 0.001f)
                        awaitEachGesture {
                            // 按下放大
                            awaitFirstDown()
                            animationScope.launch {
                                progressAnimation.animateTo(1f, animationSpec)
                            }
                            // 松开回弹
                            waitForUpOrCancellation()
                            animationScope.launch {
                                progressAnimation.animateTo(0f, animationSpec)
                            }
                        }
                    }
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = tabItem.icon,
                        contentDescription = tabItem.label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        tabItem.label,
                        color = Color.White.copy(alpha = if (isSelected) 1f else 0.62f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
