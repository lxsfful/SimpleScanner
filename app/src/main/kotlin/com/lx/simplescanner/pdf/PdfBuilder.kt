package com.lx.simplescanner.pdf

import android.graphics.Bitmap
import com.lx.simplescanner.SimpleScannerApp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 多页 PDF 生成器（PDFBox-Android）。
 *
 * 阶段 1 实现单页生成；多页通过 List<Bitmap> 串接，逻辑已就绪。
 */
object PdfBuilder {

    private const val A4_PT_WIDTH = 595f
    private const val A4_PT_HEIGHT = 842f
    private const val MARGIN_PT = 24f
    private const val JPEG_QUALITY = 85

    init {
        // 初始化 PDFBox 资源（assets 中的字体/资源），线程安全
        PDFBoxResourceLoader.init(SimpleScannerApp.appContext)
    }

    /**
     * 将 [bitmaps] 合并为多页 PDF，写入 [outFile]。
     * 每页等比缩放至 A4，居中放置。
     */
    suspend fun build(bitmaps: List<Bitmap>, outFile: File): File = withContext(Dispatchers.IO) {
        require(bitmaps.isNotEmpty()) { "bitmaps is empty" }
        outFile.parentFile?.mkdirs()

        PDDocument().use { doc ->
            bitmaps.forEach { bm -> addPage(doc, bm) }
            doc.save(outFile)
        }
        outFile
    }

    private fun addPage(doc: PDDocument, bitmap: Bitmap) {
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)

        val img = createImage(doc, bitmap)
        val (drawW, drawH) = scaleToFit(
            img.width.toFloat(),
            img.height.toFloat(),
            A4_PT_WIDTH - MARGIN_PT * 2,
            A4_PT_HEIGHT - MARGIN_PT * 2
        )
        val x = (A4_PT_WIDTH - drawW) / 2f
        val y = (A4_PT_HEIGHT - drawH) / 2f

        PDPageContentStream(doc, page).use { cs ->
            cs.drawImage(img, x, y, drawW, drawH)
        }
    }

    private fun createImage(doc: PDDocument, bitmap: Bitmap): PDImageXObject {
        // LosslessFactory 接受 Bitmap 直接生成 PNG 嵌入
        return try {
            LosslessFactory.createFromImage(doc, bitmap)
        } catch (e: Exception) {
            // 兜底：JPEG 编码
            val jpegBytes = ByteArrayOutputStream().use { baos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
                baos.toByteArray()
            }
            PDImageXObject.createFromByteArray(doc, jpegBytes, "page")
        }
    }

    /** 等比缩放到 (maxW, maxH) 内。 */
    private fun scaleToFit(srcW: Float, srcH: Float, maxW: Float, maxH: Float): Pair<Float, Float> {
        if (srcW <= 0 || srcH <= 0) return maxW to maxH
        val ratio = minOf(maxW / srcW, maxH / srcH)
        return srcW * ratio to srcH * ratio
    }
}
