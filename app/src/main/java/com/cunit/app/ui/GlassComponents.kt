package com.cunit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val Inverse = Color(0xFFFFFFFF)
val InverseMuted = Color(0xCCFFFFFF)
val InverseFaint = Color(0x99FFFFFF)

/**
 * 页面滚动尾距 = 系统导航条高度 + 玻璃栏总高（6dp 外边距 + 64dp 栏身 + 6dp 余量）。
 * 保证滚到底时最后一项完整停在玻璃栏上方；滚动过程中内容仍从栏底穿过（透字）。
 */
@Composable
fun bottomBarTailPadding(): Dp {
    val density = LocalDensity.current
    return with(density) { WindowInsets.navigationBars.getBottom(density).toDp() } + 76.dp
}

/** 主题渐变背景（画在 backdrop 记录的内容里，玻璃折射才有效果） */
@Composable
fun AppBackgroundGradient(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Color(0xFF0B0C11))
                drawAppBlobs()
            }
    )
}

private fun DrawScope.drawAppBlobs() {
    val w = size.width
    val h = size.height
    val blobs = listOf(
        Triple(Color(0xFF6C4DF6), Offset(w * 0.15f, h * 0.10f), w * 0.75f),
        Triple(Color(0xFF0E8EDB), Offset(w * 1.05f, h * 0.32f), w * 0.80f),
        Triple(Color(0xFFE86A92), Offset(w * 0.10f, h * 0.62f), w * 0.65f),
        Triple(Color(0xFF2BB673), Offset(w * 0.95f, h * 0.88f), w * 0.70f)
    )
    for ((color, center, radius) in blobs) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.45f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/** 玻璃卡片：liquid glass 材质容器 */
@Composable
fun GlassCard(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(24.dp),
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(3f.dp.toPx())
                    lens(12f.dp.toPx(), 40f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.10f)) }
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/** 玻璃按钮 */
@Composable
fun GlassButton(
    label: String,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.White,
    height: Dp = 48.dp,
    shape: CornerBasedShape = RoundedCornerShape(24.dp)
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(10f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(tint.copy(alpha = if (enabled) 0.22f else 0.10f))
                }
            )
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) tint else tint.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 玻璃输入框 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = false
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(18.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(8f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.10f)) }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 占位文字叠加在 BasicTextField 上（而非独立一行），
        // 点击占位区域才能命中输入框并弹出输入法
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Inverse, fontSize = 15.sp),
                cursorBrush = SolidColor(Inverse),
                minLines = minLines,
                maxLines = maxLines,
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(
                    hint,
                    color = InverseFaint,
                    fontSize = 15.sp,
                    modifier = Modifier.align(
                        if (minLines > 1) Alignment.TopStart else Alignment.CenterStart
                    )
                )
            }
        }
    }
}
