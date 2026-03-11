@echo off
REM ─── DirBrute Build Script (Windows) ──────────────────────────
echo [*] Compiling DirBrute...
mkdir out 2>nul

javac -d out src\main\java\com\dirbrute\DirBrute.java

if %errorlevel% neq 0 (
    echo [!] Compilation failed.
    exit /b 1
)

echo [*] Packaging JAR...
jar cfm DirBrute.jar manifest.txt -C out .

echo.
echo [+] Build successful! Run with:
echo     java -jar DirBrute.jar ^<url^> ^<wordlist^> [threads] [timeout_ms]
echo.
echo Example:
echo     java -jar DirBrute.jar http://example.com wordlists/common.txt 20 3000
