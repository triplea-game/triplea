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
 * TestDelegateBridge.java
 *
 * Created on November 10, 2001, 7:39 PM
 */

package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.util.Random;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
public class TestDelegateBridge implements IDelegateBridge
{
    GameData m_data;
    PlayerID m_id;
    String m_stepName = "no name specified";

    Random m_rand = new Random(System.currentTimeMillis());

    private DelegateHistoryWriter m_historyWriter;

    /** Creates new TestDelegateBridge */
    public TestDelegateBridge(GameData data, PlayerID id)
    {
        m_data = data;
        m_id = id;
        History history = new History(m_data);
        HistoryWriter historyWriter = new HistoryWriter(history);
        historyWriter.startNextStep("", "", PlayerID.NULL_PLAYERID, "");
        m_historyWriter = new DelegateHistoryWriter(new ChannelMessenger( new UnifiedMessenger( new DummyMessenger())));

    }

    /**
     * Delegates should not use random data that comes from any other source.
     */
    public int getRandom(int max, String annotation)
    {
        return m_rand.nextInt(max);
    }

    public int[] getRandom(int max, int count, String annotation)
    {
        int[] r = new int[count];
        for (int i = 0; i < count; i++)
        {
            r[i] = getRandom(max, annotation);
        }
        return r;
    }

    /**
     * Changing the player has the effect of commiting the current transaction.
     * Player is initialized to the player specified in the xml data.
     */
    public void setPlayerID(PlayerID aPlayer)
    {
        m_id = aPlayer;

    }

    public boolean inTransaction()
    {
        return false;
    }

    public PlayerID getPlayerID()
    {
        return m_id;
    }

    public void addChange(Change aChange)
    {
        aChange.perform(m_data);
    }

    public void commit()
    {
    }

    public void startTransaction()
    {
    }

    public void rollback()
    {
    }



    public void setStepName(String name)
    {
        m_stepName = name;
    }

    /**
     * Returns the current step name
     */
    public String getStepName()
    {
        return m_stepName;
    }

    public DelegateHistoryWriter getHistoryWriter()
    {
        return m_historyWriter;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote()
     */
    public IRemote getRemote()
    {

        return null;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
     */
    public IRemote getRemote(PlayerID id)
    {

        return null;
    }

    /* (non-Javadoc)
     * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
     */
    public IChannelSubscribor getDisplayChannelBroadcaster()
    {
        return null;
    }


}