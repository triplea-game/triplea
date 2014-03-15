/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * BattleDelegate.java
 * 
 * Created on November 2, 2001, 12:26 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseTripleADelegate;
import games.strategy.common.delegate.GameDelegateBridge;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Sean Bridges
 * @version 1.0
 */
@AutoSave(beforeStepStart = true, afterStepEnd = true)
public class BattleDelegate extends BaseTripleADelegate implements IBattleDelegate
{
	private BattleTracker m_battleTracker = new BattleTracker();
	// private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
	private boolean m_needToInitialize = true;
	private boolean m_needToScramble = true;
	private boolean m_needToKamikazeSuicideAttacks = true;
	private boolean m_needToClearEmptyAirBattleAttacks = true;
	private boolean m_needToAddBombardmentSources = true;
	private boolean m_needToRecordBattleStatistics = true;
	private boolean m_needToCheckDefendingPlanesCanLand = true;
	private boolean m_needToCleanup = true;
	private IBattle m_currentBattle = null;
	
	/**
	 * Called before the delegate will run, AND before "start" is called.
	 */
	@Override
	public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge)
	{
		super.setDelegateBridgeAndPlayer(new GameDelegateBridge(iDelegateBridge));
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		// we may start multiple times due to loading after saving
		// only initialize once
		if (m_needToInitialize)
		{
			doInitialize(m_battleTracker, m_bridge);
			m_needToInitialize = false;
		}
		// do pre-combat stuff, like scrambling, after we have setup all battles, but before we have bombardment, etc.
		// the order of all of this stuff matters quite a bit.
		if (m_needToScramble)
		{
			doScrambling();
			m_needToScramble = false;
		}
		if (m_needToKamikazeSuicideAttacks)
		{
			doKamikazeSuicideAttacks();
			m_needToKamikazeSuicideAttacks = false;
		}
		if (m_needToClearEmptyAirBattleAttacks)
		{
			clearEmptyAirBattleAttacks(m_battleTracker, m_bridge);
			m_needToClearEmptyAirBattleAttacks = false;
		}
		if (m_needToAddBombardmentSources)
		{
			addBombardmentSources();
			m_needToAddBombardmentSources = false;
		}
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		if (m_needToRecordBattleStatistics)
		{
			getBattleTracker().sendBattleRecordsToGameData(m_bridge);
			m_needToRecordBattleStatistics = false;
		}
		if (m_needToCleanup)
		{
			getBattleTracker().clearBattleRecords();
			scramblingCleanup();
			airBattleCleanup();
			m_needToCleanup = false;
		}
		if (m_needToCheckDefendingPlanesCanLand)
		{
			checkDefendingPlanesCanLand();
			m_needToCheckDefendingPlanesCanLand = false;
		}
		super.end();
		m_needToInitialize = true;
		m_needToScramble = true;
		m_needToKamikazeSuicideAttacks = true;
		m_needToClearEmptyAirBattleAttacks = true;
		m_needToAddBombardmentSources = true;
		m_needToRecordBattleStatistics = true;
		m_needToCleanup = true;
		m_needToCheckDefendingPlanesCanLand = true;
	}
	
	@Override
	public Serializable saveState()
	{
		final BattleExtendedDelegateState state = new BattleExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_battleTracker = m_battleTracker;
		// state.m_originalOwnerTracker = m_originalOwnerTracker;
		state.m_needToInitialize = m_needToInitialize;
		state.m_needToScramble = m_needToScramble;
		state.m_needToKamikazeSuicideAttacks = m_needToKamikazeSuicideAttacks;
		state.m_needToClearEmptyAirBattleAttacks = m_needToClearEmptyAirBattleAttacks;
		state.m_needToAddBombardmentSources = m_needToAddBombardmentSources;
		state.m_needToRecordBattleStatistics = m_needToRecordBattleStatistics;
		state.m_needToCheckDefendingPlanesCanLand = m_needToCheckDefendingPlanesCanLand;
		state.m_needToCleanup = m_needToCleanup;
		state.m_currentBattle = m_currentBattle;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final BattleExtendedDelegateState s = (BattleExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_battleTracker = s.m_battleTracker;
		// m_originalOwnerTracker = s.m_originalOwnerTracker;
		m_needToInitialize = s.m_needToInitialize;
		m_needToScramble = s.m_needToScramble;
		m_needToKamikazeSuicideAttacks = s.m_needToKamikazeSuicideAttacks;
		m_needToClearEmptyAirBattleAttacks = s.m_needToClearEmptyAirBattleAttacks;
		m_needToAddBombardmentSources = s.m_needToAddBombardmentSources;
		m_needToRecordBattleStatistics = s.m_needToRecordBattleStatistics;
		m_needToCheckDefendingPlanesCanLand = s.m_needToCheckDefendingPlanesCanLand;
		m_needToCleanup = s.m_needToCleanup;
		m_currentBattle = s.m_currentBattle;
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		final BattleListing battles = getBattles();
		if (battles.isEmpty())
		{
			final IBattle battle = getCurrentBattle();
			if (battle != null)
			{
				return true;
			}
			return false;
		}
		return true;
	}
	
	public static void doInitialize(final BattleTracker battleTracker, final IDelegateBridge aBridge)
	{
		setupUnitsInSameTerritoryBattles(battleTracker, aBridge);
		setupTerritoriesAbandonedToTheEnemy(battleTracker, aBridge);
		battleTracker.clearFinishedBattles(aBridge); // these are "blitzed" and "conquered" territories without a fight, without a pending battle
		resetMaxScrambleCount(aBridge);
	}
	
	public static void clearEmptyAirBattleAttacks(final BattleTracker battleTracker, final IDelegateBridge aBridge)
	{
		battleTracker.clearEmptyAirBattleAttacks(aBridge); // these are air battle and air raids where there is no defender, probably because no air is in range to defend
	}
	
	public String fightCurrentBattle()
	{
		if (m_currentBattle == null)
			return null;
		// fight the battle
		m_currentBattle.fight(m_bridge);
		m_currentBattle = null;
		// and were done
		return null;
	}
	
	public String fightBattle(final Territory territory, final boolean bombing, final BattleType type)
	{
		final IBattle battle = m_battleTracker.getPendingBattle(territory, bombing, type);
		if (m_currentBattle != null && m_currentBattle != battle)
		{
			return "Must finish " + getFightingWord(m_currentBattle) + " in " + m_currentBattle.getTerritory() + " first";
		}
		// does the battle exist
		if (battle == null)
			return "No pending battle in" + territory.getName();
		// are there battles that must occur first
		final Collection<IBattle> allMustPrecede = m_battleTracker.getDependentOn(battle);
		if (!allMustPrecede.isEmpty())
		{
			final IBattle firstPrecede = allMustPrecede.iterator().next();
			final String name = firstPrecede.getTerritory().getName();
			return "Must complete " + getFightingWord(firstPrecede) + " in " + name + " first";
		}
		m_currentBattle = battle;
		// fight the battle
		battle.fight(m_bridge);
		m_currentBattle = null;
		// and were done
		return null;
	}
	
	private String getFightingWord(final IBattle battle)
	{
		return battle.getBattleType().toString();
	}
	
	public BattleListing getBattles()
	{
		return m_battleTracker.getPendingBattleSites();
	}
	
	/**
	 * @return
	 */
	private boolean isShoreBombardPerGroundUnitRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getShoreBombardPerGroundUnitRestricted(data);
	}
	
	public BattleTracker getBattleTracker()
	{
		return m_battleTracker;
	}
	
	public IDelegateBridge getBattleBridge()
	{
		return getBridge();
	}
	
	/*
	public OriginalOwnerTracker getOriginalOwnerTracker()
	{
		return m_originalOwnerTracker;
	}*/
	
	/**
	 * Add bombardment units to battles.
	 */
	private void addBombardmentSources()
	{
		final PlayerID attacker = m_bridge.getPlayerID();
		final ITripleaPlayer remotePlayer = getRemotePlayer();
		final Match<Unit> ownedAndCanBombard = new CompositeMatchAnd<Unit>(Matches.unitCanBombard(attacker), Matches.unitIsOwnedBy(attacker));
		final Map<Territory, Collection<IBattle>> adjBombardment = getPossibleBombardingTerritories();
		final Iterator<Territory> territories = adjBombardment.keySet().iterator();
		final boolean shoreBombardPerGroundUnitRestricted = isShoreBombardPerGroundUnitRestricted(getData());
		while (territories.hasNext())
		{
			final Territory t = territories.next();
			if (!m_battleTracker.hasPendingBattle(t, false))
			{
				Collection<IBattle> battles = adjBombardment.get(t);
				battles = Match.getMatches(battles, Matches.BattleIsAmphibious);
				if (!battles.isEmpty())
				{
					final Collection<Unit> bombardUnits = t.getUnits().getMatches(ownedAndCanBombard);
					final List<Unit> ListedBombardUnits = new ArrayList<Unit>();
					ListedBombardUnits.addAll(bombardUnits);
					sortUnitsToBombard(ListedBombardUnits, attacker);
					final Iterator<Unit> bombarding = ListedBombardUnits.iterator();
					if (!bombardUnits.isEmpty())
					{
						// ask if they want to bombard
						if (!remotePlayer.selectShoreBombard(t))
						{
							continue;
						}
					}
					while (bombarding.hasNext())
					{
						final Unit u = bombarding.next();
						final IBattle battle = selectBombardingBattle(u, t, battles);
						if (battle != null)
						{
							if (shoreBombardPerGroundUnitRestricted)
							{
								if (battle.getAmphibiousLandAttackers().size() <= battle.getBombardingUnits().size())
								{
									battles.remove(battle);
									break;
								}
							}
							battle.addBombardingUnit(u);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Sort the specified units in preferred movement or unload order.
	 */
	private void sortUnitsToBombard(final List<Unit> units, final PlayerID player)
	{
		if (units.isEmpty())
			return;
		Collections.sort(units, UnitComparator.getDecreasingAttackComparator(player));
	}
	
	/**
	 * Return map of adjacent territories along attack routes in battles where fighting will occur.
	 */
	private Map<Territory, Collection<IBattle>> getPossibleBombardingTerritories()
	{
		final Map<Territory, Collection<IBattle>> possibleBombardingTerritories = new HashMap<Territory, Collection<IBattle>>();
		final Iterator<Territory> battleTerritories = m_battleTracker.getPendingBattleSites(false).iterator();
		while (battleTerritories.hasNext())
		{
			final Territory t = battleTerritories.next();
			final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
			// we only care about battles where we must fight
			// this check is really to avoid implementing getAttackingFrom() in other battle subclasses
			if (!(battle instanceof MustFightBattle))
				continue;
			// bombarding can only occur in territories from which at least 1 land unit attacked
			final Map<Territory, Collection<Unit>> attackingFromMap = ((MustFightBattle) battle).getAttackingFromMap();
			final Iterator<Territory> bombardingTerritories = ((MustFightBattle) battle).getAttackingFrom().iterator();
			while (bombardingTerritories.hasNext())
			{
				final Territory neighbor = bombardingTerritories.next();
				// we do not allow bombarding from certain sea zones (like if there was a kamikaze suicide attack there, etc)
				if (m_battleTracker.noBombardAllowedFromHere(neighbor))
					continue;
				// If all units from a territory are air- no bombard
				if (Match.allMatch(attackingFromMap.get(neighbor), Matches.UnitIsAir))
				{
					continue;
				}
				Collection<IBattle> battles = possibleBombardingTerritories.get(neighbor);
				if (battles == null)
				{
					battles = new ArrayList<IBattle>();
					possibleBombardingTerritories.put(neighbor, battles);
				}
				battles.add(battle);
			}
		}
		return possibleBombardingTerritories;
	}
	
	/**
	 * Select which territory to bombard.
	 */
	private IBattle selectBombardingBattle(final Unit u, final Territory uTerritory, final Collection<IBattle> battles)
	{
		final Boolean bombardRestricted = isShoreBombardPerGroundUnitRestricted(getData());
		// If only one battle to select from just return that battle
		// boolean hasNotMoved = TripleAUnit.get(u).getAlreadyMoved() == 0;
		// if ((battles.size() == 1) && !hasNotMoved)
		if ((battles.size() == 1))
		{
			return battles.iterator().next();
		}
		final List<Territory> territories = new ArrayList<Territory>();
		final Map<Territory, IBattle> battleTerritories = new HashMap<Territory, IBattle>();
		final Iterator<IBattle> battlesIter = battles.iterator();
		while (battlesIter.hasNext())
		{
			final IBattle battle = battlesIter.next();
			// If Restricted & # of bombarding units => landing units, don't add territory to list to bombard
			if (bombardRestricted)
			{
				if (battle.getBombardingUnits().size() < battle.getAmphibiousLandAttackers().size())
					territories.add(battle.getTerritory());
			}
			else
			{
				territories.add(battle.getTerritory());
			}
			battleTerritories.put(battle.getTerritory(), battle);
		}
		final ITripleaPlayer remotePlayer = getRemotePlayer();
		Territory bombardingTerritory = null;
		if (!territories.isEmpty())
			bombardingTerritory = remotePlayer.selectBombardingTerritory(u, uTerritory, territories, true);
		if (bombardingTerritory != null)
		{
			return battleTerritories.get(bombardingTerritory);
		}
		return null; // User elected not to bombard with this unit
	}
	
	private static void landParatroopers(final PlayerID player, final Territory battleSite, final GameData data, final IDelegateBridge bridge)
	{
		if (TechTracker.hasParatroopers(player))
		{
			final Collection<Unit> airTransports = Match.getMatches(battleSite.getUnits().getUnits(), Matches.UnitIsAirTransport);
			final Collection<Unit> paratroops = Match.getMatches(battleSite.getUnits().getUnits(), Matches.UnitIsAirTransportable);
			if (!airTransports.isEmpty() && !paratroops.isEmpty())
			{
				final CompositeChange change = new CompositeChange();
				for (final Unit u : paratroops)
				{
					final TripleAUnit taUnit = (TripleAUnit) u;
					final Unit transport = taUnit.getTransportedBy();
					if (transport == null || !airTransports.contains(transport))
						continue;
					change.add(TransportTracker.unloadAirTransportChange(taUnit, battleSite, player, false));
				}
				if (!change.isEmpty())
				{
					bridge.getHistoryWriter().startEvent(player.getName() + " lands units in " + battleSite.getName());
					bridge.addChange(change);
				}
			}
		}
	}
	
	/**
	 * Setup the battles where the battle occurs because units are in the
	 * same territory. This happens when subs emerge (after being submerged), and
	 * when naval units are placed in enemy occupied sea zones, and also
	 * when political relationships change and potentially leave units in now-hostile territories.
	 */
	private static void setupUnitsInSameTerritoryBattles(final BattleTracker battleTracker, final IDelegateBridge aBridge)
	{
		final PlayerID player = aBridge.getPlayerID();
		final GameData data = aBridge.getData();
		final boolean ignoreTransports = isIgnoreTransportInMovement(data);
		final boolean ignoreSubs = isIgnoreSubInMovement(data);
		final CompositeMatchAnd<Unit> seaTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsSea);
		final CompositeMatchOr<Unit> seaTranportsOrSubs = new CompositeMatchOr<Unit>(seaTransports, Matches.UnitIsSub);
		// we want to match all sea zones with our units and enemy units
		final CompositeMatch<Territory> anyTerritoryWithOwnAndEnemy = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player), Matches.territoryHasEnemyUnits(player, data));
		final CompositeMatch<Territory> enemyTerritoryAndOwnUnits = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
					Matches.territoryHasUnitsOwnedBy(player));
		final CompositeMatch<Territory> enemyUnitsOrEnemyTerritory = new CompositeMatchOr<Territory>(anyTerritoryWithOwnAndEnemy, enemyTerritoryAndOwnUnits);
		final Iterator<Territory> battleTerritories = Match.getMatches(data.getMap().getTerritories(), enemyUnitsOrEnemyTerritory).iterator();
		while (battleTerritories.hasNext())
		{
			final Territory territory = battleTerritories.next();
			final List<Unit> attackingUnits = territory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			// now make sure to add any units that must move with these attacking units, so that they get included as dependencies
			final Map<Unit, Collection<Unit>> transportMap = TransportTracker.transporting(territory.getUnits().getUnits());
			final HashSet<Unit> dependants = new HashSet<Unit>();
			for (final Entry<Unit, Collection<Unit>> entry : transportMap.entrySet())
			{
				// only consider those transports that we are attacking with. allied and enemy transports are not added.
				if (attackingUnits.contains(entry.getKey()))
				{
					dependants.addAll(entry.getValue());
				}
			}
			dependants.removeAll(attackingUnits); // no duplicates
			attackingUnits.addAll(dependants); // add the dependants to the attacking list
			
			final List<Unit> enemyUnits = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
			final IBattle bombingBattle = battleTracker.getPendingBattle(territory, true, null);
			if (bombingBattle != null)
			{
				// we need to remove any units which are participating in bombing raids
				attackingUnits.removeAll(bombingBattle.getAttackingUnits());
			}
			if (attackingUnits.isEmpty() || Match.allMatch(attackingUnits, Matches.UnitIsInfrastructure))
				continue;
			IBattle battle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
			if (battle == null)
			{
				// we must land any paratroopers here, but only if there is not going to be a battle (cus battles land them separately, after aa fires)
				if (enemyUnits.isEmpty() || Match.allMatch(enemyUnits, Matches.UnitIsInfrastructure))
					landParatroopers(player, territory, data, aBridge);
				aBridge.getHistoryWriter().startEvent(player.getName() + " creates battle in territory " + territory.getName());
				battleTracker.addBattle(new RouteScripted(territory), attackingUnits, false, player, aBridge, null, null);
				battle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
			}
			if (battle == null)
				continue;
			if (bombingBattle != null)
			{
				battleTracker.addDependency(battle, bombingBattle);
			}
			if (battle.isEmpty())
				battle.addAttackChange(new RouteScripted(territory), attackingUnits, null);
			if (!battle.getAttackingUnits().containsAll(attackingUnits))
			{
				List<Unit> attackingUnitsNeedToBeAdded = new ArrayList<Unit>(attackingUnits);
				attackingUnitsNeedToBeAdded.removeAll(battle.getAttackingUnits());
				attackingUnitsNeedToBeAdded.removeAll(battle.getDependentUnits(battle.getAttackingUnits()));
				if (territory.isWater())
					attackingUnitsNeedToBeAdded = Match.getMatches(attackingUnitsNeedToBeAdded, Matches.UnitIsLand.invert());
				else
					attackingUnitsNeedToBeAdded = Match.getMatches(attackingUnitsNeedToBeAdded, Matches.UnitIsSea.invert());
				if (!attackingUnitsNeedToBeAdded.isEmpty())
				{
					battle.addAttackChange(new RouteScripted(territory), attackingUnitsNeedToBeAdded, null);
				}
			}
			// Reach stalemate if all attacking and defending units are transports
			if ((ignoreTransports && Match.allMatch(attackingUnits, seaTransports) && Match.allMatch(enemyUnits, seaTransports))
						|| ((Match.allMatch(attackingUnits, Matches.unitHasAttackValueOfAtLeast(1).invert())) && Match.allMatch(enemyUnits, Matches.unitHasDefendValueOfAtLeast(1).invert())))
			{
				final BattleResults results = new BattleResults(battle, WhoWon.DRAW, data);
				battleTracker.getBattleRecords(data).addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.STALEMATE, results, 0);
				battle.cancelBattle(aBridge);
				battleTracker.removeBattle(battle);
				continue;
			}
			// possibility to ignore battle altogether
			if (!attackingUnits.isEmpty())
			{
				final ITripleaPlayer remotePlayer = getRemotePlayer(aBridge);
				if (territory.isWater() && games.strategy.triplea.Properties.getSeaBattlesMayBeIgnored(data))
				{
					if (!remotePlayer.selectAttackUnits(territory))
					{
						final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
						battleTracker.getBattleRecords(data).addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.NO_BATTLE, results, 0);
						battle.cancelBattle(aBridge);
						battleTracker.removeBattle(battle);
					}
					continue;
				}
				// Check for ignored units
				if (ignoreTransports || ignoreSubs)
				{
					// TODO check if incoming units can attack before asking
					// if only enemy transports... attack them?
					if (ignoreTransports && Match.allMatch(enemyUnits, seaTransports))
					{
						if (!remotePlayer.selectAttackTransports(territory))
						{
							final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
							battleTracker.getBattleRecords(data).addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.NO_BATTLE, results, 0);
							battle.cancelBattle(aBridge);
							battleTracker.removeBattle(battle);
							// TODO perhaps try to reverse the setting of 0 movement left
							/*CompositeChange change = new CompositeChange();
							Iterator<Unit> attackIter = attackingUnits.iterator();
							while(attackIter.hasNext())
							{
							TripleAUnit attacker = (TripleAUnit) attackIter.next();
							change.add(ChangeFactory.unitPropertyChange(attacker, TripleAUnit.get(unit).getMaxMovementAllowed(), TripleAUnit.ALREADY_MOVED));
							//change.add(DelegateFinder.moveDelegate(m_data).markNoMovementChange(attackingUnits));    + attacker.getMovementLeft()
							}*/
						}
						continue;
					}
					// if only enemy subs... attack them?
					if (ignoreSubs && Match.allMatch(enemyUnits, Matches.UnitIsSub))
					{
						if (!remotePlayer.selectAttackSubs(territory))
						{
							final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
							battleTracker.getBattleRecords(data).addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.NO_BATTLE, results, 0);
							battle.cancelBattle(aBridge);
							battleTracker.removeBattle(battle);
						}
						continue;
					}
					// if only enemy transports and subs... attack them?
					if (ignoreSubs && ignoreTransports && Match.allMatch(enemyUnits, seaTranportsOrSubs))
					{
						if (!remotePlayer.selectAttackUnits(territory))
						{
							final BattleResults results = new BattleResults(battle, WhoWon.NOTFINISHED, data);
							battleTracker.getBattleRecords(data).addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.NO_BATTLE, results, 0);
							battle.cancelBattle(aBridge);
							battleTracker.removeBattle(battle);
						}
						continue;
					}
				}
			}
		}
	}
	
	/**
	 * Setup the battles where we have abandoned a contested territory during combat move to the enemy.
	 * The enemy then takes over the territory in question.
	 */
	private static void setupTerritoriesAbandonedToTheEnemy(final BattleTracker battleTracker, final IDelegateBridge aBridge)
	{
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(data))
			return;
		final PlayerID player = aBridge.getPlayerID();
		final Iterator<Territory> battleTerritories = Match.getMatches(data.getMap().getTerritories(),
					Matches.territoryHasEnemyUnitsThatCanCaptureTerritoryAndTerritoryOwnedByTheirEnemyAndIsNotUnownedWater(player, data))
					.iterator();
		// all territories that contain enemy units, where the territory is owned by an enemy of these units
		while (battleTerritories.hasNext())
		{
			final Territory territory = battleTerritories.next();
			final List<Unit> abandonedToUnits = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
			final PlayerID abandonedToPlayer = AbstractBattle.findPlayerWithMostUnits(abandonedToUnits);
			{
				// now make sure to add any units that must move with these units, so that they get included as dependencies
				final Map<Unit, Collection<Unit>> transportMap = TransportTracker.transporting(territory.getUnits().getUnits());
				final HashSet<Unit> dependants = new HashSet<Unit>();
				for (final Entry<Unit, Collection<Unit>> entry : transportMap.entrySet())
				{
					// only consider those transports that are part of our group
					if (abandonedToUnits.contains(entry.getKey()))
					{
						dependants.addAll(entry.getValue());
					}
				}
				dependants.removeAll(abandonedToUnits); // no duplicates
				abandonedToUnits.addAll(dependants); // add the dependants to the attacking list
			}
			// either we have abandoned the territory (so there are no more units that are enemy units of our enemy units)
			// or we are possibly bombing the territory (so we may have units there still)
			final Set<Unit> enemyUnitsOfAbandonedToUnits = new HashSet<Unit>();
			final Set<PlayerID> enemyPlayers = new HashSet<PlayerID>();
			for (final Unit u : abandonedToUnits)
			{
				enemyPlayers.add(u.getOwner());
			}
			for (final PlayerID p : enemyPlayers)
			{
				enemyUnitsOfAbandonedToUnits.addAll(territory.getUnits().getMatches(Matches.unitIsEnemyOf(data, p)));
			}
			// only look at bombing battles, because otherwise the normal attack will determine the ownership of the territory
			final IBattle bombingBattle = battleTracker.getPendingBattle(territory, true, null);
			if (bombingBattle != null)
				enemyUnitsOfAbandonedToUnits.removeAll(bombingBattle.getAttackingUnits());
			if (!enemyUnitsOfAbandonedToUnits.isEmpty())
				continue;
			final IBattle nonFightingBattle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
			if (nonFightingBattle != null)
				throw new IllegalStateException("Should not be possible to have a normal battle in: " + territory.getName() + " and have abandoned or only bombing there too.");
			aBridge.getHistoryWriter().startEvent(player.getName() + " has abandoned " + territory.getName() + " to " + abandonedToPlayer.getName(), abandonedToUnits);
			battleTracker.takeOver(territory, abandonedToPlayer, aBridge, null, abandonedToUnits);
			// TODO: if there are multiple defending unit owners, allow picking which one takes over the territory
			/* below way could be changed to use a FinishedBattle, but this is overly complicated and would only be needed if people plan to use BattleRecords or a condition based on BattleRecords to find the 'conquering' of an abandoned territory
			// nonFightingBattle = new FinishedBattle(territory, abandonedToPlayer, battleTracker, false, BattleType.NORMAL, data, BattleRecord.BattleResultDescription.CONQUERED, WhoWon.ATTACKER, abandonedToUnits);
			// m_pendingBattles.add(nonFightingBattle);
			// getBattleRecords(data).addBattle(id, nonFight.getBattleID(), current, nonFightingBattle.getBattleType(), data);
			aBridge.getHistoryWriter().startEvent(abandonedToPlayer.getName() + " creates battle in territory " + territory.getName());
			battleTracker.addBattle(new RouteScripted(territory), abandonedToUnits, false, abandonedToPlayer, aBridge, null, null);
			nonFightingBattle = battleTracker.getPendingBattle(territory, false, BattleType.NORMAL);
			if (nonFightingBattle == null)
				continue;
			// There is a potential bug, where if we are bombing a territory that would otherwise be taken over by the enemy (through this method), the bomber will 'defend' against the 'abandonedToPlayer'.
			if (nonFightingBattle.isEmpty())
				nonFightingBattle.addAttackChange(new RouteScripted(territory), abandonedToUnits, null);
			if (!nonFightingBattle.getAttackingUnits().containsAll(abandonedToUnits))
			{
				List<Unit> attackingUnitsNeedToBeAdded = new ArrayList<Unit>(abandonedToUnits);
				attackingUnitsNeedToBeAdded.removeAll(nonFightingBattle.getAttackingUnits());
				if (territory.isWater())
					attackingUnitsNeedToBeAdded = Match.getMatches(attackingUnitsNeedToBeAdded, Matches.UnitIsLand.invert());
				else
					attackingUnitsNeedToBeAdded = Match.getMatches(attackingUnitsNeedToBeAdded, Matches.UnitIsSea.invert());
				if (!attackingUnitsNeedToBeAdded.isEmpty())
				{
					nonFightingBattle.addAttackChange(new RouteScripted(territory), attackingUnitsNeedToBeAdded, null);
				}
			}*/
		}
	}
	
	private void doScrambling()
	{
		// first, figure out all the territories where scrambling units could scramble to
		// then ask the defending player if they wish to scramble units there, and actually move the units there
		final GameData data = getData();
		if (!games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data))
			return;
		final boolean fromIslandOnly = games.strategy.triplea.Properties.getScramble_From_Island_Only(data);
		final boolean toSeaOnly = games.strategy.triplea.Properties.getScramble_To_Sea_Only(data);
		final boolean toAnyAmphibious = games.strategy.triplea.Properties.getScrambleToAnyAmphibiousAssault(data);
		final boolean toSBR = games.strategy.triplea.Properties.getCanScrambleIntoAirBattles(data);
		int maxScrambleDistance = 0;
		final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
		while (utIter.hasNext())
		{
			final UnitAttachment ua = UnitAttachment.get(utIter.next());
			if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance())
				maxScrambleDistance = ua.getMaxScrambleDistance();
		}
		final Match<Unit> airbasesCanScramble = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, m_player), Matches.UnitIsAirBase, Matches.UnitIsNotDisabled);
		final CompositeMatchAnd<Territory> canScramble = new CompositeMatchAnd<Territory>(new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, Matches.isTerritoryEnemy(m_player, data)),
					Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.UnitCanScramble, Matches.unitIsEnemyOf(data, m_player), Matches.UnitIsNotDisabled)),
					Matches.territoryHasUnitsThatMatch(airbasesCanScramble));
		if (fromIslandOnly)
			canScramble.add(Matches.TerritoryIsIsland);
		final HashMap<Territory, HashSet<Territory>> scrambleTerrs = new HashMap<Territory, HashSet<Territory>>();
		final Set<Territory> territoriesWithBattles = m_battleTracker.getPendingBattleSites().getNormalBattlesIncludingAirBattles();
		if (toSBR)
			territoriesWithBattles.addAll(m_battleTracker.getPendingBattleSites().getStrategicBombingRaidsIncludingAirBattles());
		final Set<Territory> territoriesWithBattlesWater = new HashSet<Territory>();
		final Set<Territory> territoriesWithBattlesLand = new HashSet<Territory>();
		territoriesWithBattlesWater.addAll(Match.getMatches(territoriesWithBattles, Matches.TerritoryIsWater));
		territoriesWithBattlesLand.addAll(Match.getMatches(territoriesWithBattles, Matches.TerritoryIsLand));
		for (final Territory battleTerr : territoriesWithBattlesWater)
		{
			final HashSet<Territory> canScrambleFrom = new HashSet<Territory>(Match.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
			if (!canScrambleFrom.isEmpty())
				scrambleTerrs.put(battleTerr, canScrambleFrom);
		}
		for (final Territory battleTerr : territoriesWithBattlesLand)
		{
			if (!toSeaOnly)
			{
				final HashSet<Territory> canScrambleFrom = new HashSet<Territory>(Match.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
				if (!canScrambleFrom.isEmpty())
					scrambleTerrs.put(battleTerr, canScrambleFrom);
			}
			final IBattle battle = m_battleTracker.getPendingBattle(battleTerr, false, BattleType.NORMAL);
			// do not forget we may already have the territory in the list, so we need to add to the collection, not overwrite it.
			if (battle != null && battle.isAmphibious())
			{
				if (battle instanceof MustFightBattle)
				{
					final MustFightBattle mfb = (MustFightBattle) battle;
					final Collection<Territory> amphibFromTerrs = mfb.getAmphibiousAttackTerritories();
					amphibFromTerrs.removeAll(territoriesWithBattlesWater);
					for (final Territory amphibFrom : amphibFromTerrs)
					{
						HashSet<Territory> canScrambleFrom = scrambleTerrs.get(amphibFrom);
						if (canScrambleFrom == null)
							canScrambleFrom = new HashSet<Territory>();
						if (toAnyAmphibious)
							canScrambleFrom.addAll(Match.getMatches(data.getMap().getNeighbors(amphibFrom, maxScrambleDistance), canScramble));
						else if (canScramble.match(battleTerr))
							canScrambleFrom.add(battleTerr);
						if (!canScrambleFrom.isEmpty())
							scrambleTerrs.put(amphibFrom, canScrambleFrom);
					}
				}
				if (battle instanceof NonFightingBattle)
				{
					final NonFightingBattle nfb = (NonFightingBattle) battle;
					final Collection<Territory> amphibFromTerrs = nfb.getAmphibiousAttackTerritories();
					amphibFromTerrs.removeAll(territoriesWithBattlesWater);
					for (final Territory amphibFrom : amphibFromTerrs)
					{
						HashSet<Territory> canScrambleFrom = scrambleTerrs.get(amphibFrom);
						if (canScrambleFrom == null)
							canScrambleFrom = new HashSet<Territory>();
						if (toAnyAmphibious)
							canScrambleFrom.addAll(Match.getMatches(data.getMap().getNeighbors(amphibFrom, maxScrambleDistance), canScramble));
						else if (canScramble.match(battleTerr))
							canScrambleFrom.add(battleTerr);
						if (!canScrambleFrom.isEmpty())
							scrambleTerrs.put(amphibFrom, canScrambleFrom);
					}
				}
			}
		}
		
		// now scrambleTerrs is a list of places we can scramble from
		if (scrambleTerrs.isEmpty())
			return;
		final HashMap<Tuple<Territory, PlayerID>, Collection<HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>> scramblersByTerritoryPlayer = new HashMap<Tuple<Territory, PlayerID>, Collection<HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>>();
		for (final Territory to : scrambleTerrs.keySet())
		{
			final HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers = new HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>>();
			// find who we should ask
			PlayerID defender = null;
			if (m_battleTracker.hasPendingBattle(to, false))
				defender = AbstractBattle.findDefender(to, m_player, data);
			for (final Territory from : scrambleTerrs.get(to))
			{
				if (defender == null)
				{
					defender = AbstractBattle.findDefender(from, m_player, data);
				}
				// find how many is the max this territory can scramble
				final Collection<Unit> airbases = from.getUnits().getMatches(airbasesCanScramble);
				final int maxCanScramble = getMaxScrambleCount(airbases);
				final Route toBattleRoute = data.getMap().getRoute_IgnoreEnd(from, to, Matches.TerritoryIsNotImpassable);
				final Collection<Unit> canScrambleAir = from.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, m_player), Matches.UnitCanScramble,
							Matches.UnitIsNotDisabled, Matches.UnitWasScrambled.invert(), Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));
				if (maxCanScramble > 0 && !canScrambleAir.isEmpty())
					scramblers.put(from, new Tuple<Collection<Unit>, Collection<Unit>>(airbases, canScrambleAir));
			}
			if (defender == null || scramblers.isEmpty())
				continue;
			final Tuple<Territory, PlayerID> terrPlayer = new Tuple<Territory, PlayerID>(to, defender);
			Collection<HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>>> tempScrambleList = scramblersByTerritoryPlayer.get(terrPlayer);
			if (tempScrambleList == null)
				tempScrambleList = new ArrayList<HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>>>();
			tempScrambleList.add(scramblers);
			scramblersByTerritoryPlayer.put(terrPlayer, tempScrambleList);
		}
		
		// now scramble them
		for (final Tuple<Territory, PlayerID> terrPlayer : scramblersByTerritoryPlayer.keySet())
		{
			final Territory to = terrPlayer.getFirst();
			final PlayerID defender = terrPlayer.getSecond();
			if (defender == null || defender.isNull())
				continue;
			boolean scrambledHere = false;
			for (final HashMap<Territory, Tuple<Collection<Unit>, Collection<Unit>>> scramblers : scramblersByTerritoryPlayer.get(terrPlayer))
			{
				// verify that we didn't already scramble any of these units
				final Iterator<Territory> tIter = scramblers.keySet().iterator();
				while (tIter.hasNext())
				{
					final Territory t = tIter.next();
					scramblers.get(t).getSecond().retainAll(t.getUnits().getUnits());
					if (scramblers.get(t).getSecond().isEmpty())
						tIter.remove();
				}
				if (scramblers.isEmpty())
					continue;
				
				final HashMap<Territory, Collection<Unit>> toScramble = getRemotePlayer(defender).scrambleUnitsQuery(to, scramblers);
				if (toScramble == null)
					continue;
				
				// verify max allowed
				if (!scramblers.keySet().containsAll(toScramble.keySet()))
					throw new IllegalStateException("Trying to scramble from illegal territory");
				for (final Territory t : scramblers.keySet())
				{
					if (toScramble.get(t) == null)
						continue;
					if (toScramble.get(t).size() > getMaxScrambleCount(scramblers.get(t).getFirst()))
						throw new IllegalStateException("Trying to scramble " + toScramble.get(t).size() + " out of " + t.getName() + ", but max allowed is " + scramblers.get(t).getFirst());
				}
				
				final CompositeChange change = new CompositeChange();
				for (final Territory t : toScramble.keySet())
				{
					final Collection<Unit> scrambling = toScramble.get(t);
					if (scrambling == null || scrambling.isEmpty())
						continue;
					int numberScrambled = scrambling.size();
					final Collection<Unit> airbases = t.getUnits().getMatches(airbasesCanScramble);
					final int maxCanScramble = getMaxScrambleCount(airbases);
					if (maxCanScramble != Integer.MAX_VALUE)
					{
						// TODO: maybe sort from biggest to smallest first?
						for (final Unit airbase : airbases)
						{
							final int allowedScramble = ((TripleAUnit) airbase).getMaxScrambleCount();
							int newAllowed = allowedScramble;
							if (allowedScramble > 0)
							{
								if (allowedScramble >= numberScrambled)
								{
									newAllowed = allowedScramble - numberScrambled;
									numberScrambled = 0;
								}
								else
								{
									newAllowed = 0;
									numberScrambled -= allowedScramble;
								}
								change.add(ChangeFactory.unitPropertyChange(airbase, newAllowed, TripleAUnit.MAX_SCRAMBLE_COUNT));
							}
							if (numberScrambled <= 0)
								break;
						}
					}
					for (final Unit u : scrambling)
					{
						change.add(ChangeFactory.unitPropertyChange(u, t, TripleAUnit.ORIGINATED_FROM));
						change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_SCRAMBLED));
					}
					change.add(ChangeFactory.moveUnits(t, to, scrambling)); // should we mark combat, or call setupUnitsInSameTerritoryBattles again?
					m_bridge.getHistoryWriter()
								.startEvent(defender.getName() + " scrambles " + scrambling.size() + " units out of " + t.getName() + " to defend against the attack in " + to.getName(), scrambling);
					scrambledHere = true;
				}
				if (!change.isEmpty())
					m_bridge.addChange(change);
			}
			if (!scrambledHere)
				continue;
			
			// make sure the units join the battle, or create a new battle.
			final IBattle bombing = m_battleTracker.getPendingBattle(to, true, null);
			IBattle battle = m_battleTracker.getPendingBattle(to, false, BattleType.NORMAL);
			if (battle == null)
			{
				final List<Unit> attackingUnits = to.getUnits().getMatches(Matches.unitIsOwnedBy(m_player));
				if (bombing != null)
					attackingUnits.removeAll(bombing.getAttackingUnits());
				// no need to create a "bombing" battle or air battle, because those are set up automatically whenever the map allows scrambling into an air battle / air raid
				if (attackingUnits.isEmpty())
					continue;
				m_bridge.getHistoryWriter().startEvent(defender.getName() + " scrambles to create a battle in territory " + to.getName());
				// TODO: the attacking sea units do not remember where they came from, so they can not retreat anywhere. Need to fix.
				m_battleTracker.addBattle(new RouteScripted(to), attackingUnits, false, m_player, m_bridge, null, null);
				battle = m_battleTracker.getPendingBattle(to, false, BattleType.NORMAL);
				if (battle instanceof MustFightBattle)
				{
					// this is an ugly mess of hacks, but will have to stay here till all transport related code is gutted and refactored.
					final MustFightBattle mfb = (MustFightBattle) battle;
					final Collection<Territory> neighborsLand = data.getMap().getNeighbors(to, Matches.TerritoryIsLand);
					if (Match.someMatch(attackingUnits, Matches.UnitIsTransport))
					{
						// first, we have to reset the "transportedBy" setting for all the land units that were offloaded
						final CompositeChange change1 = new CompositeChange();
						mfb.reLoadTransports(attackingUnits, change1);
						if (!change1.isEmpty())
							m_bridge.addChange(change1);
						// after that is applied, we have to make a map of all dependencies
						final Map<Territory, Map<Unit, Collection<Unit>>> dependencies = new HashMap<Territory, Map<Unit, Collection<Unit>>>();
						final Map<Unit, Collection<Unit>> dependenciesForMFB = TransportTracker.transporting(attackingUnits, attackingUnits);
						for (final Unit transport : Match.getMatches(attackingUnits, Matches.UnitIsTransport))
						{
							// however, the map we add to the newly created battle, can not hold any units that are NOT in this territory.
							// BUT it must still hold all transports
							if (!dependenciesForMFB.containsKey(transport))
								dependenciesForMFB.put(transport, new ArrayList<Unit>());
						}
						dependencies.put(to, dependenciesForMFB);
						for (final Territory t : neighborsLand)
						{
							// All other maps, must hold only the transported units that in their territory
							final Collection<Unit> allNeighborUnits = new ArrayList<Unit>(Match.getMatches(attackingUnits, Matches.UnitIsTransport));
							allNeighborUnits.addAll(t.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(m_player)));
							final Map<Unit, Collection<Unit>> dependenciesForNeighbors = TransportTracker.transporting(
										Match.getMatches(allNeighborUnits, Matches.UnitIsTransport), Match.getMatches(allNeighborUnits, Matches.UnitIsTransport.invert()));
							dependencies.put(t, dependenciesForNeighbors);
						}
						mfb.addDependentUnits(dependencies.get(to));
						for (final Territory territoryNeighborToNewBattle : neighborsLand)
						{
							final IBattle battleInTerritoryNeighborToNewBattle = m_battleTracker.getPendingBattle(territoryNeighborToNewBattle, false, BattleType.NORMAL);
							if (battleInTerritoryNeighborToNewBattle != null && battleInTerritoryNeighborToNewBattle instanceof MustFightBattle)
							{
								final MustFightBattle mfbattleInTerritoryNeighborToNewBattle = (MustFightBattle) battleInTerritoryNeighborToNewBattle;
								mfbattleInTerritoryNeighborToNewBattle.addDependentUnits(dependencies.get(territoryNeighborToNewBattle));
							}
							else if (battleInTerritoryNeighborToNewBattle != null && battleInTerritoryNeighborToNewBattle instanceof NonFightingBattle)
							{
								final NonFightingBattle nfbattleInTerritoryNeighborToNewBattle = (NonFightingBattle) battleInTerritoryNeighborToNewBattle;
								nfbattleInTerritoryNeighborToNewBattle.addDependentUnits(dependencies.get(territoryNeighborToNewBattle));
							}
						}
					}
					if (Match.someMatch(attackingUnits, Matches.UnitIsAir.invert()))
					{
						// TODO: for now, we will hack and say that the attackers came from Everywhere, and hope the user will choose the correct place to retreat to! (TODO: Fix this)
						final Map<Territory, Collection<Unit>> attackingFromMap = new HashMap<Territory, Collection<Unit>>();
						final Collection<Territory> neighbors = data.getMap().getNeighbors(to, (Matches.TerritoryIsLand.match(to) ? Matches.TerritoryIsLand : Matches.TerritoryIsWater));
						// neighbors.removeAll(territoriesWithBattles);
						// neighbors.removeAll(Match.getMatches(neighbors, Matches.territoryHasEnemyUnits(m_player, data)));
						for (final Territory t : neighbors)
						{
							attackingFromMap.put(t, attackingUnits);
						}
						mfb.setAttackingFromAndMap(attackingFromMap);
					}
				}
			}
			else if (battle instanceof MustFightBattle)
			{
				((MustFightBattle) battle).resetDefendingUnits(to, m_player, data);
			}
			// now make sure any amphibious battles that are dependent on this 'new' sea battle have their dependencies set.
			if (to.isWater())
			{
				for (final Territory t : data.getMap().getNeighbors(to, Matches.TerritoryIsLand))
				{
					final IBattle battleAmphib = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
					if (battleAmphib != null)
					{
						if (!m_battleTracker.getDependentOn(battle).contains(battleAmphib))
							m_battleTracker.addDependency(battleAmphib, battle);
						if (battleAmphib instanceof MustFightBattle)
						{
							// and we want to reset the defenders if the scrambling air has left that battle
							((MustFightBattle) battleAmphib).resetDefendingUnits(t, m_player, data);
						}
					}
				}
			}
		}
	}
	
	public static int getMaxScrambleCount(final Collection<Unit> airbases)
	{
		if (!Match.allMatch(airbases, new CompositeMatchAnd<Unit>(Matches.UnitIsAirBase, Matches.UnitIsNotDisabled)))
			throw new IllegalStateException("All units must be viable airbases");
		// find how many is the max this territory can scramble
		int maxScrambled = 0;
		for (final Unit base : airbases)
		{
			final int baseMax = ((TripleAUnit) base).getMaxScrambleCount();
			if (baseMax == -1)
				return Integer.MAX_VALUE;
			maxScrambled += baseMax;
		}
		return maxScrambled;
	}
	
	private void scramblingCleanup()
	{
		// return scrambled units to their original territories, or let them move 1 or x to a new territory.
		final GameData data = getData();
		if (!games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data))
			return;
		final boolean mustReturnToBase = games.strategy.triplea.Properties.getScrambled_Units_Return_To_Base(data);
		for (final Territory t : data.getMap().getTerritories())
		{
			int carrierCostOfCurrentTerr = 0;
			final Collection<Unit> wasScrambled = t.getUnits().getMatches(Matches.UnitWasScrambled);
			for (final Unit u : wasScrambled)
			{
				final CompositeChange change = new CompositeChange();
				final Territory originatedFrom = TripleAUnit.get(u).getOriginatedFrom();
				Territory landingTerr = null;
				String historyText = "";
				if (!mustReturnToBase || !Matches.isTerritoryAllied(u.getOwner(), data).match(originatedFrom))
				{
					final Collection<Territory> possible = whereCanAirLand(Collections.singletonList(u), t, u.getOwner(), data,
								m_battleTracker, carrierCostOfCurrentTerr, 1, true, !mustReturnToBase, true);
					if (possible.size() > 1)
						landingTerr = getRemotePlayer(u.getOwner()).selectTerritoryForAirToLand(possible, t,
									"Select territory for air units to land. (Current territory is " + t.getName() + "): " + MyFormatter.unitsToText(Collections.singletonList(u)));
					else if (possible.size() == 1)
						landingTerr = possible.iterator().next();
					if (landingTerr == null || landingTerr.equals(t))
					{
						carrierCostOfCurrentTerr += AirMovementValidator.carrierCost(Collections.singletonList(u));
						historyText = "Scrambled unit stays in territory " + t.getName();
					}
					else
						historyText = "Moving scrambled unit from " + t.getName() + " to " + landingTerr.getName();
				}
				else
				{
					landingTerr = originatedFrom;
					historyText = "Moving scrambled unit from " + t.getName() + " back to originating territory: " + landingTerr.getName();
				}
				// if null, we leave it to die
				if (landingTerr != null)
					change.add(ChangeFactory.moveUnits(t, landingTerr, Collections.singletonList(u)));
				change.add(ChangeFactory.unitPropertyChange(u, null, TripleAUnit.ORIGINATED_FROM));
				change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_SCRAMBLED));
				if (!change.isEmpty())
				{
					m_bridge.getHistoryWriter().startEvent(historyText, u);
					m_bridge.addChange(change);
				}
			}
		}
	}
	
	private static void resetMaxScrambleCount(final IDelegateBridge aBridge)
	{
		// reset the tripleaUnit property for all airbases that were used
		final GameData data = aBridge.getData();
		if (!games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data))
			return;
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> airbases = t.getUnits().getMatches(Matches.UnitIsAirBase);
			for (final Unit u : airbases)
			{
				final UnitAttachment ua = UnitAttachment.get(u.getType());
				final int currentMax = ((TripleAUnit) u).getMaxScrambleCount();
				final int allowedMax = ua.getMaxScrambleCount();
				if (currentMax != allowedMax)
					change.add(ChangeFactory.unitPropertyChange(u, allowedMax, TripleAUnit.MAX_SCRAMBLE_COUNT));
			}
		}
		if (!change.isEmpty())
		{
			aBridge.getHistoryWriter().startEvent("Preparing Airbases for Possible Scrambling");
			aBridge.addChange(change);
		}
	}
	
	private void airBattleCleanup()
	{
		final GameData data = getData();
		if (!games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data))
			return;
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			for (final Unit u : t.getUnits().getMatches(Matches.UnitWasInAirBattle))
			{
				change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_IN_AIR_BATTLE));
			}
		}
		if (!change.isEmpty())
		{
			m_bridge.getHistoryWriter().startEvent("Cleaning up after air battles");
			m_bridge.addChange(change);
		}
	}
	
	private void checkDefendingPlanesCanLand()
	{
		final GameData data = getData();
		final Map<Territory, Collection<Unit>> defendingAirThatCanNotLand = m_battleTracker.getDefendingAirThatCanNotLand();
		final boolean isWW2v2orIsSurvivingAirMoveToLand = games.strategy.triplea.Properties.getWW2V2(data) || games.strategy.triplea.Properties.getSurvivingAirMoveToLand(data);
		final CompositeMatch<Unit> alliedDefendingAir = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitWasScrambled.invert());
		for (final Entry<Territory, Collection<Unit>> entry : defendingAirThatCanNotLand.entrySet())
		{
			final Territory battleSite = entry.getKey();
			final Collection<Unit> defendingAir = entry.getValue();
			if (defendingAir == null || defendingAir.isEmpty())
				continue;
			final PlayerID defender = AbstractBattle.findDefender(battleSite, m_player, data);
			// Get all land territories where we can land
			final Set<Territory> neighbors = data.getMap().getNeighbors(battleSite);
			final CompositeMatch<Territory> alliedLandTerritories = new CompositeMatchAnd<Territory>(Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(defender, data));
			// Get those that are neighbors
			final Collection<Territory> canLandHere = Match.getMatches(neighbors, alliedLandTerritories);
			// Get all sea territories where there are allies
			final CompositeMatch<Territory> neighboringSeaZonesWithAlliedUnits = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasAlliedUnits(defender, data));
			// Get those that are neighbors
			final Collection<Territory> areSeaNeighbors = Match.getMatches(neighbors, neighboringSeaZonesWithAlliedUnits);
			// Set up match criteria for allied carriers
			final CompositeMatch<Unit> alliedCarrier = new CompositeMatchAnd<Unit>();
			alliedCarrier.add(Matches.UnitIsCarrier);
			alliedCarrier.add(Matches.alliedUnit(defender, data));
			// Set up match criteria for allied planes
			final CompositeMatch<Unit> alliedPlane = new CompositeMatchAnd<Unit>();
			alliedPlane.add(Matches.UnitIsAir);
			alliedPlane.add(Matches.alliedUnit(defender, data));
			// See if neighboring carriers have any capacity available
			for (final Territory currentTerritory : areSeaNeighbors)
			{
				// get the capacity of the carriers and cost of fighters
				final Collection<Unit> alliedCarriers = currentTerritory.getUnits().getMatches(alliedCarrier);
				final Collection<Unit> alliedPlanes = currentTerritory.getUnits().getMatches(alliedPlane);
				final int alliedCarrierCapacity = AirMovementValidator.carrierCapacity(alliedCarriers, currentTerritory);
				final int alliedPlaneCost = AirMovementValidator.carrierCost(alliedPlanes);
				// if there is free capacity, add the territory to landing possibilities
				if (alliedCarrierCapacity - alliedPlaneCost >= 1)
				{
					canLandHere.add(currentTerritory);
				}
			}
			if (isWW2v2orIsSurvivingAirMoveToLand)
			{
				Territory territory = null;
				while (canLandHere.size() > 1 && defendingAir.size() > 0)
				{
					territory = getRemotePlayer(defender).selectTerritoryForAirToLand(canLandHere, battleSite,
								"Select territory for air units to land. (Current territory is " + battleSite.getName() + "): " + MyFormatter.unitsToText(defendingAir));
					// added for test script
					if (territory == null)
					{
						territory = canLandHere.iterator().next();
					}
					if (territory.isWater())
					{
						landPlanesOnCarriers(m_bridge, alliedDefendingAir, defendingAir, canLandHere, alliedCarrier, alliedPlane, territory, battleSite);
					}
					else
					{
						moveAirAndLand(m_bridge, defendingAir, defendingAir, territory, battleSite);
						continue;
					}
					// remove the territory from those available
					canLandHere.remove(territory);
				}
				// Land in the last remaining territory
				if (canLandHere.size() > 0 && defendingAir.size() > 0)
				{
					territory = canLandHere.iterator().next();
					if (territory.isWater())
					{
						landPlanesOnCarriers(m_bridge, alliedDefendingAir, defendingAir, canLandHere, alliedCarrier, alliedPlane, territory, battleSite);
					}
					else
					{
						moveAirAndLand(m_bridge, defendingAir, defendingAir, territory, battleSite);
						continue;
					}
				}
			}
			else if (canLandHere.size() > 0)
			{
				// now defending air has what cant stay, is there a place we can go?
				// check for an island in this sea zone
				for (final Territory currentTerritory : canLandHere)
				{
					// only one neighbor, its an island.
					if (data.getMap().getNeighbors(currentTerritory).size() == 1)
					{
						moveAirAndLand(m_bridge, defendingAir, defendingAir, currentTerritory, battleSite);
						continue;
					}
				}
			}
			if (defendingAir.size() > 0)
			{
				// no where to go, they must die
				m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToText(defendingAir) + " could not land and were killed", defendingAir);
				final Change change = ChangeFactory.removeUnits(battleSite, defendingAir);
				m_bridge.addChange(change);
			}
		}
	}
	
	private static void landPlanesOnCarriers(final IDelegateBridge bridge, final CompositeMatch<Unit> alliedDefendingAir, final Collection<Unit> defendingAir, final Collection<Territory> canLandHere,
				final CompositeMatch<Unit> alliedCarrier, final CompositeMatch<Unit> alliedPlane, final Territory newTerritory, final Territory battleSite)
	{
		// Get the capacity of the carriers in the selected zone
		final Collection<Unit> alliedCarriersSelected = newTerritory.getUnits().getMatches(alliedCarrier);
		final Collection<Unit> alliedPlanesSelected = newTerritory.getUnits().getMatches(alliedPlane);
		final int alliedCarrierCapacitySelected = AirMovementValidator.carrierCapacity(alliedCarriersSelected, newTerritory);
		final int alliedPlaneCostSelected = AirMovementValidator.carrierCost(alliedPlanesSelected);
		// Find the available capacity of the carriers in that territory
		final int territoryCapacity = alliedCarrierCapacitySelected - alliedPlaneCostSelected;
		if (territoryCapacity > 0)
		{
			// move that number of planes from the battlezone
			// TODO: this seems to assume that the air units all have 1 carrier cost!! fixme
			final Collection<Unit> movingAir = Match.getNMatches(defendingAir, territoryCapacity, alliedDefendingAir);
			moveAirAndLand(bridge, movingAir, defendingAir, newTerritory, battleSite);
		}
	}
	
	private static void moveAirAndLand(final IDelegateBridge bridge, final Collection<Unit> defendingAirBeingMoved, final Collection<Unit> defendingAirTotal, final Territory newTerritory,
				final Territory battleSite)
	{
		bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToText(defendingAirBeingMoved) + " forced to land in " + newTerritory.getName(), defendingAirBeingMoved);
		final Change change = ChangeFactory.moveUnits(battleSite, newTerritory, defendingAirBeingMoved);
		bridge.addChange(change);
		// remove those that landed in case it was a carrier
		defendingAirTotal.removeAll(defendingAirBeingMoved);
	}
	
	/**
	 * KamikazeSuicideAttacks are attacks that are made during an Opponent's turn, using Resources that you own that have been designated.
	 * The resources are designated in PlayerAttachment, and hold information like the attack power of the resource.
	 * KamikazeSuicideAttacks are done in any territory that is a kamikazeZone, and the attacks are done by the original owner of that territory.
	 * The user has the option not to do any attacks, and they make target any number of units with any number of resource tokens.
	 * The units are then attacked individually by each resource token (meaning that casualties do not get selected because the attacks are targeted).
	 * The enemies of current player should decide all their attacks before the attacks are rolled.
	 */
	private void doKamikazeSuicideAttacks()
	{
		final GameData data = getData();
		if (!games.strategy.triplea.Properties.getUseKamikazeSuicideAttacks(data))
			return;
		// the current player is not the one who is doing these attacks, it is the all the enemies of this player who will do attacks
		final Collection<PlayerID> enemies = Match.getMatches(data.getPlayerList().getPlayers(), Matches.isAtWar(m_player, data));
		if (enemies.isEmpty())
			return;
		final Match<Unit> canBeAttackedDefault = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(m_player), Matches.UnitIsSea,
					Matches.UnitIsNotTransportButCouldBeCombatTransport, Matches.UnitIsNotSub);
		
		final boolean onlyWhereThereAreBattlesOrAmphibious = games.strategy.triplea.Properties.getKamikazeSuicideAttacksOnlyWhereBattlesAre(data);
		final Collection<Territory> pendingBattles = m_battleTracker.getPendingBattleSites(false);
		// create a list of all kamikaze zones, listed by enemy
		final HashMap<PlayerID, Collection<Territory>> kamikazeZonesByEnemy = new HashMap<PlayerID, Collection<Territory>>();
		for (final Territory t : data.getMap().getTerritories())
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				continue;
			if (!ta.getKamikazeZone())
				continue;
			final PlayerID owner;
			if (!games.strategy.triplea.Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data))
			{
				owner = ta.getOriginalOwner();
				if (owner == null)
					continue;
			}
			else
			{
				owner = t.getOwner();
				if (owner == null)
					continue;
			}
			if (enemies.contains(owner))
			{
				if (Match.noneMatch(t.getUnits().getUnits(), Matches.unitIsOwnedBy(m_player)))
					continue;
				if (onlyWhereThereAreBattlesOrAmphibious)
				{
					// if no battle or amphibious from here, ignore it
					if (!pendingBattles.contains(t))
					{
						if (!Matches.TerritoryIsWater.match(t))
							continue;
						boolean amphib = false;
						final Collection<Territory> landNeighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
						for (final Territory neighbor : landNeighbors)
						{
							final IBattle battle = m_battleTracker.getPendingBattle(neighbor, false, BattleType.NORMAL);
							if (battle == null)
							{
								final Map<Territory, Collection<Unit>> whereFrom = m_battleTracker.getFinishedBattlesUnitAttackFromMap().get(neighbor);
								if (whereFrom != null && whereFrom.containsKey(t))
								{
									amphib = true;
									break;
								}
								continue;
							}
							if (battle.isAmphibious() &&
										((battle instanceof MustFightBattle && ((MustFightBattle) battle).getAmphibiousAttackTerritories().contains(t))
										|| (battle instanceof NonFightingBattle && ((NonFightingBattle) battle).getAmphibiousAttackTerritories().contains(t))))
							{
								amphib = true;
								break;
							}
						}
						if (amphib == false)
							continue;
					}
				}
				Collection<Territory> currentTerrs = kamikazeZonesByEnemy.get(owner);
				if (currentTerrs == null)
					currentTerrs = new ArrayList<Territory>();
				currentTerrs.add(t);
				kamikazeZonesByEnemy.put(owner, currentTerrs);
			}
		}
		if (kamikazeZonesByEnemy.isEmpty())
			return;
		for (final Entry<PlayerID, Collection<Territory>> entry : kamikazeZonesByEnemy.entrySet())
		{
			final PlayerID currentEnemy = entry.getKey();
			final PlayerAttachment pa = PlayerAttachment.get(currentEnemy);
			if (pa == null)
				continue;
			Match<Unit> canBeAttacked = canBeAttackedDefault;
			final Set<UnitType> suicideAttackTargets = pa.getSuicideAttackTargets();
			if (suicideAttackTargets != null)
			{
				canBeAttacked = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(m_player), Matches.unitIsOfTypes(suicideAttackTargets));
			}
			// See if the player has any attack tokens
			final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
			if (resourcesAndAttackValues.size() <= 0)
				continue;
			final IntegerMap<Resource> playerResourceCollection = currentEnemy.getResources().getResourcesCopy();
			final IntegerMap<Resource> attackTokens = new IntegerMap<Resource>();
			for (final Resource possible : resourcesAndAttackValues.keySet())
			{
				final int amount = playerResourceCollection.getInt(possible);
				if (amount > 0)
					attackTokens.put(possible, amount);
			}
			if (attackTokens.size() <= 0)
				continue;
			// now let the enemy decide if they will do attacks
			final Collection<Territory> kamikazeZones = entry.getValue();
			final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack = new HashMap<Territory, Collection<Unit>>();
			for (final Territory t : kamikazeZones)
			{
				final List<Unit> validTargets = t.getUnits().getMatches(canBeAttacked);
				if (!validTargets.isEmpty())
					possibleUnitsToAttack.put(t, validTargets);
			}
			final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> attacks = getRemotePlayer(currentEnemy).selectKamikazeSuicideAttacks(possibleUnitsToAttack);
			if (attacks == null || attacks.isEmpty())
				continue;
			// now validate that we have the resources and those units are valid targets
			for (final Entry<Territory, HashMap<Unit, IntegerMap<Resource>>> territoryEntry : attacks.entrySet())
			{
				final Territory t = territoryEntry.getKey();
				final Collection<Unit> possibleUnits = possibleUnitsToAttack.get(t);
				if (possibleUnits == null || !possibleUnits.containsAll(territoryEntry.getValue().keySet()))
					throw new IllegalStateException("Player has chosen illegal units during Kamikaze Suicide Attacks");
				for (final IntegerMap<Resource> rMap : territoryEntry.getValue().values())
					attackTokens.subtract(rMap);
			}
			if (!attackTokens.isPositive())
				throw new IllegalStateException("Player has chosen illegal resource during Kamikaze Suicide Attacks");
			for (final Entry<Territory, HashMap<Unit, IntegerMap<Resource>>> territoryEntry : attacks.entrySet())
			{
				final Territory location = territoryEntry.getKey();
				for (final Entry<Unit, IntegerMap<Resource>> unitEntry : territoryEntry.getValue().entrySet())
				{
					final Unit unitUnderFire = unitEntry.getKey();
					final IntegerMap<Resource> numberOfAttacks = unitEntry.getValue();
					if (numberOfAttacks != null && numberOfAttacks.size() > 0 && numberOfAttacks.totalValues() > 0)
						fireKamikazeSuicideAttacks(unitUnderFire, numberOfAttacks, resourcesAndAttackValues, currentEnemy, location);
				}
			}
		}
	}
	
	/**
	 * This rolls the dice and validates them to see if units died or not.
	 * It will use LowLuck or normal dice.
	 * If any units die, we remove them from the game, and if units take damage but live, we also do that here.
	 * 
	 * @param unitUnderFire
	 * @param numberOfAttacks
	 * @param resourcesAndAttackValues
	 * @param firingEnemy
	 * @param location
	 */
	private void fireKamikazeSuicideAttacks(final Unit unitUnderFire, final IntegerMap<Resource> numberOfAttacks,
				final IntegerMap<Resource> resourcesAndAttackValues, final PlayerID firingEnemy, final Territory location)
	{
		// TODO: find a way to autosave after each dice roll.
		final GameData data = getData();
		final int diceSides = data.getDiceSides();
		final CompositeChange change = new CompositeChange();
		int hits = 0;
		int[] rolls = null;
		if (games.strategy.triplea.Properties.getLow_Luck(data))
		{
			int power = 0;
			for (final Entry<Resource, Integer> entry : numberOfAttacks.entrySet())
			{
				final Resource r = entry.getKey();
				final int num = entry.getValue();
				change.add(ChangeFactory.changeResourcesChange(firingEnemy, r, -num));
				power += num * resourcesAndAttackValues.getInt(r);
			}
			if (power > 0)
			{
				hits = power / diceSides;
				final int remainder = power % diceSides;
				if (remainder > 0)
				{
					rolls = m_bridge.getRandom(diceSides, 1, firingEnemy, DiceType.COMBAT, "Rolling for remainder in Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
					if (remainder > rolls[0])
						hits++;
				}
			}
		}
		else
		{
			// avoid multiple calls of getRandom, so just do it once at the beginning
			final int numTokens = numberOfAttacks.totalValues();
			rolls = m_bridge.getRandom(diceSides, numTokens, firingEnemy, DiceType.COMBAT, "Rolling for Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
			final int[] powerOfTokens = new int[numTokens];
			int j = 0;
			for (final Entry<Resource, Integer> entry : numberOfAttacks.entrySet())
			{
				final Resource r = entry.getKey();
				int num = entry.getValue();
				change.add(ChangeFactory.changeResourcesChange(firingEnemy, r, -num));
				final int power = resourcesAndAttackValues.getInt(r);
				while (num > 0)
				{
					powerOfTokens[j] = power;
					j++;
					num--;
				}
			}
			for (int i = 0; i < rolls.length; i++)
			{
				if (powerOfTokens[i] > rolls[i])
					hits++;
			}
		}
		final String title = "Kamikaze Suicide Attack attacks " + MyFormatter.unitsToText(Collections.singleton(unitUnderFire));
		final String dice = " scoring " + hits + " hits.  Rolls: " + MyFormatter.asDice(rolls);
		m_bridge.getHistoryWriter().startEvent(title + dice, unitUnderFire);
		if (hits > 0)
		{
			final UnitAttachment ua = UnitAttachment.get(unitUnderFire.getType());
			final int currentHits = unitUnderFire.getHits();
			if (ua.getHitPoints() <= currentHits + hits)
			{
				// TODO: kill dependents
				change.add(ChangeFactory.removeUnits(location, Collections.singleton(unitUnderFire)));
			}
			else
			{
				final IntegerMap<Unit> hitMap = new IntegerMap<Unit>();
				hitMap.put(unitUnderFire, hits + unitUnderFire.getHits());
				change.add(ChangeFactory.unitsHit(hitMap));
				m_bridge.getHistoryWriter().addChildToEvent("Units damaged: " + MyFormatter.unitsToText(Collections.singleton(unitUnderFire)), unitUnderFire);
			}
		}
		if (!change.isEmpty())
		{
			m_bridge.addChange(change);
		}
		// kamikaze suicide attacks, even if unsuccessful, deny the ability to bombard from this sea zone
		m_battleTracker.addNoBombardAllowedFromHere(location);
		// TODO: display this as actual dice for both players
		final Collection<PlayerID> playersInvolved = new ArrayList<PlayerID>();
		playersInvolved.add(m_player);
		playersInvolved.add(firingEnemy);
		this.getDisplay().reportMessageToPlayers(playersInvolved, null, title + dice, title);
	}
	
	public static void markDamaged(final Collection<Unit> damaged, final IDelegateBridge bridge, final boolean addPreviousHits)
	{
		if (damaged.size() == 0)
			return;
		final IntegerMap<Unit> damagedMap = new IntegerMap<Unit>();
		for (final Unit u : damaged)
		{
			damagedMap.add(u, 1);
		}
		markDamaged(damagedMap, bridge, addPreviousHits);
	}
	
	public static void markDamaged(final IntegerMap<Unit> damagedMap, final IDelegateBridge bridge, final boolean addPreviousHits)
	{
		final Set<Unit> units = new HashSet<Unit>(damagedMap.keySet());
		if (addPreviousHits)
		{
			for (final Unit u : units)
			{
				damagedMap.add(u, u.getHits());
			}
		}
		final Change damagedChange = ChangeFactory.unitsHit(damagedMap);
		bridge.getHistoryWriter().addChildToEvent("Units damaged: " + MyFormatter.unitsToText(units), units);
		bridge.addChange(damagedChange);
	}
	
	public static Collection<Territory> whereCanAirLand(final Collection<Unit> strandedAir, final Territory currentTerr, final PlayerID alliedPlayer, final GameData data,
				final BattleTracker battleTracker, final int carrierCostForCurrentTerr, final int allowedMovement, final boolean byMovementCost, final boolean useMaxScrambleDistance,
				final boolean landInConquered)
	{
		final HashSet<Territory> whereCanLand = new HashSet<Territory>();
		int maxDistance = allowedMovement;
		if ((byMovementCost && maxDistance > 1) || useMaxScrambleDistance)
		{
			UnitType ut = null;
			for (final Unit u : strandedAir)
			{
				if (ut == null)
					ut = u.getType();
				else if (!ut.equals(u.getType()))
					throw new IllegalStateException("whereCanAirLand can only accept 1 UnitType if byMovementCost or scrambled is true");
			}
			if (useMaxScrambleDistance)
				maxDistance = UnitAttachment.get(ut).getMaxScrambleDistance();
		}
		if (maxDistance < 1 || strandedAir == null || strandedAir.isEmpty())
			return Collections.singletonList(currentTerr);
		/*for (final Unit u : strandedAir)
		{
			if (!data.getRelationshipTracker().isAllied(u.getOwner(), alliedPlayer))
				throw new IllegalStateException("whereCanAirLand all air units must be allied with alliedPlayer");
		}*/
		final boolean areNeutralsPassableByAir = (games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data) && !games.strategy.triplea.Properties.getNeutralsImpassable(data));
		final HashSet<Territory> canNotLand = new HashSet<Territory>();
		canNotLand.addAll(battleTracker.getPendingBattleSites(false));
		canNotLand.addAll(Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasEnemyUnits(alliedPlayer, data)));
		if (!landInConquered)
			canNotLand.addAll(battleTracker.getConquered());
		
		final Collection<Territory> possibleTerrs = new ArrayList<Territory>(data.getMap().getNeighbors(currentTerr, maxDistance));
		if (byMovementCost && maxDistance > 1)
		{
			final Iterator<Territory> possibleIter = possibleTerrs.iterator();
			while (possibleIter.hasNext())
			{
				final Route route = data.getMap().getRoute(currentTerr, possibleIter.next(), Matches.airCanFlyOver(alliedPlayer, data, areNeutralsPassableByAir));
				if (route == null || route.getMovementCost(strandedAir.iterator().next()) > maxDistance)
					possibleIter.remove();
			}
		}
		possibleTerrs.add(currentTerr);
		
		final HashSet<Territory> availableLand = new HashSet<Territory>();
		availableLand.addAll(Match.getMatches(possibleTerrs, new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(alliedPlayer, data), Matches.TerritoryIsLand)));
		availableLand.removeAll(canNotLand);
		whereCanLand.addAll(availableLand);
		
		// now for carrier-air-landing validation
		if (Match.allMatch(strandedAir, Matches.UnitCanLandOnCarrier))
		{
			final HashSet<Territory> availableWater = new HashSet<Territory>();
			availableWater.addAll(Match.getMatches(possibleTerrs, new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsThatMatch(Matches.UnitIsAlliedCarrier(alliedPlayer, data)),
						Matches.TerritoryIsWater)));
			availableWater.removeAll(battleTracker.getPendingBattleSites(false));
			// a rather simple calculation, either we can take all the air, or we can't, nothing in the middle
			final int carrierCost = AirMovementValidator.carrierCost(strandedAir);
			final Iterator<Territory> waterIter = availableWater.iterator();
			while (waterIter.hasNext())
			{
				final Territory t = waterIter.next();
				int carrierCapacity = AirMovementValidator.carrierCapacity(t.getUnits().getMatches(Matches.UnitIsAlliedCarrier(alliedPlayer, data)), t);
				if (!t.equals(currentTerr))
					carrierCapacity -= AirMovementValidator.carrierCost(t.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.alliedUnit(alliedPlayer, data))));
				else
					carrierCapacity -= carrierCostForCurrentTerr;
				if (carrierCapacity < carrierCost)
					waterIter.remove();
			}
			whereCanLand.addAll(availableWater);
		}
		
		return whereCanLand;
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreTransportInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreTransportInMovement(data);
	}
	
	/**
	 * @return
	 */
	private static boolean isIgnoreSubInMovement(final GameData data)
	{
		return games.strategy.triplea.Properties.getIgnoreSubInMovement(data);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IBattleDelegate.class;
	}
	
	public Territory getCurrentBattleTerritory()
	{
		final IBattle b = m_currentBattle;
		if (b != null)
		{
			return b.getTerritory();
		}
		else
		{
			return null;
		}
	}
	
	public IBattle getCurrentBattle()
	{
		return m_currentBattle;
	}
}


class BattleExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 7899007486408723505L;
	Serializable superState;
	// add other variables here:
	public BattleTracker m_battleTracker = new BattleTracker();
	// public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
	public boolean m_needToInitialize;
	public boolean m_needToScramble;
	public boolean m_needToKamikazeSuicideAttacks;
	public boolean m_needToClearEmptyAirBattleAttacks;
	public boolean m_needToAddBombardmentSources;
	public boolean m_needToRecordBattleStatistics;
	public boolean m_needToCheckDefendingPlanesCanLand;
	public boolean m_needToCleanup;
	public IBattle m_currentBattle;
}
