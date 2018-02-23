package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Used to keep track of where battles have occurred.
 */
public class BattleTracker implements Serializable {
  private static final long serialVersionUID = 8806010984321554662L;
  // List of pending battles
  private final Set<IBattle> m_pendingBattles = new HashSet<>();
  // List of battle dependencies
  // maps blocked -> Collection of battles that must precede
  private final Map<IBattle, HashSet<IBattle>> m_dependencies = new HashMap<>();
  // enemy and neutral territories that have been conquered
  // blitzed is a subset of this
  private final Set<Territory> m_conquered = new HashSet<>();
  // blitzed territories
  private final Set<Territory> m_blitzed = new HashSet<>();
  // territories where a battle occurred
  // TODO: fix typo in name, 'fough' -> 'fought'
  private final Set<Territory> m_foughBattles = new HashSet<>();
  // these territories have had battleships bombard during a naval invasion
  // used to make sure that the same battleship doesn't bombard twice
  private final Set<Territory> m_bombardedFromTerritories = new HashSet<>();
  // list of territory we have conquered in a FinishedBattle and where from and if amphibious
  private final HashMap<Territory, Map<Territory, Collection<Unit>>> m_finishedBattlesUnitAttackFromMap =
      new HashMap<>();
  // things like kamikaze suicide attacks disallow bombarding from that sea zone for that turn
  private final Set<Territory> m_noBombardAllowed = new HashSet<>();
  private final Map<Territory, Collection<Unit>> m_defendingAirThatCanNotLand =
      new HashMap<>();
  private BattleRecords m_battleRecords = null;
  // to keep track of all relationships that have changed this turn
  // (so we can validate things like transports loading in newly created hostile zones)
  private final Collection<Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>>> m_relationshipChangesThisTurn =
      new ArrayList<>();

  /**
   * @param t
   *        referring territory.
   * @return whether a battle is to be fought in the given territory
   */
  public boolean hasPendingBattle(final Territory t, final boolean bombing) {
    return getPendingBattle(t, bombing, null) != null;
  }

  void addToConquered(final Territory territory) {
    m_conquered.add(territory);
  }

  /**
   * @param t
   *        referring territory.
   * @return whether territory was conquered
   */
  public boolean wasConquered(final Territory t) {
    return m_conquered.contains(t);
  }

  public Set<Territory> getConquered() {
    return m_conquered;
  }

  /**
   * @param t
   *        referring territory.
   * @return whether territory was conquered by blitz
   */
  public boolean wasBlitzed(final Territory t) {
    return m_blitzed.contains(t);
  }

  public boolean wasBattleFought(final Territory t) {
    return m_foughBattles.contains(t);
  }

  public boolean noBombardAllowedFromHere(final Territory t) {
    return m_noBombardAllowed.contains(t);
  }

  public void addNoBombardAllowedFromHere(final Territory t) {
    m_noBombardAllowed.add(t);
  }

  public HashMap<Territory, Map<Territory, Collection<Unit>>> getFinishedBattlesUnitAttackFromMap() {
    return m_finishedBattlesUnitAttackFromMap;
  }

  public void addRelationshipChangesThisTurn(final PlayerID p1, final PlayerID p2, final RelationshipType oldRelation,
      final RelationshipType newRelation) {
    m_relationshipChangesThisTurn.add(Tuple.of(
        Tuple.of(p1, p2),
        Tuple.of(oldRelation, newRelation)));
  }

  boolean didAllThesePlayersJustGoToWarThisTurn(final PlayerID p1, final Collection<Unit> enemyUnits,
      final GameData data) {
    final Set<PlayerID> enemies = new HashSet<>();
    for (final Unit u : CollectionUtils.getMatches(enemyUnits, Matches.unitIsEnemyOf(data, p1))) {
      enemies.add(u.getOwner());
    }
    for (final PlayerID e : enemies) {
      if (!didThesePlayersJustGoToWarThisTurn(p1, e)) {
        return false;
      }
    }
    return true;
  }

  private boolean didThesePlayersJustGoToWarThisTurn(final PlayerID p1, final PlayerID p2) {
    // check all relationship changes that are p1 and p2, to make sure that oldRelation is not war,
    // and newRelation is war
    for (final Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>> t : m_relationshipChangesThisTurn) {
      final Tuple<PlayerID, PlayerID> players = t.getFirst();
      if (players.getFirst().equals(p1)) {
        if (!players.getSecond().equals(p2)) {
          continue;
        }
      } else if (players.getSecond().equals(p1)) {
        if (!players.getFirst().equals(p2)) {
          continue;
        }
      } else {
        continue;
      }
      final Tuple<RelationshipType, RelationshipType> relations = t.getSecond();
      if (!Matches.relationshipTypeIsAtWar().test(relations.getFirst())) {
        if (Matches.relationshipTypeIsAtWar().test(relations.getSecond())) {
          return true;
        }
      }
    }
    return false;
  }

  void clearFinishedBattles(final IDelegateBridge bridge) {
    for (final IBattle battle : new ArrayList<>(m_pendingBattles)) {
      if (FinishedBattle.class.isAssignableFrom(battle.getClass())) {
        final FinishedBattle finished = (FinishedBattle) battle;
        m_finishedBattlesUnitAttackFromMap.put(finished.getTerritory(), finished.getAttackingFromMap());
        finished.fight(bridge);
      }
    }
  }

  void clearEmptyAirBattleAttacks(final IDelegateBridge bridge) {
    for (final IBattle battle : new ArrayList<>(m_pendingBattles)) {
      if (AirBattle.class.isAssignableFrom(battle.getClass())) {
        final AirBattle airBattle = (AirBattle) battle;
        airBattle.updateDefendingUnits();
        if (airBattle.getDefendingUnits().isEmpty()) {
          airBattle.finishBattleAndRemoveFromTrackerHeadless(bridge);
        }
      }
    }
  }

  void undoBattle(final Route route, final Collection<Unit> units, final PlayerID player,
      final IDelegateBridge bridge) {
    for (final IBattle battle : new ArrayList<>(m_pendingBattles)) {
      if (battle.getTerritory().equals(route.getEnd())) {
        battle.removeAttack(route, units);
        if (battle.isEmpty()) {
          removeBattleForUndo(player, battle);
        }
      }
    }
    final RelationshipTracker relationshipTracker = bridge.getData().getRelationshipTracker();
    // if we have no longer conquered it, clear the blitz state
    // We must look at all territories,
    // because we could have conquered the end territory if there are no units there
    for (final Territory current : route.getAllTerritories()) {
      if (!relationshipTracker.isAllied(current.getOwner(), player) && m_conquered.contains(current)) {
        m_conquered.remove(current);
        m_blitzed.remove(current);
      }
    }
    // say they weren't in combat
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(ChangeFactory.unitPropertyChange(unit, false, TripleAUnit.WAS_IN_COMBAT));
    }
    bridge.addChange(change);
  }

  private void removeBattleForUndo(final PlayerID player, final IBattle battle) {
    if (m_battleRecords != null) {
      m_battleRecords.removeBattle(player, battle.getBattleId());
    }
    m_pendingBattles.remove(battle);
    m_dependencies.remove(battle);
    for (final Collection<IBattle> battles : m_dependencies.values()) {
      battles.remove(battle);
    }
  }

  void addBombingBattle(final Route route, final Collection<Unit> units, final PlayerID id,
      final IDelegateBridge bridge, final UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute) {

    this.addBattle(route, units, true, id, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute, null, false);
  }

  private void addBombingBattle(final Route route, final Collection<Unit> units, final PlayerID attacker,
      final GameData data, final HashMap<Unit, HashSet<Unit>> targets) {
    IBattle battle = getPendingBattle(route.getEnd(), true, BattleType.BOMBING_RAID);
    if (battle == null) {
      battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, this);
      m_pendingBattles.add(battle);
      getBattleRecords().addBattle(attacker, battle.getBattleId(), route.getEnd(), battle.getBattleType());
    }
    final Change change = battle.addAttackChange(route, units, targets);
    // when state is moved to the game data, this will change
    if (!change.isEmpty()) {
      throw new IllegalStateException("Non empty change");
    }
    // dont let land battles in the same territory occur before bombing battles
    final IBattle dependent = getPendingBattle(route.getEnd(), false, BattleType.NORMAL);
    if (dependent != null) {
      addDependency(dependent, battle);
    }
    final IBattle dependentAirBattle = getPendingBattle(route.getEnd(), false, BattleType.AIR_BATTLE);
    if (dependentAirBattle != null) {
      addDependency(dependentAirBattle, battle);
    }
  }

  public void addBattle(final Route route, final Collection<Unit> units, final PlayerID id,
      final IDelegateBridge bridge, final UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute) {
    this.addBattle(route, units, false, id, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute, null, false);
  }

  void addBattle(final Route route, final Collection<Unit> units, final boolean bombing, final PlayerID id,
      final IDelegateBridge bridge, final UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute, final HashMap<Unit, HashSet<Unit>> targets,
      final boolean airBattleCompleted) {
    final GameData data = bridge.getData();
    if (bombing) {
      // create only either an air battle OR a bombing battle.
      // (the air battle will create a bombing battle when done, if needed)
      if (!airBattleCompleted && Properties.getRaidsMayBePreceededByAirBattles(data)
          && AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(route.getEnd(), id, data, bombing)) {
        addAirBattle(route, units, id, data, true);
      } else {
        addBombingBattle(route, units, id, data, targets);
      }
      // say they were in combat
      markWasInCombat(units, bridge, changeTracker);
    } else {
      // create both an air battle and a normal battle
      if (!airBattleCompleted && Properties.getBattlesMayBePreceededByAirBattles(data)
          && AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(route.getEnd(), id, data, bombing)) {
        addAirBattle(route, CollectionUtils.getMatches(units, AirBattle.attackingGroundSeaBattleEscorts()), id, data,
            false);
      }
      final Change change = addMustFightBattleChange(route, units, id, data);
      bridge.addChange(change);
      if (changeTracker != null) {
        changeTracker.addChange(change);
      }
      if (units.stream().anyMatch(Matches.unitIsLand())
          || units.stream().anyMatch(Matches.unitIsSea())) {
        addEmptyBattle(route, units, id, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute);
      }
    }
  }

  private static void markWasInCombat(final Collection<Unit> units, final IDelegateBridge bridge,
      final UndoableMove changeTracker) {
    if (units == null) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.WAS_IN_COMBAT));
    }
    bridge.addChange(change);
    if (changeTracker != null) {
      changeTracker.addChange(change);
    }
  }

  private void addAirBattle(final Route route, final Collection<Unit> units, final PlayerID attacker,
      final GameData data, final boolean bombingRun) {
    if (units.isEmpty()) {
      return;
    }
    IBattle battle =
        getPendingBattle(route.getEnd(), bombingRun, (bombingRun ? BattleType.AIR_RAID : BattleType.AIR_BATTLE));
    if (battle == null) {
      battle = new AirBattle(route.getEnd(), bombingRun, data, attacker, this);
      m_pendingBattles.add(battle);
      getBattleRecords().addBattle(attacker, battle.getBattleId(), route.getEnd(), battle.getBattleType());
    }
    final Change change = battle.addAttackChange(route, units, null);
    // when state is moved to the game data, this will change
    if (!change.isEmpty()) {
      throw new IllegalStateException("Non empty change");
    }
    // dont let land battles in the same territory occur before bombing battles
    if (bombingRun) {
      final IBattle dependentAirBattle = getPendingBattle(route.getEnd(), false, BattleType.AIR_BATTLE);
      if (dependentAirBattle != null) {
        addDependency(dependentAirBattle, battle);
      }
    } else {
      final IBattle airRaid = getPendingBattle(route.getEnd(), true, BattleType.AIR_RAID);
      if (airRaid != null) {
        addDependency(battle, airRaid);
      }
      final IBattle raid = getPendingBattle(route.getEnd(), true, BattleType.BOMBING_RAID);
      if (raid != null) {
        addDependency(battle, raid);
      }
    }
    final IBattle dependent = getPendingBattle(route.getEnd(), false, BattleType.NORMAL);
    if (dependent != null) {
      addDependency(dependent, battle);
    }
  }

  /**
   * No enemies.
   */
  private void addEmptyBattle(final Route route, final Collection<Unit> units, final PlayerID id,
      final IDelegateBridge bridge, final UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute) {
    final GameData data = bridge.getData();
    final Collection<Unit> canConquer = CollectionUtils.getMatches(units,
        Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, id, data, false).negate());
    if (canConquer.stream().noneMatch(Matches.unitIsNotAir())) {
      return;
    }
    final Collection<Unit> presentFromStartTilEnd = new ArrayList<>(canConquer);
    if (unitsNotUnloadedTilEndOfRoute != null) {
      presentFromStartTilEnd.removeAll(unitsNotUnloadedTilEndOfRoute);
    }
    final boolean canConquerMiddleSteps = presentFromStartTilEnd.stream().anyMatch(Matches.unitIsNotAir());
    final boolean scramblingEnabled = Properties.getScrambleRulesInEffect(data);
    final Predicate<Territory> conquerable = Matches.territoryIsEmptyOfCombatUnits(data, id)
        .and(Matches.territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(id)
            .or(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(id, data)));
    final Collection<Territory> conquered = new ArrayList<>();
    if (canConquerMiddleSteps) {
      conquered.addAll(route.getMatches(conquerable));
      // in case we begin in enemy territory, and blitz out of it, check the first territory
      if ((route.getStart() != route.getEnd()) && conquerable.test(route.getStart())) {
        conquered.add(route.getStart());
      }
    }
    // we handle the end of the route later
    conquered.remove(route.getEnd());
    final Collection<Territory> blitzed = CollectionUtils.getMatches(conquered, Matches.territoryIsBlitzable(id, data));
    m_blitzed.addAll(CollectionUtils.getMatches(blitzed, Matches.isTerritoryEnemy(id, data)));
    m_conquered.addAll(CollectionUtils.getMatches(conquered, Matches.isTerritoryEnemy(id, data)));
    for (final Territory current : conquered) {
      IBattle nonFight = getPendingBattle(current, false, BattleType.NORMAL);
      // TODO: if we ever want to scramble to a blitzed territory, then we need to fix this
      if (nonFight == null) {
        nonFight = new FinishedBattle(current, id, this, false, BattleType.NORMAL, data,
            BattleRecord.BattleResultDescription.CONQUERED, WhoWon.ATTACKER);
        m_pendingBattles.add(nonFight);
        getBattleRecords().addBattle(id, nonFight.getBattleId(), current, nonFight.getBattleType());
      }
      final Change change = nonFight.addAttackChange(route, units, null);
      bridge.addChange(change);
      if (changeTracker != null) {
        changeTracker.addChange(change);
      }
      takeOver(current, id, bridge, changeTracker, units);
      // }
    }
    // check the last territory
    if (conquerable.test(route.getEnd())) {
      IBattle precede = getDependentAmphibiousAssault(route);
      if (precede == null) {
        precede = getPendingBattle(route.getEnd(), true, null);
      }
      // if we have a preceding battle, then we must use a non-fighting-battle
      // if we have scrambling on, and this is an amphibious attack,
      // we may wish to scramble to kill the transports, so must use non-fighting-battle also
      if ((precede != null) || (scramblingEnabled && route.isUnload() && route.hasExactlyOneStep())) {
        IBattle nonFight = getPendingBattle(route.getEnd(), false, BattleType.NORMAL);
        if (nonFight == null) {
          nonFight = new NonFightingBattle(route.getEnd(), id, this, data);
          m_pendingBattles.add(nonFight);
          getBattleRecords().addBattle(id, nonFight.getBattleId(), route.getEnd(), nonFight.getBattleType());
        }
        final Change change = nonFight.addAttackChange(route, units, null);
        bridge.addChange(change);
        if (changeTracker != null) {
          changeTracker.addChange(change);
        }
        if (precede != null) {
          addDependency(nonFight, precede);
        }
      } else {
        if (Matches.isTerritoryEnemy(id, data).test(route.getEnd())) {
          if (Matches.territoryIsBlitzable(id, data).test(route.getEnd())) {
            m_blitzed.add(route.getEnd());
          }
          m_conquered.add(route.getEnd());
        }
        IBattle nonFight = getPendingBattle(route.getEnd(), false, BattleType.NORMAL);
        if (nonFight == null) {
          nonFight = new FinishedBattle(route.getEnd(), id, this, false, BattleType.NORMAL, data,
              BattleRecord.BattleResultDescription.CONQUERED, WhoWon.ATTACKER);
          m_pendingBattles.add(nonFight);
          getBattleRecords().addBattle(id, nonFight.getBattleId(), route.getEnd(), nonFight.getBattleType());
        }
        final Change change = nonFight.addAttackChange(route, units, null);
        bridge.addChange(change);
        if (changeTracker != null) {
          changeTracker.addChange(change);
        }
        takeOver(route.getEnd(), id, bridge, changeTracker, units);
      }
    }
    // TODO: else what?
  }

  public void takeOver(final Territory territory, final PlayerID id, final IDelegateBridge bridge,
      final UndoableMove changeTracker, final Collection<Unit> arrivingUnits) {
    // This could be NULL if unowned water
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if (ta == null) {
      // TODO: allow capture/destroy of infrastructure on unowned water
      return;
    }
    final GameData data = bridge.getData();
    final Collection<Unit> arrivedUnits = ((arrivingUnits == null) ? null : new ArrayList<>(arrivingUnits));
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final boolean isTerritoryOwnerAnEnemy = relationshipTracker.canTakeOverOwnedTerritory(id, territory.getOwner());
    // If this is a convoy (we wouldn't be in this method otherwise)
    // check to make sure attackers have more than just transports. If they don't, exit here.
    if (territory.isWater() && (arrivedUnits != null)) {
      // Total Attacking Sea units = all units - land units - air units - submerged subs
      // Also subtract transports & subs (if they can't control sea zones)
      int totalMatches = arrivedUnits.size() - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsLand())
          - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsAir())
          - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsSubmerged());
      // If transports are restricted from controlling sea zones, subtract them
      final Predicate<Unit> transportsCanNotControl = Matches.unitIsTransportAndNotDestroyer()
          .and(Matches.unitIsTransportButNotCombatTransport());
      if (!Properties.getTransportControlSeaZone(data)) {
        totalMatches -= CollectionUtils.countMatches(arrivedUnits, transportsCanNotControl);
      }
      // TODO check if istrn and NOT isDD
      // If subs are restricted from controlling sea zones, subtract them
      if (Properties.getSubControlSeaZoneRestricted(data)) {
        totalMatches -= CollectionUtils.countMatches(arrivedUnits, Matches.unitIsSub());
      }
      if (totalMatches == 0) {
        return;
      }
    }
    // If it was a Convoy Route- check ownership of the associated neighboring territory and set message
    if (ta.getConvoyRoute()) {
      // we could be part of a convoy route for another territory
      final Collection<Territory> attachedConvoyTo =
          TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data);
      for (final Territory convoy : attachedConvoyTo) {
        final TerritoryAttachment cta = TerritoryAttachment.get(convoy);
        if (!cta.getConvoyRoute()) {
          continue;
        }
        final PlayerID convoyOwner = convoy.getOwner();
        if (relationshipTracker.isAllied(id, convoyOwner)) {
          if (CollectionUtils.getMatches(cta.getConvoyAttached(), Matches.isTerritoryAllied(convoyOwner, data))
              .size() <= 0) {
            bridge.getHistoryWriter()
                .addChildToEvent(convoyOwner.getName() + " gains " + cta.getProduction() + " production in "
                    + convoy.getName() + " for the liberation the convoy route in " + territory.getName());
          }
        } else if (relationshipTracker.isAtWar(id, convoyOwner)) {
          if (CollectionUtils.getMatches(cta.getConvoyAttached(), Matches.isTerritoryAllied(convoyOwner, data))
              .size() == 1) {
            bridge.getHistoryWriter()
                .addChildToEvent(convoyOwner.getName() + " loses " + cta.getProduction() + " production in "
                    + convoy.getName() + " due to the capture of the convoy route in " + territory.getName());
          }
        }
      }
    }
    // if neutral, we may charge money to enter
    if (territory.getOwner().isNull() && !territory.isWater()
        && (Properties.getNeutralCharge(data) >= 0)) {
      final Resource pus = data.getResourceList().getResource(Constants.PUS);
      final int puChargeIdeal = -Properties.getNeutralCharge(data);
      final int puChargeReal = Math.min(0, Math.max(puChargeIdeal, -id.getResources().getQuantity(pus)));
      final Change neutralFee = ChangeFactory.changeResourcesChange(id, pus, puChargeReal);
      bridge.addChange(neutralFee);
      if (changeTracker != null) {
        changeTracker.addChange(neutralFee);
      }
      if (puChargeIdeal == puChargeReal) {
        bridge.getHistoryWriter().addChildToEvent(id.getName() + " loses " + -puChargeReal + " "
            + MyFormatter.pluralize("PU", -puChargeReal) + " for violating " + territory.getName() + "s neutrality.");
      } else {
        System.out.println("Player, " + id.getName() + " attacks a Neutral territory, and should have had to pay "
            + puChargeIdeal + ", but did not have enough PUs to pay! This is a bug.");
        bridge.getHistoryWriter()
            .addChildToEvent(id.getName() + " loses " + -puChargeReal + " " + MyFormatter.pluralize("PU", -puChargeReal)
                + " for violating " + territory.getName() + "s neutrality.  Correct amount to charge is: "
                + puChargeIdeal + ".  Player should not have been able to make this attack!");
      }
    }
    // if its a capital we take the money
    // NOTE: this is not checking to see if it is an enemy.
    // instead it is relying on the fact that the capital should be owned by the person it is attached to
    if (isTerritoryOwnerAnEnemy && (ta.getCapital() != null)) {
      // if the capital is owned by the capitols player take the money
      final PlayerID whoseCapital = data.getPlayerList().getPlayerId(ta.getCapital());
      final PlayerAttachment pa = PlayerAttachment.get(id);
      final PlayerAttachment paWhoseCapital = PlayerAttachment.get(whoseCapital);
      final List<Territory> capitalsList =
          new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(whoseCapital, data));
      // we are losing one right now, so it is < not <=
      if ((paWhoseCapital != null) && (paWhoseCapital.getRetainCapitalNumber() < capitalsList.size())) {
        // do nothing, we keep our money since we still control enough capitals
        bridge.getHistoryWriter()
            .addChildToEvent(id.getName() + " captures one of " + whoseCapital.getName() + " capitals");
      } else if (whoseCapital.equals(territory.getOwner())) {
        final Resource pus = data.getResourceList().getResource(Constants.PUS);
        final int capturedPuCount = whoseCapital.getResources().getQuantity(pus);
        if (pa != null) {
          if (isPacificTheater(data)) {
            final Change changeVp =
                ChangeFactory.attachmentPropertyChange(pa, (capturedPuCount + pa.getCaptureVps()), "captureVps");
            bridge.addChange(changeVp);
            if (changeTracker != null) {
              changeTracker.addChange(changeVp);
            }
          }
        }
        final Change remove = ChangeFactory.changeResourcesChange(whoseCapital, pus, -capturedPuCount);
        bridge.addChange(remove);
        if ((paWhoseCapital != null) && paWhoseCapital.getDestroysPUs()) {
          bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys " + capturedPuCount
              + MyFormatter.pluralize("PU", capturedPuCount) + " while taking " + whoseCapital.getName() + " capital");
          if (changeTracker != null) {
            changeTracker.addChange(remove);
          }
        } else {
          bridge.getHistoryWriter().addChildToEvent(id.getName() + " captures " + capturedPuCount
              + MyFormatter.pluralize("PU", capturedPuCount) + " while taking " + whoseCapital.getName() + " capital");
          if (changeTracker != null) {
            changeTracker.addChange(remove);
          }
          final Change add = ChangeFactory.changeResourcesChange(id, pus, capturedPuCount);
          bridge.addChange(add);
          if (changeTracker != null) {
            changeTracker.addChange(add);
          }
        }
        // remove all the tokens of the captured player
        final Resource tokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
        if (tokens != null) {
          final int currTokens = whoseCapital.getResources().getQuantity(Constants.TECH_TOKENS);
          final Change removeTokens = ChangeFactory.changeResourcesChange(whoseCapital, tokens, -currTokens);
          bridge.addChange(removeTokens);
          if (changeTracker != null) {
            changeTracker.addChange(removeTokens);
          }
        }
      }
    }
    // is this an allied territory, revert to original owner if it is, unless they dont own there captital
    final PlayerID terrOrigOwner = OriginalOwnerTracker.getOriginalOwner(territory);
    PlayerID newOwner = id;
    // if the original owner is the current owner, and the current owner is our enemy or canTakeOver,
    // then we do not worry about this.
    if (isTerritoryOwnerAnEnemy && (terrOrigOwner != null) && relationshipTracker.isAllied(terrOrigOwner, id)
        && !terrOrigOwner.equals(territory.getOwner())) {
      final List<Territory> capitalsListOwned =
          new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(terrOrigOwner, data));
      if (!capitalsListOwned.isEmpty()) {
        newOwner = terrOrigOwner;
      } else {
        newOwner = id;
        final List<Territory> capitalsListOriginal =
            new ArrayList<>(TerritoryAttachment.getAllCapitals(terrOrigOwner, data));
        for (final Territory current : capitalsListOriginal) {
          if (territory.equals(current) || current.getOwner().equals(PlayerID.NULL_PLAYERID)) {
            // if a neutral controls our capital, our territories get liberated (ie: china in ww2v3)
            newOwner = terrOrigOwner;
          }
        }
      }
    }
    // if we have specially set this territory to have whenCapturedByGoesTo,
    // then we set that here (except we don't set it if we are liberating allied owned territory)
    if (isTerritoryOwnerAnEnemy && newOwner.equals(id) && Matches.territoryHasWhenCapturedByGoesTo().test(territory)) {
      for (final String value : ta.getWhenCapturedByGoesTo()) {
        final String[] s = value.split(":");
        final PlayerID capturingPlayer = data.getPlayerList().getPlayerId(s[0]);
        final PlayerID goesToPlayer = data.getPlayerList().getPlayerId(s[1]);
        if (capturingPlayer.equals(goesToPlayer)) {
          continue;
        }
        if (capturingPlayer.equals(id)) {
          newOwner = goesToPlayer;
          break;
        }
      }
    }
    if (isTerritoryOwnerAnEnemy) {
      final Change takeOver = ChangeFactory.changeOwner(territory, newOwner);
      bridge.getHistoryWriter().addChildToEvent(takeOver.toString());
      bridge.addChange(takeOver);
      if (changeTracker != null) {
        changeTracker.addChange(takeOver);
        changeTracker.addToConquered(territory);
      }
      // play a sound
      if (territory.isWater()) {
        // should probably see if there is something actually happening for water
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, id);
      } else if (ta.getCapital() != null) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, id);
      } else if (m_blitzed.contains(territory) && arrivedUnits.stream().anyMatch(Matches.unitCanBlitz())) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, id);
      } else {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, id);
      }
    }
    // Remove any bombing raids against captured territory
    // TODO: see if necessary
    if (territory.getUnits().anyMatch(Matches.unitIsEnemyOf(data, id).and(Matches.unitCanBeDamaged()))) {
      final IBattle bombingBattle = getPendingBattle(territory, true, null);
      if (bombingBattle != null) {
        final BattleResults results = new BattleResults(bombingBattle, WhoWon.DRAW, data);
        getBattleRecords().addResultToBattle(id, bombingBattle.getBattleId(), null, 0, 0,
            BattleRecord.BattleResultDescription.NO_BATTLE, results);
        bombingBattle.cancelBattle(bridge);
        removeBattle(bombingBattle);
        throw new IllegalStateException(
            "Bombing Raids should be dealt with first! Be sure the battle has dependencies set correctly!");
      }
    }
    captureOrDestroyUnits(territory, id, newOwner, bridge, changeTracker);
    // is this territory our capitol or a capitol of our ally
    // Also check to make sure playerAttachment even HAS a capital to fix abend
    if (isTerritoryOwnerAnEnemy && (terrOrigOwner != null) && (ta.getCapital() != null)
        && TerritoryAttachment.getAllCapitals(terrOrigOwner, data).contains(territory)
        && relationshipTracker.isAllied(terrOrigOwner, id)) {
      // if it is give it back to the original owner
      final Collection<Territory> originallyOwned = OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
      final List<Territory> friendlyTerritories =
          CollectionUtils.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner, data));
      // give back the factories as well.
      for (final Territory item : friendlyTerritories) {
        if (item.getOwner() == terrOrigOwner) {
          continue;
        }
        final Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
        bridge.addChange(takeOverFriendlyTerritories);
        bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
        if (changeTracker != null) {
          changeTracker.addChange(takeOverFriendlyTerritories);
        }
        final Collection<Unit> units =
            CollectionUtils.getMatches(item.getUnits().getUnits(), Matches.unitIsInfrastructure());
        if (!units.isEmpty()) {
          final Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, territory);
          bridge.addChange(takeOverNonComUnits);
          if (changeTracker != null) {
            changeTracker.addChange(takeOverNonComUnits);
          }
        }
      }
    }
    // say they were in combat
    // if the territory being taken over is water, then do not say any land units were in combat
    // (they may want to unload from the transport and attack)
    if (Matches.territoryIsWater().test(territory) && (arrivedUnits != null)) {
      arrivedUnits.removeAll(CollectionUtils.getMatches(arrivedUnits, Matches.unitIsLand()));
    }
    markWasInCombat(arrivedUnits, bridge, changeTracker);
  }

  /**
   * Called when a territory is conquered to determine if remaining enemy units should be
   * captured, destroyed, or take damage.
   */
  public static void captureOrDestroyUnits(final Territory territory, final PlayerID id, final PlayerID newOwner,
      final IDelegateBridge bridge, final UndoableMove changeTracker) {
    final GameData data = bridge.getData();
    // destroy any units that should be destroyed on capture
    if (Properties.getUnitsCanBeDestroyedInsteadOfCaptured(data)) {
      final Predicate<Unit> enemyToBeDestroyed = Matches.enemyUnit(id, data)
          .and(Matches.unitDestroyedWhenCapturedByOrFrom(id));
      final Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
      if (!destroyed.isEmpty()) {
        final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
        bridge.getHistoryWriter().addChildToEvent("Some non-combat units are destroyed: ", destroyed);
        bridge.addChange(destroyUnits);
        if (changeTracker != null) {
          changeTracker.addChange(destroyUnits);
        }
      }
    }
    // destroy any capture on entering units, IF the property to destroy them instead of capture is turned on
    if (Properties.getOnEnteringUnitsDestroyedInsteadOfCaptured(data)) {
      final Collection<Unit> destroyed =
          territory.getUnits().getMatches(Matches.unitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));
      if (!destroyed.isEmpty()) {
        final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
        bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some units instead of capturing them",
            destroyed);
        bridge.addChange(destroyUnits);
        if (changeTracker != null) {
          changeTracker.addChange(destroyUnits);
        }
      }
    }
    // destroy any disabled units owned by the enemy that are NOT infrastructure or factories
    final Predicate<Unit> enemyToBeDestroyed = Matches.enemyUnit(id, data)
        .and(Matches.unitIsDisabled())
        .and(Matches.unitIsInfrastructure().negate());
    final Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
    if (!destroyed.isEmpty()) {
      final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
      bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some disabled combat units", destroyed);
      bridge.addChange(destroyUnits);
      if (changeTracker != null) {
        changeTracker.addChange(destroyUnits);
      }
    }
    // take over non combatants
    final Predicate<Unit> enemyNonCom = Matches.enemyUnit(id, data).and(Matches.unitIsInfrastructure());
    final Predicate<Unit> willBeCaptured = enemyNonCom
        .or(Matches.unitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));
    final Collection<Unit> nonCom = territory.getUnits().getMatches(willBeCaptured);
    // change any units that change unit types on capture
    if (Properties.getUnitsCanBeChangedOnCapture(data)) {
      final Collection<Unit> toReplace =
          CollectionUtils.getMatches(nonCom, Matches.unitWhenCapturedChangesIntoDifferentUnitType());
      for (final Unit u : toReplace) {
        final Map<String, Tuple<String, IntegerMap<UnitType>>> map =
            UnitAttachment.get(u.getType()).getWhenCapturedChangesInto();
        final PlayerID currentOwner = u.getOwner();
        for (final String value : map.keySet()) {
          final String[] s = value.split(":");
          if (!(s[0].equals("any") || data.getPlayerList().getPlayerId(s[0]).equals(currentOwner))) {
            continue;
          }
          // we could use "id" or "newOwner" here... not sure which to use
          if (!(s[1].equals("any") || data.getPlayerList().getPlayerId(s[1]).equals(id))) {
            continue;
          }
          final CompositeChange changes = new CompositeChange();
          final Collection<Unit> toAdd = new ArrayList<>();
          final Tuple<String, IntegerMap<UnitType>> toCreate = map.get(value);
          final boolean translateAttributes = toCreate.getFirst().equalsIgnoreCase("true");
          for (final UnitType ut : toCreate.getSecond().keySet()) {
            toAdd.addAll(ut.create(toCreate.getSecond().getInt(ut), newOwner));
          }
          if (!toAdd.isEmpty()) {
            if (translateAttributes) {
              final Change translate = TripleAUnit.translateAttributesToOtherUnits(u, toAdd, territory);
              if (!translate.isEmpty()) {
                changes.add(translate);
              }
            }
            changes.add(ChangeFactory.removeUnits(territory, Collections.singleton(u)));
            changes.add(ChangeFactory.addUnits(territory, toAdd));
            changes.add(ChangeFactory.markNoMovementChange(toAdd));
            bridge.getHistoryWriter()
                .addChildToEvent(id.getName() + " converts " + u.toStringNoOwner() + " into different units", toAdd);
            bridge.addChange(changes);
            if (changeTracker != null) {
              changeTracker.addChange(changes);
            }
            // don't forget to remove this unit from the list
            nonCom.remove(u);
            break;
          }
        }
      }
    }
    if (!nonCom.isEmpty()) {
      // FYI: a dummy delegate will not do anything with this change,
      // meaning that the battle calculator will think this unit lived,
      // even though it died or was captured, etc!
      final Change capture = ChangeFactory.changeOwner(nonCom, newOwner, territory);
      bridge.addChange(capture);
      if (changeTracker != null) {
        changeTracker.addChange(capture);
      }
      final Change noMovementChange = ChangeFactory.markNoMovementChange(nonCom);
      bridge.addChange(noMovementChange);
      if (changeTracker != null) {
        changeTracker.addChange(noMovementChange);
      }
      final IntegerMap<Unit> damageMap = new IntegerMap<>();
      for (final Unit unit : CollectionUtils.getMatches(nonCom, Matches.unitWhenCapturedSustainsDamage())) {
        final TripleAUnit taUnit = (TripleAUnit) unit;
        final int damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(unit, territory);
        final int sustainedDamage = UnitAttachment.get(unit.getType()).getWhenCapturedSustainsDamage();
        final int actualDamage = Math.max(0, Math.min(sustainedDamage, damageLimit));
        final int totalDamage = taUnit.getUnitDamage() + actualDamage;
        damageMap.put(unit, totalDamage);
      }
      if (!damageMap.isEmpty()) {
        final Change damageChange = ChangeFactory.bombingUnitDamage(damageMap);
        bridge.addChange(damageChange);
        if (changeTracker != null) {
          changeTracker.addChange(damageChange);
        }
        // Kill any units that can die if they have reached max damage
        if (damageMap.keySet().stream().anyMatch(Matches.unitCanDieFromReachingMaxDamage())) {
          final List<Unit> unitsCanDie =
              CollectionUtils.getMatches(damageMap.keySet(), Matches.unitCanDieFromReachingMaxDamage());
          unitsCanDie.retainAll(
              CollectionUtils.getMatches(unitsCanDie, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(territory)));
          if (!unitsCanDie.isEmpty()) {
            final Change removeDead = ChangeFactory.removeUnits(territory, unitsCanDie);
            bridge.addChange(removeDead);
            if (changeTracker != null) {
              changeTracker.addChange(removeDead);
            }
          }
        }
      }
    }
  }

  private Change addMustFightBattleChange(final Route route, final Collection<Unit> units, final PlayerID id,
      final GameData data) {
    // it is possible to add a battle with a route that is just
    // the start territory, ie the units did not move into the country
    // they were there to start with
    // this happens when you have submerged subs emerging
    Territory site = route.getEnd();
    if (site == null) {
      site = route.getStart();
    }
    // this will be taken care of by the non fighting battle
    if (!Matches.territoryHasEnemyUnits(id, data).test(site)) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    // if just an enemy factory &/or AA then no battle
    final Collection<Unit> enemyUnits =
        CollectionUtils.getMatches(site.getUnits().getUnits(), Matches.enemyUnit(id, data));
    if ((route.getEnd() != null) && !enemyUnits.isEmpty()
        && enemyUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    IBattle battle = getPendingBattle(site, false, BattleType.NORMAL);
    // If there are no pending battles- add one for units already in the combat zone
    if (battle == null) {
      battle = new MustFightBattle(site, id, data, this);
      m_pendingBattles.add(battle);
      getBattleRecords().addBattle(id, battle.getBattleId(), site, battle.getBattleType());
    }
    // Add the units that moved into the battle
    final Change change = battle.addAttackChange(route, units, null);
    // make amphibious assaults dependent on possible naval invasions
    // its only a dependency if we are unloading
    final IBattle precede = getDependentAmphibiousAssault(route);
    if ((precede != null) && units.stream().anyMatch(Matches.unitIsLand())) {
      addDependency(battle, precede);
    }
    // dont let land battles in the same territory occur before bombing
    // battles
    final IBattle bombing = getPendingBattle(route.getEnd(), true, null);
    if (bombing != null) {
      addDependency(battle, bombing);
    }
    final IBattle airBattle = getPendingBattle(route.getEnd(), false, BattleType.AIR_BATTLE);
    if (airBattle != null) {
      addDependency(battle, airBattle);
    }
    return change;
  }

  private IBattle getDependentAmphibiousAssault(final Route route) {
    if (!route.isUnload()) {
      return null;
    }
    return getPendingBattle(route.getStart(), false, BattleType.NORMAL);
  }

  public IBattle getPendingBattle(final Territory t, final boolean bombing, final BattleType type) {
    for (final IBattle battle : m_pendingBattles) {
      if (battle.getTerritory().equals(t) && (battle.isBombingRun() == bombing)) {
        if ((type == null) || (type == battle.getBattleType())) {
          return battle;
        }
      }
    }
    return null;
  }

  public IBattle getPendingBattle(final GUID guid) {
    if (guid == null) {
      return null;
    }
    for (final IBattle battle : m_pendingBattles) {
      if (guid.equals(battle.getBattleId())) {
        return battle;
      }
    }
    return null;
  }

  Collection<IBattle> getPendingBattles(final Territory t, final BattleType type) {
    final Collection<IBattle> battles = new HashSet<>();
    for (final IBattle battle : m_pendingBattles) {
      if (battle.getTerritory().equals(t) && ((type == null) || (type == battle.getBattleType()))) {
        battles.add(battle);
      }
    }
    return battles;
  }

  /**
   * @param bombing
   *        whether only battles where there is bombing.
   * @return a collection of territories where battles are pending
   */
  public Collection<Territory> getPendingBattleSites(final boolean bombing) {
    final Collection<IBattle> pending = new HashSet<>(m_pendingBattles);
    final Collection<Territory> battles = new ArrayList<>();
    for (final IBattle battle : pending) {
      if ((battle != null) && !battle.isEmpty() && (battle.isBombingRun() == bombing)) {
        battles.add(battle.getTerritory());
      }
    }
    return battles;
  }

  BattleListing getPendingBattleSites() {
    final Map<BattleType, Collection<Territory>> battles = new HashMap<>();
    final Collection<IBattle> pending = new HashSet<>(m_pendingBattles);
    for (final IBattle battle : pending) {
      if ((battle != null) && !battle.isEmpty()) {
        Collection<Territory> territories = battles.get(battle.getBattleType());
        if (territories == null) {
          territories = new HashSet<>();
        }
        territories.add(battle.getTerritory());
        battles.put(battle.getBattleType(), territories);
      }
    }
    return new BattleListing(battles);
  }

  /**
   * @param blocked
   *        the battle that is blocked.
   * @return the battle that must occur before dependent can occur
   */
  public Collection<IBattle> getDependentOn(final IBattle blocked) {
    final Collection<IBattle> dependent = m_dependencies.get(blocked);
    if (dependent == null) {
      return Collections.emptyList();
    }
    return CollectionUtils.getMatches(dependent, Matches.battleIsEmpty().negate());
  }

  /**
   * @param blocking
   *        the battle that is blocking the other battles.
   * @return the battles that cannot occur until the given battle occurs
   */
  public Collection<IBattle> getBlocked(final IBattle blocking) {
    return m_dependencies.keySet().stream()
        .filter(current -> getDependentOn(current).contains(blocking))
        .collect(Collectors.toList());
  }

  public void addDependency(final IBattle blocked, final IBattle blocking) {
    m_dependencies.putIfAbsent(blocked, new HashSet<>());
    m_dependencies.get(blocked).add(blocking);
  }

  private void removeDependency(final IBattle blocked, final IBattle blocking) {
    final Collection<IBattle> dependencies = m_dependencies.get(blocked);
    dependencies.remove(blocking);
    if (dependencies.isEmpty()) {
      m_dependencies.remove(blocked);
    }
  }

  public void removeBattle(final IBattle battle) {
    if (battle != null) {
      for (final IBattle current : getBlocked(battle)) {
        removeDependency(current, battle);
      }
      m_pendingBattles.remove(battle);
      m_foughBattles.add(battle.getTerritory());
    }
  }

  private static boolean isPacificTheater(final GameData data) {
    return data.getProperties().get(Constants.PACIFIC_THEATER, false);
  }

  public void clear() {
    m_finishedBattlesUnitAttackFromMap.clear();
    m_bombardedFromTerritories.clear();
    m_pendingBattles.clear();
    m_blitzed.clear();
    m_foughBattles.clear();
    m_conquered.clear();
    m_dependencies.clear();
    m_defendingAirThatCanNotLand.clear();
    m_noBombardAllowed.clear();
    m_relationshipChangesThisTurn.clear();
  }

  void addToDefendingAirThatCanNotLand(final Collection<Unit> units, final Territory szTerritoryTheyAreIn) {
    Collection<Unit> current = m_defendingAirThatCanNotLand.get(szTerritoryTheyAreIn);
    if (current == null) {
      current = new ArrayList<>();
    }
    current.addAll(units);
    m_defendingAirThatCanNotLand.put(szTerritoryTheyAreIn, current);
  }

  public Map<Territory, Collection<Unit>> getDefendingAirThatCanNotLand() {
    return m_defendingAirThatCanNotLand;
  }

  public void clearBattleRecords() {
    if (m_battleRecords != null) {
      m_battleRecords.clear();
      m_battleRecords = null;
    }
  }

  BattleRecords getBattleRecords() {
    if (m_battleRecords == null) {
      m_battleRecords = new BattleRecords();
    }
    return m_battleRecords;
  }

  void sendBattleRecordsToGameData(final IDelegateBridge bridge) {
    if ((m_battleRecords != null) && !m_battleRecords.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Recording Battle Statistics");
      bridge.addChange(ChangeFactory.addBattleRecords(m_battleRecords, bridge.getData()));
    }
  }


  /**
   * 'Auto-fight' all of the air battles and strategic bombing runs.
   * Auto fight means we automatically begin the fight without user action. This is to avoid clicks during the
   * air battle and SBR phase, and to enforce game rules that these phases are fought first before any other combat.
   */
  void fightAirRaidsAndStrategicBombing(final IDelegateBridge delegateBridge) {
    final boolean bombing = true;
    fightAirRaidsAndStrategicBombing(delegateBridge, () -> getPendingBattleSites(bombing),
        (territory, battleType) -> getPendingBattle(territory, bombing, battleType));
  }

  @VisibleForTesting
  void fightAirRaidsAndStrategicBombing(final IDelegateBridge delegateBridge,
      final Supplier<Collection<Territory>> pendingBattleSiteSupplier,
      final BiFunction<Territory, BattleType, IBattle> pendingBattleFunction) {
    // First we'll fight all of the air battles (air raids)
    // Then we will have a wave of battles for the SBR. AA guns will shoot, and we'll roll for damage.
    // CAUTION: air raid battles when completed will potentially spawn new bombing raids. Would be good to refactor
    // that out, in the meantime be aware there are mass side effects in these calls..

    for (final Territory t : pendingBattleSiteSupplier.get()) {
      final IBattle airRaid = pendingBattleFunction.apply(t, BattleType.AIR_RAID);
      if (airRaid != null) {
        airRaid.fight(delegateBridge);
      }
    }

    // now that we've done all of the air battles, do all of the SBR's as a second wave.
    for (final Territory t : pendingBattleSiteSupplier.get()) {
      final IBattle bombingRaid = pendingBattleFunction.apply(t, BattleType.BOMBING_RAID);
      if (bombingRaid != null) {
        bombingRaid.fight(delegateBridge);
      }
    }
  }

  /**
   * Kill undefended transports. Done first to remove potentially dependent sea battles
   * Which could block amphibious assaults later
   */
  public void fightDefenselessBattles(final IDelegateBridge bridge) {
    final GameData gameData = bridge.getData();
    // Here and below parameter "false" to getPendingBattleSites & getPendingBattle denote non-SBR battles
    for (final Territory territory : getPendingBattleSites(false)) {
      final IBattle battle = getPendingBattle(territory, false, BattleType.NORMAL);
      final List<Unit> defenders = new ArrayList<>();
      defenders.addAll(battle.getDefendingUnits());
      final List<Unit> sortedUnitsList = getSortedDefendingUnits(bridge, gameData, territory, defenders);
      if (getDependentOn(battle).isEmpty() && (DiceRoll.getTotalPower(
          DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, defenders, false, false, gameData,
              territory, TerritoryEffectHelper.getEffects(territory), false, null),
          gameData) == 0)) {
        battle.fight(bridge);
      }
    }
    getPendingBattleSites(false).stream()
        .map(territory -> getPendingBattle(territory, false, BattleType.NORMAL))
        .filter(NonFightingBattle.class::isInstance)
        .filter(battle -> getDependentOn(battle).isEmpty())
        .forEach(battle -> battle.fight(bridge));
  }

  private static List<Unit> getSortedDefendingUnits(final IDelegateBridge bridge, final GameData gameData,
      final Territory territory, final List<Unit> defenders) {
    final List<Unit> sortedUnitsList = new ArrayList<>(CollectionUtils.getMatches(defenders,
        Matches.unitCanBeInBattle(true, !territory.isWater(), 1, false, true, true)));
    Collections.sort(sortedUnitsList,
        new UnitBattleComparator(false, TuvUtils.getCostsForTuv(bridge.getPlayerId(), gameData),
            TerritoryEffectHelper.getEffects(territory), gameData, false, false));
    Collections.reverse(sortedUnitsList);
    return sortedUnitsList;
  }

  /**
   * Fight battle automatically if there is only one left to pick from.
   */
  public void fightBattleIfOnlyOne(final IDelegateBridge bridge) {
    final Collection<Territory> territories = getPendingBattleSites(false);
    if (territories.size() == 1) {
      final IBattle battle = getPendingBattle(territories.iterator().next(), false, BattleType.NORMAL);
      battle.fight(bridge);
    }
  }

  @Override
  public String toString() {
    return "BattleTracker:" + "\n" + "Conquered:" + m_conquered + "\n" + "Blitzed:" + m_blitzed + "\n" + "Fought:"
        + m_foughBattles + "\n" + "Pending:" + m_pendingBattles;
  }
}
