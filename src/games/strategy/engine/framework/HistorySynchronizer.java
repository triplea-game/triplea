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

package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.EventChild;

import javax.swing.SwingUtilities;

/**
 * Synchronizes a GameData by listening on the history channel for messages.
 * 
 * All modifications to the History are done in the SwingEventThread, so 
 * this class can be used to display a history tree to the user.
 */
public class HistorySynchronizer
{

    //Note the GameData here and the game are not the same
    //we are keeping m_data in synch with the history of the game by listening
    //for changes
    //we do this because our data can change depending where in the history we
    // are
    //we want to be able to do this without changing the data for the game
    private final GameData m_data;
    private int m_currentRound;

    private final IGame m_game;

    public HistorySynchronizer(GameData data, IGame game)
    {
        //this is not the way to use this.
        if (game.getData() == data)
            throw new IllegalStateException("You dont need a history synchronizer to synchronize game data that is managed by an IGame");

        m_data = data;
        m_data.forceChangesOnlyInSwingEventThread();
        data.acquireReadLock();
        try
        {
            m_currentRound = data.getSequence().getRound();
        }
        finally 
        {
            data.releaseReadLock();
        }

        m_game = game;
        m_game.getChannelMessenger().registerChannelSubscriber(m_gameModifiedChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
    }

    private IGameModifiedChannel m_gameModifiedChannelListener = new IGameModifiedChannel()
    {

        public void gameDataChanged(final Change aChange)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {

                    Change localizedChange = (Change) translateIntoMyData(aChange);
                    m_data.getHistory().getHistoryWriter().addChange(localizedChange);
                }
            });
        }

        public void startHistoryEvent(final String event)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {

                    m_data.getHistory().getHistoryWriter().startEvent(event);
                }
            });

        }

        public void addChildToEvent(final String text, final Object renderingData)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {

                    Object translatedRenderingData = translateIntoMyData(renderingData);
                    m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, translatedRenderingData));
                }
            });
        }

        public void setRenderingData(final Object renderingData)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {

                    Object translatedRenderingData = translateIntoMyData(renderingData);
                    m_data.getHistory().getHistoryWriter().setRenderingData(translatedRenderingData);
                }
            });

        }

        public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, boolean loadedFromSavedGame)
        {
            
            //we dont need to advance the game step in this case
            if(loadedFromSavedGame)
                return;
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    if (m_currentRound != round)
                    {
                        m_currentRound = round;
                        m_data.getHistory().getHistoryWriter().startNextRound(m_currentRound);
                    }
                    m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
                }
            });

        }

        public void shutDown()
        {}

    };


    public void deactivate()
    {
        m_game.getChannelMessenger().unregisterChannelSubscriber(m_gameModifiedChannelListener, IGame.GAME_MODIFICATION_CHANNEL);
    }

    /**
     * Serializes the object and then deserializes it, resolving object
     * references into m_data. Note the the history we are synching may refer to
     * a different game data than the GaneData held by the IGame. A clone is
     * made so that we can walk up and down the history without changing the
     * game.
     */
    private Object translateIntoMyData(Object msg)
    {
        return GameDataUtils.translateIntoOtherGameData(msg, m_data);
    }

}