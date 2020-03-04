package org.triplea.game.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.game.startup.ServerSetupModel;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;

/** Setup panel model for headless server. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Log
public class HeadlessServerSetupPanelModel implements ServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private HeadlessServerSetup headlessServerSetup;

  @Override
  public void showSelectType() {
    new ServerModel(gameSelectorModel, this, null, new HeadlessLaunchAction(), log::severe);
  }

  @Override
  public void onServerMessengerCreated(
      final ServerModel serverModel, final GameHostingResponse gameHostingResponse) {
    Preconditions.checkNotNull(
        gameHostingResponse, "hosting response is null, did the bot connect to lobby?");
    Preconditions.checkNotNull(System.getProperty(LOBBY_URI));

    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);

    final ClientLoginValidator loginValidator = new ClientLoginValidator();
    loginValidator.setServerMessenger(checkNotNull(serverModel.getMessenger()));
    // TODO: Project#12 Wrap loginValidator here and instead inject headlessLoginValidator
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
