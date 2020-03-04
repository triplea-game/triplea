package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.panels.main.MainPanelBuilder;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.triplea.swing.JFrameBuilder;

public class MainFrame {

  private static JFrame mainFrame;

  public MainFrame(SetupPanelModel setupPanelModel, GameSelectorModel gameSelectorModel) {
    mainFrame =
        JFrameBuilder.builder()
            .title("TripleA")
            .windowClosedAction(GameRunner::exitGameIfFinished)
            .build();
    BackgroundTaskRunner.setMainFrame(mainFrame);


    LookAndFeelSwingFrameListener.register(mainFrame);

    mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    mainFrame.add(new MainPanelBuilder().buildMainPanel(setupPanelModel, gameSelectorModel));
    mainFrame.pack();

    setupPanelModel.setUi(mainFrame);

    SwingUtilities.invokeLater(
        () -> {
          mainFrame.requestFocus();
          mainFrame.toFront();
          mainFrame.setVisible(true);
        });
  }
}
