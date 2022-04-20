package games.strategy.engine.framework.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.startup.ui.panels.main.SetupPanelModel;
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
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.triplea.java.timer.Timers;
import org.triplea.swing.JFrameBuilder;

/** Represents the outermost JFrame, maintains the reference to it and controls access. */
public class MainFrame {

  private static MainFrame instance;

  private final JFrame mainFrame;
  private final MainPanel mainPanel;
  private final List<Runnable> quitActions = new ArrayList<>();

  private MainFrame(
      final SetupPanelModel setupPanelModel, final GameSelectorModel gameSelectorModel) {
    mainFrame =
        JFrameBuilder.builder()
            .title("TripleA")
            .iconImage(EngineImageLoader.loadFrameIcon())
            .windowClosedAction(GameRunner::exitGameIfNoWindowsVisible)
            .build();
    BackgroundTaskRunner.setMainFrame(mainFrame);

    LookAndFeelSwingFrameListener.register(mainFrame);

    final Runnable quitAction =
        () -> {
          quitActions.forEach(Runnable::run);
          mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
        };

    mainPanel = new MainPanelBuilder(quitAction).buildMainPanel(setupPanelModel, gameSelectorModel);
    mainFrame.add(mainPanel);
    mainFrame.pack();

    setupPanelModel.setUi(mainFrame);
  }

  public static void buildMainFrame(
      final SetupPanelModel setupPanelModel, final GameSelectorModel gameSelectorModel) {
    Preconditions.checkState(instance == null);
    instance = new MainFrame(setupPanelModel, gameSelectorModel);
  }

  public static void show() {
    SwingUtilities.invokeLater(
        () -> {
          instance.mainFrame.requestFocus();
          instance.mainFrame.toFront();
          instance.mainFrame.setVisible(true);
        });
  }

  public static void hide() {
    SwingUtilities.invokeLater(() -> instance.mainFrame.setVisible(false));
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
