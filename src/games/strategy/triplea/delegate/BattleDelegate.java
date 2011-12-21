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

import games.strategy.common.delegate.BaseDelegate;
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
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.formatter.MyFormatter;
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

/**
 * @author Sean Bridges
 * @version 1.0
 */
@AutoSave(beforeStepStart = true, afterStepEnd = true)
public class BattleDelegate extends BaseDelegate implements IBattleDelegate
{
	private BattleTracker m_battleTracker = new BattleTracker();
	private OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
	private boolean m_needToInitialize = true;
	private IBattle m_currentBattle = null;
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(new TripleADelegateBridge(aBridge));
		// we may start multiple times due to loading after saving
		// only initialize once
		if (m_needToInitialize)
		{
			setupUnitsInSameTerritoryBattles();
			// do pre-combat stuff, like scrambling, after we have setup all battles, but before we have bombardment, etc.
			doScrambling();
			doKamikazeSuicideAttacks();
			addBombardmentSources();
			m_needToInitialize = false;
		}
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		getBattleTracker().sendBattleRecordsToGameData(m_bridge);
		getBattleTracker().clearBattleRecords();
		scramblingCleanup();
		airBattleCleanup();
		super.end();
		m_needToInitialize = true;
	}
	
	@Override
	public Serializable saveState()
	{
		final BattleExtendedDelegateState state = new BattleExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_battleTracker = m_battleTracker;
		state.m_originalOwnerTracker = m_originalOwnerTracker;
		state.m_needToInitialize = m_needToInitialize;
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
		m_originalOwnerTracker = s.m_originalOwnerTracker;
		m_needToInitialize = s.m_needToInitialize;
		m_currentBattle = s.m_currentBattle;
	}
	
	public String fightBattle(final Territory territory, final boolean bombing)
	{
		final IBattle battle = m_battleTracker.getPendingBattle(territory, bombing);
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
			return "Must complete " + getFightingWord(battle) + " in " + name + " first";
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
		return battle.isBombingRun() ? "Bombing Run" : "Battle";
	}
	
	public BattleListing getBattles()
	{
		final Collection<Territory> battles = m_battleTracker.getPendingBattleSites(false);
		final Collection<Territory> bombing = m_battleTracker.getPendingBattleSites(true);
		return new BattleListing(battles, bombing);
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
	
	public OriginalOwnerTracker getOriginalOwnerTracker()
	{
		return m_originalOwnerTracker;
	}
	
	/**
	 * Add bombardment units to battles.
	 */
	private void addBombardmentSources()
	{
		final PlayerID attacker = m_bridge.getPlayerID();
		final ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
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
			final IBattle battle = m_battleTracker.getPendingBattle(t, false);
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
		final ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
		Territory bombardingTerritory = null;
		if (!territories.isEmpty())
			bombardingTerritory = remotePlayer.selectBombardingTerritory(u, uTerritory, territories, true);
		if (bombardingTerritory != null)
		{
			return battleTerritories.get(bombardingTerritory);
		}
		return null; // User elected not to bombard with this unit
	}
	
	/**
	 * Setup the battles where the battle occurs because units are in the
	 * same territory. This happens when subs emerge (after being submerged), and
	 * when naval units are placed in enemy occupied sea zones, and also
	 * when political relationships change and potentially leave units in now-hostile territories.
	 */
	private void setupUnitsInSameTerritoryBattles()
	{
		final PlayerID player = m_bridge.getPlayerID();
		final GameData data = getData();
		final boolean ignoreTransports = isIgnoreTransportInMovement(data);
		final boolean ignoreSubs = isIgnoreSubInMovement(data);
		final CompositeMatchAnd<Unit> seaTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransportButNotCombatTransport, Matches.UnitIsSea);
		final CompositeMatchOr<Unit> seaTranportsOrSubs = new CompositeMatchOr<Unit>(seaTransports, Matches.UnitIsSub);
		// we want to match all sea zones with our units and enemy units
		final CompositeMatch<Territory> anyTerritoryWithOwnAndEnemy = new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(player), Matches.territoryHasEnemyUnits(player, data));
		final CompositeMatch<Territory> enemyTerritoryAndOwnUnits = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWater(player, getData()),
					Matches.territoryHasUnitsOwnedBy(player));
		final CompositeMatch<Territory> enemyUnitsOrEnemyTerritory = new CompositeMatchOr<Territory>(anyTerritoryWithOwnAndEnemy, enemyTerritoryAndOwnUnits);
		final Iterator<Territory> battleTerritories = Match.getMatches(data.getMap().getTerritories(), enemyUnitsOrEnemyTerritory).iterator();
		while (battleTerritories.hasNext())
		{
			final Territory territory = battleTerritories.next();
			final List<Unit> attackingUnits = territory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			final List<Unit> enemyUnits = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
			final IBattle bombingBattle = m_battleTracker.getPendingBattle(territory, true);
			if (bombingBattle != null)
			{
				// we need to remove any units which are participating in bombing raids
				attackingUnits.removeAll(bombingBattle.getAttackingUnits());
			}
			if (attackingUnits.isEmpty() || Match.allMatch(attackingUnits, Matches.UnitIsAAOrIsFactoryOrIsInfrastructure))
				continue;
			IBattle battle = m_battleTracker.getPendingBattle(territory, false);
			if (battle == null)
			{
				m_bridge.getHistoryWriter().startEvent(player.getName() + " creates battle in territory " + territory.getName());
				m_battleTracker.addBattle(new RouteScripted(territory), attackingUnits, false, player, m_bridge, null);
				battle = m_battleTracker.getPendingBattle(territory, false);
			}
			if (battle == null)
				continue;
			if (battle != null && bombingBattle != null)
			{
				m_battleTracker.addDependency(battle, bombingBattle);
			}
			if (battle != null && battle.isEmpty())
				battle.addAttackChange(new RouteScripted(territory), attackingUnits, null);
			if (battle != null && !battle.getAttackingUnits().containsAll(attackingUnits))
			{
				List<Unit> attackingUnitsNeedToBeAdded = attackingUnits;
				attackingUnitsNeedToBeAdded.removeAll(battle.getAttackingUnits());
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
			if (battle != null && (ignoreTransports && Match.allMatch(attackingUnits, seaTransports) && Match.allMatch(enemyUnits, seaTransports))
						|| ((Match.allMatch(attackingUnits, Matches.unitHasAttackValueOfAtLeast(1).invert())) && Match.allMatch(enemyUnits, Matches.unitHasDefendValueOfAtLeast(1).invert())))
			{
				m_battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecords.BattleResult.STALEMATE, 0);
				m_battleTracker.removeBattle(battle);
				continue;
			}
			// Check for ignored units
			if (battle != null && !attackingUnits.isEmpty() && (ignoreTransports || ignoreSubs))
			{
				// TODO check if incoming units can attack before asking
				final ITripleaPlayer remotePlayer = (ITripleaPlayer) m_bridge.getRemote();
				// if only enemy transports... attack them?
				if (ignoreTransports && Match.allMatch(enemyUnits, seaTransports))
				{
					if (!remotePlayer.selectAttackTransports(territory))
					{
						m_battleTracker.removeBattle(battle);
						m_battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecords.BattleResult.WON_WITH_ENEMY_LEFT, 0);
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
						m_battleTracker.removeBattle(battle);
						m_battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecords.BattleResult.WON_WITH_ENEMY_LEFT, 0);
					}
					continue;
				}
				// if only enemy transports and subs... attack them?
				if (ignoreSubs && ignoreTransports && Match.allMatch(enemyUnits, seaTranportsOrSubs))
				{
					if (!remotePlayer.selectAttackUnits(territory))
					{
						m_battleTracker.removeBattle(battle);
						m_battleTracker.getBattleRecords().addResultToBattle(player, battle.getBattleID(), null, 0, 0, BattleRecords.BattleResult.WON_WITH_ENEMY_LEFT, 0);
					}
					continue;
				}
			}
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
		int maxScrambleDistance = 0;
		final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
		while (utIter.hasNext())
		{
			final UnitAttachment ua = UnitAttachment.get(utIter.next());
			if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance())
				maxScrambleDistance = ua.getMaxScrambleDistance();
		}
		final CompositeMatchAnd<Territory> canScramble = new CompositeMatchAnd<Territory>(new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, Matches.isTerritoryEnemy(m_player, data)),
					Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.UnitCanScramble, Matches.unitIsEnemyOf(data, m_player), Matches.UnitIsDisabled().invert())),
					Matches.territoryHasUnitsThatMatch(new CompositeMatchAnd<Unit>(Matches.UnitIsAirBase, Matches.unitIsEnemyOf(data, m_player), Matches.UnitIsDisabled().invert())));
		if (fromIslandOnly)
			canScramble.add(Matches.TerritoryIsIsland);
		final HashMap<Territory, HashSet<Territory>> scrambleTerrs = new HashMap<Territory, HashSet<Territory>>();
		final Collection<Territory> territoriesWithBattles = m_battleTracker.getPendingBattleSites(false);
		final Collection<Territory> territoriesWithBattlesWater = Match.getMatches(territoriesWithBattles, Matches.TerritoryIsWater);
		final Collection<Territory> territoriesWithBattlesLand = Match.getMatches(territoriesWithBattles, Matches.TerritoryIsLand);
		for (final Territory battleTerr : territoriesWithBattlesWater)
		{
			final HashSet<Territory> canScrambleFrom = new HashSet<Territory>(Match.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
			if (!canScrambleFrom.isEmpty())
				scrambleTerrs.put(battleTerr, canScrambleFrom);
		}
		for (final Territory battleTerr : territoriesWithBattlesLand)
		{
			final IBattle battle = m_battleTracker.getPendingBattle(battleTerr, false);
			if (!toSeaOnly)
			{
				final HashSet<Territory> canScrambleFrom = new HashSet<Territory>(Match.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
				if (!canScrambleFrom.isEmpty())
					scrambleTerrs.put(battleTerr, canScrambleFrom);
			}
			// do not forget we may already have the territory in the list, so we need to add to the collection, not overwrite it.
			if (battle.isAmphibious())
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
							canScrambleFrom.addAll(Match.getMatches(data.getMap().getNeighbors(battleTerr, maxScrambleDistance), canScramble));
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
		final HashMap<Tuple<Territory, PlayerID>, Collection<HashMap<Territory, Tuple<Integer, Collection<Unit>>>>> scramblersByTerritoryPlayer = new HashMap<Tuple<Territory, PlayerID>, Collection<HashMap<Territory, Tuple<Integer, Collection<Unit>>>>>();
		for (final Territory to : scrambleTerrs.keySet())
		{
			final HashMap<Territory, Tuple<Integer, Collection<Unit>>> scramblers = new HashMap<Territory, Tuple<Integer, Collection<Unit>>>();
			// find who we should ask
			PlayerID defender = null;
			if (m_battleTracker.hasPendingBattle(to, false))
				defender = MustFightBattle.findDefender(to, m_player, data);
			for (final Territory from : scrambleTerrs.get(to))
			{
				final Collection<Unit> airbases = from.getUnits().getMatches(
							new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, m_player), Matches.UnitIsAirBase, Matches.UnitIsDisabled().invert()));
				if (defender == null)
				{
					defender = MustFightBattle.findDefender(from, m_player, data);
				}
				
				// find how many is the max this territory can scramble
				int maxScrambled = 0;
				for (final Unit base : airbases)
				{
					int tempMax = UnitAttachment.get(base.getType()).getMaxScrambleCount();
					if (tempMax == -1)
						tempMax = Integer.MAX_VALUE;
					if (tempMax > maxScrambled)
						maxScrambled = tempMax;
				}
				final Route toBattleRoute = data.getMap().getRoute_IgnoreEnd(from, to, Matches.TerritoryIsNotImpassable);
				final Collection<Unit> canScrambleAir = from.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, m_player), Matches.UnitCanScramble,
							Matches.UnitIsDisabled().invert(), Matches.UnitWasScrambled.invert(), Matches.unitCanScrambleOnRouteDistance(toBattleRoute)));
				if (maxScrambled > 0 && !canScrambleAir.isEmpty())
					scramblers.put(from, new Tuple<Integer, Collection<Unit>>(maxScrambled, canScrambleAir));
			}
			if (defender == null || scramblers.isEmpty())
				continue;
			final Tuple<Territory, PlayerID> terrPlayer = new Tuple<Territory, PlayerID>(to, defender);
			Collection<HashMap<Territory, Tuple<Integer, Collection<Unit>>>> tempScrambleList = scramblersByTerritoryPlayer.get(terrPlayer);
			if (tempScrambleList == null)
				tempScrambleList = new ArrayList<HashMap<Territory, Tuple<Integer, Collection<Unit>>>>();
			tempScrambleList.add(scramblers);
			scramblersByTerritoryPlayer.put(terrPlayer, tempScrambleList);
		}
		
		// now scramble them
		for (final Tuple<Territory, PlayerID> terrPlayer : scramblersByTerritoryPlayer.keySet())
		{
			final Territory to = terrPlayer.getFirst();
			final PlayerID defender = terrPlayer.getSecond();
			boolean scrambledHere = false;
			for (final HashMap<Territory, Tuple<Integer, Collection<Unit>>> scramblers : scramblersByTerritoryPlayer.get(terrPlayer))
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
				
				final HashMap<Territory, Collection<Unit>> toScramble = ((ITripleaPlayer) m_bridge.getRemote(defender)).scrambleUnitsQuery(to, scramblers);
				if (toScramble == null)
					continue;
				
				// verify max allowed
				if (!scramblers.keySet().containsAll(toScramble.keySet()))
					throw new IllegalStateException("Trying to scramble from illegal territory");
				for (final Territory t : scramblers.keySet())
				{
					if (toScramble.get(t) == null)
						continue;
					if (toScramble.get(t).size() > scramblers.get(t).getFirst())
						throw new IllegalStateException("Trying to scramble " + toScramble.get(t).size() + " out of " + t.getName() + ", but max allowed is " + scramblers.get(t).getFirst());
				}
				
				final CompositeChange change = new CompositeChange();
				for (final Territory t : toScramble.keySet())
				{
					final Collection<Unit> scrambling = toScramble.get(t);
					if (scrambling == null || scrambling.isEmpty())
						continue;
					for (final Unit u : scrambling)
					{
						change.add(ChangeFactory.unitPropertyChange(u, t, TripleAUnit.ORIGINATED_FROM));
						change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_SCRAMBLED));
					}
					change.add(ChangeFactory.moveUnits(t, to, scrambling)); // should we mark combat, or call setupUnitsInSameTerritoryBattles again?
					m_bridge.getHistoryWriter()
								.startEvent(defender.getName() + " scrambles " + scrambling.size() + " units out of " + t.getName() + " to defend against the attack in " + to.getName());
					m_bridge.getHistoryWriter().setRenderingData(scrambling);
					scrambledHere = true;
				}
				if (!change.isEmpty())
					m_bridge.addChange(change);
			}
			if (!scrambledHere)
				continue;
			
			// make sure the units join the battle, or create a new battle.
			IBattle battle = m_battleTracker.getPendingBattle(to, false);
			if (battle == null)
			{
				final List<Unit> attackingUnits = to.getUnits().getMatches(Matches.unitIsOwnedBy(m_player));
				m_bridge.getHistoryWriter().startEvent(defender.getName() + " scrambles to create a battle in territory " + to.getName());
				// TODO: the attacking sea units do not remember where they came from, so they can not retreat anywhere. Need to fix
				m_battleTracker.addBattle(new RouteScripted(to), attackingUnits, false, m_player, m_bridge, null);
				battle = m_battleTracker.getPendingBattle(to, false);
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
					final IBattle battleAmphib = m_battleTracker.getPendingBattle(t, false);
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
						landingTerr = ((ITripleaPlayer) m_bridge.getRemote(u.getOwner())).selectTerritoryForAirToLand(possible, t, MyFormatter.unitsToText(Collections.singletonList(u)));
					else if (possible.size() == 1)
						landingTerr = possible.iterator().next();
					if (landingTerr == null || landingTerr.equals(t))
					{
						carrierCostOfCurrentTerr += MoveValidator.carrierCost(Collections.singletonList(u));
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
					m_bridge.getHistoryWriter().startEvent(historyText);
					m_bridge.getHistoryWriter().setRenderingData(u);
					m_bridge.addChange(change);
				}
			}
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
	
	private void doKamikazeSuicideAttacks()
	{
		final GameData data = getData();
		if (!games.strategy.triplea.Properties.getUseKamikazeSuicideAttacks(data))
			return;
		// the current player is not the one who is doing these attacks, it is the all the enemies of this player who will do attacks
		final Collection<PlayerID> enemies = Match.getMatches(data.getPlayerList().getPlayers(), Matches.isAtWar(m_player, data));
		if (enemies.isEmpty())
			return;
		final Match<Unit> canBeAttacked = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(m_player), Matches.UnitIsSea,
					Matches.UnitIsNotTransportButCouldBeCombatTransport, Matches.UnitIsNotSub);
		// create a list of all kamikaze zones, listed by enemy
		final HashMap<PlayerID, Collection<Territory>> kamikazeZonesByEnemy = new HashMap<PlayerID, Collection<Territory>>();
		for (final Territory t : data.getMap().getTerritories())
		{
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			if (ta == null)
				continue;
			if (!ta.isKamikazeZone())
				continue;
			// TODO: we should have a game option to decide if it is the original or current owner. default is original.
			PlayerID owner = null;
			owner = ta.getOccupiedTerrOf();
			if (owner == null)
				owner = ta.getOriginalOwner();
			if (owner == null)
				continue;
			if (enemies.contains(owner))
			{
				// TODO: we should have a game option or a couple, or player attachments, to decide what units can be attacked
				if (Match.noneMatch(t.getUnits().getUnits(), canBeAttacked))
					continue;
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
				possibleUnitsToAttack.put(t, t.getUnits().getMatches(canBeAttacked));
			}
			final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> attacks = ((ITripleaPlayer) m_bridge.getRemote(currentEnemy)).selectKamikazeSuicideAttacks(possibleUnitsToAttack);
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
					rolls = m_bridge.getRandom(diceSides, 1, "Rolling for remainder in Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
					if (remainder > rolls[0])
						hits++;
				}
			}
		}
		else
		{
			final int numTokens = numberOfAttacks.totalValues();
			rolls = m_bridge.getRandom(diceSides, numTokens, "Rolling for Kamikaze Suicide Attack on unit: " + unitUnderFire.getType().getName());
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
		m_bridge.getHistoryWriter().startEvent(title + dice);
		m_bridge.getHistoryWriter().setRenderingData(unitUnderFire);
		if (hits > 0)
		{
			final UnitAttachment ua = UnitAttachment.get(unitUnderFire.getType());
			final int currentHits = unitUnderFire.getHits();
			if (ua.isTwoHit() && currentHits < 1 && hits == 1)
			{
				final IntegerMap<Unit> hitMap = new IntegerMap<Unit>();
				hitMap.put(unitUnderFire, hits);
				change.add(ChangeFactory.unitsHit(hitMap));
				markDamaged(Collections.singleton(unitUnderFire), m_bridge);
			}
			else
			{
				// TODO: kill dependents
				change.add(ChangeFactory.removeUnits(location, Collections.singleton(unitUnderFire)));
			}
		}
		if (!change.isEmpty())
		{
			m_bridge.addChange(change);
		}
		// TODO: display this as actual dice for both players
		((ITripleaPlayer) m_bridge.getRemote(m_player)).reportMessage(title + dice, title);
		((ITripleaPlayer) m_bridge.getRemote(firingEnemy)).reportMessage(title + dice, title);
	}
	
	public static void markDamaged(final Collection<Unit> damaged, final IDelegateBridge bridge)
	{
		if (damaged.size() == 0)
			return;
		Change damagedChange = null;
		final IntegerMap<Unit> damagedMap = new IntegerMap<Unit>();
		damagedMap.putAll(damaged, 1);
		damagedChange = ChangeFactory.unitsHit(damagedMap);
		bridge.getHistoryWriter().addChildToEvent("Units damaged: " + MyFormatter.unitsToText(damaged), damaged);
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
				final Route route = data.getMap().getRoute(currentTerr, possibleIter.next(), Matches.territoryIsNotNeutralAndNotImpassibleOrRestricted(alliedPlayer, data));
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
			final int carrierCost = MoveValidator.carrierCost(strandedAir);
			final Iterator<Territory> waterIter = availableWater.iterator();
			while (waterIter.hasNext())
			{
				final Territory t = waterIter.next();
				int carrierCapacity = MoveValidator.carrierCapacity(t.getUnits().getMatches(Matches.UnitIsAlliedCarrier(alliedPlayer, data)), t);
				if (!t.equals(currentTerr))
					carrierCapacity -= MoveValidator.carrierCost(t.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, Matches.alliedUnit(alliedPlayer, data))));
				else
					carrierCapacity -= carrierCostForCurrentTerr;
				// carrierCapacity -= MoveValidator.carrierCost(dependentunits)
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
	
	public Territory getCurentBattle()
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
}


@SuppressWarnings("serial")
class BattleExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	public BattleTracker m_battleTracker = new BattleTracker();
	public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
	public boolean m_needToInitialize;
	public IBattle m_currentBattle;
}
