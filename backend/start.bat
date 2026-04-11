@echo off
setlocal

set JAVA_HOME=%USERPROFILE%\.jdks\azul-17.0.7
set PROFILE=%1

REM .env 파일 로드
if exist .env (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        if not "%%A"=="" set "%%A=%%B"
    )
)

if "%PROFILE%"=="" (
    echo Usage: start.bat [dev^|prod^|local]
    echo Example: start.bat dev
    exit /b 1
)

echo ================================================
echo  ESM Server Starting - Profile: %PROFILE%
echo  JAVA_HOME: %JAVA_HOME%
echo ================================================

call gradlew.bat bootJar --no-daemon
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo.
echo ================================================
echo  Swagger UI : http://localhost:7092/swagger-ui/index.html
echo  API Docs   : http://localhost:7092/v3/api-docs
echo  Health     : http://localhost:7092/actuator/health
echo ------------------------------------------------
echo  SES API    : http://localhost:7092/ses
echo  Scheduler  : http://localhost:7092/scheduler
echo  Tenant     : http://localhost:7092/tenant
echo ================================================
echo.

java -Dspring.profiles.active=%PROFILE% -jar build\libs\ems.jar
