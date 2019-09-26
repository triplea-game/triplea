package org.triplea.game.server;

import static games.strategy.engine.framework.CliProperties.LOBBY_HOST;
import static games.strategy.engine.framework.CliProperties.LOBBY_PORT;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.LocalServerAvailabilityCheck;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.java.Log;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.Interruptibles;

/** Server setup model. */
@Log
class HeadlessServerSetup implements IRemoteModelListener, SetupModel {
  private final List<Observer> listeners = new CopyOnWriteArrayList<>();
  private final ServerModel model;
  private final GameSelectorModel gameSelectorModel;
  private final InGameLobbyWatcherWrapper lobbyWatcher = new InGameLobbyWatcherWrapper();

  HeadlessServerSetup(final ServerModel model, final GameSelectorModel gameSelectorModel) {
    this.model = model;
    this.gameSelectorModel = gameSelectorModel;
    this.model.setRemoteModelListener(this);
    createLobbyWatcher();
    internalPlayerListChanged();
  }

  private void createLobbyWatcher() {
    final InGameLobbyWatcher watcher =
        InGameLobbyWatcher.newInGameLobbyWatcher(
                model.getMessenger(), lobbyWatcher.getInGameLobbyWatcher())
            .orElseThrow(CouldNotConnectToLobby::new);

    lobbyWatcher.setInGameLobbyWatcher(watcher);
    lobbyWatcher.setGameSelectorModel(gameSelectorModel);
    LocalServerAvailabilityCheck.builder()
        .localNode(model.getMessenger().getLocalNode())
        .controller(watcher.getRemoteMessenger().getLobbyGameController())
        .errorHandler(log::severe)
        .build()
        .run();
  }

  private static class CouldNotConnectToLobby extends RuntimeException {
    private static final long serialVersionUID = -5946931858867131622L;

    CouldNotConnectToLobby() {
      super(
          String.format(
              "Unable to connect to lobby at: %s, port: %s",
              System.getProperty(LOBBY_HOST), System.getProperty(LOBBY_PORT)));
    }
  }

  synchronized void repostLobbyWatcher() {
    lobbyWatcher.shutDown();
    Interruptibles.sleep(3000);
    createLobbyWatcher();
  }

  @Override
  public void cancel() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
    lobbyWatcher.shutDown();
  }

  @Override
  public boolean canGameStart() {
    if (gameSelectorModel.getGameData() == null || model == null) {
      return false;
    }
    final Map<String, String> players = model.getPlayersToNodeListing();
    if (players == null || players.isEmpty() || players.containsValue(null)) {
      return false;
    }
    // make sure at least 1 player is enabled
    return model.getPlayersEnabledListing().containsValue(Boolean.TRUE);
  }

  @Override
  public void playerListChanged() {
    internalPlayerListChanged();
  }

  @Override
  public void playersTakenChanged() {
    internalPlayersTakenChanged();
  }

  private void internalPlayersTakenChanged() {
    notifyObservers();
  }

  private void internalPlayerListChanged() {
    internalPlayersTakenChanged();
  }

  @Override
  public ChatModel getChatModel() {
    return model.getChatModel();
  }

  ServerModel getModel() {
    return model;
  }

  @Override
  public synchronized Optional<ILauncher> getLauncher() {
    return model
        .getLauncher()
        .map(
            launcher -> {
              launcher.setInGameLobbyWatcher(lobbyWatcher);
              return launcher;
            });
  }

  @Override
  public void addObserver(final Observer observer) {
    listeners.add(observer);
  }

  @Override
  public void notifyObservers() {
    for (final Observer observer : listeners) {
      observer.update(null, null);
    }
  }

  @Override
  public void postStartGame() {
    SetupModel.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
  }
}
