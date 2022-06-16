package org.triplea.game.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;

import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.game.startup.ServerSetupModel;
import org.triplea.http.client.lobby.game.hosting.request.GameHostingResponse;

/** Setup panel model for headless server. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HeadlessServerSetupModel implements ServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private final HeadlessGameServer headlessGameServer;
  private HeadlessServerSetup headlessServerSetup;

  @Override
  public void showSelectType() {
    new ServerModel(gameSelectorModel, this, new HeadlessLaunchAction(headlessGameServer));
  }

  @Override
  public void onServerMessengerCreated(
      final ServerModel serverModel, final GameHostingResponse gameHostingResponse) {
    checkNotNull(gameHostingResponse, "hosting response is null, did the bot connect to lobby?");
    checkNotNull(System.getProperty(LOBBY_URI));

    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);

    final ClientLoginValidator loginValidator =
        new ClientLoginValidator(ProductVersionReader.getCurrentVersion());
    loginValidator.setServerMessenger(checkNotNull(serverModel.getMessenger()));
    serverModel.getMessenger().setLoginValidator(loginValidator);
    Optional.ofNullable(serverModel.getLobbyWatcherThread())
        .map(LobbyWatcherThread::getLobbyWatcher)
        .ifPresent(lobbyWatcher -> lobbyWatcher.setGameSelectorModel(gameSelectorModel));
    headlessServerSetup = new HeadlessServerSetup(serverModel, gameSelectorModel);
  }

  public HeadlessServerSetup getPanel() {
    return headlessServerSetup;
  }
}
