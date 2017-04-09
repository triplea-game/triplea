package games.strategy.engine.framework.startup.ui;

import java.util.List;
import java.util.Observer;

import javax.swing.Action;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;

/**
 * Made so that we can have a headless setup. (this is probably a hack, but used because i do not want to rewrite the
 * entire setup model).
 */
public interface ISetupPanel extends java.io.Serializable {
  boolean isMetaSetupPanelInstance();

  void addObserver(final Observer observer);

  void removeObserver(final Observer observer);

  void notifyObservers();

  /**
   * Subclasses that have chat override this.
   */
  IChatPanel getChatPanel();

  /**
   * Cleanup should occur here that occurs when we cancel.
   */
  void cancel();

  void shutDown();

  /**
   * Indicates we can start the game.
   */
  boolean canGameStart();

  void setWidgetActivation();

  void preStartGame();

  void postStartGame();

  ILauncher getLauncher();

  List<Action> getUserActions();
}
