package com.lx.simplescanner.ui.capture

import androidx.lifecycle.ViewModel
import com.lx.simplescanner.storage.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * 拍照 ViewModel（阶段 3）：
 *  - 累积拍下的照片（JPEG 路径列表）
 *  - 「完成」时把所有照片 addFromPath 到 [ScanRepository]
 *  - 后续 PDF 生成由 Edit 屏 / Preview 屏处理
 */
class CaptureViewModel : ViewModel() {

    data class UiState(
        val photos: List<File> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun addPhoto(file: File) {
        _state.update { it.copy(photos = it.photos + file) }
    }

    fun removeLastPhoto() {
        _state.update {
            val list = it.photos
            if (list.isEmpty()) it
            else {
                list.last().delete()
                it.copy(photos = list.dropLast(1))
            }
        }
    }

    /**
     * 把已拍照片提交到 [ScanRepository]，准备进入 Edit 屏。
     * 阶段 3：不再在 Capture 屏生成 PDF，留给 Edit / Preview。
     */
    fun commitToRepository() {
        ScanRepository.reset()
        _state.value.photos.forEach { ScanRepository.addFromPath(it.absolutePath) }
    }
}
