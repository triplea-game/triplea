package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.UserActionAttachment;

import java.util.Collection;

public interface IUserActionDelegate extends IRemote, IDelegate
{
	public void attemptAction(UserActionAttachment actionChoice);
	
	public Collection<UserActionAttachment> getValidActions();
}
