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
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.HashSet;

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
	private boolean m_startBaseStepsFinished = false;
	private boolean m_endBaseStepsFinished = false;
	
	/**
	 * Creates a new instance of the Delegate
	 */
	public BaseDelegate()
	{
	}
	
	public void initialize(final String name, final String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 */
	public void start(final IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
		if (!m_startBaseStepsFinished)
		{
			m_startBaseStepsFinished = true;
			triggerWhenTriggerAttachments(TriggerAttachment.BEFORE);
		}
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
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 */
	public void end()
	{
		// normally nothing to do here
		// we are firing triggers, for maps that include them
		if (!m_endBaseStepsFinished)
		{
			m_endBaseStepsFinished = true;
			triggerWhenTriggerAttachments(TriggerAttachment.AFTER);
		}
		// these should probably be somewhere else, but we are relying on the fact that reloading a save go into the start step,
		// but nothing goes into the end step, and therefore there is no way to save then have the end step repeat itself
		m_startBaseStepsFinished = false;
		m_endBaseStepsFinished = false;
	}
	
	/**
	 * Returns the state of the Delegate.
	 * All classes should super.saveState if they override this.
	 */
	public Serializable saveState()
	{
		final BaseDelegateState state = new BaseDelegateState();
		state.m_startBaseStepsFinished = m_startBaseStepsFinished;
		state.m_endBaseStepsFinished = m_endBaseStepsFinished;
		return state;
	}
	
	/**
	 * Loads the delegates state
	 */
	public void loadState(final Serializable state)
	{
		final BaseDelegateState s = (BaseDelegateState) state;
		m_startBaseStepsFinished = s.m_startBaseStepsFinished;
		m_endBaseStepsFinished = s.m_endBaseStepsFinished;
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
	
	private void triggerWhenTriggerAttachments(final String beforeOrAfter)
	{
		final GameData data = getData();
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			final String stepName = data.getSequence().getStep().getName();
			
			// we use AND in order to make sure there are uses and when is set correctly.
			final Match<TriggerAttachment> baseDelegateWhenTriggerMatch = new CompositeMatchAnd<TriggerAttachment>(
						TriggerAttachment.availableUses,
						TriggerAttachment.whenOrDefaultMatch(beforeOrAfter, stepName));
			
			TriggerAttachment.collectAndFireTriggers(new HashSet<PlayerID>(data.getPlayerList().getPlayers()), baseDelegateWhenTriggerMatch, m_bridge, beforeOrAfter, stepName);
		}
		PoliticsDelegate.chainAlliancesTogether(m_bridge);
	}
}


@SuppressWarnings("serial")
class BaseDelegateState implements Serializable
{
	public boolean m_startBaseStepsFinished = false;
	public boolean m_endBaseStepsFinished = false;
}
/*
All overriding classes should use the following format for saveState and loadState, in order to save and load the superstate

@SuppressWarnings("serial")
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
