package com.lx.simplescanner.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/** 当前平台读取媒体所需的权限（按 API 分支） */
fun mediaReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** 简单同步检查 */
fun Context.hasPermission(perm: String): Boolean =
    ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

/**
 * Composable 形式的「一次性权限申请」包装：
 *  - 进入时立刻尝试申请
 *  - 返回最终结果（true = 已授权）
 *  - 用户拒绝后可通过 [requestAgain] 再次弹窗
 */
@Composable
fun rememberPermissionRequester(permission: String): PermissionRequester {
    var granted by remember { mutableStateOf(false) }
    var requested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        requested = true
    }

    LaunchedEffect(Unit) {
        // 首次进入先检查是否已授权；未授权则自动弹窗
        if (!requested) {
            granted = false
            launcher.launch(permission)
        }
    }

    return PermissionRequester(
        granted = granted,
        requestAgain = { launcher.launch(permission) }
    )
}

class PermissionRequester(
    val granted: Boolean,
    val requestAgain: () -> Unit,
)
