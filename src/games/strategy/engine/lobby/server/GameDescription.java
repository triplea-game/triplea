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
    private final GameStatus m_status;
    
    public GameDescription(INode hostedBy, int port, Date startDateTime, String gameName, int playerCount, GameStatus status)
    {
        m_hostedBy = hostedBy;
        m_port = port;
        m_startDateTime = startDateTime;
        m_gameName = gameName;
        m_playerCount = playerCount;
        m_status = status;
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
