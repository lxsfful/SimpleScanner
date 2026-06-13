package com.lx.simplescanner.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.lx.simplescanner.SimpleScannerApp
import java.io.InputStream

/** Bitmap 通用工具 */

/** 限制最长边为 [maxEdge] 像素，等比缩放。 */
fun Bitmap.scaledToMaxEdge(maxEdge: Int): Bitmap {
    val longer = maxOf(width, height)
    if (longer <= maxEdge) return this
    val ratio = maxEdge.toFloat() / longer
    val newW = (width * ratio).toInt().coerceAtLeast(1)
    val newH = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, newW, newH, true)
}

/** 按 EXIF 旋转信息自动修正方向。 */
fun Bitmap.applyExifRotation(rotation: Int): Bitmap {
    if (rotation == 0) return this
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated !== this) recycle()
    return rotated
}

/**
 * 读取 [uri] 的 EXIF 旋转角度（0/90/180/270）。
 * 失败返回 0。
 */
fun readExifRotation(uri: Uri): Int = try {
    SimpleScannerApp.appContext.contentResolver.openInputStream(uri).use { input: InputStream? ->
        if (input == null) 0
        else when (ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
} catch (e: Exception) {
    0
}

/** 解码 [uri] 为 Bitmap，启用 inSampleSize 控制内存。 */
fun decodeBitmapFromUri(
    uri: Uri,
    maxEdge: Int = 2048,
): Bitmap? {
    val resolver = SimpleScannerApp.appContext.contentResolver

    // 第一次：只读尺寸
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val (w, h) = bounds.outWidth to bounds.outHeight
    if (w <= 0 || h <= 0) return null

    // 第二次：按 inSampleSize 解码
    val sample = calculateInSampleSize(w, h, maxEdge)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?.scaledToMaxEdge(maxEdge)
}

/** 计算 inSampleSize 使解码后的尺寸 ≥ [maxEdge] / 2 且 ≤ [maxEdge] * 2 */
private fun calculateInSampleSize(w: Int, h: Int, maxEdge: Int): Int {
    var sample = 1
    val longer = maxOf(w, h)
    while (longer / sample > maxEdge * 2) sample *= 2
    return sample
}
