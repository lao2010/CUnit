package com.cunit.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object AppIcons {

    private fun build(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathBuilder().apply(block).nodes,
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
            )
        }.build()

    /** 状态：柱状图 */
    val Status: ImageVector by lazy {
        build("status") {
            moveTo(4f, 20f)
            verticalLineTo(10f)
            moveTo(12f, 20f)
            verticalLineTo(4f)
            moveTo(20f, 20f)
            verticalLineTo(13f)
        }
    }

    /** 存入：加号 */
    val Add: ImageVector by lazy {
        build("add") {
            moveTo(12f, 5f)
            verticalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(19f)
        }
    }

    /** 检索：放大镜 */
    val Search: ImageVector by lazy {
        build("search") {
            moveTo(10.5f, 4f)
            arcTo(6.5f, 6.5f, 0f, false, true, 10.5f, 17f)
            arcTo(6.5f, 6.5f, 0f, false, true, 10.5f, 4f)
            close()
            moveTo(15.5f, 15.5f)
            lineTo(20f, 20f)
        }
    }

    /** 设置：滑杆 */
    val Settings: ImageVector by lazy {
        build("settings") {
            moveTo(4f, 7f)
            horizontalLineTo(20f)
            moveTo(4f, 12f)
            horizontalLineTo(20f)
            moveTo(4f, 17f)
            horizontalLineTo(20f)
            moveTo(9f, 7f)
            arcTo(2f, 2f, 0f, false, true, 9f, 3f)
            arcTo(2f, 2f, 0f, false, true, 9f, 7f)
            moveTo(15f, 12f)
            arcTo(2f, 2f, 0f, false, true, 15f, 8f)
            arcTo(2f, 2f, 0f, false, true, 15f, 12f)
            moveTo(8f, 17f)
            arcTo(2f, 2f, 0f, false, true, 8f, 13f)
            arcTo(2f, 2f, 0f, false, true, 8f, 17f)
        }
    }
}
