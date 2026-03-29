@echo off
:: ─────────────────────────────────────────────────────────────
::  Hermes — Notification Engine  |  Setup Script (Windows)
:: ─────────────────────────────────────────────────────────────

echo.
echo  Hermes -- Notification Engine Setup
echo  ------------------------------------

:: Check Docker is installed
where docker >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] Docker is not installed. Please install Docker Desktop.
    echo          https://www.docker.com/products/docker-desktop/
    exit /b 1
)

:: Check Docker daemon is running
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] Docker daemon is not running. Please start Docker Desktop.
    exit /b 1
)

echo  [OK] Docker found
echo.

:: Stop any existing containers
echo  [*] Stopping any existing containers...
docker compose down --remove-orphans 2>nul || docker-compose down --remove-orphans 2>nul

:: Build and start
echo.
echo  [*] Building and starting services (MySQL, Redis, Kafka, App)...
echo      This may take a few minutes on first run.
echo.

docker compose up --build -d
if %ERRORLEVEL% NEQ 0 (
    docker-compose up --build -d
)

:: Wait for app to respond
echo.
echo  [*] Waiting for app to start...
set ATTEMPTS=0
:WAIT_LOOP
    set /a ATTEMPTS+=1
    if %ATTEMPTS% GEQ 30 (
        echo.
        echo  [WARN] App did not start in expected time. Check logs with:
        echo         docker compose logs app
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
    curl -s -o nul -w "%%{http_code}" http://localhost:8080/notify 2>nul | findstr /R "^[24]" >nul
    if %ERRORLEVEL% NEQ 0 goto WAIT_LOOP

echo.
echo  ------------------------------------
echo  [OK] Hermes is running!
echo.
echo   API:   http://localhost:8080/notify
echo.
echo   Try it (PowerShell):
echo   Invoke-RestMethod -Method POST -Uri http://localhost:8080/notify ^
echo     -ContentType "application/json" ^
echo     -Body '{"tenantId":"tenant1","eventType":"ORDER_PLACED","referenceId":"order-001","payload":{"email":"user@example.com"}}'
echo.
echo   Logs:  docker compose logs -f app
echo   Stop:  docker compose down
echo  ------------------------------------
