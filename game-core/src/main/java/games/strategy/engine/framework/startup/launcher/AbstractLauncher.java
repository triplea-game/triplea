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
public abstract class AbstractLauncher<T> implements ILauncher {
  protected final GameData gameData;
  protected final GameSelectorModel gameSelectorModel;
  protected final boolean headless;

  AbstractLauncher(final GameSelectorModel gameSelectorModel) {
    this(gameSelectorModel, false);
  }

  AbstractLauncher(final GameSelectorModel gameSelectorModel, final boolean headless) {
    this.headless = headless;
    this.gameSelectorModel = gameSelectorModel;
    gameData = gameSelectorModel.getGameData();
  }

  @Override
  public void launch(final Component parent) {
    if (headless == SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    final WaitWindow gameLoadingWindow = headless ? null : new WaitWindow();
    if (!headless) {
      gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
      gameLoadingWindow.setVisible(true);
      gameLoadingWindow.showWait();
    }
    if (parent != null) {
      JOptionPane.getFrameForComponent(parent).setVisible(false);
    }
    new Thread(() -> {
      final T result;
      try {
        result = loadGame(parent);
      } finally {
        if (!headless) {
          gameLoadingWindow.doneWait();
        }
      }
      launchInternal(parent, result);
    }, "Triplea start thread").start();
  }

  abstract T loadGame(Component parent);

  abstract void launchInternal(Component parent, T data);
}
