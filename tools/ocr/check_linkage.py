"""Verify Android JNI imports are provided by the exact packaged runtime."""
import argparse,json,re,subprocess,tempfile,zipfile
from pathlib import Path

def check(aar: Path, nm: Path):
    result = {}
    with zipfile.ZipFile(aar) as archive, tempfile.TemporaryDirectory() as tmp:
        for abi in ["arm64-v8a", "armeabi-v7a", "x86_64"]:
            core, bridge = Path(tmp) / "core.so", Path(tmp) / "bridge.so"
            core.write_bytes(archive.read(f"jni/{abi}/libonnxruntime.so"))
            bridge.write_bytes(archive.read(f"jni/{abi}/libonnxruntime4j_jni.so"))
            provided = subprocess.check_output([str(nm), "--dynamic", "--defined-only", str(core)], text=True)
            required = subprocess.check_output([str(nm), "--dynamic", "--undefined-only", str(bridge)], text=True)
            names = [line.split()[-1].split("@")[0] for line in required.splitlines() if line.split() and line.split()[-1].startswith("Ort")]
            missing = [name for name in names if not re.search(r"\b" + re.escape(name) + r"\b", provided)]
            result[abi] = {"required_ort_symbols": names, "missing": missing}
            if missing:
                raise RuntimeError(f"{abi}: JNI requires missing symbols {missing}")
    return result

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("aar", type=Path)
    parser.add_argument("--nm", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(check(args.aar, args.nm), indent=2))
