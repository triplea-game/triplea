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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * Base class designed to make writing custom delegates simpler.
 * Code common to all delegates is implemented here.
 * 
 * @author Lane Schwartz
 */
public abstract class BaseDelegate implements IDelegate
{
	protected String m_name;
	protected String m_displayName;
	protected PlayerID m_player;
	protected IDelegateBridge m_bridge;
	
	/**
	 * Creates a new instance of the Delegate
	 */
	public BaseDelegate()
	{
	}
	
	@Override
	public void initialize(String name, String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		// No need to do anything special when this delegate stops
	}
	
	/**
	 * Returns the state of the Delegate.
	 */
	@Override
	public Serializable saveState()
	{
		// This delegate does not maintain internal state
		return null;
	}
	
	/**
	 * Loads the delegates state
	 */
	@Override
	public void loadState(Serializable state)
	{
		// This delegate does not maintain internal state
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public abstract Class<? extends IRemote> getRemoteType();
	
	@Override
	public IDelegateBridge getBridge()
	{
		return m_bridge;
	}
	
	protected GameData getData()
	{
		return m_bridge.getData();
	}
}
