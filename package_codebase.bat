@echo off
:: ================================================================================================
:: WHAT IS IT:
:: Windows batch script for 1-click execution of the codebase packaging & APK signing pipeline.
::
:: WHAT IT DOES:
:: 1. Invokes 'package_codebase_to_apk.py' via Python.
:: 2. Checks return error level and reports pipeline status (SUCCESS / ERROR).
::
:: WHAT THINGS IT IS LINKED WITH:
:: - Upstream Runner: Windows CLI / Double-click explorer execution
:: - Downstream Tool: package_codebase_to_apk.py
:: ================================================================================================
setlocal
echo Packaging complete codebase into AutoDialer APK...
python package_codebase_to_apk.py
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] APK created with embedded codebase at autodialer-release-signed.apk
) else (
    echo [ERROR] Packaging failed with exit code %ERRORLEVEL%
)
endlocal
