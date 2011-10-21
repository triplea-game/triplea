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

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author Sean Bridges
 */
public class InitializationDelegate extends BaseDelegate
{
	
	/** Creates a new instance of InitializationDelegate */
	public InitializationDelegate()
	{
	}
	
	@Override
	public void initialize(String name, String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(IDelegateBridge aBridge)
	{
		super.start(aBridge);
		init(aBridge);
	}
	
	protected void init(IDelegateBridge aBridge)
	{
		initDestroyerArtillery(aBridge);
		
		initShipyards(aBridge);
		
		initTwoHitBattleship(aBridge);
		
		initOriginalOwner(aBridge);
		
		initTech(aBridge);
		
		initSkipUnusedBids(aBridge.getData());
	}
	
	private void initSkipUnusedBids(GameData data)
	{
		// we have a lot of bid steps, 12 for pact of steel
		// in multi player this can be time consuming, since each vm
		// must be notified (and have its ui) updated for each step,
		// so remove the bid steps that arent used
		
		for (GameStep step : data.getSequence())
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
	
	private void initTech(IDelegateBridge bridge)
	{
		GameData data = bridge.getData();
		Iterator<PlayerID> players = data.getPlayerList().getPlayers().iterator();
		while (players.hasNext())
		{
			PlayerID player = players.next();
			Iterator<TechAdvance> advances = TechTracker.getTechAdvances(player, data).iterator();
			if (advances.hasNext())
			{
				bridge.getHistoryWriter().startEvent("Initializing " + player.getName() + " with tech advances");
				while (advances.hasNext())
				{
					
					TechAdvance advance = advances.next();
					advance.perform(player, bridge);
				}
				
			}
		}
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initDestroyerArtillery(IDelegateBridge aBridge)
	{
		GameData data = aBridge.getData();
		boolean addArtilleryAndDestroyers = games.strategy.triplea.Properties.getUse_Destroyers_And_Artillery(data);
		
		if (!isWW2V2(data) && addArtilleryAndDestroyers)
		{
			CompositeChange change = new CompositeChange();
			ProductionRule artillery = data.getProductionRuleList().getProductionRule("buyArtillery");
			ProductionRule destroyer = data.getProductionRuleList().getProductionRule("buyDestroyer");
			ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier("production");
			
			change.add(ChangeFactory.addProductionRule(artillery, frontier));
			change.add(ChangeFactory.addProductionRule(destroyer, frontier));
			
			ProductionRule artilleryIT = data.getProductionRuleList().getProductionRule("buyArtilleryIndustrialTechnology");
			ProductionRule destroyerIT = data.getProductionRuleList().getProductionRule("buyDestroyerIndustrialTechnology");
			ProductionFrontier frontierIT = data.getProductionFrontierList().getProductionFrontier("productionIndustrialTechnology");
			
			change.add(ChangeFactory.addProductionRule(artilleryIT, frontierIT));
			change.add(ChangeFactory.addProductionRule(destroyerIT, frontierIT));
			
			aBridge.getHistoryWriter().startEvent("Adding destroyers and artillery production rules");
			aBridge.addChange(change);
			
		}
		
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initShipyards(IDelegateBridge aBridge)
	{
		GameData data = aBridge.getData();
		boolean useShipyards = games.strategy.triplea.Properties.getUse_Shipyards(data);
		
		if (useShipyards)
		{
			CompositeChange change = new CompositeChange();
			ProductionFrontier frontierShipyards = data.getProductionFrontierList().getProductionFrontier("productionShipyards");
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
			ProductionFrontier frontierNONShipyards = data.getProductionFrontierList().getProductionFrontier("production");
			Collection<ProductionRule> rules = frontierNONShipyards.getRules();
			Iterator<ProductionRule> ruleIter = rules.iterator();
			while (ruleIter.hasNext())
			{
				ProductionRule rule = ruleIter.next();
				String ruleName = rule.getName();
				IntegerMap<NamedAttachable> ruleResults = rule.getResults();
				
				String unitName = ruleResults.keySet().iterator().next().getName();
				UnitType unit = data.getUnitTypeList().getUnitType(unitName);
				boolean isSea = UnitAttachment.get(unit).isSea();
				if (!isSea)
				{
					ProductionRule prodRule = data.getProductionRuleList().getProductionRule(ruleName);
					change.add(ChangeFactory.addProductionRule(prodRule, frontierShipyards));
				}
			}
			
			aBridge.getHistoryWriter().startEvent("Adding shipyard production rules - land/air units");
			aBridge.addChange(change);
		}
	}
	
	private boolean isWW2V2(GameData data)
	{
		return games.strategy.triplea.Properties.getWW2V2(data);
	}
	
	/**
	 * @param data
	 * @param aBridge
	 */
	private void initTwoHitBattleship(IDelegateBridge aBridge)
	{
		GameData data = aBridge.getData();
		boolean userEnabled = games.strategy.triplea.Properties.getTwoHitBattleships(data);
		
		UnitType battleShipUnit = data.getUnitTypeList().getUnitType(Constants.BATTLESHIP_TYPE);
		if (battleShipUnit == null)
			return;
		
		UnitAttachment battleShipAttatchment = UnitAttachment.get(battleShipUnit);
		boolean defaultEnabled = battleShipAttatchment.isTwoHit();
		
		if (userEnabled != defaultEnabled)
		{
			aBridge.getHistoryWriter().startEvent("TwoHitBattleships:" + userEnabled);
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(battleShipAttatchment, "" + userEnabled, Constants.TWO_HIT));
		}
	}
	
	/**
	 * @param data
	 */
	private void initOriginalOwner(IDelegateBridge aBridge)
	{
		GameData data = aBridge.getData();
		OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
		
		CompositeChange changes = new CompositeChange();
		
		for (Territory current : data.getMap())
		{
			if (!current.getOwner().isNull())
			{
				// TODO: veqryn, maybe need to have infrastructure also initialized? Depends on if they should go back to their original owner or not. UnitIsAAOrIsFactoryOrIsInfrastructure
				changes.add(origOwnerTracker.addOriginalOwnerChange(current, current.getOwner()));
				Collection<Unit> aaAndFactory = current.getUnits().getMatches(Matches.UnitIsAAOrIsFactoryOrIsInfrastructure);
				changes.add(origOwnerTracker.addOriginalOwnerChange(aaAndFactory, current.getOwner()));
				TerritoryAttachment territoryAttachment = TerritoryAttachment.get(current);
				
				if (territoryAttachment == null)
					throw new IllegalStateException("No territory attachment for " + current);
				changes.add(ChangeFactory.attachmentPropertyChange(territoryAttachment, current.getOwner(), Constants.ORIGINAL_OWNER));
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
