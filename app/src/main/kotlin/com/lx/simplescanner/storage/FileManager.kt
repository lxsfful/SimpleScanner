package com.lx.simplescanner.storage

import com.lx.simplescanner.SimpleScannerApp
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 文件管理：所有扫描结果统一在 `filesDir/scans/` 下。
 *
 * 阶段 1 暂不写缩略图（缩略图复用 PDF 首页，Preview 屏时再考虑）。
 */
object FileManager {

    private const val SCANS_DIR = "scans"
    private const val CACHE_DIR = "scan_cache"
    private val FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /** `filesDir/scans/`，自动创建。 */
    fun scansDir(): File =
        File(SimpleScannerApp.appContext.filesDir, SCANS_DIR).apply { mkdirs() }

    /** `cacheDir/scan_cache/`，存放处理前的临时图片。 */
    fun cacheDir(): File =
        File(SimpleScannerApp.appContext.cacheDir, CACHE_DIR).apply { mkdirs() }

    /** 拍照时的临时 JPEG 文件路径。 */
    fun newCaptureFile(): File =
        File(cacheDir(), "cap_${System.currentTimeMillis()}.jpg")

    /** 用户从相册导入的临时缓存文件（避免 Uri 失效）。 */
    fun newImportFile(): File =
        File(cacheDir(), "imp_${System.currentTimeMillis()}.jpg")

    /** 生成一个唯一 PDF 文件路径。 */
    fun newPdfFile(): File =
        File(scansDir(), "${FILENAME_FORMAT.format(LocalDateTime.now())}.pdf")

    /** 列出所有扫描结果 PDF（按修改时间倒序）。 */
    fun listPdfs(): List<File> =
        scansDir().listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** 删除文件。返回是否实际删除。 */
    fun delete(file: File): Boolean = file.exists() && file.delete()
}
