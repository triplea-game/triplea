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
 * TechActivationDelegate.java
 * 
 * Created on December 7, 2004, 9:55 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Logic for activating tech rolls. This delegate requires the
 * TechnologyDelegate to run correctly.
 * 
 * @author Ali Ibrahim
 * @version 1.0
 */
public class TechActivationDelegate extends BaseDelegate
{
	private boolean m_needToInitialize = true;
	
	/** Creates new TechActivationDelegate */
	public TechActivationDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run. In this class, this does all the
	 * work.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(aBridge);
		final GameData data = getData();
		if (!m_needToInitialize)
			return;
		// Activate techs
		final Map<PlayerID, Collection<TechAdvance>> techMap = DelegateFinder.techDelegate(data).getAdvances();
		final Collection<TechAdvance> advances = techMap.get(m_player);
		if ((advances != null) && (advances.size() > 0))
		{
			// Start event
			m_bridge.getHistoryWriter().startEvent(m_player.getName() + " activating " + advancesAsString(advances));
			for (final TechAdvance advance : advances)
			{
				// advance.perform(m_bridge.getPlayerID(), m_bridge, m_data);
				TechTracker.addAdvance(m_player, m_bridge, advance);
			}
		}
		// empty
		techMap.put(m_player, null);
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			// First set up a match for what we want to have fire as a default in this delegate. List out as a composite match OR.
			// use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
			final Match<TriggerAttachment> techActivationDelegateTriggerMatch = new CompositeMatchAnd<TriggerAttachment>(
						TriggerAttachment.availableUses,
						TriggerAttachment.whenOrDefaultMatch(null, null),
						new CompositeMatchOr<TriggerAttachment>(
									TriggerAttachment.unitPropertyMatch(),
									TriggerAttachment.techMatch(),
									TriggerAttachment.supportMatch()));
			// get all possible triggers based on this match.
			final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
						new HashSet<PlayerID>(Collections.singleton(m_player)), techActivationDelegateTriggerMatch, m_bridge);
			if (!toFirePossible.isEmpty())
			{
				// get all conditions possibly needed by these triggers, and then test them.
				final HashMap<ICondition, Boolean> testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
				// get all triggers that are satisfied based on the tested conditions.
				final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(Match.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
				// now list out individual types to fire, once for each of the matches above.
				TriggerAttachment.triggerUnitPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true, true);
				TriggerAttachment.triggerTechChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true, true);
				TriggerAttachment.triggerSupportChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true, true);
			}
		}
		shareTechnology();
		m_needToInitialize = false;
	}
	
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
	}
	
	private void shareTechnology()
	{
		final PlayerAttachment pa = PlayerAttachment.get(m_player);
		if (pa == null)
			return;
		final Collection<PlayerID> shareWith = pa.getShareTechnology();
		if (shareWith == null || shareWith.isEmpty())
			return;
		final GameData data = getData();
		final Collection<TechAdvance> currentAdvances = TechTracker.getCurrentTechAdvances(m_player, data);
		for (final PlayerID p : shareWith)
		{
			final Collection<TechAdvance> availableTechs = TechnologyDelegate.getAvailableTechs(p, data);
			final Collection<TechAdvance> toGive = Util.intersection(currentAdvances, availableTechs);
			if (!toGive.isEmpty())
			{
				// Start event
				m_bridge.getHistoryWriter().startEvent(m_player.getName() + " giving technology to " + p.getName() + ": " + advancesAsString(toGive));
				for (final TechAdvance advance : toGive)
				{
					TechTracker.addAdvance(p, m_bridge, advance);
				}
			}
		}
	}
	
	@Override
	public Serializable saveState()
	{
		final TechActivationExtendedDelegateState state = new TechActivationExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final TechActivationExtendedDelegateState s = (TechActivationExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
	}
	
	// Return string representing all advances in collection
	private String advancesAsString(final Collection<TechAdvance> advances)
	{
		final Iterator<TechAdvance> iter = advances.iterator();
		int count = advances.size();
		final StringBuilder text = new StringBuilder();
		while (iter.hasNext())
		{
			final TechAdvance advance = iter.next();
			text.append(advance.getName());
			count--;
			if (count > 1)
				text.append(", ");
			if (count == 1)
				text.append(" and ");
		}
		return text.toString();
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return null;
	}
}


class TechActivationExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 1742776261442260882L;
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
}
