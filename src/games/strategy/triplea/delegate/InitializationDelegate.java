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
/**
 * InitializationDelegate.java
 * 
 * Created on January 4, 2002, 3:53 PM
 * 
 * Subclasses can override init(), which will be called exactly once.
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * This delegate is only supposed to be run once, per game, at the start of the game.
 * 
 * @author Sean Bridges
 */
public class InitializationDelegate extends BaseTripleADelegate
{
	private boolean m_needToInitialize = true;
	
	/** Creates a new instance of InitializationDelegate */
	public InitializationDelegate()
	{
	}
	
	@Override
	public void initialize(final String name, final String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		if (m_needToInitialize)
		{
			init(m_bridge);
			m_needToInitialize = false;
		}
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final InitializationExtendedDelegateState state = new InitializationExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final InitializationExtendedDelegateState s = (InitializationExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return false;
	}
	
	protected void init(final IDelegateBridge aBridge)
	{
		initDestroyerArtillery(aBridge);
		initShipyards(aBridge);
		initTwoHitBattleship(aBridge);
		initOriginalOwner(aBridge);
		initTech(aBridge);
		initSkipUnusedBids(aBridge.getData());
		initDeleteAssetsOfDisabledPlayers(aBridge);
		initTransportedLandUnits(aBridge);
	}
	
	/**
	 * Want to make sure that all units in the sea that can be transported are
	 * marked as being transported by something.
	 * 
	 * We assume that all transportable units in the sea are in a transport, no
	 * exceptions.
	 * 
	 */
	private void initTransportedLandUnits(final IDelegateBridge aBridge)
	{
		// m_firstRun = false;
		final GameData data = aBridge.getData();
		// check every territory
		final Iterator<Territory> allTerritories = data.getMap().getTerritories().iterator();
		while (allTerritories.hasNext())
		{
			final Territory current = allTerritories.next();
			// only care about water
			if (!current.isWater())
				continue;
			final Collection<Unit> units = current.getUnits().getUnits();
			if (units.size() == 0 || !Match.someMatch(units, Matches.UnitIsLand))
				continue;
			boolean historyItemCreated = false;
			// map transports, try to fill
			final Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
			final Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);
			for (final Unit toLoad : land)
			{
				final UnitAttachment ua = UnitAttachment.get(toLoad.getType());
				final int cost = ua.getTransportCost();
				if (cost == -1)
					throw new IllegalStateException("Non transportable unit in sea");
				// find the next transport that can hold it
				final Iterator<Unit> transportIter = transports.iterator();
				boolean found = false;
				while (transportIter.hasNext())
				{
					final Unit transport = transportIter.next();
					final int capacity = TransportTracker.getAvailableCapacity(transport);
					if (capacity >= cost)
					{
						if (!historyItemCreated)
						{
							aBridge.getHistoryWriter().startEvent("Initializing Units in Transports");
							historyItemCreated = true;
						}
						try
						{
							aBridge.addChange(TransportTracker.loadTransportChange((TripleAUnit) transport, toLoad));
						} catch (final IllegalStateException e)
						{
							System.err.println("You can only edit add transports+units after the initialization delegate of the game is finished.  "
										+ "If this error came up and you have not used Edit Mode to add units + transports, then please report this as a bug:  \r\n" + e.getMessage());
						}
						found = true;
						break;
					}
				}
				if (!found)
					throw new IllegalStateException("Cannot load all land units in sea transports. " + "Please make sure you have enough transports. "
								+ "You may need to re-order the xml's placement of transports and land units, " + "as the engine will try to fill them in the order they are given.");
			}
		}
	}
	
	private void initDeleteAssetsOfDisabledPlayers(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getDisabledPlayersAssetsDeleted(data))
			return;
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			if (player.isNull() || !player.getIsDisabled())
				continue;
			// delete all the stuff they have
			final CompositeChange change = new CompositeChange();
			for (final Resource r : player.getResources().getResourcesCopy().keySet())
			{
				final int deleted = player.getResources().getQuantity(r);
				if (deleted != 0)
				{
					change.add(ChangeFactory.changeResourcesChange(player, r, -deleted));
				}
			}
			final Collection<Unit> heldUnits = player.getUnits().getUnits();
			if (!heldUnits.isEmpty())
			{
				change.add(ChangeFactory.removeUnits(player, heldUnits));
			}
			final Match<Unit> owned = Matches.unitIsOwnedBy(player);
			for (final Territory t : data.getMap().getTerritories())
			{
				final Collection<Unit> terrUnits = t.getUnits().getMatches(owned);
				if (!terrUnits.isEmpty())
				{
					change.add(ChangeFactory.removeUnits(t, terrUnits));
				}
			}
			if (!change.isEmpty())
			{
				aBridge.getHistoryWriter().startEvent("Remove all resources and units from: " + player.getName());
				aBridge.addChange(change);
			}
		}
	}
	
	private void initSkipUnusedBids(final GameData data)
	{
		// we have a lot of bid steps, 12 for pact of steel
		// in multi player this can be time consuming, since each vm
		// must be notified (and have its ui) updated for each step,
		// so remove the bid steps that arent used
		for (final GameStep step : data.getSequence())
		{
			if (step.getDelegate() instanceof BidPlaceDelegate || step.getDelegate() instanceof BidPurchaseDelegate)
			{
				if (!BidPurchaseDelegate.doesPlayerHaveBid(data, step.getPlayerID()))
				{
					step.setMaxRunCount(0);
				}
			}
		}
	}
	
	private void initTech(final IDelegateBridge bridge)
	{
		final GameData data = bridge.getData();
		final Iterator<PlayerID> players = data.getPlayerList().getPlayers().iterator();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			final Iterator<TechAdvance> advances = TechTracker.getCurrentTechAdvances(player, data).iterator();
			if (advances.hasNext())
			{
				bridge.getHistoryWriter().startEvent("Initializing " + player.getName() + " with tech advances");
				while (advances.hasNext())
				{
					final TechAdvance advance = advances.next();
					advance.perform(player, bridge);
				}
			}
		}
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initDestroyerArtillery(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final boolean addArtilleryAndDestroyers = games.strategy.triplea.Properties.getUse_Destroyers_And_Artillery(data);
		if (!isWW2V2(data) && addArtilleryAndDestroyers)
		{
			final CompositeChange change = new CompositeChange();
			final ProductionRule artillery = data.getProductionRuleList().getProductionRule("buyArtillery");
			final ProductionRule destroyer = data.getProductionRuleList().getProductionRule("buyDestroyer");
			final ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier("production");
			if (artillery != null && !frontier.getRules().contains(artillery))
				change.add(ChangeFactory.addProductionRule(artillery, frontier));
			if (destroyer != null && !frontier.getRules().contains(destroyer))
				change.add(ChangeFactory.addProductionRule(destroyer, frontier));
			final ProductionRule artilleryIT = data.getProductionRuleList().getProductionRule("buyArtilleryIndustrialTechnology");
			final ProductionRule destroyerIT = data.getProductionRuleList().getProductionRule("buyDestroyerIndustrialTechnology");
			final ProductionFrontier frontierIT = data.getProductionFrontierList().getProductionFrontier("productionIndustrialTechnology");
			if (artilleryIT != null && !frontierIT.getRules().contains(artilleryIT))
				change.add(ChangeFactory.addProductionRule(artilleryIT, frontierIT));
			if (destroyerIT != null && !frontierIT.getRules().contains(destroyerIT))
				change.add(ChangeFactory.addProductionRule(destroyerIT, frontierIT));
			if (!change.isEmpty())
			{
				aBridge.getHistoryWriter().startEvent("Adding destroyers and artillery production rules");
				aBridge.addChange(change);
			}
		}
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initShipyards(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final boolean useShipyards = games.strategy.triplea.Properties.getUse_Shipyards(data);
		if (useShipyards)
		{
			final CompositeChange change = new CompositeChange();
			final ProductionFrontier frontierShipyards = data.getProductionFrontierList().getProductionFrontier("productionShipyards");
			/*
			 * Remove the hardcoded productionRules and work through those from the XML as specified
			 */
			/*ProductionRule buyInfantry = data.getProductionRuleList().getProductionRule("buyInfantry");
			ProductionRule buyArtillery = data.getProductionRuleList().getProductionRule("buyArtillery");
			ProductionRule buyArmour = data.getProductionRuleList().getProductionRule("buyArmour");
			ProductionRule buyFighter = data.getProductionRuleList().getProductionRule("buyFighter");
			ProductionRule buyBomber = data.getProductionRuleList().getProductionRule("buyBomber");
			ProductionRule buyFactory = data.getProductionRuleList().getProductionRule("buyFactory");
			ProductionRule buyAAGun = data.getProductionRuleList().getProductionRule("buyAAGun");

			change.add(ChangeFactory.addProductionRule(buyInfantry, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyArtillery, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyArmour, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyFighter, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyBomber, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyFactory, frontierShipyards));
			change.add(ChangeFactory.addProductionRule(buyAAGun, frontierShipyards));*/
			/*
			 * Find the productionRules, if the unit is NOT a sea unit, add it to the ShipYards prod rule.
			 */
			final ProductionFrontier frontierNONShipyards = data.getProductionFrontierList().getProductionFrontier("production");
			final Collection<ProductionRule> rules = frontierNONShipyards.getRules();
			for (final ProductionRule rule : rules)
			{
				final String ruleName = rule.getName();
				final IntegerMap<NamedAttachable> ruleResults = rule.getResults();
				final NamedAttachable named = ruleResults.keySet().iterator().next();
				if (!(named instanceof UnitType))
					continue;
				final UnitType unit = data.getUnitTypeList().getUnitType(named.getName());
				final boolean isSea = UnitAttachment.get(unit).getIsSea();
				if (!isSea)
				{
					final ProductionRule prodRule = data.getProductionRuleList().getProductionRule(ruleName);
					change.add(ChangeFactory.addProductionRule(prodRule, frontierShipyards));
				}
			}
			aBridge.getHistoryWriter().startEvent("Adding shipyard production rules - land/air units");
			aBridge.addChange(change);
		}
	}
	
	private boolean isWW2V2(final GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initTwoHitBattleship(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final boolean userEnabled = games.strategy.triplea.Properties.getTwoHitBattleships(data);
		final UnitType battleShipUnit = data.getUnitTypeList().getUnitType(Constants.BATTLESHIP_TYPE);
		if (battleShipUnit == null)
			return;
		final UnitAttachment battleShipAttachment = UnitAttachment.get(battleShipUnit);
		final boolean defaultEnabled = battleShipAttachment.getHitPoints() > 1;
		if (userEnabled != defaultEnabled)
		{
			aBridge.getHistoryWriter().startEvent("TwoHitBattleships:" + userEnabled);
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(battleShipAttachment, userEnabled ? 2 : 1, "hitPoints"));
		}
	}
	
	/**
	 * @param data
	 */
	private void initOriginalOwner(final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		final CompositeChange changes = new CompositeChange();
		for (final Territory current : data.getMap())
		{
			if (!current.getOwner().isNull())
			{
				final TerritoryAttachment territoryAttachment = TerritoryAttachment.get(current);
				if (territoryAttachment == null)
					throw new IllegalStateException("No territory attachment for " + current);
				if (territoryAttachment.getOriginalOwner() == null && current.getOwner() != null)
					changes.add(OriginalOwnerTracker.addOriginalOwnerChange(current, current.getOwner()));
				final Collection<Unit> factoryAndInfrastructure = current.getUnits().getMatches(Matches.UnitIsInfrastructure);
				changes.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, current.getOwner()));
			}
			else if (!current.isWater())
			{
				final TerritoryAttachment territoryAttachment = TerritoryAttachment.get(current);
				if (territoryAttachment == null)
					throw new IllegalStateException("No territory attachment for " + current);
			}
		}
		aBridge.getHistoryWriter().startEvent("Adding original owners");
		aBridge.addChange(changes);
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


class InitializationExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -9000446777655823735L;
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
}
