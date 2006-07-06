package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.lobby.server.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class GameTableModel extends AbstractTableModel
{
    private enum Column {HostedBy, GameName, Players, Status, Join, PostDate}
    
    private final IMessenger m_messenger;
    private final IChannelMessenger m_channelMessenger;
    private final IRemoteMessenger m_remoteMessenger;
    
    //these must only be accessed in the swing event thread
    private List<GUID> m_gameIDs = new ArrayList<GUID>();
    private List<GameDescription> m_games = new ArrayList<GameDescription>();
    
    
    public GameTableModel(final IMessenger messenger, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger)
    {
        m_messenger = messenger;
        m_channelMessenger = channelMessenger;
        m_remoteMessenger = remoteMessenger;
        
        m_channelMessenger.registerChannelSubscriber(new IGameBroadcaster()
        {
        
            public void gameUpdated(GUID gameId, GameDescription description)
            {
                updateGame(gameId, description);
            }
        
            public void gameAdded(GUID gameId, GameDescription description)
            {
                addGame(gameId, description);
            }
        
        }, IGameBroadcaster.GAME_BROADCASTER_CHANNEL  );
        
        Map<GUID, GameDescription> games = ((IGameController) m_remoteMessenger.getRemote(IGameController.GAME_CONTROLLER_REMOTE)).listGames();
        for(GUID id : games.keySet())
        {
            addGame(id, games.get(id));
        }
        
        
    }
    
    
    


    private void addGame(final GUID gameId, final GameDescription description)
    {
        assertSentFromServer();
        
        SwingUtilities.invokeLater(
        new Runnable()
        {
            public void run()
            {
                m_gameIDs.add(gameId);
                m_games.add(description);
                
                fireTableRowsInserted(m_gameIDs.size() -1, m_gameIDs.size() -1 );
            }
        });
        
        
    }


    private void assertSentFromServer()
    {
        assertSentFromServer();
        
    }


    private void updateGame(final GUID gameId, final GameDescription description)
    {
        if(!MessageContext.getSender().equals(m_messenger.getServerNode()))
            throw new IllegalStateException("Invalid sender");
        
        SwingUtilities.invokeLater(
            new Runnable()
            {
                public void run()
                {
                    int index = m_gameIDs.indexOf(gameId);
                    
                    
                    m_games.set(index, description);
                    
                    fireTableRowsUpdated(index, index);
                }
            });
        
        
    }

    
    public String getColumnName(int column) 
    {
        return Column.values()[column].toString();
    }
    

    public int getColumnCount()
    {
        return Column.values().length;
    }


    public int getRowCount()
    {
        return m_gameIDs.size();
    }


    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Column column = Column.values()[columnIndex];
        GameDescription description = m_games.get(rowIndex);
        
        switch (column)
        {
            case HostedBy:
                return description.getHostedBy();
                
            case GameName:
                return description.getGameName();
                
            case Players:
                return description.getPlayerCount();
    
            case Join:
                return "Click To Join";
                
            case PostDate:
                return description.getStartDateTime();
            default:
                throw new IllegalStateException("Unknown column:" + column);
         }
    }
    
    

}
