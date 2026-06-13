package com.lx.simplescanner.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.opencv.core.Point

/**
 * [DocumentDetector.order] 的纯函数测试，**不依赖** OpenCV native 库。
 * 完整 detect() 流程需要在 Android 设备/模拟器上跑（见 androidTest）。
 */
class DocumentDetectorTest {

    @Test
    fun `order sorts standard 4 points to TL TR BR BL`() {
        // 顺时针给：TL(10,10) TR(110,10) BR(110,210) BL(10,210)
        val pts = arrayOf(
            Point(110.0, 210.0), // BR
            Point(10.0, 210.0),  // BL
            Point(10.0, 10.0),   // TL
            Point(110.0, 10.0),  // TR
        )
        val quad = DocumentDetector.order(pts)
        assertEquals("TL", Point(10.0, 10.0), quad.tl)
        assertEquals("TR", Point(110.0, 10.0), quad.tr)
        assertEquals("BR", Point(110.0, 210.0), quad.br)
        assertEquals("BL", Point(10.0, 210.0), quad.bl)
    }

    @Test
    fun `order is independent of input sequence`() {
        val canonical = arrayOf(
            Point(10.0, 10.0),  // TL
            Point(110.0, 10.0), // TR
            Point(110.0, 210.0), // BR
            Point(10.0, 210.0)  // BL
        )
        val expected = DocumentDetector.order(canonical)
        // 打乱顺序
        val shuffled = arrayOf(canonical[2], canonical[0], canonical[3], canonical[1])
        val actual = DocumentDetector.order(shuffled)
        assertEquals(expected.tl, actual.tl)
        assertEquals(expected.tr, actual.tr)
        assertEquals(expected.br, actual.br)
        assertEquals(expected.bl, actual.bl)
    }

    @Test
    fun `area computes correct value for unit square`() {
        val quad = DocumentDetector.Quad(
            tl = Point(0.0, 0.0),
            tr = Point(100.0, 0.0),
            br = Point(100.0, 100.0),
            bl = Point(0.0, 100.0)
        )
        assertEquals(10000.0, quad.area(), 0.001)
    }

    @Test
    fun `entireImage returns image bounds`() {
        val quad = DocumentDetector.Quad.entireImage(640, 480)
        assertEquals(0.0, quad.tl.x, 0.001)
        assertEquals(0.0, quad.tl.y, 0.001)
        assertEquals(640.0, quad.tr.x, 0.001)
        assertEquals(640.0, quad.br.x, 0.001)
        assertEquals(480.0, quad.br.y, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `order throws on wrong point count`() {
        DocumentDetector.order(arrayOf(Point(0.0, 0.0), Point(1.0, 1.0)))
    }
}
