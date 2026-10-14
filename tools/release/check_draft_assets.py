"""Verify prepared distributable files before Actions makes the draft public."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

assets = Path(sys.argv[1])
tag = sys.argv[2]
if not re.fullmatch(r"v\d+\.\d+\.\d+", tag):
    raise SystemExit("Invalid release tag")
version = tag[1:]
manifest = json.loads((assets / "ReleaseManifest.json").read_text("utf-8"))
commit = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
if manifest["version"] != version or manifest["source_commit"] != commit:
    raise SystemExit("Prepared APK/source commit does not match the release tag")
expected = {f"Course-Schedule-{version}-{abi}.apk" for abi in
            ("arm64-v8a", "armeabi-v7a", "x86_64", "universal")}
expected |= {f"Course-Schedule-{version}-Source.zip", "ReleaseManifest.json"}
entries = {}
for line in (assets / "SHA256SUMS.txt").read_text("utf-8").splitlines():
    digest, name = line.split("  ", 1)
    if Path(name).name != name or name in entries or not re.fullmatch(r"[0-9a-f]{64}", digest):
        raise SystemExit("Invalid checksum entry")
    entries[name] = digest
if set(entries) != expected or {p.name for p in assets.iterdir()} != expected | {"SHA256SUMS.txt"}:
    raise SystemExit("Release asset set is incomplete or contains unexpected files")
for name, digest in entries.items():
    if hashlib.sha256((assets / name).read_bytes()).hexdigest() != digest:
        raise SystemExit(f"Checksum mismatch: {name}")
print(f"Prepared release {tag} matches commit {commit}")
