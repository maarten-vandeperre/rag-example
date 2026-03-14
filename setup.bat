@echo off
setlocal enabledelayedexpansion

echo === RAG Application Development Environment Setup ===

set JAVA_CMD=java
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set JAVA_CMD=%JAVA_HOME%\bin\java.exe

%JAVA_CMD% -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install Java 17.
    exit /b 1
)

for /f "tokens=3" %%g in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%~g
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "tokens=1 delims=." %%g in ("%JAVA_VERSION%") do set JAVA_MAJOR=%%~g
echo Java version detected: %JAVA_VERSION%
if not "%JAVA_MAJOR%"=="17" (
    echo ERROR: Java 17 is required, but found Java %JAVA_VERSION%
    exit /b 1
)

node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found. Please install Node.js 18+.
    exit /b 1
)

for /f %%i in ('node --version') do set NODE_VERSION=%%i
set NODE_VERSION=%NODE_VERSION:v=%
for /f "tokens=1 delims=." %%i in ("%NODE_VERSION%") do set NODE_MAJOR=%%i
echo Node.js version detected: %NODE_VERSION%
if %NODE_MAJOR% lss 18 (
    echo ERROR: Node.js 18+ is required, but found Node.js %NODE_VERSION%
    exit /b 1
)

echo Running environment health check...
call gradlew.bat healthCheck
if %errorlevel% neq 0 (
    echo Health check failed. Please review the errors above.
    exit /b 1
)

echo Installing frontend dependencies...
call gradlew.bat :frontend:npmInstall
if %errorlevel% neq 0 (
    echo Frontend dependency installation failed. Please review the errors above.
    exit /b 1
)

echo === Setup Complete ===
echo You can now run:
echo   gradlew.bat dev            - Start development environment
echo   gradlew.bat testAll        - Run all tests
echo   gradlew.bat buildWorkspace - Build verified release outputs
endlocal
