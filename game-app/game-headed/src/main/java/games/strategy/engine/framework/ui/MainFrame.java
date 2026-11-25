package games.strategy.engine.framework.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.ui.panels.main.HeadedServerSetupModel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.EngineImageLoader;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.triplea.game.client.HeadedGameRunner;
import org.triplea.java.timer.Timers;
import org.triplea.swing.JFrameBuilder;

/** Represents the outermost JFrame, maintains the reference to it and controls access. */
public class MainFrame {

  private static MainFrame instance;

  private final JFrame mainJFrame;
  private final MainPanel mainPanel;
  private final List<Runnable> quitActions = new ArrayList<>();

  private MainFrame(
      final HeadedServerSetupModel headedServerSetupModel,
      final GameSelectorModel gameSelectorModel) {
    mainJFrame =
        JFrameBuilder.builder()
            .title("TripleA")
            .iconImage(EngineImageLoader.loadFrameIcon())
            .windowClosedAction(HeadedGameRunner::exitGameIfNoWindowsVisible)
            .build();
    BackgroundTaskRunner.setMainFrame(mainJFrame);

    LookAndFeelSwingFrameListener.register(mainJFrame);

    final Runnable quitAction =
        () -> {
          quitActions.forEach(Runnable::run);
          mainJFrame.dispatchEvent(new WindowEvent(mainJFrame, WindowEvent.WINDOW_CLOSING));
        };

    mainPanel =
        new MainPanelBuilder(quitAction).buildMainPanel(headedServerSetupModel, gameSelectorModel);
    mainJFrame.add(mainPanel);
    mainJFrame.pack();

    headedServerSetupModel.setUi(mainJFrame);
  }

  public static void buildMainFrame(
      final HeadedServerSetupModel headedServerSetupModel,
      final GameSelectorModel gameSelectorModel) {
    Preconditions.checkState(instance == null);
    instance = new MainFrame(headedServerSetupModel, gameSelectorModel);
  }

  public static void show() {
    SwingUtilities.invokeLater(
        () -> {
          instance.mainJFrame.requestFocus();
          instance.mainJFrame.toFront();
          instance.mainJFrame.setLocationRelativeTo(null); // center on screen
          instance.mainJFrame.setVisible(true);
        });
  }

  public static void startGameDirectly(final HeadedServerSetupModel headedServerSetupModel) {
    Consumer<MainPanel> mainPanelLaunchAction =
        MainPanelBuilder.getMainPanelLaunchAction(headedServerSetupModel);
    SwingUtilities.invokeLater(() -> mainPanelLaunchAction.accept(instance.mainPanel));
  }

  public static void hide() {
    SwingUtilities.invokeLater(() -> instance.mainJFrame.setVisible(false));
  }

  public static void addQuitAction(final Runnable onQuitAction) {
    instance.quitActions.add(onQuitAction);
  }

  public static void loadSaveFile(final Path file) {
    loadSaveFileImpl(file, Instant.now());
  }

  private static void loadSaveFileImpl(final Path file, final Instant startTime) {
    // This may be called at any time, including during start up.
    SwingUtilities.invokeLater(
        () -> {
          // If the MainPanel is up already, load the save file.
          if (instance != null) {
            instance.mainPanel.loadSaveFile(file);
            return;
          }

          if (Duration.between(startTime, Instant.now()).toSeconds() >= 60) {
            throw new IllegalStateException("MainFrame instance not created in 60s");
          }

          // Otherwise "recurse" with a 100ms delay to try again later.
          Timers.executeAfterDelay(
              100, TimeUnit.MILLISECONDS, () -> loadSaveFileImpl(file, startTime));
        });
  }
}
