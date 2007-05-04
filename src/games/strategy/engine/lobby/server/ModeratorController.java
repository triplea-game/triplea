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

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

import java.util.Date;

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
        new BannedIpController().addBannedIp(node.getAddress().getHostAddress(), banExpires);
        boot(node);
    }

    public void boot(INode node)
    {
        assertUserIsAdmin();
        
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
        String name = getRealName(node);
        DBUserController controller = new DBUserController();
        DBUser user = controller.getUser(name);
        if(user == null)
            return false;
        return user.isAdmin(); 
    }

    private String getRealName(INode node)
    {
        //remove any (n) that is added to distinguish duplicate names
        String name = node.getName().split(" ")[0];
        return name;
    }

    public boolean setPassword(INode node, String hashedPassword)
    {
        assertUserIsAdmin();
        DBUserController controller = new DBUserController();
        DBUser user = controller.getUser(getRealName(node));
        if(user == null)
            return false;
        //don't allow changing an admin password
        if(user.isAdmin()) 
            return false;
        
        controller.updateUser(user.getName(), user.getEmail(), hashedPassword, user.isAdmin());
        return true;
    }
    
    
    
}