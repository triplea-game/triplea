/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.lobby.server.userDB.MutedIpController;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.ServerMessenger;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

public class ModeratorController implements IModeratorController
{

    private final IServerMessenger m_messenger;
    
    public static final RemoteName getModeratorControllerName() 
    {
        return new RemoteName(IModeratorController.class, "games.strategy.engine.lobby.server.ModeratorController:Global");
    }
    
    public void register(IRemoteMessenger messenger)
    {
        messenger.registerRemote(this, getModeratorControllerName());
    }
    
    public ModeratorController(IServerMessenger messenger)
    {
        m_messenger = messenger;
    }
    public void banIp(INode node, Date banExpires)
    {
        assertUserIsAdmin();
        if(isPlayerAdmin(node))
            throw new IllegalStateException("Can't ban an admin");

        new BannedIpController().addBannedIp(node.getAddress().getHostAddress(), banExpires);
        boot(node);
    }
    public void banMac(INode node, Date banExpires)
    {
        assertUserIsAdmin();
        if(isPlayerAdmin(node))
            throw new IllegalStateException("Can't ban an admin");

        String mac = getNodeMacAddress(node);
        new BannedMacController().addBannedMac(mac, banExpires);
        boot(node);
    }
    public void muteIp(INode node, Date muteExpires)
    {
        assertUserIsAdmin();
        if(isPlayerAdmin(node))
            throw new IllegalStateException("Can't mute an admin");

        new MutedIpController().addMutedIp(node.getAddress().getHostAddress(), muteExpires);
        ServerMessenger.getInstance().NotifyIPMutingOfPlayer(node.getAddress().getHostAddress(), muteExpires.getTime());
    }
    public void muteMac(INode node, Date muteExpires)
    {
        assertUserIsAdmin();
        if(isPlayerAdmin(node))
            throw new IllegalStateException("Can't mute an admin");

        String mac = getNodeMacAddress(node);
        new MutedMacController().addMutedMac(mac, muteExpires);
        ServerMessenger.getInstance().NotifyMacMutingOfPlayer(mac, muteExpires.getTime());
    }
    private String getNodeMacAddress(INode node)
    {
        return ServerMessenger.getInstance().GetMacAddressesOfPlayers().get(node.getName());
    }

    public void boot(INode node)
    {
        assertUserIsAdmin();
        if(!MessageContext.getSender().getName().equals("Admin") && isPlayerAdmin(node)) //Let the master lobby administrator boot admins
            throw new IllegalStateException("Can't boot an admin");
        
        //you can't boot the server node
        if(m_messenger.getServerNode().equals(node)) 
        {            
            throw new IllegalStateException("Cant boot server node");
        }
        m_messenger.removeConnection(node);
    }

    void assertUserIsAdmin() 
    {
        if(!isAdmin()) 
        {
            throw new IllegalStateException("Not an admin");
        }            
    }
    
    public boolean isAdmin() 
    {
        INode node = MessageContext.getSender();
        return isPlayerAdmin(node);
    }
    private boolean isPlayerAdmin(INode node)
    {
        String name = getRealName(node);
        DBUserController controller = new DBUserController();
        DBUser user = controller.getUser(name);
        if (user == null)
            return false;
        return user.isAdmin();
    }

    private String getRealName(INode node)
    {
        //remove any (n) that is added to distinguish duplicate names
        String name = node.getName().split(" ")[0];
        return name;
    }
    
    public String getInformationOn(INode node)
    {
        assertUserIsAdmin();
        
        Set<String> aliases = new TreeSet<String>();
        
        for(INode currentNode : m_messenger.getNodes()) 
        {            
            if(currentNode.getAddress().equals(node.getAddress()))
                aliases.add(currentNode.getName());            
        }
        String mac = getNodeMacAddress(node);
                
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ").append(node.getName());
        builder.append("\r\nHost Name: ").append(node.getAddress().getHostName());
        builder.append("\r\nIP Address: ").append(node.getAddress().getHostAddress());
        builder.append("\r\nMAC Address: ").append(mac);
        builder.append("\r\nPort: ").append(node.getPort());
        builder.append("\r\nAliases: ").append(getAliasesFor(node));
        return builder.toString();
    }
    private String getAliasesFor(INode node)
    {
        StringBuilder builder = new StringBuilder();
        String nodeMac = getNodeMacAddress(node);
        for (INode cur : m_messenger.getNodes())
        {
            if(cur.equals(node) || cur.getName().equals("Admin"))
                continue;
            if (cur.getAddress().equals(node.getAddress()) || getNodeMacAddress(cur).equals(nodeMac))
            {
                if(builder.length() > 0)
                    builder.append(", ");
                builder.append(cur.getName());
            }
        }
        return builder.toString();
    }

    public String setPassword(INode node, String hashedPassword)
    {
        assertUserIsAdmin();
        DBUserController controller = new DBUserController();
        DBUser user = controller.getUser(getRealName(node));
        if(user == null)
            return "Can't set the password of an anonymous player";
        //Don't allow changing an admin password
        if(user.isAdmin()) 
            return "Can't set the password of an admin";
        
        controller.updateUser(user.getName(), user.getEmail(), hashedPassword, user.isAdmin());
        return null;
    }
}