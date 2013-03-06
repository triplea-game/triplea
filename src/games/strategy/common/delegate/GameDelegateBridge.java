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
package games.strategy.common.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;

import java.util.Properties;

/**
 * 
 * TripleA implementation of DelegateBridge
 * 
 * @author Tony Clayton
 */
public class GameDelegateBridge implements IDelegateBridge
{
	private final IDelegateBridge m_bridge;
	private final GameDelegateHistoryWriter m_historyWriter;
	
	/**
	 * Creates new TripleADelegateBridge to wrap an existing IDelegateBridge
	 * 
	 * @param bridge
	 *            delegate bridge
	 * @param data
	 *            GameData object
	 * */
	public GameDelegateBridge(final IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_historyWriter = new GameDelegateHistoryWriter(m_bridge.getHistoryWriter(), getData());
	}
	
	public GameData getData()
	{
		return m_bridge.getData();
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
	public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_bridge.getRandom(max, player, diceType, annotation);
	}
	
	/**
	 * Delegates should not use random data that comes from any other source.
	 */
	public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_bridge.getRandom(max, count, player, diceType, annotation);
	}
	
	public void addChange(final Change aChange)
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
	public IRemotePlayer getRemotePlayer()
	{
		return m_bridge.getRemotePlayer();
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
	 */
	public IRemotePlayer getRemotePlayer(final PlayerID id)
	{
		return m_bridge.getRemotePlayer(id);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
	 */
	public IDisplay getDisplayChannelBroadcaster()
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
