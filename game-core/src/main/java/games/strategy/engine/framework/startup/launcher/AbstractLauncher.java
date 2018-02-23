package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.background.WaitWindow;



/**
 * Abstract class for launching a game.
 */
public abstract class AbstractLauncher implements ILauncher {
  protected final GameData gameData;
  protected final GameSelectorModel gameSelectorModel;
  protected final WaitWindow gameLoadingWindow;
  protected final boolean headless;

  protected AbstractLauncher(final GameSelectorModel gameSelectorModel) {
    this(gameSelectorModel, false);
  }

  protected AbstractLauncher(final GameSelectorModel gameSelectorModel, final boolean headless) {
    this.headless = headless;
    if (this.headless) {
      gameLoadingWindow = null;
    } else {
      gameLoadingWindow = new WaitWindow();
    }
    this.gameSelectorModel = gameSelectorModel;
    gameData = gameSelectorModel.getGameData();
  }

  @Override
  public void launch(final Component parent) {
    if (!headless && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    if (!headless && (gameLoadingWindow != null)) {
      gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
      gameLoadingWindow.setVisible(true);
      gameLoadingWindow.showWait();
    }
    if (parent != null) {
      JOptionPane.getFrameForComponent(parent).setVisible(false);
    }
    new Thread(() -> launchInNewThread(parent), "Triplea start thread").start();
  }

  protected abstract void launchInNewThread(Component parent);
}
