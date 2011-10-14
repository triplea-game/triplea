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
 * PurchaseDelegate.java
 * 
 * Created on November 2, 2001, 12:28 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * Logic for purchasing units.
 * 
 * Subclasses can override canAfford(...) to test if a purchase can be made
 * 
 * Subclasses can over ride addToPlayer(...) and removeFromPlayer(...) to change how
 * the adding or removing of resources is done.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class PurchaseDelegate extends BaseDelegate implements IPurchaseDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		GameData data = getData();
		if (games.strategy.triplea.Properties.getTriggers(data))
		{
			TriggerAttachment.triggerProductionChange(m_player, m_bridge, null, null);
			TriggerAttachment.triggerProductionFrontierEditChange(m_player, m_bridge, null, null);
			TriggerAttachment.triggerPurchase(m_player, m_bridge, null, null);
		}
	}
	
	/**
	 * subclasses can over ride this method to use different restrictions as to what a player can buy
	 */
	protected boolean canAfford(IntegerMap<Resource> costs, PlayerID player)
	{
		return player.getResources().has(costs);
	}
	
	/**
	 * Returns an error code, or null if all is good.
	 */
	@Override
	public String purchase(IntegerMap<ProductionRule> productionRules)
	{
		IntegerMap<Resource> costs = getCosts(productionRules);
		IntegerMap<NamedAttachable> results = getResults(productionRules);
		
		if (!(canAfford(costs, m_player)))
			return "Not enough resources";
		
		// check to see if player has too many of any building with a building limit
		Iterator<NamedAttachable> iter2 = results.keySet().iterator();
		while (iter2.hasNext())
		{
			Object next = iter2.next();
			if (!(next instanceof Resource))
			{
				UnitType type = (UnitType) next;
				int quantity = results.getInt(type);
				UnitAttachment ua = UnitAttachment.get(type);
				int maxBuilt = ua.getMaxBuiltPerPlayer();
				if (maxBuilt == 0)
					return "May not build any of this unit right now: " + type.getName();
				else if (maxBuilt > 0)
				{
					// check to see how many are currently fielded by this player
					int currentlyBuilt = 0;
					CompositeMatch<Unit> unitTypeOwnedBy = new CompositeMatchAnd<Unit>(Matches.unitIsOfType(type), Matches.unitIsOwnedBy(m_player));
					Collection<Territory> allTerrs = getData().getMap().getTerritories();
					for (Territory t : allTerrs)
					{
						currentlyBuilt += t.getUnits().countMatches(unitTypeOwnedBy);
					}
					int allowedBuild = maxBuilt - currentlyBuilt;
					if (allowedBuild - quantity < 0)
						return "May only build " + allowedBuild + " of " + type.getName() + " this turn, may only build " + maxBuilt + " total";
				}
			}
		}
		// remove first, since add logs PUs remaining
		
		Iterator<NamedAttachable> iter = results.keySet().iterator();
		Collection<Unit> totalUnits = new ArrayList<Unit>();
		CompositeChange changes = new CompositeChange();
		
		// add changes for added resources
		// and find all added units
		while (iter.hasNext())
		{
			Object next = iter.next();
			if (next instanceof Resource)
			{
				Resource resource = (Resource) next;
				int quantity = results.getInt(resource);
				Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
				changes.add(change);
			}
			else
			{
				UnitType type = (UnitType) next;
				int quantity = results.getInt(type);
				Collection<Unit> units = type.create(quantity, m_player);
				totalUnits.addAll(units);
				
			}
		}
		
		// add changes for added units
		if (!totalUnits.isEmpty())
		{
			Change change = ChangeFactory.addUnits(m_player, totalUnits);
			changes.add(change);
		}
		
		// add changes for spent resources
		String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);
		
		addHistoryEvent(totalUnits, remaining, false);
		
		// commit changes
		m_bridge.addChange(changes);
		
		return null;
	}
	
	/**
	 * Returns an error code, or null if all is good.
	 */
	@Override
	public String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		IntegerMap<Resource> costs = getRepairCosts(repairRules);
		
		IntegerMap<NamedAttachable> results = getRepairResults(repairRules);
		
		if (!(canAfford(costs, m_player)))
			return "Not enough resources";
		
		// remove first, since add logs PUs remaining
		CompositeChange changes = new CompositeChange();
		Collection<Unit> totalUnits = new ArrayList<Unit>();
		
		Iterator<NamedAttachable> iter = results.keySet().iterator();
		
		// add changes for added resources
		// and find all added units
		while (iter.hasNext())
		{
			Object next = iter.next();
			if (next instanceof Resource)
			{
				Resource resource = (Resource) next;
				int quantity = results.getInt(resource);
				Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
				changes.add(change);
			}
			else
			{
				UnitType type = (UnitType) next;
				int quantity = results.getInt(type);
				Collection<Unit> units = type.create(quantity, m_player);
				totalUnits.addAll(units);
				
			}
		}
		
		// Get the map of the factories that were repaired and how much for each
		Map<Unit, Integer> repairMap = getTerritoryRepairs(repairRules);
		
		if (!repairMap.isEmpty())
		{
			Collection<Unit> repairUnits = repairMap.keySet();
			
			for (Unit u : repairUnits)
			{
				if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
				{
					int repairCount = repairMap.get(u);
					
					// Display appropriate damaged/repaired factory and factory damage totals
					if (repairCount > 0)
					{
						Territory terr = u.getTerritoryUnitIsIn();
						TerritoryAttachment ta = TerritoryAttachment.get(terr);
						int currentDamage = ta.getUnitProduction();
						IntegerMap<Unit> hits = new IntegerMap<Unit>();
						
						int newDamageTotal = ta.getProduction() - (currentDamage + repairCount);
						if (newDamageTotal < 0)
						{
							return "You cannot repair more than a territory has been hit";
						}
						hits.put(u, newDamageTotal);
						changes.add(ChangeFactory.unitsHit(hits));
						changes.add(ChangeFactory.attachmentPropertyChange(ta, (new Integer(currentDamage + repairCount)).toString(), "unitProduction"));
					}
				}
				else
				// if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data))
				{
					int repairCount = repairMap.get(u);
					
					// Display appropriate damaged/repaired factory and factory damage totals
					if (repairCount > 0)
					{
						IntegerMap<Unit> hits = new IntegerMap<Unit>();
						TripleAUnit taUnit = (TripleAUnit) u;
						
						int newDamageTotal = taUnit.getUnitDamage() - repairCount;
						if (newDamageTotal < 0)
						{
							return "You cannot repair more than a unit has been hit";
						}
						hits.put(u, newDamageTotal);
						changes.add(ChangeFactory.unitsHit(hits));
						changes.add(ChangeFactory.unitPropertyChange(u, newDamageTotal, TripleAUnit.UNIT_DAMAGE));
					}
				}
			}
		}
		else
		{
			return null;
			// return "m_repairCount is empty";
		}
		
		// add changes for spent resources
		String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);
		
		addHistoryEvent(totalUnits, remaining, true);
		
		// commit changes
		m_bridge.addChange(changes);
		
		return null;
	}
	
	private HashMap<Unit, Integer> getTerritoryRepairs(Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		HashMap<Unit, Integer> repairMap = new HashMap<Unit, Integer>();
		
		for (Unit u : repairRules.keySet())
		{
			
			IntegerMap<RepairRule> rules = repairRules.get(u);
			
			TreeSet<RepairRule> repRules = new TreeSet<RepairRule>(repairRuleComparator);
			repRules.addAll(rules.keySet());
			Iterator<RepairRule> ruleIter = repRules.iterator();
			while (ruleIter.hasNext())
			{
				RepairRule repairRule = ruleIter.next();
				int quantity = rules.getInt(repairRule);
				
				repairMap.put(u, quantity);
			}
		}
		
		return repairMap;
	}
	
	Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>()
	{
		UnitTypeComparator utc = new UnitTypeComparator();
		
		@Override
		public int compare(RepairRule o1, RepairRule o2)
		{
			UnitType u1 = (UnitType) o1.getResults().keySet().iterator().next();
			UnitType u2 = (UnitType) o2.getResults().keySet().iterator().next();
			return utc.compare(u1, u2);
		}
	};
	
	private void addHistoryEvent(Collection<Unit> totalUnits, String remainingText, boolean repair)
	{
		// add history event
		String transcriptText;
		if (!repair)
		{
			if (!totalUnits.isEmpty())
				transcriptText = m_player.getName() + " buy " + MyFormatter.unitsToTextNoOwner(totalUnits) + "; " + remainingText;
			else
				transcriptText = m_player.getName() + " buy nothing; " + remainingText;
		}
		else
		{
			if (!totalUnits.isEmpty())
				transcriptText = m_player.getName() + " repair " + totalUnits.size() + " damage on " + MyFormatter.unitsToTextNoOwner(totalUnits) + "; " + remainingText;
			else
				transcriptText = m_player.getName() + " buy nothing; " + remainingText;
		}
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		m_bridge.getHistoryWriter().setRenderingData(totalUnits);
	}
	
	private IntegerMap<Resource> getCosts(IntegerMap<ProductionRule> productionRules)
	{
		IntegerMap<Resource> costs = new IntegerMap<Resource>();
		
		Iterator<ProductionRule> rules = productionRules.keySet().iterator();
		while (rules.hasNext())
		{
			ProductionRule rule = rules.next();
			costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
		}
		return costs;
	}
	
	private IntegerMap<Resource> getRepairCosts(Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		Collection<Unit> units = repairRules.keySet();
		Iterator<Unit> iter = units.iterator();
		IntegerMap<Resource> costs = new IntegerMap<Resource>();
		
		while (iter.hasNext())
		{
			Unit u = iter.next();
			
			Iterator<RepairRule> rules = repairRules.get(u).keySet().iterator();
			
			while (rules.hasNext())
			{
				RepairRule rule = rules.next();
				costs.addMultiple(rule.getCosts(), repairRules.get(u).getInt(rule));
			}
		}
		
		return costs;
	}
	
	private IntegerMap<NamedAttachable> getResults(IntegerMap<ProductionRule> productionRules)
	{
		IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();
		
		Iterator<ProductionRule> rules = productionRules.keySet().iterator();
		while (rules.hasNext())
		{
			ProductionRule rule = rules.next();
			costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
		}
		return costs;
	}
	
	private IntegerMap<NamedAttachable> getRepairResults(Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		Collection<Unit> units = repairRules.keySet();
		Iterator<Unit> iter = units.iterator();
		IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();
		
		while (iter.hasNext())
		{
			Unit u = iter.next();
			
			Iterator<RepairRule> rules = repairRules.get(u).keySet().iterator();
			
			while (rules.hasNext())
			{
				RepairRule rule = rules.next();
				costs.addMultiple(rule.getResults(), repairRules.get(u).getInt(rule));
			}
		}
		/*IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();

		Iterator<RepairRule> rules = repairRules.keySet().iterator();
		while(rules.hasNext() )
		{
		    RepairRule rule = rules.next();
		    costs.addMultiple(rule.getResults(), repairRules.getInt(rule));
		}*/
		return costs;
	}
	
	/* 
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IPurchaseDelegate.class;
	}
	
	protected String removeFromPlayer(PlayerID player, IntegerMap<Resource> costs, CompositeChange changes, Collection<Unit> totalUnits)
	{
		Iterator<Resource> costsIter = costs.keySet().iterator();
		// int AvailPUs = player.getResources().getQuantity(Constants.PUS);
		
		while (costsIter.hasNext())
		{
			Resource resource = costsIter.next();
			float quantity = costs.getInt(resource);
			int cost = (int) quantity;
			
			if (isIncreasedFactoryProduction(player))
			{
				Set<UnitCategory> categorized = UnitSeperator.categorize(totalUnits);
				if (categorized.size() == 1)
				{
					// UnitCategory unitCategory = categorized.iterator().next();
					// if(unitCategory.getType().getName().endsWith("_hit"))
					cost = (int) (Math.round(quantity / 2));
				}
			}
			
			Change change = ChangeFactory.changeResourcesChange(m_player, resource, -cost);
			changes.add(change);
			
			return m_player.getResources().getQuantity(resource) - cost + " PUs remaining";
		}
		return "";
	}
	
	private boolean isIncreasedFactoryProduction(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasIncreasedFactoryProduction();
	}
	
}
