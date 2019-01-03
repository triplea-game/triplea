package games.strategy.engine.pbem;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.help.HelpSupport;

/**
 * Posts turn summaries to www.axisandallies.org/forums.
 *
 * <p>URL format is {@code https://www.axisandallies.org/forums/api/v2/topics/<topicID>}.
 */
public class AxisAndAlliesForumPoster extends NodeBbForumPoster {
  private static final long serialVersionUID = -823830258973002082L;

  @Override
  String getForumUrl() {
    return UrlConstants.AXIS_AND_ALLIES_FORUM.toString();
  }

  @Override
  public String getDisplayName() {
    return "www.axisandallies.org/forums/";
  }

  @Override
  public IForumPoster doClone() {
    final AxisAndAlliesForumPoster clone = new AxisAndAlliesForumPoster();
    clone.setTopicId(getTopicId());
    clone.setIncludeSaveGame(getIncludeSaveGame());
    clone.setAlsoPostAfterCombatMove(getAlsoPostAfterCombatMove());
    clone.setPassword(getPassword());
    clone.setUsername(getUsername());
    clone.setCredentialsSaved(areCredentialsSaved());
    return clone;
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("axisAndAlliesForum.html");
  }
}
