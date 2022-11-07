package games.strategy.engine.posted.game.pbf;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game
 * if the implementing class supports this.
 */
// TODO: merge these constants into another class, perhaps GameProperties or NodeBbForumPoster
public interface IForumPoster {
  String NAME = "FORUM_POSTER_NAME";
  String TOPIC_ID = "FORUM_POSTER_TOPIC_ID";
  String INCLUDE_SAVEGAME = "FORUM_POSTER_INCLUDE_SAVEGAME";
  String POST_AFTER_COMBAT = "FORUM_POSTER_POST_AFTER_COMBAT";
}
