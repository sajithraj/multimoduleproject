@echo off
REM LocalStack Helper Script for Windows
REM Automates LocalStack setup and testing

setlocal enabledelayedexpansion

REM Configuration
set ENDPOINT_URL=http://localhost:4566
set LAMBDA_FUNCTION_NAME=my-token-auth-lambda
set LAMBDA_HANDLER=org.example.Main::handleRequest
set LAMBDA_RUNTIME=java21
set LAMBDA_MEMORY=512
set LAMBDA_TIMEOUT=60
set IAM_ROLE=lambda-execution-role
set IAM_ROLE_ARN=arn:aws:iam::000000000000:role/%IAM_ROLE%
set SECRET_NAME=external-api/token

REM AWS Credentials
set AWS_ACCESS_KEY_ID=test
set AWS_SECRET_ACCESS_KEY=test
set AWS_DEFAULT_REGION=us-east-1

:menu
cls
echo.
echo ========================================
echo LocalStack Helper Script (Windows)
echo ========================================
echo.
if "%1"=="" (
    echo Commands:
    echo.
    echo   localstack-helper.bat start    - Start LocalStack
    echo   localstack-helper.bat stop     - Stop LocalStack
    echo   localstack-helper.bat status   - Check status
    echo   localstack-helper.bat build    - Build JAR
    echo   localstack-helper.bat setup    - Create/update Lambda
    echo   localstack-helper.bat invoke   - Invoke Lambda
    echo   localstack-helper.bat logs     - View logs
    echo   localstack-helper.bat secrets  - View secrets
    echo   localstack-helper.bat full     - Full setup
    echo   localstack-helper.bat clean    - Cleanup
    echo.
    exit /b 0
)

goto %1

:start
echo [*] Starting LocalStack...
docker-compose up -d
echo [*] Waiting for LocalStack to be ready...
timeout /t 30 /nobreak
echo [+] LocalStack started successfully
goto end

:stop
echo [*] Stopping LocalStack...
docker-compose down
echo [+] LocalStack stopped
goto end

:status
echo [*] Checking LocalStack status...
curl -s %ENDPOINT_URL% >nul
if errorlevel 1 (
    echo [-] LocalStack is NOT running
    exit /b 1
) else (
    echo [+] LocalStack is running
)
goto end

:build
echo [*] Building JAR with Maven...
call mvn clean install -q
if errorlevel 1 (
    echo [-] Build failed
    exit /b 1
)
echo [+] JAR built successfully
goto end

:setup
call :status
if errorlevel 1 exit /b 1

echo [*] Creating Lambda function...

REM Check if function exists
aws lambda get-function ^
  --function-name %LAMBDA_FUNCTION_NAME% ^
  --endpoint-url %ENDPOINT_URL% >nul 2>&1

if errorlevel 1 (
    echo [*] Creating new function...
    aws lambda create-function ^
      --function-name %LAMBDA_FUNCTION_NAME% ^
      --runtime %LAMBDA_RUNTIME% ^
      --role %IAM_ROLE_ARN% ^
      --handler %LAMBDA_HANDLER% ^
      --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar ^
      --environment Variables=^"{ ^
        EXTERNAL_API_URL=https://exchange-staging.motiveintegrator.com, ^
        TOKEN_ENDPOINT_URL=https://exchange-staging.motiveintegrator.com/v1/authorize/token, ^
        CLIENT_ID=test-client-id, ^
        CLIENT_SECRET=test-client-secret, ^
        TOKEN_SECRET_NAME=%SECRET_NAME% ^
      }^" ^
      --timeout %LAMBDA_TIMEOUT% ^
      --memory-size %LAMBDA_MEMORY% ^
      --endpoint-url %ENDPOINT_URL% >nul
) else (
    echo [*] Updating existing function...
    aws lambda update-function-code ^
      --function-name %LAMBDA_FUNCTION_NAME% ^
      --zip-file fileb://target/SetUpProject-1.0-SNAPSHOT.jar ^
      --endpoint-url %ENDPOINT_URL% >nul
)

echo [+] Lambda function created/updated
goto end

:invoke
call :status
if errorlevel 1 exit /b 1

echo [*] Invoking Lambda function...

if "%2"=="" (
    set PAYLOAD={}
) else (
    set PAYLOAD=%2
)

echo [*] Payload: %PAYLOAD%

aws lambda invoke ^
  --function-name %LAMBDA_FUNCTION_NAME% ^
  --payload "%PAYLOAD%" ^
  --endpoint-url %ENDPOINT_URL% ^
  response.json >nul

echo [+] Lambda invoked successfully
echo.
echo Response:
type response.json
echo.
goto end

:logs
call :status
if errorlevel 1 exit /b 1

echo [*] Fetching CloudWatch logs...
aws logs tail /aws/lambda/%LAMBDA_FUNCTION_NAME% ^
  --follow ^
  --endpoint-url %ENDPOINT_URL%

goto end

:secrets
call :status
if errorlevel 1 exit /b 1

echo [*] Listing Secrets...
aws secretsmanager list-secrets ^
  --endpoint-url %ENDPOINT_URL%

echo.
echo [*] Getting secret: %SECRET_NAME%
aws secretsmanager get-secret-value ^
  --secret-id %SECRET_NAME% ^
  --endpoint-url %ENDPOINT_URL%

goto end

:full
echo [*] Starting full setup...
echo.
call :start
call :status
if errorlevel 1 exit /b 1
echo.
call :build
echo.
call :setup
echo.
echo [+] Setup complete!
echo.
echo Next steps:
echo   1. Test token auth: localstack-helper.bat invoke "{\"action\":\"token\"}"
echo   2. View logs: localstack-helper.bat logs
echo   3. View secrets: localstack-helper.bat secrets
echo   4. Stop: localstack-helper.bat stop
goto end

:clean
echo [*] Cleaning up all containers and volumes...
docker-compose down -v
echo [+] Cleaned successfully
goto end

:end
exit /b 0

