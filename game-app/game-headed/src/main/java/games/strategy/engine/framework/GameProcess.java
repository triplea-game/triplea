package games.strategy.engine.framework;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_HOST;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.net.URI;
import java.util.List;
import org.triplea.domain.data.UserName;
import org.triplea.game.client.HeadedApplicationContext;
import org.triplea.lobby.common.GameDescription;

public class GameProcess {

  private static String getMainClassName() {
    return new HeadedApplicationContext().getMainClass().getName();
  }

  /** Spawns a new process to host a network game. */
  public static void hostGame(
      final int port,
      final String playerName,
      final String comments,
      final char[] password,
      final URI lobbyUri) {
    final List<String> commands = ProcessRunnerUtil.createBasicJavaArgs();
    commands.add("-D" + TRIPLEA_SERVER + "=true");
    commands.add("-D" + TRIPLEA_PORT + "=" + port);
    commands.add("-D" + TRIPLEA_NAME + "=" + playerName);
    commands.add("-D" + LOBBY_URI + "=" + lobbyUri);
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
    if (password != null && password.length > 0) {
      commands.add("-D" + SERVER_PASSWORD + "=" + String.valueOf(password));
    }
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.length() > 0) {
      commands.add("-D" + TRIPLEA_GAME + "=" + fileName);
    }
    commands.add(getMainClassName());
    ProcessRunnerUtil.exec(commands);
  }

  /** Spawns a new process to join a network game. */
  public static void joinGame(final GameDescription description, final UserName userName) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING == status) {
      return;
    }

    final List<String> commands = ProcessRunnerUtil.createBasicJavaArgs();
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT + "=true");
    commands.add(prefix + TRIPLEA_PORT + "=" + description.getHostedBy().getPort());
    commands.add(
        prefix + TRIPLEA_HOST + "=" + description.getHostedBy().getAddress().getHostAddress());
    commands.add(prefix + TRIPLEA_NAME + "=" + userName.getValue());
    commands.add(getMainClassName());
    ProcessRunnerUtil.exec(commands);
  }
}
