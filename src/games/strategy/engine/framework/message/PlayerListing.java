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
 * PlayerListinge.java
 *
 * Created on February 1, 2002, 2:34 PM
 */

package games.strategy.engine.framework.message;

import java.io.Serializable;
import java.util.*;

import games.strategy.util.Version;

/**
 * data from the server indicating what players are available to be
 * taken, and what players are being played.
 *
 * This object also contains versioning info which the client should
 * check to ensure that it is playing the same game as the server.
 *
 * @author  Sean Bridges
 */
public class PlayerListing implements Serializable
{
 
   //keep compatability with older versions
   static final long serialVersionUID = -8913538086737733980L;
    
  /**
   * Maps String player name -> node Name
   * if node name is null then the player is available to play.
   */
  private final Map<String, String> m_playerListing;
  private final Version m_gameVersion;
  private final String m_gameName;
  private final String m_gameRound;

  /**
   * Creates a new instance of PlayerListingMessage
   */
  public PlayerListing(Map<String, String> map, Version gameVersion, String gameName, String gameRound)
  {
    m_playerListing = new HashMap<String, String>(map);
    m_gameVersion = gameVersion;
    m_gameName = gameName;
    m_gameRound = gameRound;
  }

  public Map<String,String> getPlayerListing()
  {
    return m_playerListing;
  }

  public String getGameName()
  {
    return m_gameName;
  }

  public Version getGameVersion()
  {
    return m_gameVersion;
  }

  public String toString()
  {
    return "PlayerListingMessage:" + m_playerListing;
  }

  public Set getPlayers()
  {
    return m_playerListing.keySet();
  }

  public String getGameRound()
  {
    return m_gameRound; 
  }


}

