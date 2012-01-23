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
 * EndTurnDelegate.java
 * 
 * Created on November 2, 2001, 12:30 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          At the end of the turn collect income.
 */
@AutoSave(afterStepEnd = true)
public class EndTurnDelegate extends AbstractEndTurnDelegate
{
	@Override
	protected void doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge)
	{
		// do national objectives
		if (isNationalObjectives())
		{
			determineNationalObjectives(bridge);
		}
		// create resources if any owned units have the ability
		createResources(bridge);
		// create units if any owned units have the ability
		createUnits(bridge);
	}
	
	/**
     *
     */
	private void createUnits(final IDelegateBridge bridge)
	{
		final GameData data = getData();
		final PlayerID player = data.getSequence().getStep().getPlayerID();
		final Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesUnits);
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
			if (myCreators != null && !myCreators.isEmpty())
			{
				final Collection<Unit> toAdd = new ArrayList<Unit>();
				final Collection<Unit> toAddSea = new ArrayList<Unit>();
				final Collection<Unit> toAddLand = new ArrayList<Unit>();
				for (final Unit u : myCreators)
				{
					final UnitAttachment ua = UnitAttachment.get(u.getType());
					final IntegerMap<UnitType> createsUnitsMap = ua.getCreatesUnitsList();
					final Collection<UnitType> willBeCreated = createsUnitsMap.keySet();
					for (final UnitType ut : willBeCreated)
					{
						if (UnitAttachment.get(ut).getIsSea() && Matches.TerritoryIsLand.match(t))
							toAddSea.addAll(ut.create(createsUnitsMap.getInt(ut), player));
						else if (!UnitAttachment.get(ut).getIsSea() && !UnitAttachment.get(ut).getIsAir() && Matches.TerritoryIsWater.match(t))
							toAddLand.addAll(ut.create(createsUnitsMap.getInt(ut), player));
						else
							toAdd.addAll(ut.create(createsUnitsMap.getInt(ut), player));
					}
				}
				if (toAdd != null && !toAdd.isEmpty())
				{
					final String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAdd) + " in " + t.getName();
					bridge.getHistoryWriter().startEvent(transcriptText);
					bridge.getHistoryWriter().setRenderingData(toAdd);
					final Change place = ChangeFactory.addUnits(t, toAdd);
					change.add(place);
				}
				if (toAddSea != null && !toAddSea.isEmpty())
				{
					final Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
					final Collection<Territory> waterNeighbors = data.getMap().getNeighbors(t, myTerrs);
					if (waterNeighbors != null && !waterNeighbors.isEmpty())
					{
						final Territory tw = waterNeighbors.iterator().next();
						final String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddSea) + " in " + tw.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						bridge.getHistoryWriter().setRenderingData(toAddSea);
						final Change place = ChangeFactory.addUnits(tw, toAddSea);
						change.add(place);
					}
				}
				if (toAddLand != null && !toAddLand.isEmpty())
				{
					final Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
					final Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, myTerrs);
					if (landNeighbors != null && !landNeighbors.isEmpty())
					{
						final Territory tl = landNeighbors.iterator().next();
						final String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddLand) + " in " + tl.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						bridge.getHistoryWriter().setRenderingData(toAddLand);
						final Change place = ChangeFactory.addUnits(tl, toAddLand);
						change.add(place);
					}
				}
			}
		}
		if (change != null && !change.isEmpty())
			bridge.addChange(change);
	}
	
	/**
	 * 
	 * @param data
	 * @param bridge
	 */
	private void createResources(final IDelegateBridge bridge)
	{
		final GameData data = getData();
		final PlayerID player = data.getSequence().getStep().getPlayerID();
		final Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesResources);
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
			if (myCreators != null && !myCreators.isEmpty())
			{
				for (final Unit u : myCreators)
				{
					final UnitAttachment ua = UnitAttachment.get(u.getType());
					final IntegerMap<Resource> createsUnitsMap = ua.getCreatesResourcesList();
					final Collection<Resource> willBeCreated = createsUnitsMap.keySet();
					for (final Resource r : willBeCreated)
					{
						int toAdd = createsUnitsMap.getInt(r);
						if (r.getName().equals(Constants.PUS))
							toAdd *= Properties.getPU_Multiplier(data);
						int total = player.getResources().getQuantity(r) + toAdd;
						if (total < 0)
						{
							toAdd -= total;
							total = 0;
						}
						final String transcriptText = u.getUnitType().getName() + " in " + t.getName() + " creates " + toAdd + " " + r.getName() + "; " + player.getName() + " end with " + total + " "
									+ r.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						final Change resources = ChangeFactory.changeResourcesChange(player, r, toAdd);
						change.add(resources);
					}
				}
			}
		}
		if (change != null && !change.isEmpty())
			bridge.addChange(change);
	}
	
	/**
	 * Determine if National Objectives have been met, and then do them.
	 * 
	 * @param data
	 */
	private void determineNationalObjectives(final IDelegateBridge bridge)
	{
		final GameData data = getData();
		final PlayerID player = data.getSequence().getStep().getPlayerID();
		
		// First figure out all the conditions that will be tested, so we can test them all at the same time.
		final HashSet<TriggerAttachment> toFirePossible = new HashSet<TriggerAttachment>();
		final HashSet<ICondition> allConditionsNeeded = new HashSet<ICondition>();
		final boolean useTriggers = games.strategy.triplea.Properties.getTriggers(data);
		if (useTriggers)
		{
			// add conditions required for triggers
			final Match<TriggerAttachment> endTurnDelegateTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
						TriggerAttachment.resourceMatch(null, null));
			toFirePossible.addAll(TriggerAttachment.collectForAllTriggersMatching(new HashSet<PlayerID>(Collections.singleton(player)), endTurnDelegateTriggerMatch, bridge));
			allConditionsNeeded.addAll(RulesAttachment.getAllConditionsRecursive(new HashSet<ICondition>(toFirePossible), null));
		}
		// add conditions required for national objectives (nat objs that have uses left)
		final List<RulesAttachment> natObjs = Match.getMatches(RulesAttachment.getNationalObjectives(player, data), availableUses);
		allConditionsNeeded.addAll(RulesAttachment.getAllConditionsRecursive(new HashSet<ICondition>(natObjs), null));
		if (allConditionsNeeded.isEmpty())
			return;
		// now test all the conditions
		final HashMap<ICondition, Boolean> testedConditions = RulesAttachment.testAllConditionsRecursive(allConditionsNeeded, null, bridge);
		
		// now that we have all testedConditions, may as well do triggers first.
		if (useTriggers)
		{
			if (!toFirePossible.isEmpty())
			{
				// get all triggers that are satisfied based on the tested conditions.
				final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(Match.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
				// now list out individual types to fire, once for each of the matches above.
				TriggerAttachment.triggerResourceChange(toFireTestedAndSatisfied, bridge, null, null);
			}
		}
		
		// now do all the national objectives
		for (final RulesAttachment rule : natObjs)
		{
			int uses = rule.getUses();
			if (uses == 0 || !rule.isSatisfied(testedConditions))
				continue;
			
			int toAdd = rule.getObjectiveValue();
			toAdd *= Properties.getPU_Multiplier(data);
			toAdd *= rule.getEachMultiple();
			int total = player.getResources().getQuantity(Constants.PUS) + toAdd;
			if (total < 0)
			{
				toAdd -= total;
				total = 0;
			}
			final Change change = ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(Constants.PUS), toAdd);
			bridge.addChange(change);
			if (uses > 0)
			{
				uses--;
				final Change use = ChangeFactory.attachmentPropertyChange(rule, new Integer(uses).toString(), "uses");
				bridge.addChange(use);
			}
			final String PUMessage = MyFormatter.attachmentNameToText(rule.getName()) + ": " + player.getName() + " met a national objective for an additional " + toAdd
						+ MyFormatter.pluralize(" PU", toAdd) + "; end with " + total + MyFormatter.pluralize(" PU", total);
			bridge.getHistoryWriter().startEvent(PUMessage);
		}
	}
	
	private boolean isNationalObjectives()
	{
		return games.strategy.triplea.Properties.getNationalObjectives(getData());
	}
	
	private static Match<RulesAttachment> availableUses = new Match<RulesAttachment>()
	{
		@Override
		public boolean match(final RulesAttachment ra)
		{
			return ra.getUses() != 0;
		}
	};
	
	@Override
	protected void addOtherResources(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final CompositeChange change = new CompositeChange();
		final Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(m_player);
		final ResourceCollection productionCollection = getResourceProduction(territories, data);
		final IntegerMap<Resource> production = productionCollection.getResourcesCopy();
		for (final Entry<Resource, Integer> resource : production.entrySet())
		{
			final Resource r = resource.getKey();
			int toAdd = resource.getValue();
			int total = m_player.getResources().getQuantity(r) + toAdd;
			if (total < 0)
			{
				toAdd -= total;
				total = 0;
			}
			aBridge.getHistoryWriter().startEvent(m_player.getName() + " collects " + toAdd + " " + MyFormatter.pluralize(r.getName(), toAdd)
						+ "; ends with " + total + " " + MyFormatter.pluralize(r.getName(), total) + " total");
			change.add(ChangeFactory.changeResourcesChange(m_player, r, toAdd));
		}
		if (!change.isEmpty())
		{
			aBridge.addChange(change);
		}
	}
	
	/**
	 * Since territory resource may contain any resource except PUs (PUs use "getProduction" instead),
	 * we will now figure out the total production of non-PUs resources.
	 * 
	 * @param territories
	 * @param data
	 * @return
	 */
	public static ResourceCollection getResourceProduction(final Collection<Territory> territories, final GameData data)
	{
		final ResourceCollection rVal = new ResourceCollection(data);
		for (final Territory current : territories)
		{
			final TerritoryAttachment attachment = (TerritoryAttachment) current.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
			if (attachment == null)
				throw new IllegalStateException("No attachment for owned territory:" + current.getName());
			final ResourceCollection toAdd = attachment.getResources();
			if (toAdd == null)
				continue;
			// Check if territory is originally owned convoy center
			if (Matches.territoryCanCollectIncomeFrom(current.getOwner(), data).match(current))
				rVal.add(toAdd);
		}
		return rVal;
	}
}
