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
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
	private boolean m_needToInitialize = true;
	public final static String NOT_ENOUGH_RESOURCES = "Not enough resources";
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(aBridge);
		final GameData data = getData();
		if (m_needToInitialize)
		{
			if (games.strategy.triplea.Properties.getTriggers(data))
			{
				// First set up a match for what we want to have fire as a default in this delegate. List out as a composite match OR.
				// use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
				final Match<TriggerAttachment> purchaseDelegateTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
							TriggerAttachment.prodMatch(null, null),
							TriggerAttachment.prodFrontierEditMatch(null, null),
							TriggerAttachment.purchaseMatch(null, null));
				// get all possible triggers based on this match.
				final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
							new HashSet<PlayerID>(Collections.singleton(m_player)), purchaseDelegateTriggerMatch, m_bridge);
				if (!toFirePossible.isEmpty())
				{
					// get all conditions possibly needed by these triggers, and then test them.
					final HashMap<ICondition, Boolean> testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
					// get all triggers that are satisfied based on the tested conditions.
					final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(Match.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
					// now list out individual types to fire, once for each of the matches above.
					TriggerAttachment.triggerProductionChange(toFireTestedAndSatisfied, m_bridge, null, null);
					TriggerAttachment.triggerProductionFrontierEditChange(toFireTestedAndSatisfied, m_bridge, null, null);
					TriggerAttachment.triggerPurchase(toFireTestedAndSatisfied, m_bridge, null, null);
				}
			}
			giveBonusIncomeToAI();
			m_needToInitialize = false;
		}
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
		final PurchaseExtendedDelegateState state = new PurchaseExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final PurchaseExtendedDelegateState s = (PurchaseExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
	}
	
	/**
	 * subclasses can over ride this method to use different restrictions as to what a player can buy
	 */
	protected boolean canAfford(final IntegerMap<Resource> costs, final PlayerID player)
	{
		return player.getResources().has(costs);
	}
	
	/**
	 * Returns an error code, or null if all is good.
	 */
	public String purchase(final IntegerMap<ProductionRule> productionRules)
	{
		final IntegerMap<Resource> costs = getCosts(productionRules, m_player);
		final IntegerMap<NamedAttachable> results = getResults(productionRules);
		if (!(canAfford(costs, m_player)))
			return NOT_ENOUGH_RESOURCES;
		// check to see if player has too many of any building with a building limit
		final Iterator<NamedAttachable> iter2 = results.keySet().iterator();
		while (iter2.hasNext())
		{
			final Object next = iter2.next();
			if (!(next instanceof Resource))
			{
				final UnitType type = (UnitType) next;
				final int quantity = results.getInt(type);
				final UnitAttachment ua = UnitAttachment.get(type);
				final int maxBuilt = ua.getMaxBuiltPerPlayer();
				if (maxBuilt == 0)
					return "May not build any of this unit right now: " + type.getName();
				else if (maxBuilt > 0)
				{
					// check to see how many are currently fielded by this player
					int currentlyBuilt = 0;
					final CompositeMatch<Unit> unitTypeOwnedBy = new CompositeMatchAnd<Unit>(Matches.unitIsOfType(type), Matches.unitIsOwnedBy(m_player));
					final Collection<Territory> allTerrs = getData().getMap().getTerritories();
					for (final Territory t : allTerrs)
					{
						currentlyBuilt += t.getUnits().countMatches(unitTypeOwnedBy);
					}
					final int allowedBuild = maxBuilt - currentlyBuilt;
					if (allowedBuild - quantity < 0)
						return "May only build " + allowedBuild + " of " + type.getName() + " this turn, may only build " + maxBuilt + " total";
				}
			}
		}
		// remove first, since add logs PUs remaining
		final Iterator<NamedAttachable> iter = results.keySet().iterator();
		final Collection<Unit> totalUnits = new ArrayList<Unit>();
		final CompositeChange changes = new CompositeChange();
		// add changes for added resources
		// and find all added units
		while (iter.hasNext())
		{
			final Object next = iter.next();
			if (next instanceof Resource)
			{
				final Resource resource = (Resource) next;
				final int quantity = results.getInt(resource);
				final Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
				changes.add(change);
			}
			else
			{
				final UnitType type = (UnitType) next;
				final int quantity = results.getInt(type);
				final Collection<Unit> units = type.create(quantity, m_player);
				totalUnits.addAll(units);
			}
		}
		// add changes for added units
		if (!totalUnits.isEmpty())
		{
			final Change change = ChangeFactory.addUnits(m_player, totalUnits);
			changes.add(change);
		}
		// add changes for spent resources
		final String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);
		addHistoryEvent(totalUnits, remaining, false);
		// commit changes
		m_bridge.addChange(changes);
		return null;
	}
	
	/**
	 * Returns an error code, or null if all is good.
	 */
	public String purchaseRepair(final Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		final IntegerMap<Resource> costs = getRepairCosts(repairRules, m_player);
		final IntegerMap<NamedAttachable> results = getRepairResults(repairRules);
		if (!(canAfford(costs, m_player)))
			return NOT_ENOUGH_RESOURCES;
		// remove first, since add logs PUs remaining
		final CompositeChange changes = new CompositeChange();
		final Collection<Unit> totalUnits = new ArrayList<Unit>();
		final Iterator<NamedAttachable> iter = results.keySet().iterator();
		// add changes for added resources
		// and find all added units
		while (iter.hasNext())
		{
			final Object next = iter.next();
			if (next instanceof Resource)
			{
				final Resource resource = (Resource) next;
				final int quantity = results.getInt(resource);
				final Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
				changes.add(change);
			}
			else
			{
				final UnitType type = (UnitType) next;
				final int quantity = results.getInt(type);
				final Collection<Unit> units = type.create(quantity, m_player);
				totalUnits.addAll(units);
			}
		}
		// Get the map of the factories that were repaired and how much for each
		final Map<Unit, Integer> repairMap = getTerritoryRepairs(repairRules);
		if (!repairMap.isEmpty())
		{
			final Collection<Unit> repairUnits = repairMap.keySet();
			for (final Unit u : repairUnits)
			{
				if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData()))
				{
					final int repairCount = repairMap.get(u);
					// Display appropriate damaged/repaired factory and factory damage totals
					if (repairCount > 0)
					{
						final Territory terr = u.getTerritoryUnitIsIn();
						final TerritoryAttachment ta = TerritoryAttachment.get(terr);
						final int currentDamage = ta.getUnitProduction();
						final IntegerMap<Unit> hits = new IntegerMap<Unit>();
						final int newDamageTotal = ta.getProduction() - (currentDamage + repairCount);
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
					final int repairCount = repairMap.get(u);
					// Display appropriate damaged/repaired factory and factory damage totals
					if (repairCount > 0)
					{
						final IntegerMap<Unit> hits = new IntegerMap<Unit>();
						final TripleAUnit taUnit = (TripleAUnit) u;
						final int newDamageTotal = taUnit.getUnitDamage() - repairCount;
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
		final String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);
		addHistoryEvent(totalUnits, remaining, true);
		// commit changes
		m_bridge.addChange(changes);
		return null;
	}
	
	private HashMap<Unit, Integer> getTerritoryRepairs(final Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		final HashMap<Unit, Integer> repairMap = new HashMap<Unit, Integer>();
		for (final Unit u : repairRules.keySet())
		{
			final IntegerMap<RepairRule> rules = repairRules.get(u);
			final TreeSet<RepairRule> repRules = new TreeSet<RepairRule>(repairRuleComparator);
			repRules.addAll(rules.keySet());
			for (final RepairRule repairRule : repRules)
			{
				final int quantity = rules.getInt(repairRule);
				repairMap.put(u, quantity);
			}
		}
		return repairMap;
	}
	
	Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>()
	{
		UnitTypeComparator utc = new UnitTypeComparator();
		
		public int compare(final RepairRule o1, final RepairRule o2)
		{
			final UnitType u1 = (UnitType) o1.getResults().keySet().iterator().next();
			final UnitType u2 = (UnitType) o2.getResults().keySet().iterator().next();
			return utc.compare(u1, u2);
		}
	};
	
	private void addHistoryEvent(final Collection<Unit> totalUnits, final String remainingText, final boolean repair)
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
	
	private IntegerMap<Resource> getCosts(final IntegerMap<ProductionRule> productionRules, final PlayerID player)
	{
		final IntegerMap<Resource> costs = new IntegerMap<Resource>();
		final Iterator<ProductionRule> rules = productionRules.keySet().iterator();
		while (rules.hasNext())
		{
			final ProductionRule rule = rules.next();
			costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
		}
		return costs;
	}
	
	private IntegerMap<Resource> getRepairCosts(final Map<Unit, IntegerMap<RepairRule>> repairRules, final PlayerID player)
	{
		final Collection<Unit> units = repairRules.keySet();
		final Iterator<Unit> iter = units.iterator();
		final IntegerMap<Resource> costs = new IntegerMap<Resource>();
		while (iter.hasNext())
		{
			final Unit u = iter.next();
			final Iterator<RepairRule> rules = repairRules.get(u).keySet().iterator();
			while (rules.hasNext())
			{
				final RepairRule rule = rules.next();
				costs.addMultiple(rule.getCosts(), repairRules.get(u).getInt(rule));
			}
		}
		if (isIncreasedFactoryProduction(player))
		{
			// UnitCategory unitCategory = categorized.iterator().next();
			// if(unitCategory.getType().getName().endsWith("_hit"))
			// cost = (int) (Math.round(quantity/2));
			costs.multiplyAllValuesBy((float) 0.5, 3);
		}
		return costs;
	}
	
	private IntegerMap<NamedAttachable> getResults(final IntegerMap<ProductionRule> productionRules)
	{
		final IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();
		final Iterator<ProductionRule> rules = productionRules.keySet().iterator();
		while (rules.hasNext())
		{
			final ProductionRule rule = rules.next();
			costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
		}
		return costs;
	}
	
	private IntegerMap<NamedAttachable> getRepairResults(final Map<Unit, IntegerMap<RepairRule>> repairRules)
	{
		final Collection<Unit> units = repairRules.keySet();
		final Iterator<Unit> iter = units.iterator();
		final IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();
		while (iter.hasNext())
		{
			final Unit u = iter.next();
			final Iterator<RepairRule> rules = repairRules.get(u).keySet().iterator();
			while (rules.hasNext())
			{
				final RepairRule rule = rules.next();
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
	
	protected String removeFromPlayer(final PlayerID player, final IntegerMap<Resource> costs, final CompositeChange changes, final Collection<Unit> totalUnits)
	{
		final StringBuffer returnString = new StringBuffer("");
		for (final Resource resource : costs.keySet())
		{
			final float quantity = costs.getInt(resource);
			final int cost = (int) quantity;
			final Change change = ChangeFactory.changeResourcesChange(m_player, resource, -cost);
			changes.add(change);
			returnString.append(m_player.getResources().getQuantity(resource) - cost + " " + resource.getName() + " remaining\n");
		}
		return returnString.toString();
	}
	
	private void giveBonusIncomeToAI()
	{
		// TODO and other resources?
		if (!m_player.isAI())
			return;
		final int currentPUs = m_player.getResources().getQuantity(Constants.PUS);
		if (currentPUs == 0)
			return;
		final int bonusPercent = games.strategy.triplea.Properties.getAIBonusIncomePercentage(getData());
		if (bonusPercent == 0)
			return;
		int toGive = (int) Math.round(((double) currentPUs * (double) bonusPercent / 100));
		if (toGive == 0 && bonusPercent > 0 && currentPUs > 0)
			toGive = 1;
		toGive += games.strategy.triplea.Properties.getAIBonusIncomeFlatRate(getData());
		if (toGive + currentPUs < 0)
			toGive = currentPUs * -1;
		if (toGive == 0)
			return;
		m_bridge.getHistoryWriter().startEvent("Giving AI player bonus income modifier of " + toGive + MyFormatter.pluralize(" PU", toGive));
		m_bridge.addChange(ChangeFactory.changeResourcesChange(m_player, getData().getResourceList().getResource(Constants.PUS), toGive));
	}
	
	private boolean isIncreasedFactoryProduction(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasIncreasedFactoryProduction();
	}
}


@SuppressWarnings("serial")
class PurchaseExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
}
