package games.strategy.engine.framework.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.swing.JFrameBuilder;

public class MainFrame {

  private JFrame mainFrame;

  private static MainFrame instance;

  public static void buildMainFrame(
      final SetupPanelModel setupPanelModel, final GameSelectorModel gameSelectorModel) {
    Preconditions.checkState(instance == null);
    instance = new MainFrame(setupPanelModel, gameSelectorModel);
  }

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
        () -> mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));

    mainFrame.add(
        new MainPanelBuilder(quitAction).buildMainPanel(setupPanelModel, gameSelectorModel));
    mainFrame.pack();

    setupPanelModel.setUi(mainFrame);
    show();

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
}
