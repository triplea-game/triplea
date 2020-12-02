package games.strategy.engine.framework.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.startup.ui.panels.main.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.swing.JFrameBuilder;

/** Represents the outermost JFrame, maintains the reference to it and controls access. */
public class MainFrame {

  private static MainFrame instance;

  private JFrame mainFrame;
  private final List<Runnable> quitActions = new ArrayList<>();

  private MainFrame(
      final SetupPanelModel setupPanelModel, final GameSelectorModel gameSelectorModel) {
    mainFrame =
        JFrameBuilder.builder()
            .title("TripleA")
            .windowClosedAction(GameRunner::exitGameIfFinished)
            .build();
    BackgroundTaskRunner.setMainFrame(mainFrame);

    LookAndFeelSwingFrameListener.register(mainFrame);

    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    final Runnable quitAction =
        () -> {
          quitActions.forEach(Runnable::run);
          mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
        };

    mainFrame.add(
        new MainPanelBuilder(quitAction).buildMainPanel(setupPanelModel, gameSelectorModel));
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
}
