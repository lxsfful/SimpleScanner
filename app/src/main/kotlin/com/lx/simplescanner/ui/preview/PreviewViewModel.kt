package com.lx.simplescanner.ui.preview

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Preview 屏 ViewModel：
 *  - 显示当前多页会话（来自 ScanRepository）
 *  - 再次保存/分享 PDF（保存是幂等的）
 *  - 提供「全部原图」「全部增强」批量切换 mode
 */
class PreviewViewModel : ViewModel() {

    enum class Phase { IDLE, SAVING, ERROR }

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val savedPdf: File? = null,
        val errorMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val pages: StateFlow<List<ScanRepository.Page>> = ScanRepository.pages

    fun setAllMode(isEnhanced: Boolean) {
        val current = pages.value
        current.forEachIndexed { idx, page ->
            if (page.isEnhanced != isEnhanced) {
                ScanRepository.toggleEnhanced(page.id)
            }
        }
    }

    fun save() {
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
                    it.copy(phase = Phase.IDLE, savedPdf = outFile)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(phase = Phase.ERROR, errorMessage = "保存失败：${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(phase = Phase.IDLE, errorMessage = null) }
    }

    /** 返回 Home 时清空会话 */
    fun finish() {
        ScanRepository.reset()
    }
}
