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
import games.strategy.net.ServerMessenger;
import games.strategy.triplea.Dynamix_AI.DUtils;
import games.strategy.util.MD5Crypt;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ModeratorController implements IModeratorController
{
	private final static Logger s_logger = Logger.getLogger(ModeratorController.class.getName());
	private final IServerMessenger m_messenger;
	
	public static final RemoteName getModeratorControllerName()
	{
		return new RemoteName(IModeratorController.class, "games.strategy.engine.lobby.server.ModeratorController:Global");
	}
	
	public void register(final IRemoteMessenger messenger)
	{
		messenger.registerRemote(this, getModeratorControllerName());
	}
	
	public ModeratorController(final IServerMessenger messenger)
	{
		m_messenger = messenger;
	}
	
	public void banUsername(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedUsernameController().addBannedUsername(getRealName(node), banExpires);
		s_logger.info(DUtils.Format("User was banned from the lobby(Username ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banExpires.toString()));
	}
	
	public void banIp(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedIpController().addBannedIp(node.getAddress().getHostAddress(), banExpires);
		s_logger.info(DUtils.Format("User was banned from the lobby(IP ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banExpires.toString()));
	}
	
	public void banMac(final INode node, final Date banExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't ban an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new BannedMacController().addBannedMac(mac, banExpires);
		s_logger.info(DUtils.Format("User was banned from the lobby(Mac ban). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banExpires.toString()));
	}
	
	public void muteUsername(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedUsernameController().addMutedUsername(getRealName(node), muteExpires);
		ServerMessenger.getInstance().NotifyUsernameMutingOfPlayer(node.getAddress().getHostAddress(), muteExpires.getTime());
		s_logger.info(DUtils.Format("User was muted on the lobby(Username mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node
					.getAddress().getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteExpires.toString()));
	}
	
	public void muteIp(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedIpController().addMutedIp(node.getAddress().getHostAddress(), muteExpires);
		ServerMessenger.getInstance().NotifyIPMutingOfPlayer(node.getAddress().getHostAddress(), muteExpires.getTime());
		s_logger.info(DUtils.Format("User was muted on the lobby(IP mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteExpires.toString()));
	}
	
	public void muteMac(final INode node, final Date muteExpires)
	{
		assertUserIsAdmin();
		if (isPlayerAdmin(node))
			throw new IllegalStateException("Can't mute an admin");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		new MutedMacController().addMutedMac(mac, muteExpires);
		ServerMessenger.getInstance().NotifyMacMutingOfPlayer(mac, muteExpires.getTime());
		s_logger.info(DUtils.Format("User was muted on the lobby(Mac mute). Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5} Expires: {6}", node.getName(), node.getAddress()
					.getHostAddress(), mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteExpires.toString()));
	}
	
	private String getNodeMacAddress(final INode node)
	{
		return ServerMessenger.getInstance().GetPlayerMac(node.getName());
	}
	
	public void boot(final INode node)
	{
		assertUserIsAdmin();
		/*if (!MessageContext.getSender().getName().equals("Admin") && isPlayerAdmin(node)) // Let the master lobby administrator boot admins
			throw new IllegalStateException("Can't boot an admin");*/
		// You can't boot the server node
		if (m_messenger.getServerNode().equals(node))
			throw new IllegalStateException("Cant boot server node");
		final INode modNode = MessageContext.getSender();
		final String mac = getNodeMacAddress(node);
		m_messenger.removeConnection(node);
		s_logger.info(DUtils.Format("User was booted from the lobby. Username: {0} IP: {1} Mac: {2} Mod Username: {3} Mod IP: {4} Mod Mac: {5}", node.getName(), node.getAddress().getHostAddress(),
					mac, modNode.getName(), modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
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
	
	private boolean isPlayerAdmin(final INode node)
	{
		final String name = getRealName(node);
		final DBUserController controller = new DBUserController();
		final DBUser user = controller.getUser(name);
		if (user == null)
			return false;
		return user.isAdmin();
	}
	
	private String getRealName(final INode node)
	{
		// Remove any (n) that is added to distinguish duplicate names
		final String name = node.getName().split(" ")[0];
		return name;
	}
	
	public String getInformationOn(final INode node)
	{
		assertUserIsAdmin();
		final Set<String> aliases = new TreeSet<String>();
		for (final INode currentNode : m_messenger.getNodes())
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
	
	private String getAliasesFor(final INode node)
	{
		final StringBuilder builder = new StringBuilder();
		final String nodeMac = getNodeMacAddress(node);
		for (final INode cur : m_messenger.getNodes())
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
