package games.strategy.engine.posted.game.pbf;

import games.strategy.engine.posted.game.pbf.NodeBbForumPoster.ForumPostingParameters;
import games.strategy.triplea.settings.ClientSetting;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game
 * if the implementing class supports this.
 */
public interface IForumPoster {
  String NAME = "FORUM_POSTER_NAME";
  String TOPIC_ID = "FORUM_POSTER_TOPIC_ID";
  String INCLUDE_SAVEGAME = "FORUM_POSTER_INCLUDE_SAVEGAME";
  String POST_AFTER_COMBAT = "FORUM_POSTER_POST_AFTER_COMBAT";

  /**
   * Creates an {@link IForumPoster} instance based on the given arguments and the configured
   * settings.
   */
  static NodeBbForumPoster newInstanceByName(final String name, final int topicId) {
    switch (name) {
      case NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME:
        return NodeBbForumPoster.newTripleaForumsPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .username(ClientSetting.tripleaForumUsername.getValueOrThrow())
                .password(ClientSetting.tripleaForumPassword.getValueOrThrow())
                .build());
      case NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME:
        return NodeBbForumPoster.newAxisAndAlliesOrgForumPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .username(ClientSetting.aaForumUsername.getValueOrThrow())
                .password(ClientSetting.aaForumPassword.getValueOrThrow())
                .build());
      default:
        throw new IllegalArgumentException(String.format("String '%s' must be a valid name", name));
    }
  }
}
