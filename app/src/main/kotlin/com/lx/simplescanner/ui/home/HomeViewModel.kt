package com.lx.simplescanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lx.simplescanner.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 首页 ViewModel：
 *  - 维护历史 PDF 列表
 *  - 处理"删除"事件
 *  - 阶段 2 接入"分享"
 */
class HomeViewModel : ViewModel() {

    data class UiState(
        val history: List<File> = emptyList(),
        val pendingDelete: File? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { FileManager.listPdfs() }
            _state.update { it.copy(history = files) }
        }
    }

    fun requestDelete(file: File) {
        _state.update { it.copy(pendingDelete = file) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val target = _state.value.pendingDelete ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FileManager.delete(target) }
            _state.update { it.copy(pendingDelete = null) }
            refresh()
        }
    }
}
