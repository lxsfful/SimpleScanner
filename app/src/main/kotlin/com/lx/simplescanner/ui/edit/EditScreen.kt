package com.lx.simplescanner.ui.edit

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.simplescanner.R
import com.lx.simplescanner.storage.ScanRepository
import com.lx.simplescanner.ui.components.ThumbnailItem
import java.io.File

/**
 * 多页编辑屏（阶段 3）。
 *
 *  - 顶部 TopAppBar：返回 + 增强全部 + 预览 PDF
 *  - 主区域：当前选中页的大图
 *  - 模式切换：原图/增强（仅作用于当前页）
 *  - 底部：缩略图横向列表（点击选中、长按拖动）
 *  - 进度：enhancing / saving 时显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    onDone: () -> Unit,
    onOpenPreview: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()

    // 首次进入若 pages 为空，提示用户
    LaunchedEffect(Unit) {
        if (com.lx.simplescanner.storage.ScanRepository.pages.value.isEmpty()) {
            com.lx.simplescanner.util.Toast.short(context, "请先拍照或导入图片")
        }
    }

    val selected = pages.firstOrNull { it.id == state.selectedPageId } ?: pages.firstOrNull()
    val isBusy = state.phase == EditViewModel.Phase.ENHANCING
            || state.phase == EditViewModel.Phase.SAVING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.edit_title))
                        if (pages.isNotEmpty()) {
                            Text(
                                "${pages.size} 页",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.enhanceAll() },
                        enabled = pages.any { it.enhancedPath == null } && !isBusy
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "增强全部")
                    }
                    IconButton(
                        onClick = {
                            viewModel.savePdf()
                        },
                        enabled = pages.isNotEmpty() && !isBusy
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "保存 PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 进度条
            AnimatedVisibility(visible = isBusy) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (state.phase == EditViewModel.Phase.ENHANCING) {
                        val ratio = if (state.progressTotal > 0)
                            (state.progressCurrent + 1).toFloat() / state.progressTotal
                        else 0f
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (state.progressLabel.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.progressLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 错误提示
            state.errorMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 主预览
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected == null) {
                    Text("没有可编辑的页面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    MainPreview(page = selected)
                }
            }

            // 模式切换 + 删除
            if (selected != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !selected.isEnhanced,
                        onClick = {
                            if (selected.isEnhanced) viewModel.toggleSelectedMode()
                        },
                        label = { Text(stringResource(R.string.edit_original)) },
                        leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                        enabled = !isBusy
                    )
                    FilterChip(
                        selected = selected.isEnhanced,
                        onClick = {
                            if (!selected.isEnhanced) {
                                if (selected.enhancedPath == null) {
                                    viewModel.enhanceSelected()
                                }
                                viewModel.toggleSelectedMode()
                            }
                        },
                        label = { Text(stringResource(R.string.edit_enhance)) },
                        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                        enabled = !isBusy
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.removeSelected() },
                        enabled = !isBusy
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除当前页",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 缩略图列表
            com.lx.simplescanner.ui.components.ReorderableThumbnailRow(
                pages = pages,
                selectedId = selected?.id,
                onSelect = { viewModel.selectPage(it) },
                onReorder = { from, to -> viewModel.reorder(from, to) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(bottom = 8.dp)
            )
        }
    }

    // 保存成功 → 跳 Preview
    LaunchedEffect(state.phase, state.savedPdf) {
        if (state.phase == EditViewModel.Phase.SAVED && state.savedPdf != null) {
            onOpenPreview()
            viewModel.clearSavedFlag()
        }
    }
}

@Composable
private fun MainPreview(page: ScanRepository.Page) {
    val context = LocalContext.current
    val model = remember(page.displayPath) {
        ImageRequest.Builder(context).data(File(page.displayPath)).crossfade(true).build()
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
