package com.lx.simplescanner.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.lx.simplescanner.storage.FileManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 封装：负责绑定 Preview + ImageCapture 到 [LifecycleOwner]。
 *
 * 用法：
 * ```
 * val controller = remember { CameraController(context) }
 * AndroidView({ controller.previewView }, modifier)
 * LaunchedEffect(Unit) { controller.bind(lifecycleOwner) }
 * val file = controller.takePhoto()
 * ```
 */
class CameraController(private val context: Context) {

    val previewView = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private var imageCapture: ImageCapture? = null
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    /** 绑定到 lifecycle。重复调用会先解绑。 */
    suspend fun bind(lifecycleOwner: LifecycleOwner) {
        val provider = awaitProvider()
        provider.unbindAll()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(90)
            .build()

        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture
        )
        imageCapture = capture
    }

    /**
     * 拍照并写入 cache 中的 JPEG 文件。
     * 成功返回文件；失败抛异常。
     */
    suspend fun takePhoto(): File {
        val capture = imageCapture ?: error("CameraController not bound")
        val file = FileManager.newCaptureFile()
        val output = ImageCapture.OutputFileOptions.Builder(file).build()

        return suspendCancellableCoroutine { cont ->
            capture.takePicture(
                output,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        cont.resume(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private suspend fun awaitProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }, mainExecutor)
        }
}
