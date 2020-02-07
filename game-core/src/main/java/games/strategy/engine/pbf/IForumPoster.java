package games.strategy.engine.pbf;

import com.google.common.collect.ImmutableSet;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.triplea.util.Arrays;

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
   * Called when the turn summary should be posted.
   *
   * @param summary the forum summary
   * @param subject the forum subject
   * @return true if the post was successful
   */
  CompletableFuture<String> postTurnSummary(String summary, String subject, Path savegame);

  String getDisplayName();

  /** Opens a browser and go to the forum post, identified by the forumId. */
  void viewPosted();

  /**
   * Each poster provides a message that is displayed on the progress bar when testing the poster.
   *
   * @return the progress bar message
   */
  String getTestMessage();

  /**
   * Creates an {@link IForumPoster} instance based on the given arguments and the configured
   * settings.
   */
  static IForumPoster newInstanceByName(final String name, final int topicId) {
    switch (name) {
      case TripleAForumPoster.DISPLAY_NAME:
        return new TripleAForumPoster(
            topicId,
            Arrays.withSensitiveArrayAndReturn(
                ClientSetting.tripleaForumUsername::getValueOrThrow, String::new),
            Arrays.withSensitiveArrayAndReturn(
                ClientSetting.tripleaForumPassword::getValueOrThrow, String::new));
      case AxisAndAlliesForumPoster.DISPLAY_NAME:
        return new AxisAndAlliesForumPoster(
            topicId,
            Arrays.withSensitiveArrayAndReturn(
                ClientSetting.aaForumUsername::getValueOrThrow, String::new),
            Arrays.withSensitiveArrayAndReturn(
                ClientSetting.aaForumPassword::getValueOrThrow, String::new));
      default:
        throw new IllegalArgumentException(String.format("String '%s' must be a valid name", name));
    }
  }

  static boolean isClientSettingSetupValidForServer(final String server) {
    if (TripleAForumPoster.DISPLAY_NAME.equals(server)) {
      return ClientSetting.tripleaForumUsername.isSet()
          && ClientSetting.tripleaForumPassword.isSet();
    } else if (AxisAndAlliesForumPoster.DISPLAY_NAME.equals(server)) {
      return ClientSetting.aaForumUsername.isSet() && ClientSetting.aaForumPassword.isSet();
    }
    return false;
  }

  static ImmutableSet<String> availablePosters() {
    return ImmutableSet.of(TripleAForumPoster.DISPLAY_NAME, AxisAndAlliesForumPoster.DISPLAY_NAME);
  }
}
