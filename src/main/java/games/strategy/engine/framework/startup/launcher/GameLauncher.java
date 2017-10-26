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
public abstract class GameLauncher {
  protected final GameData m_gameData;
  protected final GameSelectorModel m_gameSelectorModel;
  protected final WaitWindow m_gameLoadingWindow;
  protected final boolean m_headless;

  protected GameLauncher(final GameSelectorModel gameSelectorModel) {
    this(gameSelectorModel, false);
  }

  protected GameLauncher(final GameSelectorModel gameSelectorModel, final boolean headless) {
    m_headless = headless;
    if (m_headless) {
      m_gameLoadingWindow = null;
    } else {
      m_gameLoadingWindow = new WaitWindow();
    }
    m_gameSelectorModel = gameSelectorModel;
    m_gameData = gameSelectorModel.getGameData();
  }

  public void launch(final Component parent) {
    if (!m_headless && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    if (!m_headless && m_gameLoadingWindow != null) {
      m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
      m_gameLoadingWindow.setVisible(true);
      m_gameLoadingWindow.showWait();
    }
    if (parent != null) {
      JOptionPane.getFrameForComponent(parent).setVisible(false);
    }
    new Thread(() -> launchInNewThread(parent), "Triplea start thread").start();
  }

  protected abstract void launchInNewThread(Component parent);
}
