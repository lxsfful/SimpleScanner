package com.lx.simplescanner.storage

import android.graphics.Bitmap
import com.lx.simplescanner.image.ImagePipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 多页扫描会话仓库。
 *
 * 持有 List<Page>（每页含原图路径、增强后路径、当前 mode）；
 * 暴露 [pages] 给 UI 订阅；提供 reorder / remove / toggleEnhanced / enhanceAll 等操作。
 *
 * Bitmap 懒加载：每页只有 path，UI 渲染时按需 decode（Edit 屏用 Coil）。
 * 增强完成后写入 enhancedPath（PNG/JPEG），下次启动可复用。
 */
object ScanRepository {

    /** 单页：原图 + 增强后（可空）+ 用户当前选择。 */
    data class Page(
        val id: String = UUID.randomUUID().toString(),
        val originalPath: String,
        val enhancedPath: String? = null,
        val isEnhanced: Boolean = false,
    ) {
        val displayPath: String get() = if (isEnhanced && enhancedPath != null) enhancedPath else originalPath
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    /** 当前正在处理的进度（-1 表示无活动） */
    private val _progress = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = _progress.asStateFlow()

    data class ProgressState(
        val active: Boolean = false,
        val current: Int = 0,
        val total: Int = 0,
        val message: String = "",
    )

    private var enhanceJob: Job? = null

    fun reset() = _pages.update { emptyList() }

    fun addFromPath(path: String) {
        val page = Page(originalPath = path)
        _pages.update { it + page }
    }

    fun remove(id: String) = _pages.update { list -> list.filterNot { it.id == id } }

    fun toggleEnhanced(id: String) = _pages.update { list ->
        list.map { if (it.id == id) it.copy(isEnhanced = !it.isEnhanced) else it }
    }

    fun reorder(fromIndex: Int, toIndex: Int) = _pages.update { list ->
        if (fromIndex !in list.indices || toIndex !in list.indices) return@update list
        list.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }
    }

    /**
     * 异步增强所有缺 enhanced 的页。
     * 已增强过的页会被跳过。
     */
    fun enhanceAll(pipeline: ImagePipeline = ImagePipeline()) {
        if (enhanceJob?.isActive == true) return
        enhanceJob = scope.launch {
            mutex.withLock {
                val targets = _pages.value.filter { it.enhancedPath == null }
                if (targets.isEmpty()) return@withLock

                _progress.value = ProgressState(
                    active = true,
                    current = 0,
                    total = targets.size,
                    message = "开始"
                )

                targets.forEachIndexed { idx, page ->
                    _progress.value = _progress.value.copy(
                        current = idx,
                        message = "处理第 ${idx + 1}/${targets.size} 张"
                    )
                    try {
                        val src = withContext(Dispatchers.IO) {
                            android.graphics.BitmapFactory.decodeFile(page.originalPath)
                        } ?: return@forEachIndexed

                        val out = pipeline.process(src, ImagePipeline.Mode.ENHANCED) { p, label ->
                            _progress.value = _progress.value.copy(
                                message = "处理第 ${idx + 1}/${targets.size} 张 · $label"
                            )
                        }
                        val outFile = File(FileManager.cacheDir(), "enh_${page.id}.png")
                        withContext(Dispatchers.IO) {
                            outFile.outputStream().use { os ->
                                out.compress(Bitmap.CompressFormat.PNG, 100, os)
                            }
                        }
                        src.recycle()
                        if (out !== src) out.recycle()

                        // 写入 path 到 page
                        _pages.update { list ->
                            list.map {
                                if (it.id == page.id) it.copy(enhancedPath = outFile.absolutePath)
                                else it
                            }
                        }
                    } catch (e: Exception) {
                        _progress.value = _progress.value.copy(
                            message = "第 ${idx + 1} 张处理失败：${e.message}"
                        )
                    }
                }

                _progress.value = ProgressState(active = false, message = "完成")
            }
        }
    }

    /**
     * 立即为指定页做增强（同步当前 ViewModel 调用）。
     * 完成后回写到 [pages] 列表，UI 自动刷新。
     */
    suspend fun enhanceOne(
        pageId: String,
        pipeline: ImagePipeline = ImagePipeline(),
    ): Boolean {
        val page = _pages.value.firstOrNull { it.id == pageId } ?: return false
        if (page.enhancedPath != null) return true

        val src = withContext(Dispatchers.IO) {
            android.graphics.BitmapFactory.decodeFile(page.originalPath)
        } ?: return false

        return try {
            val out = pipeline.process(src, ImagePipeline.Mode.ENHANCED)
            val outFile = File(FileManager.cacheDir(), "enh_${page.id}.png")
            withContext(Dispatchers.IO) {
                outFile.outputStream().use { os ->
                    out.compress(Bitmap.CompressFormat.PNG, 100, os)
                }
            }
            src.recycle()
            if (out !== src) out.recycle()
            _pages.update { list ->
                list.map { if (it.id == pageId) it.copy(enhancedPath = outFile.absolutePath) else it }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
