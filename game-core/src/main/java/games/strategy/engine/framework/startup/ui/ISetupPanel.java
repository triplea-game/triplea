package games.strategy.engine.framework.startup.ui;

import java.util.List;
import java.util.Observer;
import java.util.Optional;

import javax.swing.Action;
import javax.swing.JComponent;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.random.IRemoteDiceServer;

/**
 * Made so that we can have a headless setup. (this is probably a hack, but used because i do not want to rewrite the
 * entire setup model).
 */
public interface ISetupPanel {
  JComponent getDrawable();

  boolean showCancelButton();

  void addObserver(final Observer observer);

  void notifyObservers();

  /**
   * Subclasses that have chat override this.
   */
  IChatPanel getChatPanel();

  /**
   * Cleanup should occur here that occurs when we cancel.
   */
  void cancel();

  /**
   * Indicates we can start the game.
   */
  boolean canGameStart();

  void postStartGame();

  Optional<ILauncher> getLauncher();

  List<Action> getUserActions();

  static void clearPbfPbemInformation(final GameProperties properties) {
    properties.set(IRemoteDiceServer.NAME, null);
    properties.set(IRemoteDiceServer.GAME_NAME, null);
    properties.set(IRemoteDiceServer.EMAIL_1, null);
    properties.set(IRemoteDiceServer.EMAIL_2, null);
    properties.set(IForumPoster.NAME, null);
    properties.set(IForumPoster.TOPIC_ID, null);
    properties.set(IForumPoster.INCLUDE_SAVEGAME, null);
    properties.set(IForumPoster.POST_AFTER_COMBAT, null);
    properties.set(IEmailSender.SUBJECT, null);
    properties.set(IEmailSender.OPPONENT, null);
    properties.set(IEmailSender.POST_AFTER_COMBAT, null);
  }
}
