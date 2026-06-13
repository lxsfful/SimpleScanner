package com.lx.simplescanner.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * 颜色保留：识别"明显彩色"区域（红头、印章、彩照）并保留原色，
 * 其余区域用二值化结果填充。
 *
 * 启发式判据（默认阈值）：
 *  - 饱和度 S 高 → 视为有彩色
 *  - 亮度 V 合理（不能太暗也不能过曝）→ 视为有效彩色
 *
 * 合成顺序：
 *  1. result = binarized（白底黑字打底）
 *  2. original.copyTo(result, colorMask)（彩色区域用原色覆盖）
 */
object ColorPreserver {

    /** 默认：S >= 60 且 V ∈ [50, 240] */
    const val DEFAULT_S_MIN = 60.0
    const val DEFAULT_V_MIN = 50.0
    const val DEFAULT_V_MAX = 240.0

    /**
     * @param original 拉正后的原图（CV_8UC4）
     * @param binarized 二值化结果（CV_8UC4，白底黑字）
     * @return 合成后 Mat（CV_8UC4）
     */
    fun combine(
        original: Mat,
        binarized: Mat,
        sMin: Double = DEFAULT_S_MIN,
        vMin: Double = DEFAULT_V_MIN,
        vMax: Double = DEFAULT_V_MAX,
    ): Mat {
        val hsv = Mat()
        val channels = ArrayList<Mat>(3)
        val satMask = Mat()
        val valMask = Mat()
        val colorMask = Mat()
        val kernel = Mat()
        val cleanedMask = Mat()
        val result = Mat()

        try {
            // 1. 转 HSV（注意 OpenCV 是 BGRA → HSV）
            Imgproc.cvtColor(original, hsv, Imgproc.COLOR_BGRA2BGR)
            Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_BGR2HSV)
            // 2. 拆分通道
            Core.split(hsv, channels)
            val sat = channels[1]
            val value = channels[2]
            // 3. 双阈值掩码
            Core.inRange(sat, Scalar(sMin), Scalar(255.0), satMask)
            Core.inRange(value, Scalar(vMin), Scalar(vMax), valMask)
            Core.bitwise_and(satMask, valMask, colorMask)
            // 4. 形态学清理（开运算去噪点 + 闭运算补小洞）
            kernel.release()
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0), Point(-1.0, -1.0))
                .copyTo(kernel)
            Imgproc.morphologyEx(colorMask, cleanedMask, Imgproc.MORPH_OPEN, kernel)
            Imgproc.morphologyEx(cleanedMask, colorMask, Imgproc.MORPH_CLOSE, kernel)
            // 5. 合成：底色用二值化，彩色区域覆盖原色
            binarized.copyTo(result)
            original.copyTo(result, colorMask)
        } catch (e: Exception) {
            hsv.release()
            channels.forEach { it.release() }
            satMask.release()
            valMask.release()
            colorMask.release()
            kernel.release()
            cleanedMask.release()
            result.release()
            throw e
        }

        hsv.release()
        channels.forEach { it.release() }
        satMask.release()
        valMask.release()
        colorMask.release()
        kernel.release()
        cleanedMask.release()
        return result
    }
}
