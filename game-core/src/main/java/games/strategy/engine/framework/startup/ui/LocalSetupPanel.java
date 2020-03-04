package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.HeadedLaunchAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import javax.swing.Action;
import org.triplea.game.startup.SetupModel;
import org.triplea.swing.SwingAction;

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
  public List<Action> getUserActions() {
    return List.of();
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
    return playerTypes.stream().anyMatch(PlayerSelectorRow::isPlayerEnabled);
  }

  @Override
  public void postStartGame() {
    SetupModel.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
  }

  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  @Override
  public void update(final Observable o, final Object arg) {
    SwingAction.invokeNowOrLater(
        () -> layoutPlayerComponents(this, playerTypes, gameSelectorModel.getGameData()));
  }

  @Override
  public Optional<ILauncher> getLauncher() {
    return Optional.of(
        LocalLauncher.create(gameSelectorModel, playerTypes, this, new HeadedLaunchAction(this)));
  }
}
