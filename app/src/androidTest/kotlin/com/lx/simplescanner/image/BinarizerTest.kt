package com.lx.simplescanner.image

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * Binarizer instrumented test：必须在 Android 设备/模拟器上跑。
 *
 * 测试策略：
 *  1. 构造一张 100x100 的 Mat，文字区域 = 黑色 (0)，背景 = 浅灰 (200)
 *  2. 调 Binarizer.binarize
 *  3. 断言：背景区域被转为接近 255（白），文字仍为 0
 */
@RunWith(AndroidJUnit4::class)
class BinarizerTest {

    init {
        // 触发 OpenCV 加载
        assertTrue("OpenCV must be loaded", OpenCVLoader.initLocal())
    }

    @Test
    fun binarize_producesWhiteBackgroundAndBlackForeground() {
        // 100x100 全白（255）作为底
        val src = Mat(100, 100, CvType.CV_8UC4, Scalar(255.0, 255.0, 255.0, 255.0))
        // 在中央画 40x40 黑块（模拟文字）
        val roi = Mat(src, org.opencv.core.Rect(30, 30, 40, 40))
        roi.setTo(Scalar(0.0, 0.0, 0.0, 255.0))
        roi.release()

        val out = Binarizer.binarize(src, blockSize = 15, c = 10.0)
        try {
            assertEquals(100, out.rows())
            assertEquals(100, out.cols())
            assertEquals(4, out.channels())

            // 检查背景（左上角 (5, 5)）接近白色
            val bgPixel = ByteArray(4)
            out.get(5, 5, bgPixel)
            val bgGray = bgPixel[0].toInt() and 0xFF

            // 检查文字区域中心 (50, 50) 为黑色
            val fgPixel = ByteArray(4)
            out.get(50, 50, fgPixel)
            val fgGray = fgPixel[0].toInt() and 0xFF

            assertTrue("Background should be near 255, was $bgGray", bgGray > 200)
            assertTrue("Foreground should be near 0, was $fgGray", fgGray < 50)
        } finally {
            out.release()
            src.release()
        }
    }
}
