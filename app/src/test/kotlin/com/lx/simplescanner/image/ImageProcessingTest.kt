package com.lx.simplescanner.image

import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.opencv.android.OpenCVLoader

/**
 * 图像处理集成测试的基础类。
 *
 * 关键点：OpenCV 的 .so 库只在 Android 设备/模拟器上可用，
 * 普通 JVM 单元测试无法加载 native 库。
 *
 * 在 Android Studio 中跑：
 *   - 模拟器/真机连上后 → Run "androidTest"
 *   - 对应的测试类在 app/src/androidTest/ 下
 *
 * 这里的 JUnit 测试仅做"前提检查"——如果 OpenCV 加载失败则跳过。
 */
abstract class ImageProcessingTest {

    @Before
    fun requireOpenCV() {
        val loaded = OpenCVLoader.initLocal()
        assumeTrue("OpenCV native lib not loaded (need Android device/emulator)", loaded)
    }

    /** 子类可以实现的烟雾测试 */
    abstract fun smoke()
}
