package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attachments.UserActionAttachment;

public interface IUserActionDelegate extends IRemote, IDelegate {
  void attemptAction(UserActionAttachment actionChoice);

  Collection<UserActionAttachment> getValidActions();
}
