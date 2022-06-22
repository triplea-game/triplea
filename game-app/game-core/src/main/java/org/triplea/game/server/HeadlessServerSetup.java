package org.triplea.game.server;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.Optional;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.startup.SetupModel;

/** Server setup model. */
class HeadlessServerSetup implements IRemoteModelListener, SetupModel {
  private final ServerModel model;
  private final GameSelectorModel gameSelectorModel;

  HeadlessServerSetup(final ServerModel model, final GameSelectorModel gameSelectorModel) {
    this.model = model;
    this.gameSelectorModel = gameSelectorModel;
    this.model.setRemoteModelListener(this);
  }

  @Override
  public void cancel() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
  }

  @Override
  public boolean canGameStart() {
    return SetupModel.staticCanGameStart(gameSelectorModel, model);
  }

  @Override
  public void playerListChanged() {}

  @Override
  public void playersTakenChanged() {}

  @Override
  public ChatModel getChatModel() {
    return model.getChatModel();
  }

  ServerModel getModel() {
    return model;
  }

  @Override
  public synchronized Optional<? extends ILauncher> getLauncher() {
    return model.getLauncher();
  }

  @Override
  public void postStartGame() {
    SetupModel.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
  }
}
