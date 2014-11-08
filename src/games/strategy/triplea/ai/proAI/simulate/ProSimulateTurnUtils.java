package games.strategy.triplea.ai.proAI.simulate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.ProAttackTerritoryData;
import games.strategy.triplea.ai.proAI.ProBattleResultData;
import games.strategy.triplea.ai.proAI.util.LogUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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
 * Pro AI simulate turn utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProSimulateTurnUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	private final ProBattleUtils battleUtils;
	private final ProMoveUtils moveUtils;
	
	public ProSimulateTurnUtils(final ProAI ai, final ProUtils utils, final ProBattleUtils battleUtils, final ProMoveUtils moveUtils)
	{
		this.ai = ai;
		this.utils = utils;
		this.battleUtils = battleUtils;
		this.moveUtils = moveUtils;
	}
	
	public void simulateBattles(final GameData data, final PlayerID player, final IDelegateBridge delegateBridge)
	{
		LogUtils.log(Level.FINE, "Starting battle simulation phase");
		
		final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
		final Map<BattleType, Collection<Territory>> battleTerritories = battleDelegate.getBattles().getBattles();
		for (final Entry<BattleType, Collection<Territory>> entry : battleTerritories.entrySet())
		{
			for (final Territory t : entry.getValue())
			{
				final IBattle battle = battleDelegate.getBattleTracker().getPendingBattle(t, entry.getKey().isBombingRun(), entry.getKey());
				final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
				attackers.retainAll(t.getUnits().getUnits());
				final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
				defenders.retainAll(t.getUnits().getUnits());
				LogUtils.log(Level.FINER, "---" + t);
				LogUtils.log(Level.FINER, "attackers=" + attackers);
				LogUtils.log(Level.FINER, "defenders=" + defenders);
				final ProBattleResultData result = battleUtils.callBattleCalculator(player, t, attackers, defenders, true);
				// final ProBattleResultData result = battleUtils.calculateBattleResults(player, t, attackers, defenders, true);
				final List<Unit> remainingUnits = result.getAverageUnitsRemaining();
				LogUtils.log(Level.FINER, "remainingUnits=" + remainingUnits);
				
				// Make updates to data
				final List<Unit> attackersToRemove = new ArrayList<Unit>(attackers);
				attackersToRemove.removeAll(remainingUnits);
				final List<Unit> defendersToRemove = Match.getMatches(defenders, Matches.UnitIsInfrastructure.invert());
				final List<Unit> infrastructureToChangeOwner = Match.getMatches(defenders, Matches.UnitIsInfrastructure);
				LogUtils.log(Level.FINER, "attackersToRemove=" + attackersToRemove);
				LogUtils.log(Level.FINER, "defendersToRemove=" + defendersToRemove);
				LogUtils.log(Level.FINER, "infrastructureToChangeOwner=" + infrastructureToChangeOwner);
				final Change attackerskilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
				delegateBridge.addChange(attackerskilledChange);
				final Change defenderskilledChange = ChangeFactory.removeUnits(t, defendersToRemove);
				delegateBridge.addChange(defenderskilledChange);
				BattleTracker.captureOrDestroyUnits(t, player, player, delegateBridge, null, remainingUnits);
				if (!checkIfCapturedTerritoryIsAlliedCapital(t, data, player, delegateBridge))
					delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
				battleDelegate.getBattleTracker().getConquered().add(t);
				final Territory updatedTerritory = data.getMap().getTerritory(t.getName());
				LogUtils.log(Level.FINER, "after changes owner=" + updatedTerritory.getOwner() + ", units=" + updatedTerritory.getUnits().getUnits());
			}
		}
	}
	
	public Map<Territory, ProAttackTerritoryData> transferMoveMap(final Map<Territory, ProAttackTerritoryData> moveMap, final Map<Unit, Territory> unitTerritoryMap, final GameData fromData,
				final GameData toData, final PlayerID player)
	{
		LogUtils.log(Level.FINE, "Transferring move map");
		
		final Map<Territory, ProAttackTerritoryData> result = new HashMap<Territory, ProAttackTerritoryData>();
		final List<Unit> usedUnits = new ArrayList<Unit>();
		for (final Territory fromTerritory : moveMap.keySet())
		{
			final Territory toTerritory = toData.getMap().getTerritory(fromTerritory.getName());
			final ProAttackTerritoryData patd = new ProAttackTerritoryData(toTerritory);
			result.put(toTerritory, patd);
			final Map<Unit, List<Unit>> amphibAttackMap = moveMap.get(fromTerritory).getAmphibAttackMap();
			final Map<Unit, Boolean> isTransportingMap = moveMap.get(fromTerritory).getIsTransportingMap();
			LogUtils.log(Level.FINER, "Transferring " + fromTerritory + " to " + toTerritory);
			final List<Unit> amphibUnits = new ArrayList<Unit>();
			for (final Unit transport : amphibAttackMap.keySet())
			{
				Unit toTransport = null;
				final List<Unit> toUnits = new ArrayList<Unit>();
				if (isTransportingMap.get(transport))
				{
					toTransport = transferLoadedTransport(transport, amphibAttackMap.get(transport), unitTerritoryMap, usedUnits, toData, player);
					toUnits.addAll(TransportTracker.transporting(toTransport));
				}
				else
				{
					toTransport = transferUnit(transport, unitTerritoryMap, usedUnits, toData, player);
					for (final Unit u : amphibAttackMap.get(transport))
					{
						final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
						toUnits.add(toUnit);
					}
				}
				patd.addUnits(toUnits);
				patd.putAmphibAttackMap(toTransport, toUnits);
				amphibUnits.addAll(amphibAttackMap.get(transport));
				LogUtils.log(Level.FINEST, "---Transferring transport=" + transport + " with units=" + amphibAttackMap.get(transport) + " to transport=" + toTransport + " with units=" + toUnits);
			}
			for (final Unit u : moveMap.get(fromTerritory).getUnits())
			{
				if (!amphibUnits.contains(u))
				{
					final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
					patd.addUnit(toUnit);
					LogUtils.log(Level.FINEST, "---Transferring unit " + u + " to " + toUnit);
				}
			}
		}
		
		return result;
	}
	
	private boolean checkIfCapturedTerritoryIsAlliedCapital(final Territory t, final GameData data, final PlayerID player, final IDelegateBridge delegateBridge)
	{
		final PlayerID terrOrigOwner = OriginalOwnerTracker.getOriginalOwner(t);
		final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
		final TerritoryAttachment ta = TerritoryAttachment.get(t);
		if (ta != null && ta.getCapital() != null && terrOrigOwner != null && TerritoryAttachment.getAllCapitals(terrOrigOwner, data).contains(t)
					&& relationshipTracker.isAllied(terrOrigOwner, player))
		{
			// Give capital and any allied territories back to original owner
			final Collection<Territory> originallyOwned = OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
			final List<Territory> friendlyTerritories = Match.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner, data));
			friendlyTerritories.add(t);
			for (final Territory item : friendlyTerritories)
			{
				if (item.getOwner() == terrOrigOwner)
					continue;
				final Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
				delegateBridge.addChange(takeOverFriendlyTerritories);
				final Collection<Unit> units = Match.getMatches(item.getUnits().getUnits(), Matches.UnitIsInfrastructure);
				if (!units.isEmpty())
				{
					final Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, t);
					delegateBridge.addChange(takeOverNonComUnits);
				}
			}
			return true;
		}
		return false;
	}
	
	private Unit transferUnit(final Unit u, final Map<Unit, Territory> unitTerritoryMap, final List<Unit> usedUnits, final GameData toData, final PlayerID player)
	{
		final Territory unitTerritory = unitTerritoryMap.get(u);
		final List<Unit> toUnits = toData.getMap().getTerritory(unitTerritory.getName()).getUnits().getMatches(ProMatches.unitIsOwnedAndMatchesTypeAndNotTransporting(player, u.getType()));
		for (final Unit toUnit : toUnits)
		{
			if (!usedUnits.contains(toUnit))
			{
				usedUnits.add(toUnit);
				return toUnit;
			}
		}
		return null;
	}
	
	private Unit transferLoadedTransport(final Unit transport, final List<Unit> transportingUnits, final Map<Unit, Territory> unitTerritoryMap, final List<Unit> usedUnits, final GameData toData,
				final PlayerID player)
	{
		final Territory unitTerritory = unitTerritoryMap.get(transport);
		final List<Unit> toTransports = toData.getMap().getTerritory(unitTerritory.getName()).getUnits().getMatches(ProMatches.unitIsOwnedAndMatchesTypeAndIsTransporting(player, transport.getType()));
		for (final Unit toTransport : toTransports)
		{
			if (!usedUnits.contains(toTransport))
			{
				final List<Unit> toTransportingUnits = (List<Unit>) TransportTracker.transporting(toTransport);
				if (transportingUnits.size() == toTransportingUnits.size())
				{
					boolean canTransfer = true;
					for (int i = 0; i < transportingUnits.size(); i++)
					{
						if (!transportingUnits.get(i).getType().equals(toTransportingUnits.get(i).getType()))
						{
							canTransfer = false;
							break;
						}
					}
					if (canTransfer)
					{
						usedUnits.add(toTransport);
						usedUnits.addAll(toTransportingUnits);
						return toTransport;
					}
				}
			}
		}
		return null;
	}
	
}
