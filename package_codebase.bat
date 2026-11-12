@echo off
setlocal
echo Packaging complete codebase into AutoDialer APK...
python package_codebase_to_apk.py
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] APK created with embedded codebase at autodialer-release-signed.apk
) else (
    echo [ERROR] Packaging failed with exit code %ERRORLEVEL%
)
endlocal
