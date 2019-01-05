package games.strategy.engine.framework.startup.ui.panels.main;

import java.util.Optional;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;

/**
 * Can be used to create a {@link MainPanel} UI component class, which is a UI holder. The final contents are added to
 * {@link MainPanel} and we set up listeners so that we can change screens by swapping the contents rendered by
 * {@link MainPanel}.
 */
public class MainPanelBuilder {

  /**
   * Creates a MainPanel instance and configures screen transition listeners.
   */
  public MainPanel buildMainPanel(
      final SetupPanelModel setupPanelModel,
      final GameSelectorModel gameSelectorModel) {
    final GameSelectorPanel gameSelectorPanel = new GameSelectorPanel(gameSelectorModel);
    gameSelectorModel.addObserver(gameSelectorPanel);

    final MainPanel mainPanel = new MainPanel(
        gameSelectorPanel,
        uiPanel -> {
          setupPanelModel.getPanel().getLauncher()
              .ifPresent(launcher -> launcher.launch(uiPanel));
          setupPanelModel.getPanel().postStartGame();
        },
        () -> Optional.ofNullable(setupPanelModel.getPanel())
            .map(ISetupPanel::getChatPanel),
        setupPanelModel::showSelectType);
    setupPanelModel.setPanelChangeListener(mainPanel);
    gameSelectorModel.addObserver(mainPanel);
    return mainPanel;
  }
}
