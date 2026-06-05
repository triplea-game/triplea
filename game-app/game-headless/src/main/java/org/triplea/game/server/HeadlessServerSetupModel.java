package org.triplea.game.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;

import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/** Setup panel model for headless server. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HeadlessServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private final HeadlessGameServer headlessGameServer;

  public HeadlessServerSetup createHeadlessServerSetup() {
    final ServerModel serverModel =
        new ServerModel(gameSelectorModel, new HeadlessLaunchAction(headlessGameServer));
    checkState(serverModel.initialize(), "failed to initialize bot, did it connect to lobby?");
    return onServerMessengerCreated(serverModel);
  }

  private HeadlessServerSetup onServerMessengerCreated(final ServerModel serverModel) {
    checkNotNull(System.getProperty(LOBBY_URI));

    serverModel
        .getMessenger()
        .setLoginValidator(
            ClientLoginValidator.builder().serverMessenger(serverModel.getMessenger()).build());
    Optional.ofNullable(serverModel.getLobbyWatcherThread())
        .map(LobbyWatcherThread::getLobbyWatcher)
        .ifPresent(lobbyWatcher -> lobbyWatcher.setGameSelectorModel(gameSelectorModel));
    return new HeadlessServerSetup(serverModel, gameSelectorModel);
  }
}
