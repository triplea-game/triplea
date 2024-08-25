package games.strategy.engine.framework.startup.ui.panels.main;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import games.strategy.engine.framework.ui.background.WaitWindow;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.ThreadRunner;

/**
 * Can be used to create a {@link MainPanel} UI component class, which is a UI holder. The final
 * contents are added to {@link MainPanel} and we set up listeners so that we can change screens by
 * swapping the contents rendered by {@link MainPanel}.
 */
@AllArgsConstructor
public class MainPanelBuilder {

  private final Runnable quitAction;

  /** Creates a MainPanel instance and configures screen transition listeners. */
  public MainPanel buildMainPanel(
      final HeadedServerSetupModel headedServerSetupModel,
      final GameSelectorModel gameSelectorModel) {
    final GameSelectorPanel gameSelectorPanel = new GameSelectorPanel(gameSelectorModel);
    gameSelectorModel.addObserver(gameSelectorPanel);

    final MainPanel mainPanel =
        new MainPanel(
            quitAction,
            gameSelectorPanel,
            getMainPanelLaunchAction(headedServerSetupModel),
            Optional.ofNullable(headedServerSetupModel.getPanel())
                .map(SetupModel::getChatModel)
                .orElse(null),
            headedServerSetupModel::showSelectType);
    headedServerSetupModel.setPanelChangeListener(mainPanel::setSetupPanel);
    gameSelectorModel.addObserver((observable, arg) -> mainPanel.updatePlayButtonState());
    return mainPanel;
  }

  public static @NotNull Consumer<MainPanel> getMainPanelLaunchAction(
      HeadedServerSetupModel headedServerSetupModel) {
    return uiPanel -> {
      final var setupPanel = headedServerSetupModel.getPanel();
      setupPanel.getLauncher().ifPresent(launcher -> launch(uiPanel, launcher));
      setupPanel.postStartGame();
    };
  }

  private static void launch(MainPanel uiPanel, ILauncher launcher) {
    final WaitWindow gameLoadingWindow = new WaitWindow();
    final var frame = JOptionPane.getFrameForComponent(uiPanel);
    gameLoadingWindow.setLocationRelativeTo(frame);
    gameLoadingWindow.setVisible(true);
    gameLoadingWindow.showWait();
    frame.setVisible(false);
    ThreadRunner.runInNewThread(
        () -> {
          try {
            launcher.launch();
          } finally {
            gameLoadingWindow.doneWait();
          }
        });
  }
}
