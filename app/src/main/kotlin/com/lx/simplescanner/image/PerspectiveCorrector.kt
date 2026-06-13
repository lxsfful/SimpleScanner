package com.lx.simplescanner.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 透视矫正：把 [src] 中的歪斜文档拉正为矩形。
 *
 * 输入是 [DocumentDetector.Quad] 定义的 4 个角点（TL, TR, BR, BL）；
 * 输出是变换后的 [Mat]（CV_8UC4，与 [src] 同通道数）。
 */
object PerspectiveCorrector {

    /**
     * @return 拉正后的 Mat（新的，调用方负责 release）
     */
    fun correct(src: Mat, quad: DocumentDetector.Quad): Mat {
        // 计算输出尺寸
        val (widthTop, _) = distance(quad.tl, quad.tr)
        val (widthBottom, _) = distance(quad.bl, quad.br)
        val (heightLeft, _) = distance(quad.tl, quad.bl)
        val (heightRight, _) = distance(quad.tr, quad.br)
        val outW = max(widthTop, widthBottom).toInt().coerceAtLeast(1)
        val outH = max(heightLeft, heightRight).toInt().coerceAtLeast(1)

        // 源四边形 + 目标矩形
        val srcQuad = MatOfPoint2f(quad.tl, quad.tr, quad.br, quad.bl)
        val dstQuad = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((outW - 1).toDouble(), 0.0),
            Point((outW - 1).toDouble(), (outH - 1).toDouble()),
            Point(0.0, (outH - 1).toDouble())
        )
        val transform = Imgproc.getPerspectiveTransform(srcQuad, dstQuad)

        val dst = Mat()
        Imgproc.warpPerspective(
            src,
            dst,
            transform,
            Size(outW.toDouble(), outH.toDouble())
        )

        transform.release()
        srcQuad.release()
        dstQuad.release()
        return dst
    }

    /** 两点欧氏距离，附带返回值 */
    private fun distance(a: Point, b: Point): Pair<Double, Double> {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy) to abs(dx * dy)
    }
}
