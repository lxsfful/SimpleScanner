# WORKLOG · 决策日志

> 关键决策 + 原因 + 替代方案。按**时间倒序**（最新在上）。
> 与 `agent-handoff.md`（任务交接）不同——本文档记**为什么**，handoff 记**做了什么**。

---

## 2026-06-14 · 收尾 + 文档

### 决策：GitHub 凭证固化到 memory

- **做了什么**：把 `lxsfful` 的 OAuth token 写到 `~/.claude/projects/C--Users-LX/memory/github-api-credentials.md`
- **为什么**：用户希望 Claude Code 启动后能直接读 token 做 GitHub 操作，避免每次问
- **风险**：memory 是纯文本，泄露即 token 失窃。已写明"不要 git add 跟踪"
- **备选**：
  - 用 `git credential fill` 每次提取（更安全但更慢）
  - 用 `gh auth login` OAuth 流程（需要 `read:org` scope，token 没这 scope）
  - 加密存储 + 密码解锁（复杂度高）
- **最终选择**：固化 token + 写明风险 + 提供备选方案

### 决策：AGENTS.md 用英文 + 中文注释

- **为什么**：项目主语言英文（代码 + 文档），但用户主要用中文。AGENTS.md 是给 AI 看的，需要兼顾国际化
- **结果**：AGENTS.md 用英文写（最通用），其他文档（README、CHANGELOG）用中英混合
- **可改进**：未来如果用户群体明确中文，AGENTS.md 可加中文版

---

## 2026-06-13 · 阶段 4 收尾（打磨 + 开源）

### 决策：缩略图重排用 ◀▶ 按钮，不用拖动库

- **为什么**：
  - `org.burnoutcrew.composereorderable:reorderable:0.9.6` 只支持 `LazyColumn`，不支持 `LazyRow`（缩略图横向排）
  - 强行用 `LazyColumn` 横向排版很别扭
  - ◀▶ 按钮实现简单稳定，零依赖
- **代价**：体验不如拖动手势流畅
- **未来**：可换到支持 LazyRow 的拖动库（如 `sh.calvin.reorderable:reorderable`），或自定义 PointerInput 拖动

### 决策：Release 用 debug 签名

- **为什么**：开发期方便，不需要每次生成 keystore
- **风险**：不能发布到 Play Store（debug 签名被拒）
- **未来**：发布前生成正式 keystore，写入 `~/.gradle/gradle.properties` + 替换 `signingConfigs`

### 决策：API 创建仓库 + 推 tag + Release + 上传 assets

- **为什么**：
  - `gh` CLI 缺 `read:org` scope，登录不了
  - GitHub REST API 直接用 OAuth token + `urllib.request` 完全可行
  - 一次跑通后所有 GitHub 操作都不再依赖 `gh`
- **过程教训**：
  - `curl -d '{...}'` 复杂 JSON 在 bash 下转义有问题 → 改用 `curl -d @file.json` 解决
  - Python `print` 中文遇到 GBK 编码错误 → 加 `PYTHONIOENCODING=utf-8` 解决
  - Windows 路径用 raw 字符串 `r'D:\...'` 避免 `\\` 转义
- **沉淀**：`github-api-credentials.md` 里写全模板

### 决策：AGENTS.md 写"5 分钟读懂"定位

- **为什么**：开源项目里 AI agent 越来越多（Claude Code / Cursor / Aider），让它们 5 分钟内能上手，比"通读 3500 行代码"友好 100 倍
- **结构**：项目一句话 + 仓库结构（⭐ 标注重要性）+ 5 个必读文件 + 关键决策表 + 已知陷阱 + 期望 AI 行为
- **效果**：下次开新会话时，AI 读完 AGENTS.md 就能干活

---

## 2026-06-13 · 阶段 3（多页编辑 + Preview 屏）

### 决策：ScanRepository 升级为多页状态

- **为什么**：阶段 1/2 是单图，阶段 3 要支持 N 张图。集中放单例 `object` 便于跨 ViewModel 共享
- **关键设计**：
  - `Page` 只存 `originalPath` + `enhancedPath`，**不缓存 Bitmap**（避免 OOM）
  - 增强图写入 `cacheDir/enh_<id>.png` 持久化，下次启动可复用
  - `enhanceAll` 用 `Mutex` 串行化，避免并发竞争
- **Bug fix**：`forEachIndexed { idx, page -> ... }` 里 `idx` warning 改成 `_` 即可

### 决策：Edit 屏每次启动新建 ViewModel

- **为什么**：用户希望每次进入 Edit 都是新会话（不记住上次）
- **代价**：返回 Edit 屏会重新触发 `enhanceAll` 进度（但因为有 enhancedPath 缓存，秒过）
- **未来**：可加 "保存草稿" 机制

---

## 2026-06-13 · 阶段 2（图像处理）

### 决策：OpenCV 用 Maven 预编译 AAR

- **为什么**：
  - 源码编译需要 NDK（用户机器没装）
  - 预编译 AAR 内置 `.so` 文件，加载快
  - Maven `org.opencv:opencv:4.10.0` 是 OpenCV 官方提供的 AAR
- **代价**：APK 多 30+ MB（按 ABI split 后 arm64 ~17 MB）
- **可优化**：未来如果体积敏感，可裁剪 OpenCV 模块（如去掉 ml 模块），但目前 17 MB 可接受

### 决策：算法参数默认 Canny(75,200) + 膨胀 5×5 + approxPolyDP 0.02*peri

- **为什么**：
  - Canny 双阈值是 OpenCV 官方推荐默认（不同场景需调）
  - 膨胀让边缘闭合，避免后续 findContours 找到碎片
  - 0.02*peri 是经典多边形拟合系数（更小拟合更紧）
- **未调优**：用户实拍后可能发现某些场景需要调
- **未来**：可做"算法参数 Profile"——给用户 3 档预设（清晰/普通/手写体）

### 决策：颜色保留用 HSV 饱和度掩码，不用深度学习

- **为什么**：
  - ML 分类器需要训练数据，模型大
  - HSV 阈值足够识别"红头/印章/彩照"等典型场景
  - 启发式公式：`S ≥ 60 且 V ∈ [50, 240]` 视为有彩色
- **漏检风险**：低饱和度彩色（如淡蓝笔批注）会被当作灰度
- **未来**：可加 ML Kit 的 Object Detection 专门识别"红头"和"印章"

### 决策：失败兜底（找不到 4 边形时返回原图）

- **为什么**：增强失败不能崩 App，最坏情况是"和原图一样"
- **代价**：用户可能看到"按了增强没变化"（UI 未提示）
- **可改进**：在 Edit 屏加"未检测到文档边缘"的提示

### 决策：Binarizer 必须奇数 blockSize，校验参数

- **为什么**：`adaptiveThreshold` 的 blockSize 必须是奇数（API 要求）
- **写法**：`require(blockSize % 2 == 1) { "blockSize must be odd" }`

### 决策：颜色保留 Bug 修正

- **原代码**：`colorPreserver.combine(warped, bw)` 用了两次 `copyTo`，互相覆盖
- **正确逻辑**：
  ```kotlin
  binarized.copyTo(result)               // 底色用二值化
  original.copyTo(result, colorMask)     // 彩色区域覆盖原色
  ```
- **修复后**：印章/红头保留，文本变黑白

---

## 2026-06-12 · 阶段 1（脚手架）

### 决策：Kotlin 1.9.24 + Compose Compiler 1.5.14，不用 `kotlin.plugin.compose`

- **为什么**：
  - `org.jetbrains.kotlin.plugin.compose` 是 Kotlin **2.0+** 的插件
  - Kotlin 1.9.x 配 Compose 用旧式：`composeOptions.kotlinCompilerExtensionVersion = "1.5.14"`
- **踩坑**：第一次配错，导致 `Plugin not found` 编译失败
- **教训**：用 Kotlin 1.9.x 别加 `kotlin.plugin.compose`

### 决策：PDFBox-Android，不用 iText / 系统 PdfDocument

- **为什么**：
  - **iText 7**：AGPL 商业许可，社区版限制多
  - **Android PdfDocument**：系统自带，零依赖，但**只支持最基础功能**（一页 + 一张图），对中文支持差
  - **PDFBox-Android**：Apache 2.0，纯 Java，中文 OK
- **代价**：APK 多 2-3 MB
- **坑**：`PDFBoxResourceLoader` 在 `com.tom_roush.pdfbox.android` 包，**不是** `multipdf`

### 决策：单 Activity + Compose Navigation

- **为什么**：现代推荐架构，状态管理简单（不像多 Activity 互相传值）
- **屏数**：3 个（home / capture → edit → preview）
- **代价**：Compose Navigation 字符串参数用 `URLEncoder.encode` 处理（避免特殊字符）

### 决策：系统分享 Intent，不集成微信 SDK

- **为什么**：
  - 微信开放平台需要注册 AppID + 企业资质（个人开发者麻烦）
  - `Intent.createChooser` 让用户从选择器选 App（微信/QQ/邮件/Drive 任意）
  - 零依赖，零注册
- **代价**：用户分享时多一步选择（但很多人习惯了）
- **未来**：如果用户**只**想发微信，可单独加微信 SDK 模块（不影响主代码）

### 决策：按 ABI 拆 APK（arm64 / armv7 / x86_64）

- **为什么**：
  - OpenCV 各 ABI `.so` 加起来几十 MB
  - 拆分后 arm64 APK 17 MB，universal 46 MB
  - 大多数手机是 arm64
- **可改进**：universal APK 主要给模拟器用，可考虑不出

### 决策：失败兜底（Canny 找不到 4 边形时返回原图）

- **为什么**：增强失败不能崩 App，最坏情况是"和原图一样"
- **可改进**：UI 加"未检测到清晰边缘"提示

### 决策：Bitmap 输入限 2048px 边长

- **为什么**：4K 照片直接处理会 OOM
- **做法**：`BitmapFactory.Options.inSampleSize` + `scaledToMaxEdge(2048)`
- **权衡**：超过 2048 的细节会丢，但对文档扫描够用

---

## 2026-06-12 · 项目启动

### 决策：先做计划再实施

- **做法**：进入 plan 模式，4 个阶段拆解 → 用户批准 → 按阶段实施
- **计划文件**：`~/.claude/plans/modular-growing-thompson.md`
- **效果**：避免一上来就乱写代码，方向明确

### 决策：技术栈选择

- **Kotlin + Jetpack Compose**（vs Flutter/RN）：图像处理密集型，Compose 启动快 + 体积小
- **OpenCV 4.10**：图像处理事实标准
- **PDFBox-Android**：轻量 PDF 生成
- **Coil 2.7**：图片加载（替代 Glide，更 Kotlin 友好）

### 决策：包名 `com.lx.simplescanner`

- **为什么**：`com.lx.<project>` 简明
- **namespace + applicationId** 都用这个

### 决策：项目路径 `D:\Projects\SimpleScanner`

- **为什么**：用户 D 盘是项目盘（之前整理过）
- **避免**：C 盘（系统盘，重装会丢）

---

## 通用决策模板

后续决策按此格式写：

```markdown
### 决策：<一句话总结>

- **为什么**：<核心原因>
- **代价**：<取舍>
- **备选**：<A、B、C>
- **最终选择**：<X 的理由>
- **可改进**：<未来可调整>
```
