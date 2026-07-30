@echo off
@REM SPDX-FileCopyrightText: 2026 Your Organization
@REM SPDX-License-Identifier: MIT
setlocal enabledelayedexpansion

@REM --- 1. Walk up to find the directory containing .mvn\ ---
set "START_DIR=%CD%"
set "PROJECT_DIR=%CD%"
:find_mvn_dir
if exist "!PROJECT_DIR!\.mvn\" goto :found_project_dir
for %%P in ("!PROJECT_DIR!\..") do set "PARENT=%%~fP"
if "!PARENT!"=="!PROJECT_DIR!" set "PROJECT_DIR=%START_DIR%" & goto :found_project_dir
set "PROJECT_DIR=!PARENT!"
goto :find_mvn_dir
:found_project_dir

@REM --- 2. Read distributionUrl ---
set "PROPS=!PROJECT_DIR!\.mvn\wrapper\maven-wrapper.properties"
if not exist "!PROPS!" echo ERROR: !PROPS! not found & exit /b 1
set "DIST_URL="
for /f "usebackq tokens=1,* delims==" %%K in ("!PROPS!") do (
  if "%%K"=="distributionUrl" set "DIST_URL=%%L"
)
if "!DIST_URL!"=="" echo ERROR: distributionUrl missing & exit /b 1

@REM --- 3. Derive a safe cache directory name ---
@REM  apache-maven-3.9.9-bin.zip -> apache-maven-3.9.9
for %%F in ("!DIST_URL!") do set "ZIP_NAME=%%~nF"
set "DIST_NAME=!ZIP_NAME:-bin=!"

@REM --- 4. Choose cache dir ---
if "%MAVEN_USER_HOME%"=="" (
  set "WRAPPER_HOME=%USERPROFILE%\.m2\wrapper\dists\!DIST_NAME!"
) else (
  set "WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper\dists\!DIST_NAME!"
)

@REM --- 5. Try to find mvn.cmd (may already be cached from a prior run) ---
set "MVN_CMD="
for /f "delims=" %%F in ('dir /b /s "!WRAPPER_HOME!\mvn.cmd" 2^>nul') do set "MVN_CMD=%%F"

@REM --- 6. Download + extract if not found ---
if "!MVN_CMD!"=="" (
  echo Downloading Maven from !DIST_URL! ...
  if not exist "!WRAPPER_HOME!" mkdir "!WRAPPER_HOME!"
  set "TMP_ZIP=!WRAPPER_HOME!\download.zip"
  powershell -NoProfile -NonInteractive -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '!DIST_URL!' -OutFile '!TMP_ZIP!'"
  if not exist "!TMP_ZIP!" echo ERROR: download failed & exit /b 1
  powershell -NoProfile -NonInteractive -Command "Expand-Archive -LiteralPath '!TMP_ZIP!' -DestinationPath '!WRAPPER_HOME!' -Force"
  del "!TMP_ZIP!"
  @REM Search recursively — the zip creates a nested subdir e.g. apache-maven-3.9.9\
  set "MVN_CMD="
  for /f "delims=" %%F in ('dir /b /s "!WRAPPER_HOME!\mvn.cmd" 2^>nul') do set "MVN_CMD=%%F"
)

if "!MVN_CMD!"=="" echo ERROR: mvn.cmd not found under !WRAPPER_HOME! & exit /b 1

@REM --- 7. Run Maven ---
"!MVN_CMD!" %*
exit /b %ERRORLEVEL%
