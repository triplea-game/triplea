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
 * IConnectionAccepter.java
 *
 * Created on February 5, 2002, 1:11 PM
 */

package games.strategy.net;

/**
 * Used to determine if a connection can be added to the server.
 *
 * @author  Sean Bridges
 */
public interface IConnectionAccepter
{
	/**
	 * Called before a connection is added.  
	 *
	 * @return null if the connection is to be accepted, otherwise an error message.
	 */
	public String acceptConnection(IServerMessenger messenger, INode node);
}
