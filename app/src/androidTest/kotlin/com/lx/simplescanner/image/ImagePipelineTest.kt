package com.lx.simplescanner.image

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

/**
 * ImagePipeline 烟雾测试：原图/增强 两种 Mode 都应返回非空 Bitmap。
 */
@RunWith(AndroidJUnit4::class)
class ImagePipelineTest {

    init {
        assertTrue("OpenCV must be loaded", OpenCVLoader.initLocal())
    }

    @Test
    fun process_originalMode_returnsInput() = runBlocking {
        val src = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.WHITE)
        val pipeline = ImagePipeline()
        val out = pipeline.process(src, ImagePipeline.Mode.ORIGINAL)
        assertEquals(100, out.width)
        assertEquals(100, out.height)
        out.recycle()
        src.recycle()
    }

    @Test
    fun process_enhancedMode_returnsNonEmpty() = runBlocking {
        val src = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        // 白底 + 中央红块
        src.eraseColor(Color.WHITE)
        for (x in 80 until 120) for (y in 80 until 120) src.setPixel(x, y, Color.RED)

        val pipeline = ImagePipeline()
        val out = pipeline.process(src, ImagePipeline.Mode.ENHANCED) { _, _ -> }
        assertNotNull(out)
        assertEquals(200, out.width)
        assertEquals(200, out.height)
        out.recycle()
        src.recycle()
    }

    @Test
    fun process_progressCallback_fires() = runBlocking {
        val src = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.WHITE)
        val steps = mutableListOf<String>()
        val pipeline = ImagePipeline()
        pipeline.process(src, ImagePipeline.Mode.ENHANCED) { _, msg -> steps += msg }
        assertTrue("progress steps should be non-empty, got: $steps", steps.isNotEmpty())
        assertEquals("完成", steps.last())
    }
}
