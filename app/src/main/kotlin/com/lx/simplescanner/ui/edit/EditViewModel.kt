package com.lx.simplescanner.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lx.simplescanner.image.ImagePipeline
import com.lx.simplescanner.pdf.PdfBuilder
import com.lx.simplescanner.storage.FileManager
import com.lx.simplescanner.storage.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 多页编辑 ViewModel（阶段 3）：
 *  - 维护"当前选中页" + 全局 UI 状态
 *  - 把多页会话委托给 [ScanRepository]
 *  - 提供 reorder / remove / toggleMode / enhanceAll / savePdf
 *
 * UI（EditScreen）直接订阅 [ScanRepository.pages] 显示列表；
 * 当前选中页通过 [EditViewModel.selectedPageId] 维护。
 */
class EditViewModel : ViewModel() {

    enum class Phase { IDLE, ENHANCING, SAVING, SAVED, ERROR }

    data class UiState(
        val selectedPageId: String? = null,
        val phase: Phase = Phase.IDLE,
        val progressLabel: String = "",
        val progressCurrent: Int = 0,
        val progressTotal: Int = 0,
        val savedPdf: File? = null,
        val errorMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 多页会话直接委托给 ScanRepository。 */
    val pages: StateFlow<List<ScanRepository.Page>> = ScanRepository.pages
    val progress: StateFlow<ScanRepository.ProgressState> = ScanRepository.progress

    private val pipeline = ImagePipeline()

    init {
        // 默认选中第一页
        viewModelScope.launch {
            pages.collect { list ->
                val current = _state.value.selectedPageId
                if (current == null || list.none { it.id == current }) {
                    _state.update { it.copy(selectedPageId = list.firstOrNull()?.id) }
                }
            }
        }
        // 同步 progress 到 phase
        viewModelScope.launch {
            progress.collect { p ->
                _state.update {
                    when {
                        p.active -> it.copy(
                            phase = Phase.ENHANCING,
                            progressLabel = p.message,
                            progressCurrent = p.current,
                            progressTotal = p.total,
                        )
                        it.phase == Phase.ENHANCING -> it.copy(phase = Phase.IDLE)
                        else -> it
                    }
                }
            }
        }
    }

    fun selectPage(id: String) {
        _state.update { it.copy(selectedPageId = id) }
    }

    fun toggleSelectedMode() {
        val id = _state.value.selectedPageId ?: return
        ScanRepository.toggleEnhanced(id)
    }

    fun removeSelected() {
        val id = _state.value.selectedPageId ?: return
        ScanRepository.remove(id)
    }

    fun reorder(from: Int, to: Int) {
        ScanRepository.reorder(from, to)
    }

    /** 触发整批增强。已增强的会跳过。 */
    fun enhanceAll() {
        ScanRepository.enhanceAll(pipeline)
    }

    /** 为当前选中页同步做增强（不阻塞 UI，但 ViewModel 内 await） */
    fun enhanceSelected() {
        val id = _state.value.selectedPageId ?: return
        viewModelScope.launch {
            ScanRepository.enhanceOne(id, pipeline)
        }
    }

    /** 把当前所有页保存为 PDF（按当前 mode）。 */
    fun savePdf() {
        val list = pages.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.SAVING, errorMessage = null) }
            try {
                val bitmaps = withContext(Dispatchers.IO) {
                    list.map { page ->
                        val path = page.displayPath
                        android.graphics.BitmapFactory.decodeFile(path)
                            ?: error("Failed to decode: $path")
                    }
                }
                val outFile = FileManager.newPdfFile()
                withContext(Dispatchers.IO) { PdfBuilder.build(bitmaps, outFile) }
                bitmaps.forEach { if (!it.isRecycled) it.recycle() }
                _state.update {
                    it.copy(phase = Phase.SAVED, savedPdf = outFile)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(phase = Phase.ERROR, errorMessage = "保存失败：${e.message}")
                }
            }
        }
    }

    fun clearSavedFlag() {
        _state.update { it.copy(savedPdf = null, phase = Phase.IDLE) }
    }
}
