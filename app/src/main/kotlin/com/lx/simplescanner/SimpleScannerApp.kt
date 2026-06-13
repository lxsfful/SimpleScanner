package com.lx.simplescanner

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * 应用入口。
 *
 * 阶段 1 职责：
 *  - 初始化 OpenCV（同步 init，避免每个 Screen 重复检查）
 *  - 提供全局 Application Context 访问入口（用于 [FileManager]）
 */
class SimpleScannerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        initOpenCV()
    }

    private fun initOpenCV() {
        // initLocal() 内置 .so 从 APK 中加载，无需 OpenCV Manager
        val ok = OpenCVLoader.initLocal()
        Log.i(TAG, "OpenCV initLocal=$ok, version=${OpenCVLoader.OPENCV_VERSION}")
    }

    companion object {
        private const val TAG = "SimpleScannerApp"

        @Volatile
        private var instance: SimpleScannerApp? = null

        /** 全局 Application Context。注意：仅在主进程初始化完成后可访问。 */
        val appContext: Application
            get() = instance
                ?: error("SimpleScannerApp.instance is null — Application not initialized yet")
    }
}
