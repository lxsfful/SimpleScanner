# AGENTS.md

> AI 协作者（Claude Code / Codex / Cursor / Aider / 其他 LLM agent）阅读指南。
>
> 本文件让你**在 5 分钟内**理解项目结构、约定、关键文件，**不需要**通读全部源码。
>
> **本文件性质**：长期稳定。少改。如需新增经验，**追加**在合适章节末尾，不要重写。

## 📚 文档三件套（先读这个！）

项目根目录有 4 个文档，**性质不同**：

| 文件 | 性质 | 何时读 | 何时改 |
|---|---|---|---|
| **AGENTS.md**（本文件） | 稳定规则、长期约束 | 接手时第一份读 | **少改**；新增经验追加在合适章节 |
| `agent-handoff.md` | 短期交接、本次任务状态 | 接手时第二份读 | 每次任务结束**重写** |
| `WORKLOG.md` | 决策日志、为什么 | 接手时第三份读 | **追加**（时间倒序），保留失败路径 |
| `ROADMAP.md` | 下阶段候选功能 | 规划下阶段时读 | 完成任务后更新状态 |
| `README.md` | 用户视角 | 给新人 / 用户看 | 功能变更时同步 |
| `CHANGELOG.md` | 版本变更 | 发版时更新 | 每个 release 追加 |

**接手的标准流程**：
```
1. 读 README.md            → 知道项目是什么
2. 读 AGENTS.md            → 知道结构和约束（你正在读）
3. 读 agent-handoff.md     → 知道上次干了啥、还差啥
4. 读 WORKLOG.md（最近 2-3 条）→ 知道最近的决策原因
5. 读 ROADMAP.md           → 知道下阶段要做什么
6. 开始干活
```

## 项目一句话

**SimpleScanner** —— 类似全能扫描王的精简版安卓文档扫描 App。

- **栈**：Kotlin + Jetpack Compose + OpenCV 4.10 (图像处理) + PDFBox-Android (PDF)
- **平台**：Android 7.0+ (API 24)
- **大小**：Release arm64-v8a 17 MB / universal 46 MB
- **协议**：Apache 2.0
- **仓库**：https://github.com/lxsfful/SimpleScanner
- **当前状态**：v1.0.0 已发布（2026-06-13），待用户实机验证

## 核心功能（用户视角）

1. 拍照 / 相册导入（多张）
2. 文档检测（边缘 → 4 点定位）
3. 透视矫正
4. 底色矫正（自适应二值化）
5. 颜色保留（红头/印章/彩照保留原色，文本变黑）
6. 多页 PDF 生成（A4）
7. 系统分享 Intent

## 仓库结构

```
D:\Projects\SimpleScanner\  (本机路径)  →  https://github.com/lxsfful/SimpleScanner

├── settings.gradle.kts                 # 模块包含 + 仓库
├── build.gradle.kts                    # 根（AGP/Kotlin 插件版本）
├── gradle/libs.versions.toml           # ⭐ 依赖版本目录（改版本号来这里）
├── gradle.properties                   # JVM 内存 + AndroidX
├── gradle/wrapper/                     # Gradle 8.7 wrapper
├── gradlew / gradlew.bat               # ⭐ 构建命令
│
├── README.md                           # 给人看的
├── CHANGELOG.md                        # 版本历史
├── CONTRIBUTING.md                     # 贡献指南（人）
├── AGENTS.md                           # ⭐ 本文件，给 AI 看
├── LICENSE                             # Apache 2.0
│
├── .github/ISSUE_TEMPLATE/             # Bug + Feature 模板
└── app/
    ├── build.gradle.kts                # 模块构建（依赖在这里）
    ├── proguard-rules.pro              # ⭐ R8/ProGuard keep 规则
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml      # ⭐ 权限 + FileProvider
        │   ├── kotlin/com/lx/simplescanner/
        │   │   ├── SimpleScannerApp.kt  # Application，OpenCV 初始化
        │   │   ├── MainActivity.kt      # 单 Activity + Nav 路由
        │   │   │
        │   │   ├── ui/                  # ⭐ Compose 屏
        │   │   │   ├── home/           # 首页 + 历史
        │   │   │   ├── capture/        # 拍照
        │   │   │   ├── edit/           # 多页编辑（增强 + 重排）
        │   │   │   ├── preview/        # PDF 预览 + 保存 + 分享
        │   │   │   ├── components/     # 共享 UI 组件
        │   │   │   └── theme/          # Material 3
        │   │   │
        │   │   ├── image/              # ⭐⭐⭐ 核心算法
        │   │   │   ├── DocumentDetector.kt    # 边缘 + 4 点
        │   │   │   ├── PerspectiveCorrector.kt
        │   │   │   ├── Binarizer.kt           # 底色矫正
        │   │   │   ├── ColorPreserver.kt      # HSV 颜色保留
        │   │   │   └── ImagePipeline.kt       # 编排（detect→warp→bw→combine）
        │   │   │
        │   │   ├── pdf/PdfBuilder.kt          # PDFBox 封装
        │   │   ├── share/SystemShareHelper.kt # 系统分享 Intent
        │   │   ├── storage/                  # FileManager + ScanRepository
        │   │   ├── camera/CameraController.kt # CameraX 封装
        │   │   └── util/                      # Bitmap/Mat/Toast/Permission
        │   │
        │   └── res/                    # 主题/颜色/字符串/图标
        │
        ├── test/                       # JVM 单元测试（不需设备）
        └── androidTest/                # 设备/模拟器测试（需 OpenCV native）
```

## 必读关键文件（按重要性排序）

如果你只能看 5 个文件：

1. **`app/build.gradle.kts`** — 全部依赖 + SDK 配置 + ABI split
2. **`app/src/main/kotlin/com/lx/simplescanner/image/ImagePipeline.kt`** — 核心处理流程（串起 4 个算法）
3. **`app/src/main/kotlin/com/lx/simplescanner/MainActivity.kt`** — 路由（home/capture/edit/preview）
4. **`app/src/main/AndroidManifest.xml`** — 权限 + FileProvider
5. **`gradle/libs.versions.toml`** — 依赖版本（要升级版本号来这里）

## 项目当前状态

| 维度 | 状态 |
|---|---|
| **版本** | v1.0.0 |
| **发布日期** | 2026-06-13 |
| **代码量** | 35 个 Kotlin 文件，~3650 行 |
| **测试** | JVM 单测 1 个 + androidTest 3 个（未在 CI 跑） |
| **CI/CD** | 无 |
| **实机测试** | 仅在 arm64 Android 10 设备做基础安装验证，未充分测图像处理 |
| **已知遗留** | 详见 `agent-handoff.md`「未完成 / 已知问题」 |
| **下一阶段** | 详见 `ROADMAP.md`（P0: 实机调优 + OCR） |

## 关键技术决策（不要轻易改动）

| 决策 | 原因 | 改动的连锁影响 |
|---|---|---|
| OpenCV 用 **Maven 预编译 AAR**（非源码编译） | 免 NDK，APK 体积可控 | 若改源码路径需 NDK 环境 |
| PDF 用 **PDFBox-Android**（非 iText） | Apache 2.0、纯 Java、Android 友好 | ProGuard keep 规则要更新 |
| **不集成微信 SDK**，用 `Intent.createChooser` | 避免注册流程 | 用户需自己从选择器选 App |
| **不依赖服务端**，所有处理本地完成 | 离线可用、隐私 | — |
| **不接入 OCR** | 简化、减小体积 | — |
| Kotlin 1.9.24 + Compose Compiler 1.5.14 | AGP 8.5.2 兼容 | 升 Kotlin 2.0 需同时升 AGP 8.7+ |
| AGP 8.5.2 + Gradle 8.7 + JDK 17 | 已验证可编译 | 升 AGP 要同步升 Gradle |

## 核心算法（理解 1 张图就够了）

```
输入 Bitmap (ARGB_8888)
   ↓
[1] DocumentDetector.detect()       → Quad(4 个 Point)
   Canny + findContours + approxPolyDP；失败兜底返回原图四顶点
   ↓
[2] PerspectiveCorrector.correct()  → warped Mat
   getPerspectiveTransform + warpPerspective 拉正
   ↓
[3] Binarizer.binarize()            → bw Mat
   adaptiveThreshold(15, 10) 自适应二值化
   ↓
[4] ColorPreserver.combine()        → final Mat
   HSV 饱和度掩码 + 形态学 + 合成
   binarized 当底色，彩色区域覆盖原色
   ↓
输出 Bitmap (白底黑字 + 彩色保留)
```

## 常用命令

```bash
cd D:\Projects\SimpleScanner

# 编译 Debug
./gradlew.bat assembleDebug
# 产物：app/build/outputs/apk/debug/app-arm64-v8a-debug.apk (~37 MB)

# 编译 Release (R8 已开启)
./gradlew.bat assembleRelease
# 产物：app/build/outputs/apk/release/app-arm64-v8a-release.apk (~17 MB)

# 安装到连接的手机
adb -s <device-id> install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk

# 单元测试 (JVM，可本地跑)
./gradlew test

# 设备测试 (需 OpenCV native)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# 清理
./gradlew clean
```

## 环境要求

- **JDK 17 或 21**（已用 JBR 21 验证 OK）
- **Android SDK 34**（在 `C:\Users\LX\AppData\Local\Android\Sdk`）
- **环境变量**：
  - `JAVA_HOME` 指向 JDK 安装路径
  - `ANDROID_HOME` 指向 Android SDK 路径
  - `PATH` 含 `$ANDROID_HOME/platform-tools`（adb）

## 开发约定

### 包结构

`com.lx.simplescanner.<feature>` 按功能分包。**不要**新建更深层的子包。

### Compose 屏

每个屏 = 1 个 `<Screen>.kt` + 1 个 `<Screen>ViewModel>.kt`，放 `ui/<feature>/`。

### 图像处理

每个算法 = 1 个 `object`（无状态），放 `image/`。新算法就新建文件，**不要**塞进 ImagePipeline。

### ViewModel

- 用 `viewModelScope` + `StateFlow` + `withContext(Dispatchers.IO/Default)`
- 持状态，不持 View
- 协程取消跟着 ViewModel 生命周期

### Bitmap

- 输入/输出都是 `ARGB_8888`
- 处理前 `scaledToMaxEdge(2048)` 避免 OOM
- 用完 `recycle()`

### OpenCV Mat

- 函数内部创建 + 释放（try-finally）
- 跨函数传递时**调用方负责 release**
- 4 通道（BGRA）与 1 通道（灰度）混用时注意 `cvtColor` 方向

## 修改时的检查清单

- [ ] 改依赖版本号：只改 `gradle/libs.versions.toml` 一处
- [ ] 改 ProGuard 规则：测 release APK，确认 OpenCV / PDFBox 没被错误删除
- [ ] 加新权限：改 `AndroidManifest.xml` + 动态申请代码
- [ ] 加新屏：注册到 `MainActivity.kt` 的 `NavHost`
- [ ] 改算法：跑 `DocumentDetectorTest` / `BinarizerTest`（androidTest 需设备）
- [ ] 改 UI：拍几张不同文档验证（歪斜/阴影/红头/彩照）

## 已知陷阱

1. **`LoadSource` 嵌套类命名冲突**：`EditViewModel.LoadSource.File` 会和 `java.io.File` 冲突，子类已命名为 `FromFile`
2. **PDFBox 包名**：`com.tom_roush.pdfbox.android.PDFBoxResourceLoader`（不是 `multipdf`）
3. **CameraX surfaceProvider**：Java API 是 `setSurfaceProvider()`，不是 `surfaceProvider = ...` 赋值
4. **Kotlin 1.9 不用 `kotlin.plugin.compose` 插件**（那是 2.0+），用 `composeOptions.kotlinCompilerExtensionVersion`
5. **AGP/Gradle/JDK 版本要锁**：AGP 8.5.2 + Gradle 8.7 + JDK 17（21 也 OK）+ Kotlin 1.9.24
6. **网络受限**时查文档：可走 `r.jina.ai` 代理 `curl https://r.jina.ai/<URL>`

## 测试

```bash
# JVM 单测（不需设备）
./gradlew test
# 输出：app/build/reports/tests/testDebugUnitTest/index.html
```

测试资源：
- `src/test/` —— 纯算法（JVM）
- `src/androidTest/` —— 需 OpenCV native（真机/模拟器）

## 发版流程

1. 改 `gradle/libs.versions.toml` 的 `versionName` / `versionCode`
2. 写 `CHANGELOG.md` 新版本条目
3. `./gradlew clean assembleRelease`
4. 在 GitHub 创 release tag + 上传 APK：
   ```bash
   # 用 GitHub API（需要 PAT 或 gh auth login）
   # tag 创建、release 创建、assets 上传
   ```
5. 更新 README 体积表

## 沟通方式

- **Issue**：用 `.github/ISSUE_TEMPLATE/bug_report.md` / `feature_request.md`
- **PR**：commit 遵循 [Conventional Commits](https://www.conventionalcommits.org/)，类型：`feat:` / `fix:` / `refactor:` / `docs:` / `test:` / `chore:`
- **不要**在 commit 里加 `Co-Authored-By`（项目全局禁用了归属）

## 任务收尾的标准动作

> 每次任务**结束前**必须做：

1. **更新 `agent-handoff.md`**：重写整个文件，记录本次目标/已完成/修改/未完成/下一步
2. **追加 `WORKLOG.md`**：在文件**顶部**追加新决策（保持时间倒序），保留失败路径
3. **按需更新本 `AGENTS.md`**：仅当发现"长期稳定经验"才追加，**不要重写**
4. **更新 `ROADMAP.md`**：勾掉完成项、补充新候选
5. **commit + push**：`chore(docs): update handoff/worklog/roadmap for <任务名>`
6. **如果用 GitHub API 发布**：参考 `~/.claude/projects/C--Users-LX/memory/github-api-credentials.md`

**自动化提示**：可配 `hookify` Stop hook，handoff 当日未更新则阻断停止（见 `agent-handoff-methodology.md` 记忆文件）。

## 期望的 AI 行为

- ✅ 改代码前先看相关文件（不要瞎改）
- ✅ 重要改动先在 plan 模式与用户确认
- ✅ 跑通 `./gradlew assembleDebug` 再说"完成"
- ✅ 体积敏感：OpenCV 不能膨胀到 50 MB+
- ✅ 错误处理要全，不要静默 catch
- ❌ 不要引入新的重型依赖（OCR 框架、新的 DI 库、新的图像库）
- ❌ 不要改 OpenCV/PDFBox 的 keep 规则（除非有明确依据）
- ❌ 不要破坏现有拍照→编辑→预览的路由流

## 快速参考

| 任务 | 看哪里 |
|---|---|
| 加新屏 | `MainActivity.kt` NavHost + `ui/<feature>/` |
| 改拍照 | `camera/CameraController.kt` + `ui/capture/` |
| 改图像处理 | `image/ImagePipeline.kt`（编排）+ `image/<具体算法>.kt`（实现） |
| 改 PDF | `pdf/PdfBuilder.kt` |
| 改主题/颜色 | `ui/theme/Color.kt` + `app/src/main/res/values/colors.xml` |
| 加新依赖 | `gradle/libs.versions.toml` |
| 改权限 | `AndroidManifest.xml` + 动态申请 |
| 改版本号 | `gradle/libs.versions.toml` + `CHANGELOG.md` |
| 发 release | 见上文「发版流程」 |

## License

Apache 2.0。详见 [LICENSE](LICENSE)。
