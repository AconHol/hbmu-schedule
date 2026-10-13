"""Build ORT 1.22 CPU kernels needed by captcha.ort, then reuse official JNI/Java.

Use the upstream v1.22.0 source at f217402897f40ebba457e2421bc0a4702771968e.
The produced AAR retains the upstream Java API, JNI, licenses and consumer rules.
"""
import argparse, hashlib, json, os, shutil, subprocess, sys, tempfile, zipfile
from pathlib import Path

ORT_COMMIT = "f217402897f40ebba457e2421bc0a4702771968e"
OFFICIAL_AAR_SHA256 = "04a4617a9c797cf49225595e45b5546081cb34c86ac817581141577d3b7dbfe2"
ABIS = ["arm64-v8a", "armeabi-v7a", "x86_64"]

def build(args):
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=args.source, text=True).strip()
    if head != ORT_COMMIT:
        raise ValueError("Build requires the pinned ONNX Runtime v1.22.0 commit")
    if hashlib.sha256(args.official_aar.read_bytes()).hexdigest() != OFFICIAL_AAR_SHA256:
        raise ValueError("Unexpected official ONNX Runtime 1.22.0 Android AAR")
    if args.eigen_source:
        eigen_head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=args.eigen_source, text=True).strip()
        if eigen_head != "1d8b82b0740839c0de7f1242a3585e3390ff5f33":
            raise ValueError("Unexpected Eigen source commit")
    strip = args.ndk / "toolchains/llvm/prebuilt" / ("windows-x86_64" if os.name == "nt" else "linux-x86_64") / "bin" / ("llvm-strip.exe" if os.name == "nt" else "llvm-strip")
    for abi in ABIS:
        command = [sys.executable, str(args.source / "tools/ci_build/build.py"),
                   "--build_dir", str(args.build_dir / abi), "--config", "MinSizeRel",
                   "--android", "--android_sdk_path", str(args.sdk), "--android_ndk_path", str(args.ndk),
                   "--android_abi", abi, "--android_api", "26", "--cmake_generator", "Ninja",
                   "--cmake_path", str(args.cmake), "--parallel", str(args.jobs),
                   "--build_shared_lib", "--minimal_build", "--disable_ml_ops", "--disable_rtti",
                   "--include_ops_by_config", str(args.ops), "--enable_reduced_operator_type_support",
                   "--skip_tests", "--skip_submodule_sync", "--cmake_extra_defines",
                   "CMAKE_POLICY_VERSION_MINIMUM=3.5", "CMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384",
                   "onnxruntime_BUILD_UNIT_TESTS=OFF"]
        if args.eigen_source:
            command.append("FETCHCONTENT_SOURCE_DIR_EIGEN3=" + str(args.eigen_source))
        subprocess.run(command, check=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    manifest = {"ort_version": "1.22.0", "source_commit": head,
                "official_aar_sha256": hashlib.sha256(args.official_aar.read_bytes()).hexdigest(),
                "model_format": "ORT", "exception_handling": True, "libraries": {}}
    with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(args.official_aar) as official, zipfile.ZipFile(args.output, "w", zipfile.ZIP_DEFLATED) as custom:
        for info in official.infolist():
            if info.filename.startswith("jni/"):
                continue
            custom.writestr(info.filename, official.read(info))
        for abi in ABIS:
            library = Path(tmp) / (abi + ".so")
            shutil.copy2(args.build_dir / abi / "MinSizeRel/libonnxruntime.so", library)
            subprocess.run([str(strip), "--strip-unneeded", str(library)], check=True)
            data = library.read_bytes()
            custom.writestr("jni/" + abi + "/libonnxruntime.so", data)
            custom.writestr("jni/" + abi + "/libonnxruntime4j_jni.so", official.read("jni/" + abi + "/libonnxruntime4j_jni.so"))
            manifest["libraries"][abi] = {"bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
        custom.writestr("META-INF/hbmu-ort-build.json", json.dumps(manifest, indent=2))
    args.output.with_suffix(".aar.sha256").write_text(hashlib.sha256(args.output.read_bytes()).hexdigest() + "\n", encoding="utf-8")
    print(json.dumps(manifest, indent=2))

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    for name in ["source", "sdk", "ndk", "cmake", "ops", "official-aar", "build-dir", "output"]:
        p.add_argument("--" + name, type=Path, required=True)
    p.add_argument("--eigen-source", type=Path)
    p.add_argument("--jobs", type=int, default=4)
    build(p.parse_args())
