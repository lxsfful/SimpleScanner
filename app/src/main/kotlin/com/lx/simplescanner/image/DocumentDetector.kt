package com.lx.simplescanner.image

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * 文档检测：在 [src]（CV_8UC4 BGRA 或 CV_8UC3 BGR）中找到文档四边形的 4 个角点。
 *
 * 流程：
 *  1. 灰度化 + 高斯模糊
 *  2. Canny 边缘 + 膨胀（让边缘闭合）
 *  3. findContours 找所有外轮廓
 *  4. 按面积降序，对前几个尝试 approxPolyDP 找 4 边形
 *  5. 4 点排序：TL/TR/BR/BL
 *
 * **失败兜底**：未找到合格 4 边形时返回 [Quad.entireImage]，不抛异常。
 */
object DocumentDetector {

    /** 4 个角点（顺序固定：TL, TR, BR, BL） */
    data class Quad(
        val tl: Point,
        val tr: Point,
        val br: Point,
        val bl: Point,
    ) {
        /** 整图作为兜底（不做变换） */
        companion object {
            fun entireImage(width: Int, height: Int): Quad {
                return Quad(
                    tl = Point(0.0, 0.0),
                    tr = Point(width.toDouble(), 0.0),
                    br = Point(width.toDouble(), height.toDouble()),
                    bl = Point(0.0, height.toDouble()),
                )
            }
        }

        /** 四点构成的多边形面积（shoelace 公式） */
        fun area(): Double {
            val pts = listOf(tl, tr, br, bl)
            var sum = 0.0
            for (i in pts.indices) {
                val p = pts[i]
                val q = pts[(i + 1) % pts.size]
                sum += p.x * q.y - q.x * p.y
            }
            return kotlin.math.abs(sum) * 0.5
        }
    }

    /**
     * 检测 [src] 中的文档四边形。
     * @return 4 个角点；若未找到合格四边形返回 [Quad.entireImage]
     *
     * 副作用：会创建并释放若干中间 Mat；**不会**修改 [src]。
     */
    fun detect(src: Mat): Quad {
        val gray = Mat()
        val blurred = Mat()
        val edged = Mat()
        val kernel = Mat()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()

        try {
            // 1. 灰度化
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY)
            // 2. 高斯模糊（去噪点）
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            // 3. Canny
            Imgproc.Canny(blurred, edged, 75.0, 200.0)
            // 4. 膨胀让边缘闭合
            kernel.release()
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0), Point(-1.0, -1.0))
                .copyTo(kernel)
            Imgproc.dilate(edged, edged, kernel)
            // 5. 找外轮廓
            Imgproc.findContours(
                edged,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )
            // 6. 按面积降序
            contours.sortByDescending { Imgproc.contourArea(it) }

            // 7. 取前 5 个尝试拟合 4 边形
            for (c in contours.take(5)) {
                val contour2f = MatOfPoint2f(*c.toArray())
                val peri = Imgproc.arcLength(contour2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true)

                if (approx.rows() == 4) {
                    // isContourConvex 需要 MatOfPoint，做一次转换
                    val approxAsPoint = MatOfPoint()
                    approx.convertTo(approxAsPoint, CvType.CV_32S)
                    if (Imgproc.isContourConvex(approxAsPoint)) {
                        val pts = approx.toArray()
                        approxAsPoint.release()
                        return order(pts)
                    }
                    approxAsPoint.release()
                }
            }
        } finally {
            gray.release()
            blurred.release()
            edged.release()
            kernel.release()
            hierarchy.release()
            contours.forEach { it.release() }
        }

        // 兜底：原图四顶点
        return Quad.entireImage(src.cols(), src.rows())
    }

    /**
     * 把无序的 4 个点排序为 TL / TR / BR / BL。
     *
     * 思路：
     *  - TL：x+y 最小
     *  - BR：x+y 最大
     *  - TR：x-y 最大（最靠右上）
     *  - BL：x-y 最小（最靠左下）
     */
    fun order(pts: Array<Point>): Quad {
        require(pts.size == 4) { "need 4 points, got ${pts.size}" }
        val sum = pts.map { it.x + it.y }
        val diff = pts.map { it.x - it.y }

        val tlIdx = sum.indexOfMin()
        val brIdx = sum.indexOfMax()
        val trIdx = diff.indexOfMax()
        val blIdx = diff.indexOfMin()

        return Quad(
            tl = pts[tlIdx],
            tr = pts[trIdx],
            br = pts[brIdx],
            bl = pts[blIdx],
        )
    }

    private fun List<Double>.indexOfMin(): Int = this.withIndex().minByOrNull { it.value }!!.index
    private fun List<Double>.indexOfMax(): Int = this.withIndex().maxByOrNull { it.value }!!.index
}
