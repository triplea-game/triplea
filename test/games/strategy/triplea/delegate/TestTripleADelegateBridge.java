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
 * TestTripleADelegateBridge.java
 *
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomSource;

/**
 * 
 * @author Tony Clayton
 * 
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
public class TestTripleADelegateBridge extends TripleADelegateBridge implements ITestDelegateBridge
{
    private final ITestDelegateBridge m_bridge;
    

    public TestTripleADelegateBridge(ITestDelegateBridge bridge, GameData data)
    {
        super(bridge, data);
        m_bridge = bridge;
        
    }


    public IDelegateHistoryWriter getHistoryWriter()
    {
        return m_bridge.getHistoryWriter();
    }

    /**
     * Changing the player has the effect of commiting the current transaction.
     * Player is initialized to the player specified in the xml data.
     */
    public void setPlayerID(PlayerID aPlayer)
    {
        m_bridge.setPlayerID(aPlayer);
    }

    public boolean inTransaction()
    {
        return m_bridge.inTransaction();
    }

    public void commit()
    {
        m_bridge.commit();
    }

    public void startTransaction()
    {
        m_bridge.startTransaction();
    }

    public void rollback()
    {
        m_bridge.rollback();
    }

    public void setStepName(String name)
    {
        m_bridge.setStepName(name);
    }

    public void setRandomSource(IRandomSource randomSource)
    {
        m_bridge.setRandomSource(randomSource);
    }

    
    public void setRemote(IRemote remote)
    {
        m_bridge.setRemote(remote);
    }

}
