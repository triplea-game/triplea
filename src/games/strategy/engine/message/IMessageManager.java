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

import java.util.Set;

/**
 * A class for routing messages.
 * 
 * An IMessageManager differs from an IMessenger in 2 respects.
 * 
 * 1) Messages sent via a IMessageManager are sent to a particular destination,
 * while messages sent to an IMessenger are sent to all the listeners
 * of the IMessenger.
 * 
 * 2) The IMessageManager has the ability to block the sending thread until the
 * message has been recieved, and return a response.  You can think of it as a 
 * very simple rmi with only 1 method Obejt send(Object message)
 * 
 * The IMessageManager is meant to sit on top of an IMessenger, adding
 * the thread blocking and routing abilities to the simpler IMessenger.
 * 
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
	
	public void broadcastAndWait(Message msg, Set destinations);

	
	/**
	 * Do we know about this destination.  
	 * The destination may be local or remote. 
	 */
	public boolean hasDestination(String destination);
}
