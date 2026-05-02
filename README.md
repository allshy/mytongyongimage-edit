# AI 图像工坊

个人使用的 Android AI 图像生成/编辑客户端。首版使用 Kotlin、Jetpack Compose、Material 3，支持 OpenAI、fal.ai、Replicate 三类 API Key。

## 功能

- 文生图：选择 Provider、模型、尺寸、数量、种子和质量。
- 图片编辑：选择本地图片，绘制蒙版，提交编辑或局部重绘。
- 本地历史：保存提示词、模型、状态和结果图片路径。
- 本机密钥：API Key 使用 Android Keystore 加密保存，不上传到服务器。
- CI 构建：GitHub Actions 自动运行单测并生成 Debug APK。

## 构建

本仓库没有提交 Gradle Wrapper 二进制文件。GitHub Actions 会先安装 Gradle 8.10.2，再生成 wrapper，然后运行：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

构建完成后，在 Actions 的 artifact 中下载 `ai-image-client-debug-apk`。

## 首版限制

- 仅面向个人本机使用，没有登录、云同步或后端代理。
- Release 签名暂未配置，CI 只输出 Debug APK。
- 模型列表位于 `app/src/main/assets/model_catalog.json`，启动失败时回退到代码内置默认值。
- ComfyUI、A1111、本地离线推理、视频生成留作后续版本。
