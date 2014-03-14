package games.strategy.engine.lobby.server;

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
public class NullModeratorController extends AbstractModeratorController implements IModeratorController
{
	public NullModeratorController(final IServerMessenger messenger, final Messengers messengers)
	{
		super(messenger, messengers);
	}
	
	public void banMac(final INode node, final String hashedMac, final Date banExpires)
	{
		// nothing
	}
	
	public String mutePlayerHeadlessHostBot(final INode node, final String playerNameToBeMuted, final int minutes, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public String bootPlayerHeadlessHostBot(final INode node, final String playerNameToBeBooted, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public String getHostConnections(final INode node)
	{
		return null;
	}
	
	public String getHeadlessHostBotSalt(final INode node)
	{
		return null;
	}
	
	public String getChatLogHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public String banPlayerHeadlessHostBot(final INode node, final String playerNameToBeBanned, final int hours, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public String stopGameHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		return null;
	}
	
	public void banUsername(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	public void banIp(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	public void banMac(final INode node, final Date banExpires)
	{
		// nothing
	}
	
	public void muteUsername(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	public void muteIp(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	public void muteMac(final INode node, final Date muteExpires)
	{
		// nothing
	}
	
	public void boot(final INode node)
	{
		// nothing
	}
	
	void assertUserIsAdmin()
	{
		throw new IllegalStateException("Not an admin");
	}
	
	public boolean isAdmin()
	{
		return false;
	}
	
	public boolean isPlayerAdmin(final INode node)
	{
		return false;
	}
	
	public String getInformationOn(final INode node)
	{
		return "Feature not enabled in NullModeratorController";
	}
	
	public String setPassword(final INode node, final String hashedPassword)
	{
		return "Feature not enabled in NullModeratorController";
	}
}
