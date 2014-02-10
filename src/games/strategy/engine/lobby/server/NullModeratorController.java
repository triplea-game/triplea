package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

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
	protected String getNodeMacAddress(final INode node)
	{
		return super.getNodeMacAddress(node);
	}
	
	@Override
	public String getHeadlessHostBotSalt(final INode node)
	{
		return null;
	}
	
	@Override
	public String banPlayerHeadlessHostBot(final INode node, final String playerNameToBeBanned, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	@Override
	public String stopGameHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	@Override
	public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	@Override
	protected String getRealName(final INode node)
	{
		return super.getRealName(node);
	}
	
	@Override
	protected String getAliasesFor(final INode node)
	{
		return super.getAliasesFor(node);
	}
	
	@Override
	public void register(final IRemoteMessenger messenger)
	{
		messenger.registerRemote(this, getModeratorControllerName());
	}
	
	public NullModeratorController(final IServerMessenger messenger, final Messengers messengers)
	{
		super(messenger, messengers);
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
