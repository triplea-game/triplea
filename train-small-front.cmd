@echo off
setlocal EnableExtensions
pushd "%~dp0"

set "TIMESTEPS=%~1"
if not defined TIMESTEPS set "TIMESTEPS=200000"
set "N_ENVS=%~2"
if not defined N_ENVS set "N_ENVS=1"
set "OUTPUT=%~3"
if not defined OUTPUT set "OUTPUT=%CD%\runs\small-front-ppo.zip"
set "CHECKPOINT_EVERY=%~4"
if not defined CHECKPOINT_EVERY set "CHECKPOINT_EVERY=20000"
set "LEARNER_PLAYER=%~5"
if not defined LEARNER_PLAYER set "LEARNER_PLAYER=Germans"

set "SCENARIO=%CD%\game-app\smoke-testing\src\test\resources\map-xmls\Small_Front_Meuse.xml"
set "VENV_PYTHON=%CD%\.venv\Scripts\python.exe"
set "TENSORBOARD_LOG=%CD%\runs\tensorboard"

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
    echo Failed to create .venv. Install Python 3.11 or newer and try again.
    popd
    exit /b 1
  )
)

"%VENV_PYTHON%" -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)"
if errorlevel 1 (
  echo Small Front training requires Python 3.11 or newer.
  popd
  exit /b 1
)

"%VENV_PYTHON%" -c "import sb3_contrib, stable_baselines3, tensorboard, triplea_battle_gym" >nul 2>nul
if errorlevel 1 (
  echo Installing Small Front training dependencies...
  "%VENV_PYTHON%" -m pip install --upgrade pip
  if errorlevel 1 goto :install_failed
  "%VENV_PYTHON%" -m pip install -e "python\battle-gym[train]"
  if errorlevel 1 goto :install_failed
)

REM Refresh the editable source link even when this virtual environment already existed.
"%VENV_PYTHON%" -m pip install -e "python\battle-gym" --no-deps >nul
if errorlevel 1 goto :install_failed

for %%D in ("%OUTPUT%") do if not exist "%%~dpD" mkdir "%%~dpD"
if not exist "%TENSORBOARD_LOG%" mkdir "%TENSORBOARD_LOG%"

for /f "delims=" %%G in ('git rev-parse --short HEAD 2^>nul') do set "COMMIT=%%G"
echo.
echo Starting Small Front Maskable PPO training
echo Learner: %LEARNER_PLAYER%
echo Opponent: deterministic scripted policy
if defined COMMIT echo Source commit: %COMMIT%
echo Timesteps: %TIMESTEPS%
echo Parallel environments: %N_ENVS%
echo Scenario: %SCENARIO%
echo Output: %OUTPUT%
echo TensorBoard: %TENSORBOARD_LOG%
echo.

"%VENV_PYTHON%" -m triplea_battle_gym.strategic_train ^
  --server-command "cmd /d /c gradlew.bat -q :game-headless:runBattleSimulationServer" ^
  --scenario "%SCENARIO%" ^
  --learner-player "%LEARNER_PLAYER%" ^
  --timesteps %TIMESTEPS% ^
  --max-actions 4096 ^
  --max-territories 64 ^
  --max-rounds 12 ^
  --n-envs %N_ENVS% ^
  --checkpoint-every %CHECKPOINT_EVERY% ^
  --tensorboard-log "%TENSORBOARD_LOG%" ^
  --output "%OUTPUT%"
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%

:install_failed
echo Failed to install the Python training dependencies.
popd
exit /b 1
