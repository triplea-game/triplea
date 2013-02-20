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
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Base class designed to make writing custom TripleA delegates simpler.
 * Code common to all TripleA delegates is implemented here.
 * 
 * @author Veqryn
 */
public abstract class BaseTripleADelegate extends AbstractDelegate implements IDelegate
{
	private boolean m_startBaseStepsFinished = false;
	private boolean m_endBaseStepsFinished = false;
	
	/**
	 * Creates a new instance of the Delegate
	 */
	public BaseTripleADelegate()
	{
		super();
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in the edit delegate.
	 */
	@Override
	public void start()
	{
		super.start();
		if (!m_startBaseStepsFinished)
		{
			m_startBaseStepsFinished = true;
			triggerWhenTriggerAttachments(TriggerAttachment.BEFORE);
		}
	}
	
	/**
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in the edit delegate.
	 */
	@Override
	public void end()
	{
		super.end();
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
	@Override
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
	@Override
	public void loadState(final Serializable state)
	{
		final BaseDelegateState s = (BaseDelegateState) state;
		m_startBaseStepsFinished = s.m_startBaseStepsFinished;
		m_endBaseStepsFinished = s.m_endBaseStepsFinished;
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
	
	protected ITripleaDisplay getDisplay()
	{
		return getDisplay(m_bridge);
	}
	
	protected static ITripleaDisplay getDisplay(final IDelegateBridge bridge)
	{
		return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
	}
	
	protected ITripleaPlayer getRemotePlayer()
	{
		return getRemotePlayer(m_bridge);
	}
	
	protected static ITripleaPlayer getRemotePlayer(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
	
	protected ITripleaPlayer getRemotePlayer(final PlayerID player)
	{
		return getRemotePlayer(player, m_bridge);
	}
	
	protected static ITripleaPlayer getRemotePlayer(final PlayerID player, final IDelegateBridge bridge)
	{
		// if its the null player, return a do nothing proxy
		if (player.isNull())
			return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		return (ITripleaPlayer) bridge.getRemote(player);
	}
}


class BaseDelegateState implements Serializable
{
	private static final long serialVersionUID = 7130686697155151908L;
	public boolean m_startBaseStepsFinished = false;
	public boolean m_endBaseStepsFinished = false;
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
