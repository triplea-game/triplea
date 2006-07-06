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

import games.strategy.net.*;

import java.io.Serializable;
import java.util.Date;

public class GameDescription implements Serializable
{
    public enum GameStatus {IN_PROGRESS, WAITING_FOR_PLAYERS}
    
    private final INode m_hostedBy;
    private final int m_port;
    private final Date m_startDateTime;
    private final String m_gameName;
    private final int m_playerCount;
    private final int m_round;
    private final GameStatus m_status;
    
    public GameDescription(INode hostedBy, int port, Date startDateTime, String gameName, int playerCount, GameStatus status, int round)
    {
        m_hostedBy = hostedBy;
        m_port = port;
        m_startDateTime = startDateTime;
        m_gameName = gameName;
        m_playerCount = playerCount;
        m_status = status;
        m_round = round;
    }
    
    public int getRound()
    {
        return m_round;
    }
    public String getGameName()
    {
        return m_gameName;
    }
    public INode getHostedBy()
    {
        return m_hostedBy;
    }
    public int getPlayerCount()
    {
        return m_playerCount;
    }
    public int getPort()
    {
        return m_port;
    }
    public Date getStartDateTime()
    {
        return m_startDateTime;
    }
    public GameStatus getStatus()
    {
        return m_status;
    }
    
    
    
    
}
