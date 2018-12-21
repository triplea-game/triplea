package games.strategy.engine.pbem;

import java.util.function.Supplier;

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

  TripleAForumPoster(final int topicId, final String username, final Supplier<String> password) {
    super(topicId, username, password);
  }

  @Override
  String getForumUrl() {
    return UrlConstants.TRIPLEA_FORUM.toString();
  }

  @Override
  public String getDisplayName() {
    return "forums.triplea-game.org";
  }

  public String getHelpText() {
    return HelpSupport.loadHelp("tripleaForum.html");
  }
}
