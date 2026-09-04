package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import java.util.Collection;

/**
 * Typed-message-backed {@link IPoliticsDelegate} handed to the politics UI in place of the
 * reflective current-delegate proxy. Forwards the two business methods the UI reaches over the
 * messenger latch, leaving the server-side lifecycle methods unsupported.
 */
public class TypedPoliticsDelegate extends AbstractTypedCurrentDelegate
    implements IPoliticsDelegate {
  public TypedPoliticsDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public void attemptAction(final PoliticalActionAttachment actionChoice) {
    invokeCurrent(new AttemptActionRequest(actionChoice), AttemptActionResponse.TYPE);
  }

  @Override
  public Collection<PoliticalActionAttachment> getValidActions() {
    return invokeCurrent(new GetValidActionsRequest(), GetValidActionsResponse.TYPE)
        .getValidActions();
  }
}
