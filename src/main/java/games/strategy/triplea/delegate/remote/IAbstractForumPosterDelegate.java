package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;

public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  boolean postTurnSummary(final PBEMMessagePoster poster, final String title, final boolean includeSaveGame);

  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  boolean getHasPostedTurnSummary();
}
