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
 * IMessageManager.java
 *
 * Created on December 26, 2001, 7:06 PM
 */

package games.strategy.engine.message;

import games.strategy.net.INode;

/**
 * A class for routing messages.
 * 
 * You can add a destination to a message manager on one machine, 
 * and messages sent to a message manager on another machine will
 * reach the destination.
 * 
 * You can use this to synchonize, since when sending a message the current 
 * thread will block until a response is received.
 * 
 * @author  Sean Bridges
 */
public interface IMessageManager
{
	public void addDestination(IDestination destination);
	public void removeDestination(IDestination destination);

	/**
	 * Blocks until a message is returned. 
	 */
	public Message send(Message msg, String destination);
	public void sendNoResponse(Message msg, String destination);
	
	/**
	 * Do we know about this destination.  
	 * The destination may be local or remote. 
	 */
	public boolean hasDestination(String destination);
}
