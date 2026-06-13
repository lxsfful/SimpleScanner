package com.lx.simplescanner.ui.preview

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.simplescanner.R
import com.lx.simplescanner.share.SystemShareHelper
import com.lx.simplescanner.storage.ScanRepository
import java.io.File

/**
 * Preview 屏：所有页缩略图 + 保存 / 分享 / 一键切 mode。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onDone: () -> Unit,
    viewModel: PreviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()

    val allEnhanced = pages.isNotEmpty() && pages.all { it.isEnhanced }
    val allOriginal = pages.isNotEmpty() && pages.none { it.isEnhanced }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.preview_title))
                        Text(
                            "共 ${pages.size} 页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.finish()
                        onDone()
                    }) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_cancel)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = pages.isNotEmpty() && state.phase != PreviewViewModel.Phase.SAVING
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = stringResource(R.string.preview_save))
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
            if (state.phase == PreviewViewModel.Phase.SAVING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 状态显示
            state.savedPdf?.let { file ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "已保存到 ${file.parentFile?.name}/${file.name}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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

            // 一键切 mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = allOriginal,
                    onClick = { viewModel.setAllMode(false) },
                    label = { Text("全部原图") },
                    enabled = !allOriginal && pages.isNotEmpty()
                )
                FilterChip(
                    selected = allEnhanced,
                    onClick = { viewModel.setAllMode(true) },
                    label = { Text("全部增强") },
                    enabled = !allEnhanced && pages.all { it.enhancedPath != null }
                )
            }

            // 缩略图列表
            Text(
                "页面预览",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pages, key = { it.id }) { page ->
                    PreviewThumbnail(page = page)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 详细页
            LazyRow(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(pages, key = { it.id }) { page ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .fillMaxWidth(0.85f)
                            .aspectRatio(0.71f) // A4 比例
                    ) {
                        AsyncImage(
                            model = remember(page.displayPath) {
                                ImageRequest.Builder(context)
                                    .data(File(page.displayPath))
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 底部按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.save() },
                    enabled = pages.isNotEmpty() && state.phase != PreviewViewModel.Phase.SAVING,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.preview_save))
                }
                Button(
                    onClick = {
                        state.savedPdf?.let { SystemShareHelper.sharePdf(context, it) }
                    },
                    enabled = state.savedPdf != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.preview_share))
                }
            }
        }
    }
}

@Composable
private fun PreviewThumbnail(page: ScanRepository.Page) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = remember(page.displayPath) {
                ImageRequest.Builder(context)
                    .data(File(page.displayPath))
                    .crossfade(true)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
