package games.strategy.engine.framework.startup.ui.panels.main;

import java.util.Optional;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.GameSelectorPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;

/**
 * When the game launches, the MainFrame is loaded which will contain
 * the MainPanel. The contents of the MainPanel are swapped out
 * until a new game has been started (TODO: check if the lobby
 * uses mainpanel at all)
 */
public class MainPanelBuilder {


  public MainPanel buildMainPanel(
      SetupPanelModel setupPanelModel,
      GameSelectorModel gameSelectorModel) {
    final GameSelectorPanel gameSelectorPanel = new GameSelectorPanel(gameSelectorModel);

    final MainPanel mainPanel = new MainPanel(
        gameSelectorPanel,
        uiPanel -> {
          setupPanelModel.getPanel().preStartGame();
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
