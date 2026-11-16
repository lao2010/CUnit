# C 单位 App

个人知识管理工具：把照片、视频、文字连同 AI 提取的关键词和语义向量一起存成一个"C 单位"，之后可以用一句话做语义检索。

UI 基于 [AndroidLiquidGlass (Backdrop)](https://github.com/Kyant0/AndroidLiquidGlass) 实现 iOS 风格的液态玻璃效果。

## 功能

- **状态页**（默认进入）：C 单位总数 / 照片 / 视频 / 纯文字统计、高频关键词、最近添加、API 配置状态
- **存入页**：
  - 可选择照片和/或视频（系统相册选择器，无需存储权限）
  - **注释必填**；没有媒体时**文字内容必填**（用文字代替媒体）；有媒体时文字可选
  - 保存流水线：LLM 提取关键词 → Embedding 向量化 → 媒体原件 + 关键词 + 向量一起持久化
  - 逐步显示进度（①提取关键词 ②向量化 ✓已保存）
- **检索页**：输入一句话 → 查询文字被向量化 → 与所有 C 单位算余弦相似度 → 按相似度排序展示（照片缩略图、视频抽帧、关键词、相似度百分比）；点开详情可查看全文并打开视频
- **设置页**：配置 API 地址 / Key / 模型，一键测试连通性

## 安装

直接安装 `app-debug.apk`（`adb install` 或传到手机上点击安装）。

## API 配置（进入 App 后到「设置」页填写）

兼容 OpenAI 接口的任意服务，默认预填智谱 GLM：

| 字段 | 默认值 | 说明 |
|---|---|---|
| API Base URL | `https://open.bigmodel.cn/api/paas/v4` | 智谱；OpenAI 填 `https://api.openai.com/v1`，DeepSeek 填 `https://api.deepseek.com/v1` |
| API Key | 空 | 必填，`Bearer` 方式鉴权 |
| Chat 模型 | `glm-4-flash` | 用于关键词提取 |
| Embedding 模型 | `embedding-3` | 用于向量化 |

**必须先在设置页填好 API Key 并保存**，存入和检索才能工作。接口失败时 C 单位仍会保存（标记 `keywordsFailed` / `embeddingFailed`），只是不参与向量检索。

## 数据存储

- C 单位元数据：`/data/data/com.cunit.app/files/units/*.json`（含关键词 + 完整向量）
- 媒体原件：`/data/data/com.cunit.app/files/media/`
- 应用私有目录，卸载即清除，无存储权限要求

## 从源码构建

```bash
# 需要 JDK 17+，Android SDK（compileSdk 36 会自动下载）
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

国内环境已配置阿里云 Maven 镜像 + 腾讯 Gradle 发行镜像；`local.properties` 需写 `sdk.dir` 指向你的 Android SDK。

## 技术栈

Kotlin 2.4 / Jetpack Compose（androidx 1.12）/ backdrop 2.0.1（玻璃效果：vibrancy + blur + lens）/ kotlinx.serialization / OkHttp 免依赖的 HttpURLConnection / 集成式 JSON 文件存储（轻量，无 Room）

> 注：构建脚本中禁用了 `checkAarMetadata` 任务，用于规避 Gradle 8.14 在异常路径上的 NPE 显示 bug（compileSdk 已是最高，检查无实际意义）。
