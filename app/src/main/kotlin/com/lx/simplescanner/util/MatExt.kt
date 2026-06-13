package com.lx.simplescanner.util

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Bitmap ↔ Mat 互转工具。
 *
 * 约定：Bitmap 是 ARGB_8888，Mat 是 CV_8UC4（4 通道 BGRA，OpenCV 习惯）。
 *
 * 资源管理：调用方负责回收 Mat；Bitmap 由调用方负责 recycle。
 */
object MatExt {

    /** [Bitmap] (ARGB_8888) → [Mat] (CV_8UC4)。**不**会持有 Bitmap 引用。 */
    fun bitmapToMat(bitmap: Bitmap): Mat {
        require(bitmap.config == Bitmap.Config.ARGB_8888) {
            "Only ARGB_8888 is supported, got ${bitmap.config}"
        }
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }

    /** [Mat] (CV_8UC4 或 CV_8UC3) → [Bitmap] (ARGB_8888)。新分配 Bitmap。 */
    fun matToBitmap(mat: Mat): Bitmap {
        val channels = mat.channels()
        require(channels == 4 || channels == 3) {
            "Only 3 or 4 channel Mats are supported, got $channels"
        }
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    /** 安全释放 Mat 列表 */
    fun releaseAll(vararg mats: Mat?) {
        mats.forEach { it?.release() }
    }

    /** 释放 List<Mat> */
    fun releaseAll(mats: Collection<Mat>) {
        mats.forEach { it.release() }
    }
}
