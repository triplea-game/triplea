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
import java.util.ArrayList;
import java.util.List;
import org.triplea.domain.data.UserName;
import org.triplea.game.ApplicationContext;
import org.triplea.lobby.common.GameDescription;
import org.triplea.util.Services;

/**
 * GameRunner - The entrance class with the main method. In this class commonly used constants are
 * getting defined and the Game is being launched
 */
public final class GameRunner {
  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final int PORT = 3300;
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  public static boolean headless() {
    return Boolean.parseBoolean(System.getProperty(TRIPLEA_HEADLESS, "false"));
  }

  /** Spawns a new process to host a network game. */
  public static void hostGame(
      final int port,
      final String playerName,
      final String comments,
      final char[] password,
      final URI lobbyUri) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
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
    commands.add(Services.loadAny(ApplicationContext.class).getMainClass().getName());
    ProcessRunnerUtil.exec(commands);
  }

  /** Spawns a new process to join a network game. */
  public static void joinGame(final GameDescription description, final UserName userName) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING == status) {
      return;
    }

    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT + "=true");
    commands.add(prefix + TRIPLEA_PORT + "=" + description.getHostedBy().getPort());
    commands.add(
        prefix + TRIPLEA_HOST + "=" + description.getHostedBy().getAddress().getHostAddress());
    commands.add(prefix + TRIPLEA_NAME + "=" + userName.getValue());
    commands.add(Services.loadAny(ApplicationContext.class).getMainClass().getName());
    ProcessRunnerUtil.exec(commands);
  }
}
