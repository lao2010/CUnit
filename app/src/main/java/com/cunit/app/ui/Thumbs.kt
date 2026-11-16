package com.cunit.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 玻璃卡片内的媒体缩略图 */
@Composable
fun MediaThumb(bitmap: Bitmap?, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    if (bitmap == null) return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner))
    )
}

/** 读取并解码缩略图：照片用 BitmapFactory，视频用 MediaMetadataRetriever 抽帧 */
@Composable
fun rememberMediaThumb(mediaFile: File?, targetSize: Int = 256): Bitmap? {
    if (mediaFile == null || !mediaFile.exists()) return null
    val path = mediaFile.absolutePath
    val isVideo = mediaFile.extension in setOf("mp4", "mov", "avi", "mkv", "3gp", "webm", "m4v")
    return produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            try {
                if (isVideo) decodeVideoFrame(path, targetSize) else decodePhoto(path, targetSize)
            } catch (_: Exception) {
                null
            }
        }
    }.value
}

private fun decodePhoto(path: String, target: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, opts)
}

private fun decodeVideoFrame(path: String, target: Int): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val w = frame.width
        val h = frame.height
        if (w <= target) return frame
        val scale = target.toFloat() / w
        return Bitmap.createScaledBitmap(frame, (w * scale).toInt(), (h * scale).toInt(), true)
    } finally {
        retriever.release()
    }
}
