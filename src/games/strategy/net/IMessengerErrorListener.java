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
 * MessengerErrorListener.java
 *
 * Created on December 12, 2001, 10:46 AM
 */

package games.strategy.net;

import java.util.List;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface IMessengerErrorListener 
{
	/**
	 * The connection to the given node has been lost.
	 * unsent contains the messages that have not been sent.
	 */
	public void connectionLost(INode node, Exception reason, List unsent);

	/**
	 * The messenger is no longer able to send or receive messages.
	 * This signals that an error has occured, will not be sent if the 
	 * node was shutdown.
	 * Unsent contains the messages that have not been sent.
	 */
	public void messengerInvalid(IMessenger messenger, Exception reason, List unsent);
}
