package com.lx.simplescanner.ui.capture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.simplescanner.R
import com.lx.simplescanner.camera.CameraController
import com.lx.simplescanner.util.hasPermission
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onDone: () -> Unit,
    onOpenEdit: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // 第一次进入若未授权则自动弹
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.capture_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.capture_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!hasCameraPermission) {
                PermissionDenied(
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                CaptureContent(
                    state = state,
                    onShutter = { controller ->
                        scope.launch {
                            try {
                                val file = controller.takePhoto()
                                viewModel.addPhoto(file)
                            } catch (e: Exception) {
                                com.lx.simplescanner.util.Toast.short(
                                    context, "拍照失败：${e.message ?: "未知错误"}"
                                )
                            }
                        }
                    },
                    onFinish = {
                        if (state.photos.isNotEmpty()) {
                            viewModel.commitToRepository()
                            onOpenEdit()
                        } else {
                            onDone()
                        }
                    },
                    onUndo = { viewModel.removeLastPhoto() },
                    lifecycleOwner = lifecycleOwner
                )
            }
        }
    }
}

@Composable
private fun PermissionDenied(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.capture_permission_denied),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onGrant) {
            Text(stringResource(R.string.capture_grant_permission))
        }
    }
}

@Composable
private fun CaptureContent(
    state: CaptureViewModel.UiState,
    onShutter: (CameraController) -> Unit,
    onFinish: () -> Unit,
    onUndo: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    val context = LocalContext.current
    val controller = remember { CameraController(context) }

    LaunchedEffect(Unit) {
        runCatching { controller.bind(lifecycleOwner) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { controller.previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 底部操作栏
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(16.dp)
        ) {
            // 已拍缩略图
            if (state.photos.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = state.photos, key = { it.absolutePath }) { file ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.capture_count_format, state.photos.size),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 撤销
                IconButton(
                    onClick = onUndo,
                    enabled = state.photos.isNotEmpty()
                ) {
                    Icon(
                        Icons.Outlined.Undo,
                        contentDescription = "撤销",
                        tint = Color.White
                    )
                }
                // 快门
                ShutterButton(
                    enabled = true,
                    onClick = { onShutter(controller) }
                )
                // 完成
                Button(
                    onClick = onFinish,
                    enabled = state.photos.isNotEmpty()
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.capture_done))
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = stringResource(R.string.capture_shutter),
                    tint = Color.White
                )
            }
        }
    }
}
