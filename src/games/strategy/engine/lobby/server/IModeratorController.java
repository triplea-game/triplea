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

import java.util.Date;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

public interface IModeratorController extends IRemote
{

    /**
     * Boot the given INode from the network.<p>
     * 
     * This method can only be called by admin users.
     * 
     */
    public void boot(INode node);
    
    /**
     * Ban the ip of the given INode. The ban will last for 48 hours.
     */
    public void banIp(INode node, Date banExpires);
    
    /**
     * Reset the password of the given user. returning true if the password was updated.<p>
     * 
     * You cannot change the password of an anonymous node, and you cannot change the password for an admin user.<p>
     */
    public boolean setPassword(INode node, String hashedPassword);
    
    
    /**
     * Is the current user an admin.
     */
    public boolean isAdmin();
}
