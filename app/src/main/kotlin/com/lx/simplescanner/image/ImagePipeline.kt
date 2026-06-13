package com.lx.simplescanner.image

import android.graphics.Bitmap
import com.lx.simplescanner.util.MatExt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * 图像处理管线：编排"边缘检测 → 透视矫正 → 底色矫正 → 颜色保留"。
 *
 * 入口 [process] 接受 [Bitmap] 和 [Mode]，输出处理后的 [Bitmap]。
 * 全程在 [Dispatchers.Default] 执行，通过 [onProgress] 反馈 (0..1, 步骤说明)。
 *
 * 资源：每次 process 内部创建并释放 Mat；输入 Bitmap 不被修改。
 * 输出 Bitmap 是不透明的 ARGB_8888。
 */
class ImagePipeline {

    enum class Mode { ORIGINAL, ENHANCED }

    suspend fun process(
        original: Bitmap,
        mode: Mode,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Bitmap = withContext(Dispatchers.Default) {
        onProgress(0.02f, "准备")
        val input = original.copy(Bitmap.Config.ARGB_8888, false)
        val src = MatExt.bitmapToMat(input)

        when (mode) {
            Mode.ORIGINAL -> {
                onProgress(1.0f, "完成")
                src.release()
                return@withContext input
            }
            Mode.ENHANCED -> {
                val outMat = enhance(src, onProgress)
                val outBitmap = MatExt.matToBitmap(outMat)
                src.release()
                outMat.release()
                onProgress(1.0f, "完成")
                return@withContext outBitmap
            }
        }
    }

    /**
     * 完整增强管线：检测 → 矫正 → 二值化 → 颜色保留。
     * 内部释放所有中间 Mat；返回的 Mat 由调用方负责 release。
     */
    private fun enhance(
        src: Mat,
        onProgress: (Float, String) -> Unit
    ): Mat {
        // 1. 检测 4 角点
        onProgress(0.10f, "检测边缘")
        val quad = DocumentDetector.detect(src)

        // 2. 透视矫正
        onProgress(0.30f, "矫正透视")
        val warped = PerspectiveCorrector.correct(src, quad)

        // 3. 二值化
        onProgress(0.55f, "去底色")
        val bw = Binarizer.binarize(warped)

        // 4. 颜色保留 + 合成
        onProgress(0.80f, "保留颜色")
        val combined = ColorPreserver.combine(warped, bw)

        warped.release()
        bw.release()
        return combined
    }
}
