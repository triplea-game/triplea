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
 * PlayerListingMessage.java
 *
 * Created on February 1, 2002, 2:34 PM
 */

package games.strategy.engine.framework.message;

import java.io.Serializable;
import java.util.*;

/**
 * A message from the server indicating what players are available to be
 * taken, and what players are being played.
 *
 * @author  Sean Bridges
 */
public class PlayerListingMessage implements Serializable
{
	/**
	 * Maps String player name -> node Name
	 * if ode name is null then the player is available to play.
	 */
	private Map m_playerListing = new HashMap();
	
	/**
	 * Creates a new instance of PlayerListingMessage
	 */
	public PlayerListingMessage(Map map)
	{
		m_playerListing = new HashMap(map);
	}
	
	public Map getPlayerListing()
	{
		return m_playerListing;
	}
	
	public String toString()
	{
		return "PlayerListingMessage:" + m_playerListing;
	}
}