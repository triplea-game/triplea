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

import java.lang.reflect.*;
import java.util.Properties;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.*;
import games.strategy.engine.message.*;

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
    
    public boolean isGameOver()
    {
        return m_game.isGameOver();
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
        if(m_game.isGameOver())
            throw new GameOverException("Game Over");
        try
        {
            IDelegate delegate = m_game.getData().getDelegateList().getDelegate(m_currentDelegate);
            return getRemoteThatChecksForGameOver(m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteName(delegate)));
        }
        catch(MessengerException me)
        {
            throw new GameOverException("Game Over");
        }
    }
    
    public Properties getStepProperties()
    {
        return m_game.getData().getSequence().getStep().getProperties();
    }
    
    private IRemote getRemoteThatChecksForGameOver(IRemote implementor)
    {
        Class[] classes = implementor.getClass().getInterfaces();
        GameOverInvocationHandler goih = new GameOverInvocationHandler(implementor, m_game);
        
        return (IRemote)  Proxy.newProxyInstance(implementor.getClass().getClassLoader(), classes, goih);
        
    }
    
}


class GameOverInvocationHandler implements InvocationHandler
{
    private final Object m_delegate;
    private final IGame m_game;
    
    public GameOverInvocationHandler(Object delegate, IGame game)
    {
        m_delegate = delegate;
        m_game = game;
    }

    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        try
        {
            return method.invoke(m_delegate, args);
        }
        catch(InvocationTargetException ite)
        {
            if(!m_game.isGameOver())
                throw ite;
            else
                throw new GameOverException("Game Over Exception");            
        }
        catch(RemoteNotFoundException rnfe)
        {
            throw new GameOverException("Game Over");
        }
        
    }
    
    
}
