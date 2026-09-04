package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import java.util.Collection;

/**
 * Typed-message-backed {@link IUserActionDelegate} handed to the user-action UI in place of the
 * reflective current-delegate proxy. Forwards the two business methods the UI reaches over the
 * messenger latch, leaving the server-side lifecycle methods unsupported.
 */
public class TypedUserActionDelegate extends AbstractTypedCurrentDelegate
    implements IUserActionDelegate {
  public TypedUserActionDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public void attemptAction(final UserActionAttachment actionChoice) {
    invokeCurrent(new AttemptActionRequest(actionChoice), AttemptActionResponse.TYPE);
  }

  @Override
  public Collection<UserActionAttachment> getValidActions() {
    return invokeCurrent(new GetValidActionsRequest(), GetValidActionsResponse.TYPE)
        .getValidActions();
  }
}
