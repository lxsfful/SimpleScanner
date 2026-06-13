package com.lx.simplescanner.image

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * 底色矫正：把彩色文档图二值化，输出"白底黑字"或"黑底白字"。
 *
 * 使用自适应阈值，能处理光照不均的文档（阴影/局部偏暗）。
 * 输出转 4 通道（与原图同维度），方便后续与彩色区域合成。
 */
object Binarizer {

    /** 输出通道数：BGRA（与原图同维度） */
    private const val OUTPUT_CHANNELS = 4

    /**
     * @param src 输入（CV_8UC4 BGRA 或 CV_8UC3 BGR）
     * @param blockSize 自适应阈值邻域大小，**必须奇数**（推荐 11/15/21）
     * @param c 阈值修正值（越大 → 黑色越多）
     * @return 4 通道二值化 Mat（白底黑字）
     */
    fun binarize(
        src: Mat,
        blockSize: Int = 15,
        c: Double = 10.0,
    ): Mat {
        require(blockSize % 2 == 1) { "blockSize must be odd, got $blockSize" }

        val gray = Mat()
        val bw = Mat()
        val bwRgba = Mat()

        try {
            // 灰度化
            if (src.channels() == 4) {
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY)
            } else {
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
            }
            // 自适应二值化（高斯法，输出白底黑字）
            Imgproc.adaptiveThreshold(
                gray,
                bw,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                blockSize,
                c
            )
            // 单通道 → 4 通道，方便后续合成
            Imgproc.cvtColor(bw, bwRgba, Imgproc.COLOR_GRAY2BGRA)
        } catch (e: Exception) {
            gray.release()
            bw.release()
            bwRgba.release()
            throw e
        }

        gray.release()
        bw.release()
        return bwRgba
    }
}
