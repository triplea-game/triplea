package games.strategy.engine.pbf;

import games.strategy.triplea.UrlConstants;

/**
 * Posts turn summaries to www.axisandallies.org/forums.
 *
 * <p>URL format is {@code https://www.axisandallies.org/forums/api/v2/topics/<topicID>}.
 */
class AxisAndAlliesForumPoster extends NodeBbForumPoster {
  static final String DISPLAY_NAME = "www.axisandallies.org/forums/";

  AxisAndAlliesForumPoster(final int topicId, final String username, final String password) {
    super(topicId, username, password);
  }

  @Override
  String getForumUrl() {
    return UrlConstants.AXIS_AND_ALLIES_FORUM;
  }

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }
}
