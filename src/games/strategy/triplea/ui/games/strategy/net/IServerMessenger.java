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

/*
 * IServerMessenger.java
 *
 * Created on December 14, 2001, 1:02 PM
 */

package games.strategy.net;

import java.util.Set;

/**
 *
 * A server messenger.  Additional methods for accepting new connections.
 *
 * @author  Sean Bridges
 */
public interface IServerMessenger extends IMessenger
{
	public void setAcceptNewConnections(boolean accept);
	
    public boolean isAcceptNewConnections();
    
    public void setLoginValidator(ILoginValidator loginValidator);
    public ILoginValidator getLoginValidator();
    
    /**
     * Add a listener for change in connection status.
     */
    public void addConnectionChangeListener(IConnectionChangeListener listener);

    /**
     * Remove a listener for change in connection status.
     */
    public void removeConnectionChangeListener(IConnectionChangeListener listener);
    
    /**
     * Remove the node from the network.
     */
    public void removeConnection(INode node);
    
    /**
     * Get a list of nodes.
     */
    public Set<INode> getNodes();
}

