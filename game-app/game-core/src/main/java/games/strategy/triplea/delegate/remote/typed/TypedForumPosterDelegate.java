package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

/**
 * Typed-message-backed {@link IAbstractForumPosterDelegate} handed to the forum-poster UI in place
 * of the reflective current-delegate proxy. The current step's move/purchase delegate is an {@link
 * IAbstractForumPosterDelegate}, so these forward over the messenger latch to it; the server-side
 * lifecycle methods stay unsupported.
 */
public class TypedForumPosterDelegate extends AbstractTypedCurrentDelegate
    implements IAbstractForumPosterDelegate {
  public TypedForumPosterDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public boolean postTurnSummary(final PbemMessagePoster poster, final String title) {
    return invokeCurrent(new PostTurnSummaryRequest(poster, title), PostTurnSummaryResponse.TYPE)
        .isPosted();
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    invokeCurrent(
        new SetHasPostedTurnSummaryRequest(hasPostedTurnSummary),
        SetHasPostedTurnSummaryResponse.TYPE);
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    return invokeCurrent(new GetHasPostedTurnSummaryRequest(), GetHasPostedTurnSummaryResponse.TYPE)
        .isHasPosted();
  }
}
