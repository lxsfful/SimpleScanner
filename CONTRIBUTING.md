# 贡献指南

欢迎参与 SimpleScanner！无论是提 Issue、PR、还是改文档，都非常感谢。

## 🐛 报告 Bug

1. 去 [Issues](../../issues) 选 **Bug Report** 模板
2. 填写：手机型号 / Android 版本 / 复现步骤 / 期望行为 / 实际行为
3. 如有可能附 `adb logcat` 错误日志：

```bash
adb logcat -d -s "SimpleScannerApp:*" "OpenCV:*" "AndroidRuntime:E" > logcat.txt
```

## 💡 提功能建议

1. 去 [Issues](../../issues) 选 **Feature Request** 模板
2. 描述使用场景和预期行为
3. 讨论通过后再实现

## 🔧 提交 PR

### 工作流

1. Fork 本仓库
2. 创建 feature 分支：`git checkout -b feature/your-feature`
3. 提交代码（遵循 [Conventional Commits](https://www.conventionalcommits.org/)）：
   - `feat:` 新功能
   - `fix:` 修 bug
   - `refactor:` 重构
   - `docs:` 文档
   - `test:` 测试
   - `chore:` 杂项
4. 跑测试 + 构建：
   ```bash
   ./gradlew test assembleDebug
   ```
5. 推送到你的 fork：`git push origin feature/your-feature`
6. 创建 PR，描述变更

### 代码规范

- 遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html)
- 公共 API 加 KDoc
- 不要提交未在 `.gitignore` 排除的二进制 / 构建产物
- ProGuard 规则变更要注释说明

### Commit 归属

本项目全局禁用了 commit 归属（`.claude/settings.json`），无需加 `Co-Authored-By`。

## 🏗️ 本地开发

### 环境

- Android Studio 2026.1.1+ / 命令行 Gradle 8.7+
- JDK 17 或 21
- Android SDK 34

### 命令

```bash
# 编译
./gradlew assembleDebug

# 测试
./gradlew test

# Lint
./gradlew lint

# 清理
./gradlew clean
```

## 📂 项目约定

- **包结构**：`com.lx.simplescanner.<feature>` 按功能分包
- **Composable 屏**：放 `ui/<screen>/`，文件名 `<Screen>.kt` + `<Screen>ViewModel>.kt`
- **图像处理**：放 `image/`，每个算法一个文件（Object）
- **不要** 修改 `gradle/wrapper/gradle-wrapper.jar` 之外的 wrapper 文件

## 🧪 写测试

- JVM 单测放 `src/test/`
- 需要 Android 框架的放 `src/androidTest/`
- 算法有纯函数部分优先写 JVM 测试

## 📜 协议

贡献的代码按 [Apache License 2.0](LICENSE) 协议开源。
