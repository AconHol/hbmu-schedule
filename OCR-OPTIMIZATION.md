# 离线验证码模型优化

## 2.2.2：修复 Android JNI 加载

2.2.1 复用了完整官方包的 Android JNI，该 JNI 依赖 `OrtSessionOptionsAppendExecutionProvider_Nnapi`，而 CPU 最小运行时没有提供这个符号。Android 加载 JNI 时会解析该依赖，因而失败；Windows JNI 不依赖它，所以先前桌面测试没有暴露问题。

2.2.2 从同一个 ORT 1.22.0 提交重新编译 CPU JNI，使用各架构核心实际生成的 `onnxruntime_config.h`，编译时用 `-Wl,-z,defs` 检查所有直接依赖；不再复用带 NNAPI 依赖的 JNI。三种架构的 JNI 只导入核心提供的 `OrtGetApiBase` 和 `OrtSessionOptionsAppendExecutionProvider_CPU`。新增 [链接依赖检查](tools/ocr/check_linkage.py)，防止再次打包不匹配的组合。

模型、字表和原生推理核心保持 2.2.1 的压缩方案，约 5 MiB 模型及 1.2–2.2 MiB 核心。新取得的 32 张真实验证码，其预测结果与原通用模型一致；其中含人眼易混淆字符，所以这项比较不等同于宣称真实识别率 100%。Android 手机上的实际运行留待设备验证。

2.2.1 将原 ddddocr 1.6.1 通用模型的输出范围缩减为数字、小写英文字母和 CTC 空白，共 37 个类别。模型与字表始终成对更新，保留原类别权重及空白、重复字符的解码规则。

卷积权重使用逐输出通道 INT8 存储，计算时恢复为 float32；循环层和输出层使用动态 INT8。输入、卷积激活和输出分数仍为 float32。最终保存为 ORT 格式，APK 不再同时包含原通用模型。

| 模型 | 文件大小 | 合成样本整码正确数 |
| --- | ---: | ---: |
| 原始通用 FP32 | 54,088,400 字节 | 436 / 512 |
| 37 类 FP32 | 20,579,094 字节 | 443 / 512 |
| 37 类动态 INT8 | 7,927,654 字节 | 443 / 512 |
| 最终混合 INT8，转换前 ONNX | 5,241,645 字节 | 442 / 512 |
| 最终 APK 内 ORT | 5,290,848 字节 | 442 / 512 |

这些是固定随机种子、8 种字体、有干扰线和轻微模糊的 **512 张合成样本**，不是学校验证码识别率。另有 **1 张人工核对的真实旧样本**，上述模型均识别正确。样本数量不足以评估真实验证码总体准确率；用户仍可查看验证码并手动修改识别结果。

相对于 37 类 FP32，最终量化模型在合成集上新增 4 个错误，同时修正 3 个错误，净下降 1 / 512，即约 0.20 个百分点。相对于旧通用模型，正确数增加 6 / 512。模型转换为 ORT 后，513 张图的结果与转换前完全一致。另用宽度 16、64、112、179、256、1024 的输入确认：裁剪前后保留类别的 FP32 分数在数值容差内一致。

本地 CPU 推理、原生 C API 和 Java/JNI 对照用于检查模型和裁剪算子集，完整与最小运行时在 513 张图上的识别结果一致。Windows Java 测试通过 `onnxruntime.native.onnxruntime.skip=true` 由 JNI 依赖加载核心库。Android 实机运行和更大真实样本集需要另行验证，不能从桌面结果推断。各候选模型的完整初步结果见 [benchmark-results.json](tools/ocr/benchmark-results.json)。

## 运行时

从 ONNX Runtime **v1.22.0** 的提交 `f217402897f40ebba457e2421bc0a4702771968e` 构建 CPU 最小运行时，仅保留 [模型实际需要的算子和类型](tools/ocr/required_operators.config)。关闭 ML 算子和 RTTI，保留异常处理。支持 `arm64-v8a`、`armeabi-v7a` 和 `x86_64`，使用 Android API 26 和 16 KiB ELF 页面对齐。

自定义 AAR 使用同版本官方包的 Java API 和消费端 ProGuard 规则，并重新编译与 CPU 核心匹配的 JNI。MIT 许可证及第三方通知随 AAR 保留。AAR 内的 `META-INF/hbmu-ort-build.json` 记录来源提交、原始 AAR 哈希以及每个原生库的哈希。应用仍保留 `ai.onnxruntime.**` 的 R8 规则。

## 复现

建议使用独立 Python 环境；依赖见 [requirements.txt](tools/ocr/requirements.txt)。准备 MIT 许可的 ddddocr 1.6.1 `common.onnx` 及原始字表，脚本会核对模型 SHA-256，然后运行：

```text
python tools/ocr/optimize_model.py common.onnx original-charset.json output
```

输出包含 `captcha.ort`、`charset.json` 和自动生成的算子配置。将前两者一起放入 `androidApp/src/main/assets/hbmu/`，不要把原通用模型同时打包。原始模型及字表的许可来自 [ddddocr](https://github.com/sml2h3/ddddocr)。

复现合成样本对比可以运行 `tools/ocr/benchmark.py --original common.onnx --original-charset original-charset.json --optimized output/captcha.ort --optimized-charset output/charset.json --output benchmark-output`。脚本不联网、不登录，输出仍只描述合成测试表现。

使用官方 ORT v1.22.0 源码、同版本官方 Android AAR、CMake、Ninja 和 Android NDK 构建运行时：

```text
python tools/ocr/build_runtime.py --source onnxruntime --sdk android-sdk --ndk android-ndk --cmake cmake --ops output/captcha.required_operators_and_types.config --official-aar onnxruntime-android-1.22.0.aar --build-dir build-ort --output third_party/maven/cn/hbmu/thirdparty/onnxruntime-captcha/1.22.0-hbmu.2/onnxruntime-captcha-1.22.0-hbmu.2.aar
```

路径应使用绝对路径。Windows 需要已安装 MSVC；脚本保留例外信息以便模型加载失败时退回手动输入。如果 Eigen 下载受阻，可通过 `--eigen-source` 指定从其官方镜像取得的相同源码提交 `1d8b82b0740839c0de7f1242a3585e3390ff5f33`。

实现依据：[官方量化说明](https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html)、[最小构建与算子类型裁剪](https://onnxruntime.ai/docs/build/custom.html)、[Android 构建说明](https://onnxruntime.ai/docs/build/android.html)。
