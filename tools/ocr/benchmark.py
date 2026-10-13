"""Compare original and optimized OCR on a fixed synthetic corpus.

The resulting percentage describes this corpus only, not school captcha accuracy.
Requires the eight Windows font files listed below (not distributed with the app).
"""
import argparse, json, random, time
from pathlib import Path
import numpy as np
import onnxruntime as ort
from PIL import Image, ImageDraw, ImageFilter, ImageFont

FONTS = ["arial.ttf", "arialbd.ttf", "times.ttf", "timesbd.ttf", "calibri.ttf", "calibrib.ttf", "consola.ttf", "tahoma.ttf"]

def evaluate(model, charset, samples):
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.log_severity_level = 3
    session = ort.InferenceSession(str(model), options, providers=["CPUExecutionProvider"])
    predictions, timings = [], []
    for image, label in samples:
        width = max(16, min(1024, image.width * 64 // image.height))
        pixels = np.asarray(image.convert("RGB").resize((width, 64), Image.Resampling.BILINEAR), dtype=np.float32)
        array = ((pixels[:, :, 0] * .299 + pixels[:, :, 1] * .587 + pixels[:, :, 2] * .114) / 255)[None, None]
        start = time.perf_counter()
        scores = session.run(None, {session.get_inputs()[0].name: array})[0][:, 0, :]
        timings.append((time.perf_counter() - start) * 1000)
        previous, word = -1, ""
        for index in scores.argmax(axis=1):
            if index and index != previous:
                word += charset[index]
            previous = index
        predictions.append(word)
    return {"correct": sum(p == label for p, (_, label) in zip(predictions, samples)),
            "total": len(samples), "median_ms": round(float(np.median(timings)), 2), "predictions": predictions}

def main(args):
    args.output.mkdir(parents=True, exist_ok=True)
    rng = random.Random(221)
    samples = []
    for i in range(512):
        label = "".join(rng.choice("0123456789abcdefghijklmnopqrstuvwxyz") for _ in range(4))
        image = Image.new("RGB", (112, 40), "white")
        draw = ImageDraw.Draw(image)
        font = ImageFont.truetype(str(args.fonts / FONTS[i % len(FONTS)]), rng.randint(24, 30))
        for j, char in enumerate(label):
            draw.text((6 + j * 25, rng.randint(0, 5)), char, font=font, fill=tuple(rng.randint(15, 100) for _ in range(3)))
        if i % 3:
            for _ in range(rng.randint(1, 3)):
                draw.line([(rng.randrange(112), rng.randrange(40)), (rng.randrange(112), rng.randrange(40))],
                          fill=tuple(rng.randint(140, 230) for _ in range(3)), width=1)
        if i % 4 == 0:
            image = image.filter(ImageFilter.GaussianBlur(.4))
        samples.append((image, label))
    original = evaluate(args.original, json.loads(args.original_charset.read_text("utf-8")), samples)
    optimized = evaluate(args.optimized, json.loads(args.optimized_charset.read_text("utf-8")), samples)
    report = {"seed": 221, "dataset": "synthetic; does not establish real captcha accuracy", "runtime": ort.__version__,
              "original": original, "optimized": optimized}
    (args.output / "benchmark.json").write_text(json.dumps(report, indent=2), "utf-8")
    print(json.dumps({name: {k: v for k, v in report[name].items() if k != "predictions"} for name in ["original", "optimized"]}))

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    for name in ["original", "original-charset", "optimized", "optimized-charset", "output"]:
        p.add_argument("--" + name, type=Path, required=True)
    p.add_argument("--fonts", type=Path, default=Path("C:/Windows/Fonts"))
    main(p.parse_args())
