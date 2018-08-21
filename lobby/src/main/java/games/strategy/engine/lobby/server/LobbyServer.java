package games.strategy.engine.lobby.server;

import java.io.IOException;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.lobby.common.ILobbyGameBroadcaster;
import games.strategy.engine.lobby.common.LobbyConstants;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;

/**
 * A lobby server.
 *
 * <p>
 * A lobby server provides the following functionality:
 * </p>
 * <ul>
 * <li>A registry of servers available to host games.</li>
 * <li>A room where players can find opponents and generally chat.</li>
 * </ul>
 */
public final class LobbyServer {
  private LobbyServer() {}

  /**
   * Starts a new lobby server using the properties given by {@code lobbyPropertyReader}.
   *
   * <p>
   * This method returns immediately after the lobby server is started; it does not block while the lobby server is
   * running.
   * </p>
   */
  static void start(final LobbyPropertyReader lobbyPropertyReader) throws IOException {
    ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);

    final IServerMessenger server =
        ServerMessenger.newInstanceForLobby(LobbyConstants.ADMIN_USERNAME, lobbyPropertyReader);
    final Messengers messengers = new Messengers(server);
    server.setLoginValidator(new LobbyLoginValidator(lobbyPropertyReader));
    // setup common objects
    new UserManager(lobbyPropertyReader).register(messengers.getRemoteMessenger());
    final ModeratorController moderatorController = new ModeratorController(server, messengers, lobbyPropertyReader);
    moderatorController.register(messengers.getRemoteMessenger());
    new ChatController(LobbyConstants.LOBBY_CHAT, messengers, moderatorController::isPlayerAdmin);

    // register the status controller
    new StatusManager(messengers).shutDown();

    final LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) messengers
        .getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.REMOTE_NAME), server);
    controller.register(messengers.getRemoteMessenger());

    // now we are open for business
    server.setAcceptNewConnections(true);
  }
}
