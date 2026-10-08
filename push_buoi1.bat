@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
if errorlevel 1 goto fail

set "expectedBranch=feature/2380601519-issue-3-metrics-collector"
set "currentBranch="
for /f "delims=" %%B in ('git branch --show-current') do set "currentBranch=%%B"
if not "%currentBranch%"=="%expectedBranch%" (
    echo ERROR: Switch to %expectedBranch% before running this file.
    goto fail
)

rem Stop if any files are already staged to avoid including unrelated work.
git diff --cached --quiet
if errorlevel 1 (
    echo ERROR: The staging area must be empty before running this file.
    goto fail
)

git add -- "src/serverhealthmonitoring/MetricsCollector.java" "src/serverhealthmonitoring/MetricsCollectorTest.java" "push_buoi1.bat"
if errorlevel 1 goto fail
git commit -m "feat: tạo khung MetricsCollector và file test (#3)" -- "src/serverhealthmonitoring/MetricsCollector.java" "src/serverhealthmonitoring/MetricsCollectorTest.java" "push_buoi1.bat"
if errorlevel 1 goto fail
git push -u origin "%expectedBranch%"
if errorlevel 1 goto fail

echo Session 1 commit and push completed.
pause
exit /b 0

:fail
echo Operation stopped. Check the Git output above before retrying.
pause
exit /b 1
