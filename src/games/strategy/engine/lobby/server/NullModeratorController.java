package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

import java.util.Date;

/**
 * For Server Games, not the Lobby.
 * 
 * @author veqryn
 * 
 */
public class NullModeratorController extends ModeratorController implements IModeratorController
{
	@Override
	public void register(final IRemoteMessenger messenger)
	{
		messenger.registerRemote(this, getModeratorControllerName());
	}
	
	public NullModeratorController(final IServerMessenger messenger)
	{
		super(messenger);
	}
	
	@Override
	public void banUsername(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	@Override
	public void banIp(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	@Override
	public void banMac(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	@Override
	public void muteUsername(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	@Override
	public void muteIp(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	@Override
	public void muteMac(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	@Override
	public void boot(final INode node)
	{
		// nothing
	}
	
	@Override
	void assertUserIsAdmin()
	{
		throw new IllegalStateException("Not an admin");
	}
	
	@Override
	public boolean isAdmin()
	{
		return false;
	}
	
	@Override
	public boolean isPlayerAdmin(final INode node)
	{
		return false;
	}
	
	@Override
	public String getInformationOn(final INode node)
	{
		return "Feature not enabled in NullModeratorController";
	}
	
	@Override
	public String setPassword(final INode node, final String hashedPassword)
	{
		return "Feature not enabled in NullModeratorController";
	}
}
