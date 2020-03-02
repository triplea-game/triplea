package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attachments.UserActionAttachment;
import java.util.Collection;

/** Logic for performing user actions. */
public interface IUserActionDelegate extends IRemote, IDelegate {
@RemoteActionCode(0)
  void attemptAction(UserActionAttachment actionChoice);

@RemoteActionCode(7)
  Collection<UserActionAttachment> getValidActions();
}
