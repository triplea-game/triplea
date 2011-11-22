/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * TestDelegateBridge.java
 * 
 * Created on November 10, 2001, 7:39 PM
 */
package games.strategy.engine.data;

import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.ui.display.ITripleaDisplay;

import java.util.Properties;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Not for actual use, suitable for testing. Never returns messages, but can get
 *          random and implements changes immediately.
 */
public class TestDelegateBridge implements ITestDelegateBridge
{
	private final GameData m_data;
	private PlayerID m_id;
	private String m_stepName = "no name specified";
	private IChannelSubscribor m_dummyDisplay;
	private IRandomSource m_randomSource;
	private final IDelegateHistoryWriter m_historyWriter;
	private IRemote m_remote;
	
	/** Creates new TestDelegateBridge */
	public TestDelegateBridge(final GameData data, final PlayerID id, final IDisplay dummyDisplay)
	{
		m_data = data;
		m_id = id;
		m_dummyDisplay = dummyDisplay;
		final History history = new History(m_data);
		final HistoryWriter historyWriter = new HistoryWriter(history);
		historyWriter.startNextStep("", "", PlayerID.NULL_PLAYERID, "");
		final ChannelMessenger channelMessenger = new ChannelMessenger(new UnifiedMessenger(new DummyMessenger()));
		m_historyWriter = new DelegateHistoryWriter(channelMessenger);
	}
	
	public void setDisplay(final ITripleaDisplay display)
	{
		m_dummyDisplay = display;
	}
	
	/**
	 * Delegates should not use random data that comes from any other source.
	 */
	public int getRandom(final int max, final String annotation)
	{
		return m_randomSource.getRandom(max, annotation);
	}
	
	public int[] getRandom(final int max, final int count, final String annotation)
	{
		return m_randomSource.getRandom(max, count, annotation);
	}
	
	/**
	 * Changing the player has the effect of commiting the current transaction.
	 * Player is initialized to the player specified in the xml data.
	 */
	public void setPlayerID(final PlayerID aPlayer)
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
	
	public void addChange(final Change aChange)
	{
		aChange.perform(m_data);
	}
	
	public void setStepName(final String name)
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
	
	public IDelegateHistoryWriter getHistoryWriter()
	{
		return m_historyWriter;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegateBridge#getRemote()
	 */
	public IRemote getRemote()
	{
		return m_remote;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
	 */
	public IRemote getRemote(final PlayerID id)
	{
		return m_remote;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
	 */
	public IChannelSubscribor getDisplayChannelBroadcaster()
	{
		return m_dummyDisplay;
	}
	
	public Properties getStepProperties()
	{
		return new Properties();
	}
	
	public void leaveDelegateExecution()
	{
	}
	
	public void enterDelegateExecution()
	{
	}
	
	public void setRandomSource(final IRandomSource randomSource)
	{
		m_randomSource = randomSource;
	}
	
	public void setRemote(final IRemote remote)
	{
		m_remote = remote;
	}
	
	public void stopGameSequence()
	{
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.engine.delegate.IDelegateBridge#getData()
	 */
	public GameData getData()
	{
		return m_data;
	}
}
