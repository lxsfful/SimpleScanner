package com.lx.simplescanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lx.simplescanner.ui.capture.CaptureScreen
import com.lx.simplescanner.ui.edit.EditScreen
import com.lx.simplescanner.ui.home.HomeScreen
import com.lx.simplescanner.ui.preview.PreviewScreen
import com.lx.simplescanner.ui.theme.SimpleScannerTheme

/**
 * 单 Activity 容器，承载 Compose Navigation 路由。
 *
 * 阶段 3 路由：
 *  - `home`    — 首页 + 历史列表
 *  - `capture` — 拍照（多张，完成后跳到 edit）
 *  - `edit`    — 多页编辑（增强 + 排序 + 删除 + mode 切换）
 *  - `preview` — 多页 PDF 预览 + 保存 + 分享
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}

@Composable
private fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.Home.path) {
        composable(Route.Home.path) {
            HomeScreen(
                onStartCapture = { navController.navigate(Route.Capture.path) },
                onStartEditFromPath = { path ->
                    navController.navigate(Route.Edit.path)
                },
                onStartEditFromUri = { uri ->
                    navController.navigate(Route.Edit.path)
                }
            )
        }
        composable(Route.Capture.path) {
            CaptureScreen(
                onDone = { navController.popBackStack() },
                onOpenEdit = {
                    navController.navigate(Route.Edit.path) {
                        popUpTo(Route.Capture.path) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Route.Edit.path) {
            EditScreen(
                onDone = { navController.popBackStack(Route.Home.path, inclusive = false) },
                onOpenPreview = { navController.navigate(Route.Preview.path) }
            )
        }
        composable(Route.Preview.path) {
            PreviewScreen(
                onDone = {
                    navController.popBackStack(Route.Home.path, inclusive = false)
                }
            )
        }
    }
}

/** 路由常量集中管理，避免字符串散落各处 */
sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Capture : Route("capture")
    data object Edit : Route("edit")
    data object Preview : Route("preview")
}
