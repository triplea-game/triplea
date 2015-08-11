package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;

public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  public boolean postTurnSummary(final PBEMMessagePoster poster, final String title, final boolean includeSaveGame);

  public void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  public boolean getHasPostedTurnSummary();
}
