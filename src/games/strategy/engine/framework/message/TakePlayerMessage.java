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
 * TakePlayerMessage.java
 *
 *
 * Created on February 1, 2002, 2:45 PM
 */

package games.strategy.engine.framework.message;

/**
 * A message sent to the server from the client indicating the 
 * client wants to play the given player, or that the client 
 * no longer wishes to play the given player.
 * 
 *
 * @author  Sean Bridges
 */
public class TakePlayerMessage implements java.io.Serializable
{
	private final String m_playerName;
	private boolean m_play;
	
	/**
	 * Creates a new instance of TakePlayerMessage
	 */
	public TakePlayerMessage(String playerName, boolean play)
	{
		m_playerName = playerName;
		m_play = play;
	}
	
	public String getPlayerName()
	{
		return m_playerName;
	}
	
	public boolean play()
	{
		return m_play;
	}
	
	public String toString()
	{
		return "TakePlayerMessage take:" + m_playerName;
	}
	
}
