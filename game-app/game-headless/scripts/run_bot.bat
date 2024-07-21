@ECHO OFF
REM
REM This is a sample batch file for running a TripleA bot on a Windows system.
REM
REM THIS SCRIPT MAY NOT RUN CORRECTLY UNLESS YOU CUSTOMIZE SOME OF THE
REM VARIABLES BELOW!  Please read each variable description carefully and
REM modify them, if appropriate.
REM
REM To run this script, change to the directory in which you unpacked the
REM headless game server archive, and type "run_bot.bat".
REM

REM ###########################################################################
REM VARIABLES THAT YOU MAY CUSTOMIZE BEGIN HERE
REM ###########################################################################

REM
REM The folder from which the bot will load game maps.
REM
REM The default value is the folder in which the TripleA game client stores
REM maps that it downloads.  Under normal circumstances, you will not have to
REM change this variable.  However, if you customized the settings in your
REM TripleA client to use a different maps folder, you should change this
REM variable to refer to the same location.
REM
SET MAPS_FOLDER=%USERPROFILE%\triplea\downloadedMaps

REM
REM The port on which the bot will listen for connections.  This port must be
REM reachable from the Internet, so you may have to configure your firewall
REM appropriately.
REM
REM Under normal circumstances, you will not have to change this variable.
REM However, if you choose to run multiple bots on your system, you need to
REM select a different available port for each bot.
REM
SET BOT_PORT=3300

REM
REM The name of the bot as displayed in the lobby.  Each bot must have a unique
REM name.
REM
REM The default value uses a combination of your username and the local port
REM you selected above on which the bot will run.  Under normal circumstances,
REM you will not have to change this variable.
REM
SET BOT_NAME=Bot_%USERNAME%_%BOT_PORT%

REM
REM The password used to secure access to games running on your bot.  Users
REM attempting to start a new game or connect to an existing game on your bot
REM will be prompted for this password.  If the password is an empty string,
REM the user will not be prompted, and any lobby user will be able to use your
REM bot.
REM
REM The default value is an empty password. You should change this to a
REM non-empty string if you do not want arbitrary users to use your bot.  For
REM example, you may wish to run a private game for you and some friends.  In
REM that case, set the password to a non-empty string and communicate it to
REM your friends in some manner (e.g. via email).
REM
SET BOT_PASSWORD=

REM
REM The hostname of the lobby to which the bot will connect.
REM
REM The default value is the hostname for the TripleA community's public lobby.
REM Under normal circumstances, you will not have to change this variable.
REM
SET LOBBY_URI=https://prod.triplea-game.org


REM
REM Whether to shut down the bot when a game is over.
REM
REM This works well (set it to true) if the bot is installed as a service
REM that is automatically restarted.
REM
SET EXIT_ON_GAME_END LOBBY_URI=false


REM ###########################################################################
REM VARIABLES THAT YOU MAY CUSTOMIZE END HERE
REM
REM DO NOT MODIFY ANYTHING BELOW THIS LINE!
REM ###########################################################################

java^
 -server^
 -Xmx256M^
 -Djava.awt.headless=true^
 -jar bin\triplea-game-headless-@version@.jar^
 -Ptriplea.lobby.uri=%LOBBY_URI%^
 -Ptriplea.map.folder="%MAPS_FOLDER%"^
 -Ptriplea.name=%BOT_NAME%^
 -Ptriplea.port=%BOT_PORT%^
 -Ptriplea.server.password=%BOT_PASSWORD%^
 -Ptriplea.exit.on.game.end=%EXIT_ON_GAME_END%
