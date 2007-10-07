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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import java.util.Properties;

/**
 * 
 * TripleA implementation of DelegateBridge
 * 
 * @author Tony Clayton
 */
public class TripleADelegateBridge implements IDelegateBridge
{

    private final IDelegateBridge m_bridge;
    private final TripleADelegateHistoryWriter m_historyWriter;
    private final GameData m_data;

    /** Creates new TripleADelegateBridge to wrap an existing IDelegateBridge */
    public TripleADelegateBridge(IDelegateBridge bridge, GameData data)
    {
        m_bridge = bridge;
        m_data = data;
        m_historyWriter = new TripleADelegateHistoryWriter(m_bridge, m_data);
    }

    /**
     * Return our custom historyWriter instead of the default one
     * 
     */
    public IDelegateHistoryWriter getHistoryWriter()
    {
        return m_historyWriter;
    }

    public PlayerID getPlayerID()
    {
        return m_bridge.getPlayerID();
    }

    /**
     * All delegates should use random data that comes from both players so that
     * neither player cheats.
     */
    public int getRandom(int max, String annotation)
    {
        return m_bridge.getRandom(max, annotation);
    }

    /**
     * Delegates should not use random data that comes from any other source.
     */
    public int[] getRandom(int max, int count, String annotation)
    {
        return m_bridge.getRandom(max, count, annotation);
    }

    public void addChange(Change aChange)
    {
        m_bridge.addChange(aChange);
    }

    /**
     * Returns the current step name
     */
    public String getStepName()
    {
        return m_bridge.getStepName();
    }
    
    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote()
     */
    public IRemote getRemote()
    {
        return  m_bridge.getRemote();
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
     */
    public IRemote getRemote(PlayerID id)
    {
        return m_bridge.getRemote(id);
    }

    /* (non-Javadoc)
     * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
     */
    public IChannelSubscribor getDisplayChannelBroadcaster()
    {
        return m_bridge.getDisplayChannelBroadcaster();
    }
    
    public Properties getStepProperties()
    {
        return m_bridge.getStepProperties();
    }
    
    public void leaveDelegateExecution()
    {
        m_bridge.leaveDelegateExecution();
    }
    
    public void enterDelegateExecution()
    {
        m_bridge.enterDelegateExecution();
    }

    public void stopGameSequence()
    {
        m_bridge.stopGameSequence();
    }
}
