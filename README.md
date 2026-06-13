# SimpleScanner · 简易扫描

> 极简的安卓端文档扫描 App —— 拍照/导入 → 透视矫正 → 底色矫正 → 颜色保留 → 多页 PDF → 系统分享。
>
> 类似全能扫描王（Camscanner）的精简版，**无账号、无广告、无云同步、无 OCR、无水印**。

[![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue)](#)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

## ✨ 核心功能

| 功能 | 说明 |
|---|---|
| 📷 **拍照扫描** | CameraX，单会话多张 |
| 🖼️ **相册导入** | 系统 PhotoPicker，多选（最多 30 张） |
| 📐 **透视矫正** | Canny + 轮廓检测 + 4 点透视变换 |
| ⬜ **底色矫正** | 自适应二值化，去阴影变白底黑字 |
| 🎨 **颜色保留** | HSV 饱和度掩码，红头/印章/彩照自动保留 |
| 📄 **多页 PDF** | PDFBox-Android，多页等比缩放至 A4 |
| 📤 **系统分享** | Intent.createChooser，从选择器选微信/QQ/邮件/Drive 任意 |
| 📚 **历史记录** | 列表 + 缩略图 + 时间 + 分享 + 删除 |
| 🔀 **页面重排** | ◀▶ 按钮调换顺序 |
| 🔄 **单页切换** | 每页独立"原图/增强"切换 |

## ❌ 不做什么（保持简单）

- ❌ OCR 文字识别
- ❌ 云同步 / 账号体系
- ❌ 水印 / 加密
- ❌ 文档分类 / 标签 / 搜索
- ❌ 微信 SDK 集成（用系统分享替代，**不需要 AppID/企业资质**）

## 📦 体积

| 构建 | arm64-v8a | universal（所有 ABI） |
|---|---|---|
| Debug | 37 MB | 68 MB |
| **Release（开启 R8）** | **17 MB** | 46 MB |

## 🚀 快速开始

### 准备环境

- **Android Studio** 2026.1.1+ 或命令行 Gradle
- **JDK 17**（或 JDK 21 也可）
- **Android SDK** 34（compileSdk = 34，minSdk = 24）

### 克隆与构建

```bash
git clone https://github.com/<your-name>/SimpleScanner.git
cd SimpleScanner

# 第一次：生成 gradle wrapper jar
gradle wrapper --gradle-version 8.7

# 编译 Debug APK
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

# 编译 Release APK（已配置 debug 签名用于开发）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-arm64-v8a-release.apk

# 安装到设备（先 adb devices 确认连接）
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### 在 Android Studio 中打开

1. **File → Open** → 选择项目根目录
2. 首次打开会自动同步 Gradle 8.7 + 下载依赖
3. **Build → Make Project** 验证编译
4. **Run → Run 'app'** 在模拟器/真机跑

## 🏗️ 技术架构

```
┌─────────────────────────────────────────┐
│ UI (Jetpack Compose)                    │
│  Home → Capture → Edit → Preview       │
│   ↘ ScanRepository ↗                   │
└────────────────┬────────────────────────┘
                 │ StateFlow / suspend
┌────────────────▼────────────────────────┐
│ ViewModels (MVVM)                       │
│  Capture / Edit / Preview ViewModel    │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│ ImagePipeline (OpenCV 4.10)             │
│  DocumentDetector → PerspectiveCorrect  │
│  → Binarizer → ColorPreserver          │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│ Storage (FileManager + ScanRepository)  │
│  scans/  (PDF)                         │
│  cache/  (增强图 + 拍照缓存)            │
└─────────────────────────────────────────┘
```

### 关键依赖

| 库 | 版本 | 用途 |
|---|---|---|
| Kotlin | 1.9.24 | 主语言 |
| Jetpack Compose | BOM 2024.09.02 | UI 框架 |
| CameraX | 1.3.4 | 拍照 |
| OpenCV | 4.10.0 | 图像处理（Maven AAR，免 NDK） |
| PDFBox-Android | 2.0.27.0 | PDF 生成 |
| Coil | 2.7.0 | 图片加载 |
| Navigation Compose | 2.8.1 | 路由 |
| Material 3 | via BOM | 设计系统 |

完整版本见 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。

## 📁 项目结构

```
SimpleScanner/
├── settings.gradle.kts
├── build.gradle.kts                       # 根
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml                 # 依赖版本目录
│   └── wrapper/gradle-wrapper.properties
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
├── .github/
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/com/lx/simplescanner/
        │   │   ├── SimpleScannerApp.kt    # Application + OpenCV init
        │   │   ├── MainActivity.kt        # 单 Activity + Navigation
        │   │   ├── camera/                # CameraX 封装
        │   │   ├── image/                 # 图像处理（核心）
        │   │   ├── pdf/                   # PDFBox 封装
        │   │   ├── share/                 # 系统分享
        │   │   ├── storage/               # FileManager + ScanRepository
        │   │   ├── ui/                    # Compose 屏
        │   │   │   ├── home/
        │   │   │   ├── capture/
        │   │   │   ├── edit/
        │   │   │   ├── preview/
        │   │   │   ├── components/
        │   │   │   └── theme/
        │   │   └── util/                  # Bitmap/Mat/Toast/Permission
        │   └── res/                       # themes/icons/strings
        ├── test/                          # JVM 单元测试
        └── androidTest/                   # 设备/模拟器测试
```

## 🔬 核心算法

详见 `image/` 目录：

### 1. 文档检测 `DocumentDetector`
- 灰度化 → 高斯模糊（5×5）→ Canny(75,200) → 膨胀（5×5）
- `findContours` 取面积前 5
- `approxPolyDP(0.02*peri)` 拟合 4 边形
- 4 点排序（TL/TR/BR/BL）
- **失败兜底**：用图像四顶点，不做变换

### 2. 透视矫正 `PerspectiveCorrector`
- 根据 4 点计算输出尺寸
- `getPerspectiveTransform` + `warpPerspective`

### 3. 底色矫正 `Binarizer`
- `adaptiveThreshold(GAUSSIAN_C, BINARY, 15, 10)`
- 单通道 → 4 通道（与原图同维度）

### 4. 颜色保留 `ColorPreserver`
- HSV 空间分离 S 通道
- 掩码：S ≥ 60 且 V ∈ [50, 240] → 视为彩色
- 形态学：开运算（去噪点）+ 闭运算（补小洞）
- 合成：底色用二值化，彩色区域覆盖原色

## 📲 权限

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />  <!-- API 33+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"  <!-- API 32- -->
                 android:maxSdkVersion="32" />
```

- **CAMERA**：拍照时申请
- **媒体读取**：相册导入时申请
- **无 INTERNET**：本 App 不联网（系统分享不需要）

## 🧪 测试

```bash
# JVM 单元测试（部分）
./gradlew test

# Instrumented 测试（需要设备/模拟器）
./gradlew connectedAndroidTest
```

测试覆盖：
- `DocumentDetectorTest` — 4 点排序纯函数测试
- `BinarizerTest` — 二值化烟雾测试
- `ColorPreserverTest` — 彩色保留烟雾测试
- `ImagePipelineTest` — 完整管线烟雾测试

## 🐛 已知问题

- 缩略图用 ◀▶ 按钮重排（不是拖动手势）—— 简单稳定
- 单页增强处理 2000×3000 像素图需 1-3 秒
- 极端低光环境下 Canny 检测可能失败，自动兜底保留原图

## 🤝 贡献

详见 [CONTRIBUTING.md](CONTRIBUTING.md)。欢迎提 Issue、PR！

## 📄 协议

[Apache License 2.0](LICENSE)

## 🙏 致谢

- [OpenCV](https://opencv.org/) — 图像处理基石
- [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) — Android PDF 库
- [CameraX](https://developer.android.com/training/camerax) — 现代相机 API
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代 UI
