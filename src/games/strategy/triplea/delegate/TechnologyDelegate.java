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
 * TechnolgoyDelegate.java
 * 
 * 
 * Created on November 25, 2001, 4:16 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Logic for dealing with player tech rolls. This class requires the
 * TechActivationDelegate which actually activates the tech.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TechnologyDelegate extends BaseDelegate implements ITechDelegate
{
	private int m_techCost;
	private HashMap<PlayerID, Collection<TechAdvance>> m_techs;
	private TechnologyFrontier m_techCategory;
	private TripleADelegateBridge m_bridge;
	private boolean m_needToInitialize = true;
	
	/** Creates new TechnolgoyDelegate */
	public TechnologyDelegate()
	{
	}
	
	@Override
	public void initialize(final String name, final String displayName)
	{
		super.initialize(name, displayName);
		m_techs = new HashMap<PlayerID, Collection<TechAdvance>>();
		m_techCost = -1;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		m_bridge = new TripleADelegateBridge(aBridge);
		super.start(m_bridge);
		if (!m_needToInitialize)
			return;
		if (games.strategy.triplea.Properties.getTriggers(getData()))
		{
			// First set up a match for what we want to have fire as a default in this delegate. List out as a composite match OR.
			// use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
			final Match<TriggerAttachment> technologyDelegateTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
						TriggerAttachment.techAvailableMatch(null, null));
			// get all possible triggers based on this match.
			final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
						new HashSet<PlayerID>(Collections.singleton(m_player)), technologyDelegateTriggerMatch, m_bridge);
			if (!toFirePossible.isEmpty())
			{
				// get all conditions possibly needed by these triggers, and then test them.
				final HashMap<ICondition, Boolean> testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
				// get all triggers that are satisfied based on the tested conditions.
				final List<TriggerAttachment> toFireTestedAndSatisfied = Match.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions));
				// now list out individual types to fire, once for each of the matches above.
				TriggerAttachment.triggerAvailableTechChange(new HashSet<TriggerAttachment>(toFireTestedAndSatisfied), m_bridge, null, null);
			}
		}
		m_needToInitialize = false;
	}
	
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
	}
	
	@Override
	public Serializable saveState()
	{
		final TechnologyExtendedDelegateState state = new TechnologyExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		state.m_techs = m_techs;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final TechnologyExtendedDelegateState s = (TechnologyExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
		m_techs = s.m_techs;
	}
	
	public Map<PlayerID, Collection<TechAdvance>> getAdvances()
	{
		return m_techs;
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	private boolean isWW2V3TechModel()
	{
		return games.strategy.triplea.Properties.getWW2V3TechModel(getData());
	}
	
	private boolean isSelectableTechRoll()
	{
		return games.strategy.triplea.Properties.getSelectableTechRoll(getData());
	}
	
	private boolean isLL_TECH_ONLY()
	{
		return games.strategy.triplea.Properties.getLL_TECH_ONLY(getData());
	}
	
	public TechResults rollTech(final int techRolls, final TechnologyFrontier techToRollFor, final int newTokens)
	{
		int rollCount = techRolls;
		if (isWW2V3TechModel())
			rollCount = newTokens;
		final boolean canPay = checkEnoughMoney(rollCount);
		if (!canPay)
			return new TechResults("Not enough money to pay for that many tech rolls.");
		chargeForTechRolls(rollCount);
		int m_currTokens = 0;
		if (isWW2V3TechModel())
			m_currTokens = m_player.getResources().getQuantity(Constants.TECH_TOKENS);
		final GameData data = getData();
		if (getAvailableTechs().isEmpty())
		{
			if (isWW2V3TechModel())
			{
				final Resource techTokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
				final String transcriptText = m_player.getName() + " No more available tech advances.";
				m_bridge.getHistoryWriter().startEvent(transcriptText);
				final Change removeTokens = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), techTokens, -m_currTokens);
				m_bridge.addChange(removeTokens);
			}
			return new TechResults("No more available tech advances.");
		}
		final String annotation = m_player.getName() + " rolling for tech.";
		int[] random;
		int techHits = 0;
		int remainder = 0;
		final int diceSides = data.getDiceSides();
		if (EditDelegate.getEditMode(data))
		{
			final ITripleaPlayer tripleaPlayer = (ITripleaPlayer) m_bridge.getRemote();
			random = tripleaPlayer.selectFixedDice(techRolls, diceSides, true, annotation, diceSides);
			techHits = getTechHits(random);
		}
		else if (isLL_TECH_ONLY())
		{
			techHits = techRolls / diceSides;
			remainder = techRolls % diceSides;
			if (remainder > 0)
			{
				random = m_bridge.getRandom(diceSides, 1, annotation);
				if (random[0] + 1 <= remainder)
					techHits++;
			}
			else
			{
				random = m_bridge.getRandom(diceSides, 1, annotation);
				remainder = diceSides;
			}
		}
		else
		{
			random = m_bridge.getRandom(diceSides, techRolls, annotation);
			techHits = getTechHits(random);
		}
		final boolean selectableTech = isSelectableTechRoll() || isWW2V2();
		final String directedTechInfo = selectableTech ? " for " + techToRollFor.getTechs().get(0) : "";
		m_bridge.getHistoryWriter().startEvent(
					m_player.getName() + (random.hashCode() > 0 ? " roll " : " rolls : ") + MyFormatter.asDice(random) + directedTechInfo + " and gets " + techHits + " "
								+ MyFormatter.pluralize("hit", techHits));
		if (techHits > 0 && isWW2V3TechModel())
		{
			m_techCategory = techToRollFor;
			// remove all the tokens
			final Resource techTokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
			final String transcriptText = m_player.getName() + " removing all Technology Tokens after successful research.";
			m_bridge.getHistoryWriter().startEvent(transcriptText);
			final Change removeTokens = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), techTokens, -m_currTokens);
			m_bridge.addChange(removeTokens);
		}
		if (isLL_TECH_ONLY())
			m_bridge.getHistoryWriter().setRenderingData(new DiceRoll(random, techHits, remainder, false));
		else
			m_bridge.getHistoryWriter().setRenderingData(new DiceRoll(random, techHits, diceSides - 1, true));
		Collection<TechAdvance> advances;
		if (selectableTech)
		{
			if (techHits > 0)
				advances = Collections.singletonList(techToRollFor.getTechs().get(0));
			else
				advances = Collections.emptyList();
		}
		else
		{
			advances = getTechAdvances(techHits);
		}
		// Put in techs so they can be activated later.
		m_techs.put(m_player, advances);
		final List<String> advancesAsString = new ArrayList<String>();
		final Iterator<TechAdvance> iter = advances.iterator();
		int count = advances.size();
		final StringBuilder text = new StringBuilder();
		while (iter.hasNext())
		{
			final TechAdvance advance = iter.next();
			text.append(advance.getName());
			count--;
			advancesAsString.add(advance.getName());
			if (count > 1)
				text.append(", ");
			if (count == 1)
				text.append(" and ");
		}
		final String transcriptText = m_player.getName() + " discover " + text.toString();
		if (advances.size() > 0)
			m_bridge.getHistoryWriter().startEvent(transcriptText);
		return new TechResults(random, remainder, techHits, advancesAsString, m_player);
	}
	
	private List<TechAdvance> getAvailableTechs()
	{
		final GameData data = getData();
		data.acquireReadLock();
		try
		{
			final Collection<TechAdvance> currentAdvances = TechTracker.getTechAdvances(m_player, data);
			final Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(data, m_player);
			return Util.difference(allAdvances, currentAdvances);
		} finally
		{
			data.releaseReadLock();
		}
	}
	
	boolean checkEnoughMoney(final int rolls)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		final int cost = rolls * getTechCost();
		final int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
		return has >= cost;
	}
	
	private void chargeForTechRolls(final int rolls)
	{
		final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
		// TODO techCost
		final int cost = rolls * getTechCost();
		final String transcriptText = m_bridge.getPlayerID().getName() + " spend " + cost + " on tech rolls";
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		final Change charge = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), PUs, -cost);
		m_bridge.addChange(charge);
		if (isWW2V3TechModel())
		{
			final Resource tokens = getData().getResourceList().getResource(Constants.TECH_TOKENS);
			final Change newTokens = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), tokens, rolls);
			m_bridge.addChange(newTokens);
		}
	}
	
	private int getTechHits(final int[] random)
	{
		int count = 0;
		for (int i = 0; i < random.length; i++)
		{
			if (random[i] == getData().getDiceSides() - 1)
				count++;
		}
		return count;
	}
	
	private Collection<TechAdvance> getTechAdvances(int hits)
	{
		List<TechAdvance> available = new ArrayList<TechAdvance>();
		if (hits > 0 && isWW2V3TechModel())
		{
			available = getAvailableAdvancesForCategory(m_techCategory);
			hits = 1;
		}
		else
		{
			available = getAvailableAdvances();
		}
		if (available.isEmpty())
			return Collections.emptyList();
		if (hits >= available.size())
			return available;
		if (hits == 0)
			return Collections.emptyList();
		final Collection<TechAdvance> newAdvances = new ArrayList<TechAdvance>(hits);
		final String annotation = m_player.getName() + " rolling to see what tech advances are aquired";
		int[] random;
		if (EditDelegate.getEditMode(getData()))
		{
			final ITripleaPlayer tripleaPlayer = (ITripleaPlayer) m_bridge.getRemote();
			random = tripleaPlayer.selectFixedDice(hits, 0, true, annotation, getData().getDiceSides());
		}
		else
		{
			random = new int[hits];
			final List<Integer> rolled = new ArrayList<Integer>();
			// generating discrete rolls. messy, can't think of a more elegant way
			// hits guaranteed to be less than available at this point.
			for (int i = 0; i < hits; i++)
			{
				int roll = m_bridge.getRandom(available.size() - i, annotation);
				for (final int r : rolled)
				{
					if (roll >= r)
						roll++;
				}
				random[i] = roll;
				rolled.add(roll);
			}
		}
		final List<Integer> rolled = new ArrayList<Integer>();
		for (int i = 0; i < random.length; i++)
		{
			final int index = random[i];
			// check in case of dice chooser.
			if (!rolled.contains(index) && index < available.size())
			{
				newAdvances.add(available.get(index));
				rolled.add(index);
			}
		}
		m_bridge.getHistoryWriter().startEvent("Rolls to resolve tech hits:" + MyFormatter.asDice(random));
		return newAdvances;
	}
	
	private List<TechAdvance> getAvailableAdvances()
	{
		// too many
		final Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(getData(), m_bridge.getPlayerID());
		final Collection<TechAdvance> playersAdvances = TechTracker.getTechAdvances(m_bridge.getPlayerID(), getData());
		final List<TechAdvance> available = Util.difference(allAdvances, playersAdvances);
		return available;
	}
	
	private List<TechAdvance> getAvailableAdvancesForCategory(final TechnologyFrontier techCategory)
	{
		// Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(m_data, techCategory);
		final Collection<TechAdvance> playersAdvances = TechTracker.getTechAdvances(m_bridge.getPlayerID(), getData());
		final List<TechAdvance> available = Util.difference(techCategory.getTechs(), playersAdvances);
		return available;
	}
	
	public int getTechCost()
	{
		m_techCost = TechTracker.getTechCost(m_player);
		return m_techCost > 0 ? m_techCost : Constants.TECH_ROLL_COST;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<ITechDelegate> getRemoteType()
	{
		return ITechDelegate.class;
	}
}


@SuppressWarnings("serial")
class TechnologyExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
	public HashMap<PlayerID, Collection<TechAdvance>> m_techs;
}
