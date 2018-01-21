package games.strategy.engine.framework.startup.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LauncherFactory;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.pbem.PBEMMessagePoster;

/** Setup panel when hosting a local game. */
public class LocalSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 2284030734590389060L;
  private final GameSelectorModel gameSelectorModel;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();

  public LocalSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData());
    setupListeners();
    setWidgetActivation();
  }

  private void setupListeners() {
    gameSelectorModel.addObserver(this);
  }

  @Override
  public void setWidgetActivation() {}

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

  @Override
  public boolean canGameStart() {
    if (gameSelectorModel.getGameData() == null) {
      return false;
    }
    // make sure at least 1 player is enabled
    for (final PlayerSelectorRow player : playerTypes) {
      if (player.isPlayerEnabled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void postStartGame() {
    final GameData data = gameSelectorModel.getGameData();
    data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, false);
  }

  @Override
  public void shutDown() {
    gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void update(final Observable o, final Object arg) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData()));
      return;
    }
    layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData());
  }

  @Override
  public ILauncher getLauncher() {
    return LauncherFactory.getLocalLaunchers(gameSelectorModel, playerTypes);
  }

}
