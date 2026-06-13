package com.lx.simplescanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.simplescanner.storage.ScanRepository
import java.io.File

/**
 * 多页缩略图横向列表 + 左右移动按钮。
 *
 * 简化实现：不依赖第三方拖动库；用 ◀ ▶ 按钮调换顺序。
 */
@Composable
fun ReorderableThumbnailRow(
    pages: List<ScanRepository.Page>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    LazyRow(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = pages, key = { it.id }) { page ->
            val index = pages.indexOf(page)
            Box {
                ThumbnailItem(
                    page = page,
                    index = index,
                    isSelected = page.id == selectedId,
                    onClick = { onSelect(page.id) },
                    modifier = Modifier.size(80.dp)
                )
                // 左右移动按钮（叠加在右上）
                if (pages.size > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .size(20.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (index > 0) onReorder(index, index - 1)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ChevronLeft,
                                contentDescription = "左移",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (index < pages.size - 1) onReorder(index, index + 1)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = "右移",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个缩略图：图 + 页码 + 增强态角标。
 */
@Composable
fun ThumbnailItem(
    page: ScanRepository.Page,
    index: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val model = remember(page.displayPath) {
        ImageRequest.Builder(context).data(File(page.displayPath)).crossfade(true).build()
    }
    val outerBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val shadowElevation = if (isDragging) 8.dp else 0.dp

    Box(
        modifier = modifier
            .shadow(shadowElevation, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(outerBorderColor)
            .padding(if (isSelected) 2.dp else 0.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "${index + 1}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (page.isEnhanced) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = "增强",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

// 私有 shadow helper 删除（直接用 import 即可）
