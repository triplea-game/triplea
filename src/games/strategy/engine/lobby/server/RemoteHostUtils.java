package games.strategy.engine.lobby.server;

import games.strategy.engine.framework.HeadlessGameServer;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

/**
 * 
 * @author veqryn
 * 
 */
public class RemoteHostUtils implements IRemoteHostUtils
{
	private final INode m_serverNode;
	
	public static final RemoteName getRemoteHostUtilsName(final INode node)
	{
		return new RemoteName(IRemoteHostUtils.class, "games.strategy.engine.lobby.server.RemoteHostUtils:" + node.toString());
	}
	
	public RemoteHostUtils(final INode serverNode)
	{
		m_serverNode = serverNode;
	}
	
	public String getChatLogHeadlessHostBot(final String hashedPassword, final String salt)
	{
		if (!MessageContext.getSender().equals(m_serverNode))
			return "Not accepted!";
		final HeadlessGameServer instance = HeadlessGameServer.getInstance();
		if (instance == null)
			return "Not a headless host bot!";
		return instance.remoteGetChatLog(hashedPassword, salt);
	}
	
	public String banPlayerHeadlessHostBot(final String playerNameToBeBanned, final String hashedPassword, final String salt)
	{
		if (!MessageContext.getSender().equals(m_serverNode))
			return "Not accepted!";
		final HeadlessGameServer instance = HeadlessGameServer.getInstance();
		if (instance == null)
			return "Not a headless host bot!";
		return instance.remoteBanPlayer(playerNameToBeBanned, hashedPassword, salt);
	}
	
	public String stopGameHeadlessHostBot(final String hashedPassword, final String salt)
	{
		if (!MessageContext.getSender().equals(m_serverNode))
			return "Not accepted!";
		final HeadlessGameServer instance = HeadlessGameServer.getInstance();
		if (instance == null)
			return "Not a headless host bot!";
		return instance.remoteStopGame(hashedPassword, salt);
	}
	
	public String shutDownHeadlessHostBot(final String hashedPassword, final String salt)
	{
		if (!MessageContext.getSender().equals(m_serverNode))
			return "Not accepted!";
		final HeadlessGameServer instance = HeadlessGameServer.getInstance();
		if (instance == null)
			return "Not a headless host bot!";
		return instance.remoteShutdown(hashedPassword, salt);
	}
	
	public String getSalt()
	{
		if (!MessageContext.getSender().equals(m_serverNode))
			return "Not accepted!";
		final HeadlessGameServer instance = HeadlessGameServer.getInstance();
		if (instance == null)
			return "Not a headless host bot!";
		return instance.getSalt();
	}
}
