package games.strategy.engine.posted.game.pbf;

public class ForumPostingException extends RuntimeException {

  public ForumPostingException(final Exception e) {
    super("Failed to communicate with forum: " + e.getMessage(), e);
  }
}
