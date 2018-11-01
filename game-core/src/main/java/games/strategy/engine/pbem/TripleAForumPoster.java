package games.strategy.engine.pbem;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.help.HelpSupport;

/**
 * Posts turn summaries to forums.triplea-game.org.
 *
 * <p>
 * URL format is {@code https://forums.triplea-game.org/api/v2/topics/<topicID>}.
 * </p>
 */
public class TripleAForumPoster extends NodeBbForumPoster {
  private static final long serialVersionUID = -3380344469767981030L;

  @Override
  protected String getForumUrl() {
    return UrlConstants.TRIPLEA_FORUM.toString();
  }

  @Override
  public IForumPoster doClone() {
    final TripleAForumPoster clone = new TripleAForumPoster();
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
    return HelpSupport.loadHelp("tripleaForum.html");
  }
}
