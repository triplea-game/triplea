/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.BannedUsernameController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.lobby.server.userDB.MutedIpController;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.engine.lobby.server.userDB.MutedUsernameController;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.util.MD5Crypt;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ModeratorController implements IModeratorController
{
	protected final static Logger s_logger = Logger.getLogger(ModeratorController.class.getName());
	protected final IServerMessenger m_serverMessenger;
	protected final Messengers m_allMessengers;
	
	public static final RemoteName getModeratorControllerName()
	{
		return new RemoteName(IModeratorController.class, "games.strategy.engine.lobby.server.ModeratorController:Global");
	}
	
	public void register(final IRemoteMessenger messenger)
	{
		messenger.registerRemote(this, getModeratorControllerName());
	}
	
	public ModeratorController(final IServerMessenger serverMessenger, final Messengers messengers)
	{
		m_serverMessenger = serverMessenger;
		m_allMessengers = messengers;
	}
	
	public void banUsername(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedUsernameController().addBannedUsername(getRealName(node), banExpires);
		final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
		s_logger.info(DUtils.Format("User was banned from the lobby(Username ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
	}
	
	public void banIp(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedIpController().addBannedIp(node.getAddress().getHostAddress(), banExpires);
		final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
		s_logger.info(DUtils.Format("User was banned from the lobby(IP ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
	}
	
	public void banMac(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedMacController().addBannedMac(mac, banExpires);
		final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
		s_logger.info(DUtils.Format("User was banned from the lobby(Mac ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
	}
	
	public void banMac(final INode node, final String hashedMac, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		new BannedMacController().addBannedMac(hashedMac, banExpires);
		final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
		s_logger.info(DUtils.Format("User was banned from the lobby(Mac ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), hashedMac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
	}
	
	public void muteUsername(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedUsernameController().addMutedUsername(getRealName(node), muteExpires);
		m_serverMessenger.NotifyUsernameMutingOfPlayer(node.getAddress().getHostAddress(), muteExpires);
		final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
		s_logger.info(DUtils.Format("User was muted on the lobby(Username mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
	}
	
	public void muteIp(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedIpController().addMutedIp(node.getAddress().getHostAddress(), muteExpires);
		m_serverMessenger.NotifyIPMutingOfPlayer(node.getAddress().getHostAddress(), muteExpires);
		final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
		s_logger.info(DUtils.Format("User was muted on the lobby(IP mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
	}
	
	public void muteMac(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedMacController().addMutedMac(mac, muteExpires);
		m_serverMessenger.NotifyMacMutingOfPlayer(mac, muteExpires);
		final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
		s_logger.info(DUtils.Format("User was muted on the lobby(Mac mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
	}
	
	protected String getNodeMacAddress(final INode node)
	{
		return m_serverMessenger.GetPlayerMac(node.getName());
	}
	
	public void boot(final INode node)
	{
		assertUserIsAdmin();
		/*if (!MessageContext.getSender().getName().equals("Admin") && isPlayerAdmin(node)) // Let the master lobby administrator boot admins
			throw new IllegalStateException("Can't boot an admin");*/
		// You can't boot the server node
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not boot server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		m_serverMessenger.removeConnection(node);
		s_logger.info(DUtils.Format("User was booted from the lobby. Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(), node.getAddress().getHostAddress(),
					mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
	}
	
	public String getHeadlessHostBotSalt(final INode node)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		s_logger.info(DUtils.Format("Getting salt for Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		return remoteHostUtils.getSalt();
	}
	
	public String getChatLogHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.getChatLogHeadlessHostBot(hashedPassword, salt);
		s_logger.info(DUtils.Format(((response == null || response.equals("Invalid password!")) ? "Failed" : "Successful")
					+ " Remote get Chat Log of Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	public String mutePlayerHeadlessHostBot(final INode node, final String playerNameToBeMuted, final int minutes, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.mutePlayerHeadlessHostBot(playerNameToBeMuted, minutes, hashedPassword, salt);
		s_logger.info(DUtils.Format((response == null ? "Successful" : "Failed (" + response + ")")
					+ " Remote Mute of " + playerNameToBeMuted + " for " + minutes + " minutes In Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}",
					node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	public String bootPlayerHeadlessHostBot(final INode node, final String playerNameToBeBooted, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
		s_logger.info(DUtils.Format((response == null ? "Successful" : "Failed (" + response + ")")
					+ " Remote Boot of " + playerNameToBeBooted + " In Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	public String banPlayerHeadlessHostBot(final INode node, final String playerNameToBeBanned, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hashedPassword, salt);
		s_logger.info(DUtils.Format((response == null ? "Successful" : "Failed (" + response + ")")
					+ " Remote Ban of " + playerNameToBeBanned + " In Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	public String stopGameHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.stopGameHeadlessHostBot(hashedPassword, salt);
		s_logger.info(DUtils.Format((response == null ? "Successful" : "Failed (" + response + ")")
					+ " Remote Stopgame of Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not shutdown server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		s_logger.info(DUtils.Format("Started Remote Shutdown of Headless HostBot. Host: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
		s_logger.info(DUtils.Format((response == null ? "Successful" : "Failed (" + response + ")")
					+ " Remote Shutdown of Headless HostBot. Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(),
					node.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
		return response;
	}
	
	void assertUserIsAdmin()
	{
		if (!isAdmin())
		{
			throw new IllegalStateException("Not an admin");
		}
	}
	
	public boolean isAdmin()
	{
		final INode node = MessageContext.getSender();
		return isPlayerAdmin(node);
	}
	
	public boolean isPlayerAdmin(final INode node)
	{
		final String name = getRealName(node);
		final DBUserController controller = new DBUserController();
		final DBUser user = controller.getUser(name);
		if (user == null)
			return false;
		return user.isAdmin();
	}
	
	protected String getRealName(final INode node)
	{
		// Remove any (n) that is added to distinguish duplicate names
		final String name = node.getName().split(" ")[0];
		return name;
	}
	
	public String getInformationOn(final INode node)
	{
		assertUserIsAdmin();
		final Set<String> aliases = new TreeSet<String>();
		for (final INode currentNode : m_serverMessenger.getNodes())
		{
			if (currentNode.getAddress().equals(node.getAddress()))
				aliases.add(currentNode.getName());
		}
		final String mac = getNodeMacAddress(node);
		final StringBuilder builder = new StringBuilder();
		builder.append("Name: ").append(node.getName());
		builder.append("\r\nHost Name: ").append(node.getAddress().getHostName());
		builder.append("\r\nIP Address: ").append(node.getAddress().getHostAddress());
		builder.append("\r\nPort: ").append(node.getPort());
		builder.append("\r\nHashed Mac: ").append((mac != null && mac.startsWith(MD5Crypt.MAGIC + "MH$") ? mac.substring(6) : mac + " (Invalid)"));
		builder.append("\r\nAliases: ").append(getAliasesFor(node));
		return builder.toString();
	}
	
	protected String getAliasesFor(final INode node)
	{
		final StringBuilder builder = new StringBuilder();
		final String nodeMac = getNodeMacAddress(node);
		for (final INode cur : m_serverMessenger.getNodes())
		{
			if (cur.equals(node) || cur.getName().equals("Admin"))
				continue;
			if (cur.getAddress().equals(node.getAddress()) || getNodeMacAddress(cur).equals(nodeMac))
			{
				if (builder.length() > 0)
					builder.append(", ");
				builder.append(cur.getName());
			}
		}
		if (builder.length() > 100) // This player seems to have an unusually long list of aliases
			return builder.toString().replace(", ", "\r\n"); // So replace comma's to keep names within screen
		return builder.toString();
	}
	
	public String getHostConnections(final INode node)
	{
		assertUserIsAdmin();
		if (m_serverMessenger.getServerNode().equals(node))
			throw new IllegalStateException("Can not do this for server node");
		final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
		final IRemoteHostUtils remoteHostUtils = (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
		final String response = remoteHostUtils.getConnections();
		return response;
	}
	
	public String setPassword(final INode node, final String hashedPassword)
	{
		assertUserIsAdmin();
		final DBUserController controller = new DBUserController();
		final DBUser user = controller.getUser(getRealName(node));
		if (user == null)
			return "Can't set the password of an anonymous player";
		// Don't allow changing an admin password
		if (user.isAdmin())
			return "Can't set the password of an admin";
		controller.updateUser(user.getName(), user.getEmail(), hashedPassword, user.isAdmin());
		return null;
	}
}
