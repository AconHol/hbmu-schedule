"""Create the offline 36-character model from the MIT-licensed ddddocr model.

Model author: sml2h3; adaptation by AconHol. Convolution activations stay float32.
"""
import argparse, hashlib, json
from pathlib import Path
import numpy as np
import onnx
from onnx import helper, numpy_helper
from onnxruntime.quantization import QuantType, quantize_dynamic
from onnxruntime.tools.convert_onnx_models_to_ort import OptimizationStyle, convert_onnx_models_to_ort

SOURCE_SHA256 = "33b5cd351ee94e73a6bf8fa18c415ed8b819b3ffd342e267c30d8ad8334e34e8"

def optimize(source: Path, charset_path: Path, output: Path):
    if hashlib.sha256(source.read_bytes()).hexdigest() != SOURCE_SHA256:
        raise ValueError("Unexpected source model; use ddddocr 1.6.1 common.onnx")
    output.mkdir(parents=True, exist_ok=True)
    charset = json.loads(charset_path.read_text(encoding="utf-8"))
    indices = [0] + [i for i, c in enumerate(charset) if len(c) == 1 and c in "0123456789abcdefghijklmnopqrstuvwxyz"]
    selected = [charset[i] for i in indices]
    assert len(selected) == 37 and selected[0] == ""
    model = onnx.load(source)
    weights = {t.name: t for t in model.graph.initializer}
    head = next(n for n in model.graph.node if n.op_type == "Gemm")
    assert numpy_helper.to_array(weights[head.input[1]]).shape == (8210, 1024)
    assert next(a for a in head.attribute if a.name == "transB").i == 1
    for name in head.input[1:]:
        tensor = weights[name]
        tensor.CopyFrom(numpy_helper.from_array(numpy_helper.to_array(tensor)[indices].copy(), name))
    assert model.graph.node[-2].op_type == "Concat"
    class_dimension = model.graph.node[-2].input[-1]
    weights[class_dimension].CopyFrom(numpy_helper.from_array(np.array([37], dtype=np.int64), class_dimension))
    model.graph.output[0].CopyFrom(helper.make_tensor_value_info(model.graph.output[0].name, onnx.TensorProto.FLOAT,
                                                               ["sequence", 1, 37]))
    onnx.checker.check_model(model)
    onnx.save(model, output / "alnum-fp32.onnx")
    model = onnx.version_converter.convert_version(model, 13)
    for tensor in list(model.graph.initializer):
        if not any(n.op_type == "Conv" and n.input[1] == tensor.name for n in model.graph.node):
            continue
        value = numpy_helper.to_array(tensor)
        scale = np.maximum(np.max(np.abs(value), axis=tuple(range(1, value.ndim))) / 127,
                           np.finfo(np.float32).tiny).astype(np.float32)
        quantized = np.clip(np.rint(value / scale.reshape([-1] + [1] * (value.ndim - 1))), -127, 127).astype(np.int8)
        name = tensor.name
        tensor.CopyFrom(numpy_helper.from_array(quantized, name + "_int8"))
        model.graph.initializer.extend([numpy_helper.from_array(scale, name + "_scale"),
                                        numpy_helper.from_array(np.zeros(value.shape[0], np.int8), name + "_zero")])
        model.graph.node.insert(0, helper.make_node("DequantizeLinear", [name + "_int8", name + "_scale", name + "_zero"],
                                                   [name], name=name + "_dequantize", axis=0))
    onnx.checker.check_model(model)
    onnx.save(model, output / "conv-int8.onnx")
    quantize_dynamic(output / "conv-int8.onnx", output / "captcha.onnx", per_channel=True,
                     op_types_to_quantize=["LSTM", "MatMul", "Gemm"], weight_type=QuantType.QInt8)
    convert_onnx_models_to_ort(output / "captcha.onnx", output_dir=output,
                              optimization_styles=[OptimizationStyle.Fixed], enable_type_reduction=True)
    (output / "charset.json").write_text(json.dumps(selected, separators=(",", ":")), encoding="utf-8")
    config = output / "captcha.required_operators_and_types.config"
    config.write_text("# Offline captcha model operator/type allowlist\n" + "\n".join(
        line for line in config.read_text(encoding="utf-8").splitlines() if not line.startswith("#")) + "\n", encoding="utf-8")
    print(json.dumps({"model_bytes": (output / "captcha.ort").stat().st_size, "characters": len(selected) - 1}))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("charset", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    optimize(args.source, args.charset, args.output)
