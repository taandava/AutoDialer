import os
import shutil
import zipfile
import subprocess

ROOT_DIR = os.path.abspath('.')
ASSETS_CODEBASE_DIR = os.path.join(ROOT_DIR, 'app', 'src', 'main', 'assets', 'codebase')
BUILD_TOOLS_DIR = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\build-tools\35.0.0')
APKSIGNER = os.path.join(BUILD_TOOLS_DIR, 'apksigner.bat')
KEYSTORE = os.path.join(ROOT_DIR, 'upload-keystore.jks')
KEY_ALIAS = 'upload'
KEY_PASS = 'pass:autodialer123'
TARGET_APK = os.path.join(ROOT_DIR, 'autodialer-release-signed.apk')

EXCLUDE_DIRS = {
    '.git', '.gradle', 'build', 'app/build', '.system_generated',
    '.user_uploaded', 'assets', 'codebase', 'app/src/main/assets'
}
EXCLUDE_EXTS = {
    '.apk', '.jks', '.idsig', '.bin', '.tmp'
}
EXCLUDE_FILES = {
    'local.properties'
}

def collect_source_files():
    if os.path.exists(ASSETS_CODEBASE_DIR):
        shutil.rmtree(ASSETS_CODEBASE_DIR)
    source_files = []
    for root, dirs, files in os.walk(ROOT_DIR):
        rel_root = os.path.relpath(root, ROOT_DIR).replace('\\', '/')
        if any(rel_root == ex or rel_root.startswith(ex + '/') for ex in EXCLUDE_DIRS):
            continue
        for f in files:
            if f in EXCLUDE_FILES or any(f.endswith(ext) for ext in EXCLUDE_EXTS):
                continue
            rel_file = os.path.join(rel_root, f).replace('\\', '/')
            if rel_file.startswith('./'):
                rel_file = rel_file[2:]
            source_files.append(rel_file)
    return source_files

def prepare_assets_codebase(source_files):
    if os.path.exists(ASSETS_CODEBASE_DIR):
        shutil.rmtree(ASSETS_CODEBASE_DIR)
    os.makedirs(ASSETS_CODEBASE_DIR, exist_ok=True)

    print(f"[1/6] Copying {len(source_files)} clean source files to assets/codebase...")
    for sf in source_files:
        src = os.path.join(ROOT_DIR, sf)
        dst = os.path.join(ASSETS_CODEBASE_DIR, sf)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)

    # Add guide
    guide_path = os.path.join(ASSETS_CODEBASE_DIR, 'HOW_TO_BUILD_AND_UPDATE.md')
    with open(guide_path, 'w', encoding='utf-8') as f:
        f.write("""# How to Build and Update Auto Dialer from Extracted Codebase

This folder contains the complete, buildable native Android project source code for **Auto Dialer**.

## Quick Start Guide

1. **Extract this folder** to any directory on your computer (e.g. `C:\\Projects\\AutoDialer` or `~/AutoDialer`).
2. **Open in Android Studio**:
   - File -> Open -> Select this project directory.
   - Android Studio will automatically sync Gradle and download dependencies.
3. **Build from Terminal**:
   - Windows:
     ```powershell
     .\\gradlew.bat assembleRelease
     ```
   - macOS / Linux:
     ```bash
     ./gradlew assembleRelease
     ```
4. **Run Unit Tests**:
   ```powershell
   .\\gradlew.bat testDebugUnitTest
   ```
5. **Signing & Installation**:
   - Generated APK will be in `app/build/outputs/apk/release/app-release-unsigned.apk`.
   - Sign with standard `apksigner` and install with `adb install -r <apk_name>`.

---
Generated directly inside `autodialer-release-signed.apk` for portable codebase extraction.
""")
    print("[1/6] assets/codebase prepared successfully.")

def build_release_apk():
    print("[2/6] Building release APK with Gradle...")
    env = os.environ.copy()
    env['JAVA_HOME'] = r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
    cmd = [os.path.join(ROOT_DIR, 'gradlew.bat'), 'assembleRelease']
    res = subprocess.run(cmd, cwd=ROOT_DIR, env=env, capture_output=True, text=True)
    if res.returncode != 0:
        print("Gradle assembleRelease FAILED:")
        print(res.stdout)
        print(res.stderr)
        raise RuntimeError("Gradle build failed")
    print("[2/6] Gradle assembleRelease succeeded.")

def package_and_sign(source_files):
    built_apk = os.path.join(ROOT_DIR, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk')
    print(f"[3/6] Packaging root 'codebase/' directory into APK from {built_apk}...")
    shutil.copy2(built_apk, TARGET_APK)

    # Inject root codebase/ folder into the APK zip
    with zipfile.ZipFile(TARGET_APK, 'a', compression=zipfile.ZIP_DEFLATED) as z:
        for sf in source_files:
            file_on_disk = os.path.join(ROOT_DIR, sf)
            zip_path = f"codebase/{sf}"
            z.write(file_on_disk, zip_path)
        
        # Add HOW_TO_BUILD_AND_UPDATE.md
        guide_path = os.path.join(ASSETS_CODEBASE_DIR, 'HOW_TO_BUILD_AND_UPDATE.md')
        z.write(guide_path, "codebase/HOW_TO_BUILD_AND_UPDATE.md")

    print("[3/6] Root 'codebase/' packaged into APK.")

    print("[4/6] Signing APK with apksigner (v2 & v3 schemes)...")
    env = os.environ.copy()
    env['JAVA_HOME'] = r'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
    sign_cmd = [
        APKSIGNER, 'sign',
        '--ks', KEYSTORE,
        '--ks-key-alias', KEY_ALIAS,
        '--ks-pass', KEY_PASS,
        '--key-pass', KEY_PASS,
        TARGET_APK
    ]
    res = subprocess.run(sign_cmd, env=env, capture_output=True, text=True)
    if res.returncode != 0:
        print("apksigner sign FAILED:", res.stderr)
        raise RuntimeError("Signing failed")

    verify_cmd = [APKSIGNER, 'verify', '--verbose', TARGET_APK]
    res_verify = subprocess.run(verify_cmd, env=env, capture_output=True, text=True)
    print("[4/6] Verification output:\n", res_verify.stdout)

def verify_extraction():
    print("[5/6] Verifying APK extraction contents...")
    with zipfile.ZipFile(TARGET_APK, 'r') as z:
        names = z.namelist()
        codebase_entries = [n for n in names if n.startswith('codebase/')]
        assets_codebase_entries = [n for n in names if n.startswith('assets/codebase/')]
        print(f"  Found {len(codebase_entries)} entries in 'codebase/'")
        print(f"  Found {len(assets_codebase_entries)} entries in 'assets/codebase/'")
        assert len(codebase_entries) > 50, "codebase entries missing!"
        assert len(assets_codebase_entries) > 50, "assets/codebase entries missing!"
    print("[5/6] Verification of extraction succeeded!")

def install_and_launch():
    print("[6/6] Installing on connected device R9ZW105MFDR...")
    res = subprocess.run(['adb', '-s', 'R9ZW105MFDR', 'install', '-r', TARGET_APK], capture_output=True, text=True)
    print("Install output:", res.stdout, res.stderr)
    assert 'Success' in res.stdout, "Install failed!"

    print("Launching MainActivity on device...")
    res_launch = subprocess.run([
        'adb', '-s', 'R9ZW105MFDR', 'shell', 'am', 'start', '-W',
        '-n', 'com.autodialer.app/.MainActivity'
    ], capture_output=True, text=True)
    print("Launch output:\n", res_launch.stdout)
    assert 'Complete' in res_launch.stdout, "Launch failed!"
    print("[6/6] Successfully installed and launched on device!")

if __name__ == '__main__':
    files = collect_source_files()
    prepare_assets_codebase(files)
    build_release_apk()
    package_and_sign(files)
    verify_extraction()
    install_and_launch()
    print("ALL STEPS COMPLETED SUCCESSFULLY!")
