"""Build the JNI bridge against the same CPU-only configuration as the core.

Do not reuse the official Android JNI: it imports NNAPI symbols from the full core.
"""
import hashlib, os, subprocess, zipfile
from pathlib import Path

TARGETS = {"arm64-v8a": "aarch64-linux-android26", "armeabi-v7a": "armv7a-linux-androideabi26", "x86_64": "x86_64-linux-android26"}

def build_cpu_jni(source: Path, ndk: Path, build_dir: Path, official_aar: Path, java_bin: Path):
    output = build_dir / "jni-cpu"
    headers, classes = output / "headers", output / "classes"
    headers.mkdir(parents=True, exist_ok=True)
    classes.mkdir(exist_ok=True)
    jar = output / "official-classes.jar"
    with zipfile.ZipFile(official_aar) as archive:
        jar.write_bytes(archive.read("classes.jar"))
    exe = ".exe" if os.name == "nt" else ""
    java_sources = list((source / "java/src/main/java/ai/onnxruntime").rglob("*.java"))
    subprocess.run([str(java_bin / ("javac" + exe)), "--release", "17", "-cp", str(jar), "-h", str(headers), "-d", str(classes)] + [str(p) for p in java_sources], check=True)
    toolchain = ndk / "toolchains/llvm/prebuilt" / ("windows-x86_64" if os.name == "nt" else "linux-x86_64")
    native = list((source / "java/src/main/native").glob("*.c"))
    results = {}
    for abi, target in TARGETS.items():
        folder = output / abi
        folder.mkdir(exist_ok=True)
        library = folder / "libonnxruntime4j_jni.so"
        config = build_dir / abi / "MinSizeRel"
        if "#define USE_NNAPI" in (config / "onnxruntime_config.h").read_text():
            raise ValueError("CPU-only bridge requires a CPU-only core configuration")
        command = [str(toolchain / "bin" / ("clang" + exe)), "--target=" + target,
                   "--sysroot=" + str(toolchain / "sysroot"), "-shared", "-fPIC", "-Oz", "-DNDEBUG", "-std=c11"]
        for include in [headers, source / "include", source / "include/onnxruntime/core/session",
                        source / "orttraining/orttraining/training_api/include", source / "java/src/main/native", config]:
            command.append("-I" + str(include))
        command += ["-Wl,-soname,libonnxruntime4j_jni.so", "-Wl,-z,max-page-size=16384", "-Wl,-z,defs",
                    "-L" + str(config), "-o", str(library)] + [str(p) for p in native] + ["-lonnxruntime", "-llog", "-ldl", "-lm"]
        subprocess.run(command, check=True)
        subprocess.run([str(toolchain / "bin" / ("llvm-strip" + exe)), "--strip-unneeded", str(library)], check=True)
        results[abi] = library
    return results
