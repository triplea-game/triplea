package org.triplea.game.startup;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.random.IRemoteDiceServer;
import java.util.Map;
import java.util.Optional;
import org.triplea.game.chat.ChatModel;

/**
 * Interface to abstract common functionality shared between headless and headed game launching
 * mechanisms.
 */
public interface SetupModel {

  ChatModel getChatModel();

  /** Cleanup should occur here that occurs when we cancel. */
  void cancel();

  /** Indicates we can start the game. */
  boolean canGameStart();

  void postStartGame();

  Optional<? extends ILauncher> getLauncher();

  /**
   * Helper method to clear PbF/PbEM related properties from a game that has potentially been used
   * for PbF/PbEM before.
   *
   * @param properties The {@link GameProperties} object the properties should be cleared from.
   */
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
    properties.set(IEmailSender.RECIPIENTS, null);
    properties.set(IEmailSender.POST_AFTER_COMBAT, null);
  }

  /** Helper method to be used by implementations to avoid duplicated code. */
  static boolean canGameStartHelper(GameSelectorModel gameSelectorModel, ServerModel model) {
    if (gameSelectorModel.getGameData() == null) {
      return false;
    }
    final Map<String, String> players = model.getPlayersToNodeListing();
    if (players.isEmpty() || players.containsValue(null)) {
      return false;
    }
    // make sure at least 1 player is enabled
    return model.getPlayersEnabledListing().containsValue(Boolean.TRUE);
  }
}
