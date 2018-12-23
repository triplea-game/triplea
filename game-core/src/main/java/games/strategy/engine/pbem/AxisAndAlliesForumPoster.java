package games.strategy.engine.pbem;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.help.HelpSupport;

/**
 * Posts turn summaries to www.axisandallies.org/forums.
 *
 * <p>
 * URL format is {@code https://www.axisandallies.org/forums/api/v2/topics/<topicID>}.
 * </p>
 */
public class AxisAndAlliesForumPoster extends NodeBbForumPoster {

  AxisAndAlliesForumPoster(final int topicId, final String username, final String password) {
    super(topicId, username, password);
  }
  @Override
  String getForumUrl() {
    return UrlConstants.AXIS_AND_ALLIES_FORUM.toString();
  }

  @Override
  public String getDisplayName() {
    return "www.axisandallies.org/forums/";
  }

  public String getHelpText() {
    return HelpSupport.loadHelp("axisAndAlliesForum.html");
  }
}
