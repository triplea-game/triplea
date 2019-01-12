package games.strategy.engine.framework.startup.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import javax.swing.JComponent;

import org.triplea.game.common.SetupConfiguration;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LauncherFactory;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.ui.SwingAction;

/** Setup panel when hosting a local game. */
public class LocalSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 2284030734590389060L;
  private final GameSelectorModel gameSelectorModel;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();

  public LocalSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData());
    setupListeners();
  }

  private void setupListeners() {
    gameSelectorModel.addObserver(this);
  }

  @Override
  public boolean isCancelButtonVisible() {
    return true;
  }

  @Override
  public boolean canGameStart() {
    if (gameSelectorModel.getGameData() == null) {
      return false;
    }
    // make sure at least 1 player is enabled
    return playerTypes.stream()
        .anyMatch(PlayerSelectorRow::isPlayerEnabled);
  }

  @Override
  public void postStartGame() {
    SetupConfiguration.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
  }

  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void update(final Observable o, final Object arg) {
    SwingAction.invokeNowOrLater(() -> layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData()));
  }

  @Override
  public Optional<ILauncher> getLauncher() {
    return Optional.of(LauncherFactory.getLocalLaunchers(gameSelectorModel, playerTypes));
  }

  @Override
  public JComponent getDrawable() {
    return this;
  }
}
