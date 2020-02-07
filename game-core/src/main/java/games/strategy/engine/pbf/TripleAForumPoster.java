package games.strategy.engine.pbf;

import games.strategy.triplea.UrlConstants;

/**
 * Posts turn summaries to forums.triplea-game.org.
 *
 * <p>URL format is {@code https://forums.triplea-game.org/api/v2/topics/<topicID>}.
 */
class TripleAForumPoster extends NodeBbForumPoster {
  static final String DISPLAY_NAME = "forums.triplea-game.org";

  TripleAForumPoster(final int topicId, final String username, final String password) {
    super(topicId, username, password);
  }

  @Override
  String getForumUrl() {
    return UrlConstants.TRIPLEA_FORUM;
  }

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }
}
