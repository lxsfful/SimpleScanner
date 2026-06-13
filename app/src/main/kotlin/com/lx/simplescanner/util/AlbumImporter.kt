package com.lx.simplescanner.util

import android.content.Context
import android.net.Uri
import com.lx.simplescanner.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 从相册 Uri 复制图片到 cache，返回本地文件路径。
 *
 * 为何需要复制：PhotoPicker 给的是临时 Uri，可能在 App 进程重启后失效；
 * 复制到 cache 后，文件路径是稳定的。
 */
object AlbumImporter {

    /**
     * 复制单个 Uri 到 cache 目录。
     * @return 成功返回本地 File；失败返回 null
     */
    suspend fun importOne(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val out = FileManager.newImportFile()
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            out
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 批量复制。
     * @return 成功复制的 File 列表（按输入顺序，跳过失败项）
     */
    suspend fun importMany(context: Context, uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        uris.mapNotNull { importOneSync(context, it) }
    }

    private fun importOneSync(context: Context, uri: Uri): File? = try {
        val out = FileManager.newImportFile()
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        out.takeIf { it.exists() && it.length() > 0 }
    } catch (e: Exception) {
        null
    }
}
