package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;

/**
 * Logic for posting a save game to a forum. Supplements other game logic at points where it makes
 * sense to record a save game (e.g. at the end of a game turn).
 */
public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  @RemoteActionCode(9)
  boolean postTurnSummary(PbemMessagePoster poster, String title);

  @RemoteActionCode(12)
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(4)
  boolean getHasPostedTurnSummary();
}
