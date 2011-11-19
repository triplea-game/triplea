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
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
	protected void doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge)
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
	private void createUnits(IDelegateBridge bridge)
	{
		GameData data = getData();
		PlayerID player = data.getSequence().getStep().getPlayerID();
		Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesUnits);
		CompositeChange change = new CompositeChange();
		for (Territory t : data.getMap().getTerritories())
		{
			Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
			if (myCreators != null && !myCreators.isEmpty())
			{
				Collection<Unit> toAdd = new ArrayList<Unit>();
				Collection<Unit> toAddSea = new ArrayList<Unit>();
				Collection<Unit> toAddLand = new ArrayList<Unit>();
				for (Unit u : myCreators)
				{
					UnitAttachment ua = UnitAttachment.get(u.getType());
					IntegerMap<UnitType> createsUnitsMap = ua.getCreatesUnitsList();
					Collection<UnitType> willBeCreated = createsUnitsMap.keySet();
					for (UnitType ut : willBeCreated)
					{
						if (UnitAttachment.get(ut).isSea() && Matches.TerritoryIsLand.match(t))
							toAddSea.addAll(ut.create(createsUnitsMap.getInt(ut), player));
						else if (!UnitAttachment.get(ut).isSea() && !UnitAttachment.get(ut).isAir() && Matches.TerritoryIsWater.match(t))
							toAddLand.addAll(ut.create(createsUnitsMap.getInt(ut), player));
						else
							toAdd.addAll(ut.create(createsUnitsMap.getInt(ut), player));
					}
				}
				if (toAdd != null && !toAdd.isEmpty())
				{
					String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAdd) + " in " + t.getName();
					bridge.getHistoryWriter().startEvent(transcriptText);
					bridge.getHistoryWriter().setRenderingData(toAdd);
					Change place = ChangeFactory.addUnits(t, toAdd);
					change.add(place);
				}
				if (toAddSea != null && !toAddSea.isEmpty())
				{
					Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
					Collection<Territory> waterNeighbors = data.getMap().getNeighbors(t, myTerrs);
					if (waterNeighbors != null && !waterNeighbors.isEmpty())
					{
						Territory tw = waterNeighbors.iterator().next();
						String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddSea) + " in " + tw.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						bridge.getHistoryWriter().setRenderingData(toAddSea);
						Change place = ChangeFactory.addUnits(tw, toAddSea);
						change.add(place);
					}
				}
				if (toAddLand != null && !toAddLand.isEmpty())
				{
					Match<Territory> myTerrs = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
					Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, myTerrs);
					if (landNeighbors != null && !landNeighbors.isEmpty())
					{
						Territory tl = landNeighbors.iterator().next();
						String transcriptText = player.getName() + " creates " + MyFormatter.unitsToTextNoOwner(toAddLand) + " in " + tl.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						bridge.getHistoryWriter().setRenderingData(toAddLand);
						Change place = ChangeFactory.addUnits(tl, toAddLand);
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
	private void createResources(IDelegateBridge bridge)
	{
		GameData data = getData();
		PlayerID player = data.getSequence().getStep().getPlayerID();
		Match<Unit> myCreatorsMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCreatesResources);
		CompositeChange change = new CompositeChange();
		
		for (Territory t : data.getMap().getTerritories())
		{
			Collection<Unit> myCreators = Match.getMatches(t.getUnits().getUnits(), myCreatorsMatch);
			if (myCreators != null && !myCreators.isEmpty())
			{
				for (Unit u : myCreators)
				{
					UnitAttachment ua = UnitAttachment.get(u.getType());
					IntegerMap<Resource> createsUnitsMap = ua.getCreatesResourcesList();
					Collection<Resource> willBeCreated = createsUnitsMap.keySet();
					for (Resource r : willBeCreated)
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
						String transcriptText = u.getUnitType().getName() + " in " + t.getName() + " creates " + toAdd + " " + r.getName() + "; " + player.getName() + " end with " + total + " "
									+ r.getName();
						bridge.getHistoryWriter().startEvent(transcriptText);
						Change resources = ChangeFactory.changeResourcesChange(player, r, toAdd);
						change.add(resources);
					}
				}
			}
		}
		if (change != null && !change.isEmpty())
			bridge.addChange(change);
	}
	
	/**
	 * Determine if National Objectives have been met
	 * 
	 * @param data
	 */
	private void determineNationalObjectives(IDelegateBridge bridge)
	{
		GameData data = getData();
		PlayerID player = data.getSequence().getStep().getPlayerID();
		
		// See if the player has National Objectives
		Set<RulesAttachment> natObjs = new HashSet<RulesAttachment>();
		Map<String, IAttachment> map = player.getAttachments();
		Iterator<String> objsIter = map.keySet().iterator();
		while (objsIter.hasNext())
		{
			IAttachment attachment = map.get(objsIter.next());
			String name = attachment.getName();
			if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
			{
				natObjs.add((RulesAttachment) attachment);
			}
		}
		
		// Check whether any National Objectives are met
		Iterator<RulesAttachment> rulesIter = natObjs.iterator();
		while (rulesIter.hasNext())
		{
			RulesAttachment rule = rulesIter.next();
			boolean objectiveMet = true;
			Integer uses = rule.getUses();
			if (uses == 0)
				continue;
			objectiveMet = rule.isSatisfied(data);
			
			//
			// If all are satisfied add the PUs for this objective
			//
			if (objectiveMet)
			{
				int toAdd = rule.getObjectiveValue();
				toAdd *= Properties.getPU_Multiplier(data);
				toAdd *= rule.getEachMultiple();
				int total = player.getResources().getQuantity(Constants.PUS) + toAdd;
				if (total < 0)
				{
					toAdd -= total;
					total = 0;
				}
				Change change = ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(Constants.PUS), toAdd);
				// player.getResources().addResource(data.getResourceList().getResource(Constants.PUS), rule.getObjectiveValue());
				bridge.addChange(change);
				if (uses > 0)
				{
					uses--;
					Change use = ChangeFactory.attachmentPropertyChange(rule, new Integer(uses).toString(), "uses");
					bridge.addChange(use);
				}
				String PUMessage = MyFormatter.attachmentNameToText(rule.getName()) + ": " + player.getName() + " met a national objective for an additional " + toAdd
							+ MyFormatter.pluralize(" PU", toAdd) +
									"; end with " + total + MyFormatter.pluralize(" PU", total);
				bridge.getHistoryWriter().startEvent(PUMessage);
			}
		} // end while
		
		// now do any triggers that add resources too
		if (games.strategy.triplea.Properties.getTriggers(data))
			TriggerAttachment.triggerResourceChange(player, bridge, null, null);
		
	} // end determineNationalObjectives
	
	private boolean isNationalObjectives()
	{
		return games.strategy.triplea.Properties.getNationalObjectives(getData());
	}
}
