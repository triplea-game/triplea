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
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.ISound;

import java.io.Serializable;

/**
 * Base class designed to make writing custom delegates simpler.
 * Code common to all delegates is implemented here.
 * 
 * @author Lane Schwartz (abstracted by Chris Duncan)
 */
public abstract class AbstractDelegate implements IDelegate
{
	protected String m_name;
	protected String m_displayName;
	protected PlayerID m_player;
	protected IDelegateBridge m_bridge;
	
	/**
	 * Creates a new instance of the Delegate
	 */
	public AbstractDelegate()
	{
	}
	
	public void initialize(final String name, final String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run, AND before "start" is called.
	 */
	public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge)
	{
		m_bridge = iDelegateBridge;
		m_player = iDelegateBridge.getPlayerID();
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 */
	public void start()
	{
		// nothing to do here
	}
	
	/**
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 */
	public void end()
	{
		// nothing to do here
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/**
	 * Returns the state of the Delegate.
	 * All classes should super.saveState if they override this.
	 */
	public Serializable saveState()
	{
		return null;
	}
	
	/**
	 * Loads the delegates state
	 */
	public void loadState(final Serializable state)
	{
		// nothing to save
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	public abstract Class<? extends IRemote> getRemoteType();
	
	public IDelegateBridge getBridge()
	{
		return m_bridge;
	}
	
	protected GameData getData()
	{
		return m_bridge.getData();
	}
	
	protected IDisplay getDisplay()
	{
		return getDisplay(m_bridge);
	}
	
	protected static IDisplay getDisplay(final IDelegateBridge bridge)
	{
		return bridge.getDisplayChannelBroadcaster();
	}
	
	protected ISound getSoundChannel()
	{
		return getSoundChannel(m_bridge);
	}
	
	protected static ISound getSoundChannel(final IDelegateBridge bridge)
	{
		return bridge.getSoundChannelBroadcaster();
	}
	
	protected IRemotePlayer getRemotePlayer()
	{
		return getRemotePlayer(m_bridge);
	}
	
	protected static IRemotePlayer getRemotePlayer(final IDelegateBridge bridge)
	{
		return bridge.getRemotePlayer();
	}
	
	/**
	 * You should override this class with some variation of the following code (changing the AI to be something meaningful if needed)
	 * because otherwise an "isNull" (ie: the static "Neutral" player) will not have any remote:
	 * <p>
	 * if (player.isNull()) { return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);} return bridge.getRemotePlayer(player);
	 * </p>
	 */
	protected IRemotePlayer getRemotePlayer(final PlayerID player)
	{
		// you should over ride this. (also, do not make a static version of this, in this class.)
		
		// if its the null player, return a do nothing proxy
		// if (player.isNull())
		// return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		return m_bridge.getRemotePlayer(player);
	}
}

/*
All overriding classes should use the following format for saveState and loadState, in order to save and load the superstate

class ExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}

@Override
public Serializable saveState()
{
	ExtendedDelegateState state = new ExtendedDelegateState();
	state.superState = super.saveState();
	// add other variables to state here:
	return state;
}

@Override
public void loadState(Serializable state)
{
	ExtendedDelegateState s = (ExtendedDelegateState) state;
	super.loadState(s.superState);
	// load other variables from state here:
}
*/
