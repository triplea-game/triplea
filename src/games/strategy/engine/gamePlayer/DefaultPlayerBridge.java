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
 * DefaultPlayerBridge.java
 *
 * Created on October 27, 2001, 8:55 PM
 */

package games.strategy.engine.gamePlayer;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.*;
import games.strategy.engine.message.IRemote;

/**
 * Default implementation of PlayerBridge.
 *
 * @author  Sean Bridges
 *
 */
public class DefaultPlayerBridge implements IPlayerBridge
{
    
    private final IGame m_game;
    private String m_currentStep;
    private String m_currentDelegate;
    
    
    /** Creates new DefaultPlayerBridge */
    public DefaultPlayerBridge(IGame aGame)
    {
        m_game = aGame;
        m_game.addGameStepListener(m_gameStepListener);
    }
    
    /**
     * Get the name of the current step being exectured.
     */
    public String getStepName()
    {
        return m_currentStep;
    }
    
    /**
     * Return the game data
     */
    public GameData getGameData()
    {
        return m_game.getData();
    }
    
    private GameStepListener m_gameStepListener = new GameStepListener()
    {
        public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
        {
            if(stepName == null)
                throw new IllegalArgumentException("Null step");
            if(delegateName == null)
                throw new IllegalArgumentException("Null delegate");
            
            m_currentStep = stepName;
            m_currentDelegate = delegateName;
        }
    };


    /* 
     * @see games.strategy.engine.gamePlayer.PlayerBridge#getRemote()
     */
    public IRemote getRemote()
    {
        IDelegate delegate = m_game.getData().getDelegateList().getDelegate(m_currentDelegate);
        return m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(delegate));
    }
    
}
