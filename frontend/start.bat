@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set "MODE=%~1"

if "!MODE!"=="" goto :usage
if "!MODE!"=="dev" goto :dev
if "!MODE!"=="prod" goto :prod
if "!MODE!"=="build" goto :build
if "!MODE!"=="preview" goto :preview
goto :usage

:usage
echo Usage: start.bat [dev/prod/build/preview]
echo.
echo   dev      Start dev server (HMR)
echo   prod     Production build + preview
echo   build    Production build only
echo   preview  Preview built output
exit /b 1

:dev
echo ================================================
echo  Joins EMS Frontend - Mode: dev
echo ================================================
echo  App      : http://localhost:5173
echo  API Proxy: http://localhost:7092
echo ================================================
echo.
npx vite --host
goto :eof

:prod
echo ================================================
echo  Joins EMS Frontend - Mode: prod
echo ================================================
echo  Building for production...
npx vite build
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)
echo.
echo  Build complete. Starting preview server...
echo  App: http://localhost:4173
echo ================================================
echo.
npx vite preview --host
goto :eof

:build
echo ================================================
echo  Joins EMS Frontend - Mode: build
echo ================================================
echo  Building for production...
npx vite build
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)
echo.
echo  Build output: dist/
echo ================================================
goto :eof

:preview
echo ================================================
echo  Joins EMS Frontend - Mode: preview
echo ================================================
echo  App: http://localhost:4173
echo ================================================
echo.
npx vite preview --host
goto :eof
