@echo off
echo Hermes -- Setup

rem Make sure Docker is installed and the daemon is running before we do anything
where docker >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker not found. Install: https://www.docker.com/products/docker-desktop/
    exit /b 1
)

docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker daemon not running. Start Docker Desktop.
    exit /b 1
)

rem Tear down old containers first to avoid port conflicts
docker compose down --remove-orphans 2>nul

rem Build images and start all services in the background
docker compose up --build -d

rem Poll until the app responds — Kafka + MySQL can take a moment to be ready
echo Waiting for app...
set ATTEMPTS=0
:WAIT_LOOP
    set /a ATTEMPTS+=1
    if %ATTEMPTS% GEQ 30 (
        echo [WARN] Timed out. Run: docker compose logs app
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
    curl -s -o nul -w "%%{http_code}" http://localhost:8080/notify 2>nul | findstr /R "^[24]" >nul
    if %ERRORLEVEL% NEQ 0 goto WAIT_LOOP

echo [OK] Running at http://localhost:8080/notify
echo Stop: docker compose down
