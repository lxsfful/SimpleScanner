# 更新日志

所有显著变更都记录在此文件。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [1.0.0] - 2026-06-13

### ✨ 新增
- 拍照扫描（CameraX，单会话多张）
- 相册导入（系统 PhotoPicker，最多 30 张）
- 文档检测（Canny + 轮廓 + 4 点定位）
- 透视矫正
- 自适应二值化底色矫正
- HSV 颜色保留（红头/印章/彩照）
- 多页 PDF 生成（A4 等比缩放）
- 系统分享 Intent
- 历史记录（缩略图 + 分享 + 删除）
- 多页编辑（缩略图 ◀▶ 调换顺序）
- 单页"原图/增强"切换
- "增强全部"批量处理

### 🛠️ 技术
- Kotlin 1.9.24 + Jetpack Compose
- AGP 8.5.2 / Gradle 8.7
- Min SDK 24 / Target SDK 34
- OpenCV 4.10（Maven 预编译 AAR）
- PDFBox-Android 2.0.27.0
- 按 ABI 拆分 Release APK
- R8/ProGuard 规则

### 📦 体积
- Debug universal: 68 MB
- Release arm64-v8a: 17 MB
- Release universal: 46 MB

[1.0.0]: https://github.com/<owner>/SimpleScanner/releases/tag/v1.0.0
