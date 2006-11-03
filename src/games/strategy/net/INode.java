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

package games.strategy.net;

import java.net.*;
import java.io.*;

/**
 * A Node in a network.
 * 
 * Node identity is based on address/port.  The name is just a display name<p>
 * 
 * Since different nodes may appear as different adresses to different nodes
 * (eg the server sees a node as its nat accesseble adress, while the node itself
 * sees itself as a subnet address), the address for a node is defined as the address
 * that the server sees!
 * 
 * @author sgb
 */
public interface INode extends Serializable, Comparable<INode>
{
    
    /**
     * 
     * @return the display/user name for the node
     */
	public String getName();

    /**
     * 
     * @return the adress for the node as seen by the server
     */
	public InetAddress getAddress();
	
    /**
     * 
     * @return the port for the node as seen by the server
     */ 
	public int getPort();
    
    /**
     * 
     * @return the adress for the node as seen by the server
     */
    public InetSocketAddress getSocketAddress();
}




