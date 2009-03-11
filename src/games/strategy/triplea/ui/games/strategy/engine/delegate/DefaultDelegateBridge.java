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

package games.strategy.engine.delegate;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.RandomStats;

import java.util.Properties;

/**
 * 
 * Default implementation of DelegateBridge
 * 
 * @author Sean Bridges
 */
public class DefaultDelegateBridge implements IDelegateBridge
{

    private final GameData m_data;
    private final IGame m_game;
    private final IDelegateHistoryWriter m_historyWriter;
    private final RandomStats m_randomStats;
    private final DelegateExecutionManager m_delegateExecutionManager;

    private IRandomSource m_randomSource;

    /** Creates new DefaultDelegateBridge */
    public DefaultDelegateBridge(GameData data, IGame game,
            IDelegateHistoryWriter historyWriter, RandomStats randomStats,
            DelegateExecutionManager delegateExecutionManager)
    {
        m_data = data;
        m_game = game;
        m_historyWriter = historyWriter;
        m_randomStats = randomStats;
        m_delegateExecutionManager = delegateExecutionManager;
    }

    public PlayerID getPlayerID()
    {
        return m_data.getSequence().getStep().getPlayerID();
    }

    public void setRandomSource(IRandomSource randomSource)
    {
        m_randomSource = randomSource;
    }

    /**
     * All delegates should use random data that comes from both players so that
     * neither player cheats.
     */
    public int getRandom(int max, String annotation)
    {
        int random = m_randomSource.getRandom(max, annotation);
        m_randomStats.addRandom(random);
        return random;
    }

    /**
     * Delegates should not use random data that comes from any other source.
     */
    public int[] getRandom(int max, int count, String annotation)
    {
        int[] rVal = m_randomSource.getRandom(max, count, annotation);
        m_randomStats.addRandom(rVal);
        return rVal;
    }

    public void addChange(Change aChange)
    {
        if(aChange instanceof CompositeChange) {
            CompositeChange c = (CompositeChange) aChange;
            if(c.getChanges().size() == 1) {
                addChange(c.getChanges().get(0));
                return;
            }
        }
        if(!aChange.isEmpty())
            m_game.addChange(aChange);
    }


    /**
     * Returns the current step name
     */
    public String getStepName()
    {
        return m_data.getSequence().getStep().getName();
    }



    public IDelegateHistoryWriter getHistoryWriter()
    {
        return m_historyWriter;
    }

    
    private Object getOutbound(Object o)
    {
        Class<?>[] interfaces = o.getClass().getInterfaces();
        return m_delegateExecutionManager.createOutboundImplementation(o, interfaces);
    }
    
    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote()
     */
    public IRemote getRemote()
    {
        return  getRemote(getPlayerID());
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
     */
    public IRemote getRemote(PlayerID id)
    {
        try
        {
            Object implementor = m_game.getRemoteMessenger().getRemote(
                    ServerGame.getRemoteName(id, id.getData()));
            return (IRemote) getOutbound(implementor);
        }
        catch(MessengerException me)
        {
            throw new GameOverException("Game Over");
        }
    }

    /* (non-Javadoc)
     * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
     */
    public IChannelSubscribor getDisplayChannelBroadcaster()
    {
        Object implementor = m_game.getChannelMessenger().getChannelBroadcastor(ServerGame.getDisplayChannel(m_game.getData()));
        return (IChannelSubscribor) getOutbound(implementor);
    }
    
    public Properties getStepProperties()
    {
        return m_data.getSequence().getStep().getProperties();
    }
    
    public void leaveDelegateExecution()
    {
        m_delegateExecutionManager.leaveDelegateExecution();
    }
    
    public void enterDelegateExecution()
    {
        m_delegateExecutionManager.enterDelegateExecution();
    }

    public void stopGameSequence()
    {
        ((ServerGame) m_game).stopGameSequence();
        
    }
}
