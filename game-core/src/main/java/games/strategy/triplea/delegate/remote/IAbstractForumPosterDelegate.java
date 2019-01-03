package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PbemMessagePoster;

/**
 * Logic for posting a save game to a forum. Supplements other game logic at points where it makes
 * sense to record a save game (e.g. at the end of a game turn).
 */
public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  boolean postTurnSummary(
      final PbemMessagePoster poster, final String title, final boolean includeSaveGame);

  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  boolean getHasPostedTurnSummary();
}
