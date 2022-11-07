package org.triplea.game.server;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.Interruptibles;

/** Server setup model. */
class HeadlessServerSetup implements IRemoteModelListener, SetupModel {
  private final ServerModel model;
  private final GameSelectorModel gameSelectorModel;
  private final ReentrantLock lock;
  private final Condition playersUpdated;

  private boolean cancelled = false;

  HeadlessServerSetup(final ServerModel model, final GameSelectorModel gameSelectorModel) {
    this.model = model;
    this.gameSelectorModel = gameSelectorModel;
    this.model.setRemoteModelListener(this);
    lock = new ReentrantLock();
    playersUpdated = lock.newCondition();
  }

  @Override
  public void cancel() {
    signalPotentialStart(true);
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
  }

  @Override
  public boolean canGameStart() {
    return SetupModel.staticCanGameStart(gameSelectorModel, model);
  }

  @Override
  public void playerListChanged() {
    signalPotentialStart(false);
  }

  @Override
  public void playersTakenChanged() {
    signalPotentialStart(false);
  }

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

  private void signalPotentialStart(boolean cancel) {
    lock.lock();
    try {
      this.cancelled |= cancel;
      playersUpdated.signal();
    } finally {
      lock.unlock();
    }
  }

  public boolean waitUntilStart() {
    lock.lock();
    try {
      while (!canGameStart() && !cancelled) {
        if (!Interruptibles.await(playersUpdated::await)) {
          return false;
        }
      }
      return !cancelled;
    } finally {
      lock.unlock();
    }
  }
}
