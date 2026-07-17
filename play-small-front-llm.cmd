@echo off
setlocal EnableExtensions
pushd "%~dp0"

set "MODEL=%~1"
if not defined MODEL set "MODEL=qwen3:8b"
set "SEED=%~2"
if not defined SEED set "SEED=1"
set "MAX_ROUNDS=%~3"
if not defined MAX_ROUNDS set "MAX_ROUNDS=12"
set "ROLLOUTS=%~4"
if not defined ROLLOUTS set "ROLLOUTS=1"
set "MAX_DECISIONS=%~5"
if not defined MAX_DECISIONS set "MAX_DECISIONS=2000"

set "SCENARIO=%CD%\game-app\smoke-testing\src\test\resources\map-xmls\Small_Front_Meuse.xml"
set "VENV_PYTHON=%CD%\.venv\Scripts\python.exe"
set "LOG=%CD%\runs\local-llm\small-front-llm.jsonl"

where ollama >nul 2>nul
if errorlevel 1 (
  echo Ollama was not found on PATH.
  echo Install Ollama for Windows, start it, then run this command again.
  popd
  exit /b 1
)

if not exist "%SCENARIO%" (
  echo Small Front scenario not found:
  echo   %SCENARIO%
  popd
  exit /b 1
)

if not exist "%VENV_PYTHON%" (
  echo Creating Python virtual environment...
  py -3.11 -m venv .venv >nul 2>nul
  if errorlevel 1 python -m venv .venv
  if errorlevel 1 (
    echo Failed to create .venv. Install Python 3.11 or newer and retry.
    popd
    exit /b 1
  )
)

"%VENV_PYTHON%" -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)"
if errorlevel 1 (
  echo Small Front local LLM play requires Python 3.11 or newer.
  popd
  exit /b 1
)

echo Installing or refreshing the local Small Front Python package...
"%VENV_PYTHON%" -m pip install -e "python\battle-gym"
if errorlevel 1 (
  echo Failed to install the Python package.
  popd
  exit /b 1
)

ollama show "%MODEL%" >nul 2>nul
if errorlevel 1 (
  echo Pulling Ollama model %MODEL%...
  ollama pull "%MODEL%"
  if errorlevel 1 (
    echo Failed to pull Ollama model %MODEL%.
    popd
    exit /b 1
  )
)

if not exist "runs\local-llm" mkdir "runs\local-llm"

for /f "delims=" %%G in ('git rev-parse --short HEAD 2^>nul') do set "COMMIT=%%G"
echo.
echo Starting purpose-explanation Small Front local LLM self-play
echo Model: %MODEL%
if defined COMMIT echo Source commit: %COMMIT%
echo Seed: %SEED%
echo Max rounds: %MAX_ROUNDS%
echo Default shadow rollouts: %ROLLOUTS%
echo Max decisions: %MAX_DECISIONS%
echo Action facts: exact engine data
echo Commander explanations: operational purpose only
echo Decision limit: graceful stop
echo Log: %LOG%
echo.

"%VENV_PYTHON%" -m triplea_battle_gym.local_llm_agent_purpose ^
  --server-command "cmd /d /c gradlew.bat -q :game-headless:runBattleSimulationServer" ^
  --scenario "%SCENARIO%" ^
  --model "%MODEL%" ^
  --seed %SEED% ^
  --max-actions 4096 ^
  --max-territories 64 ^
  --max-rounds %MAX_ROUNDS% ^
  --simulation-rollouts %ROLLOUTS% ^
  --max-simulation-rollouts 8 ^
  --max-tool-rounds 10 ^
  --max-decisions %MAX_DECISIONS% ^
  --log "%LOG%"
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
