package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.INode;

import java.util.logging.Logger;

public class UserManager implements IUserManager
{
	private final static Logger s_logger = Logger.getLogger(UserManager.class.getName());
	
	public void register(final IRemoteMessenger messenger)
	{
		messenger.registerRemote(this, IUserManager.USER_MANAGER);
	}
	
	/**
	 * Update hte user info, returning an error string if an error occurs
	 */
	public String updateUser(final String userName, final String emailAddress, final String hashedPassword)
	{
		final INode remote = MessageContext.getSender();
		if (!userName.equals(remote.getName()))
		{
			s_logger.severe("Tried to update user permission, but not correct user, userName:" + userName + " node:" + remote);
			return "Sorry, but I can't let you do that";
		}
		try
		{
			new DBUserController().updateUser(userName, emailAddress, hashedPassword, false);
		} catch (final IllegalStateException e)
		{
			return e.getMessage();
		}
		return null;
	}
	
	/**
	 * Update hte user info, returning an error string if an error occurs
	 */
	public DBUser getUserInfo(final String userName)
	{
		final INode remote = MessageContext.getSender();
		if (!userName.equals(remote.getName()))
		{
			s_logger.severe("Tried to get user info, but not correct user, userName:" + userName + " node:" + remote);
			throw new IllegalStateException("Sorry, but I can't let you do that");
		}
		return new DBUserController().getUser(userName);
	}
}
