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

/**
 * 
 * NOTE - this class is not thread safe. Modifications should be done holding an
 * external lock.
 * 
 * @author sgb
 */
public class GameDescription implements Serializable, Cloneable
{
    public enum GameStatus
    {
        LAUNCHING
        {
            public String toString()
            {
                return "Launching";
            }
        },
        IN_PROGRESS
        {
            public String toString()
            {
                return "In Progress";
            }
        },
        WAITING_FOR_PLAYERS
        {
            public String toString()
            {
                return "Waiting For Players";
            }
        }
    }

    private INode m_hostedBy;
    private int m_port;
    private Date m_startDateTime;
    private String m_gameName;
    private int m_playerCount;
    private String m_round;
    private GameStatus m_status;
    private int m_version = Integer.MIN_VALUE;
    private String m_hostName;
    private String m_comment;

    public GameDescription(INode hostedBy, int port, Date startDateTime, String gameName, int playerCount, GameStatus status, String round, String hostName, String comment)
    {
        m_hostName = hostName;
        m_hostedBy = hostedBy;
        m_port = port;
        m_startDateTime = startDateTime;
        m_gameName = gameName;
        m_playerCount = playerCount;
        m_status = status;
        m_round = round;
        m_comment = comment;
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        } catch (CloneNotSupportedException e)
        {
            throw new IllegalStateException("how did that happen");
        }
    }

    /**
     * The version number is updated after every change. This handles
     * synchronization problems where updates arrive out of order
     * 
     */
    public int getVersion()
    {
        return m_version;
    }

    public void setGameName(String gameName)
    {
        m_version++;
        m_gameName = gameName;
    }

    public void setHostedBy(INode hostedBy)
    {
        m_version++;
        m_hostedBy = hostedBy;
    }

    public void setPlayerCount(int playerCount)
    {
        m_version++;
        m_playerCount = playerCount;
    }

    public void setPort(int port)
    {
        m_version++;
        m_port = port;
    }

    public void setRound(String round)
    {
        m_version++;
        m_round = round;
    }

    public void setStartDateTime(Date startDateTime)
    {
        m_version++;
        m_startDateTime = startDateTime;
    }

    public void setStatus(GameStatus status)
    {
        m_version++;
        m_status = status;
    }

    public String getRound()
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

    public String getHostName()
    {
        return m_hostName;
    }

    public void setHostName(String hostName)
    {
        m_version++;
        m_hostName = hostName;
    }

    public String getComment()
    {
        return m_comment;
    }

    public void setComment(String comment)
    {
        m_version++;
        m_comment = comment;
    }

}
