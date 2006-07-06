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

package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.lobby.server.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class LobbyGameTableModel extends AbstractTableModel
{
    private enum Column {HostedBy, GameName, Players, Status, Join, PostDate, Port, GameRound}
    
    private final IMessenger m_messenger;
    private final IChannelMessenger m_channelMessenger;
    private final IRemoteMessenger m_remoteMessenger;
    
    //these must only be accessed in the swing event thread
    private List<GUID> m_gameIDs = new ArrayList<GUID>();
    private List<GameDescription> m_games = new ArrayList<GameDescription>();
    
    
    public LobbyGameTableModel(final IMessenger messenger, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger)
    {
        m_messenger = messenger;
        m_channelMessenger = channelMessenger;
        m_remoteMessenger = remoteMessenger;
        
        m_channelMessenger.registerChannelSubscriber(new ILobbyGameBroadcaster()
        {
        
            public void gameUpdated(GUID gameId, GameDescription description)
            {
                assertSentFromServer();
                updateGame(gameId, description);
            }
        
            public void gameAdded(GUID gameId, GameDescription description)
            {
                assertSentFromServer();
                addGame(gameId, description);
            }

            public void gameRemoved(GUID gameId)
            {
                assertSentFromServer();
                removeGame(gameId);
                
            }
        
        }, ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL  );
        
        Map<GUID, GameDescription> games = ((ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE)).listGames();
        for(GUID id : games.keySet())
        {
            addGame(id, games.get(id));
        }
        
        
    }
    
    public GameDescription get(int i)
    {
        return m_games.get(i);
    }
    


    private void removeGame(final GUID gameId)
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        int index = m_gameIDs.indexOf(gameId);
                        
                        m_gameIDs.remove(index);
                        m_games.remove(index);
                        
                        fireTableRowsDeleted(index, index);
                    }
                });
        
    }





    private void addGame(final GUID gameId, final GameDescription description)
    {
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
        if(!MessageContext.getSender().equals(m_messenger.getServerNode()))
            throw new IllegalStateException("Invalid sender");
        
    }


    private void updateGame(final GUID gameId, final GameDescription description)
    {
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
                
            case GameRound :
                return description.getRound();
                
            case GameName:
                return description.getGameName();
                
            case Players:
                return description.getPlayerCount();
    
            case Join:
                return "Click To Join";
            case Port:
                return description.getPort();

            case Status:
                return description.getStatus();
            case PostDate:
                return description.getStartDateTime();
            default:
                throw new IllegalStateException("Unknown column:" + column);
         }
    }
    
    

}
