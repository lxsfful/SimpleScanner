package com.lx.simplescanner.image

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * ColorPreserver instrumented test：必须在 Android 设备/模拟器上跑。
 *
 * 测试策略：合成一张 100x100 图——
 *  - 大部分区域是灰白（会被二值化）
 *  - 中央一个红色方块（应被识别为彩色并保留）
 * 然后调 ColorPreserver.combine 验证：
 *  - 红色方块区域仍是红色
 *  - 周围灰白区域变成白底黑字（实际是黑字+白底，但这里我们只检查彩色区域）
 */
@RunWith(AndroidJUnit4::class)
class ColorPreserverTest {

    init {
        assertTrue("OpenCV must be loaded", OpenCVLoader.initLocal())
    }

    @Test
    fun combine_preservesRedRegionInOutput() {
        // 200x200 灰色底 (128)
        val original = Mat(200, 200, CvType.CV_8UC4, Scalar(128.0, 128.0, 128.0, 255.0))
        // 中央 40x40 红色块（BGR = 0, 0, 255）
        val roi = Mat(original, org.opencv.core.Rect(80, 80, 40, 40))
        roi.setTo(Scalar(0.0, 0.0, 255.0, 255.0))
        roi.release()

        // 模拟二值化结果（全白 = 255）
        val binarized = Mat(200, 200, CvType.CV_8UC4, Scalar(255.0, 255.0, 255.0, 255.0))

        val out = ColorPreserver.combine(original, binarized)
        try {
            // 中央红色区域应该是红色
            val redPixel = ByteArray(4)
            out.get(100, 100, redPixel)
            val blue = redPixel[0].toInt() and 0xFF
            val green = redPixel[1].toInt() and 0xFF
            val red = redPixel[2].toInt() and 0xFF
            assertTrue("Red region: B=$blue G=$green R=$red (expect high R, low G)", red > 200)
            assertTrue("Red region: B=$blue G=$green R=$red (expect low G)", green < 50)
        } finally {
            out.release()
            original.release()
            binarized.release()
        }
    }
}
