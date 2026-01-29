package games.strategy.triplea.delegate.battle;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.IBattle.WhoWon;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.BattleRecords;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.ISound;
import org.triplea.sound.SoundPath;
import org.triplea.util.Tuple;

/** Used to keep track of where battles have occurred. */
@Slf4j
public class BattleTracker implements Serializable {
  private static final long serialVersionUID = 8806010984321554662L;

  public static final String BOMBING_DEPENDENCY_ERROR =
      "Bombing Raids should be dealt with first! Be sure the battle has dependencies set correctly!";

  // List of pending battles
  private final Set<IBattle> pendingBattles = new HashSet<>();
  // List of battle dependencies
  // maps blocked -> Collection of battles that must precede
  private final Map<IBattle, Set<IBattle>> dependencies = new HashMap<>();
  // enemy and neutral territories that have been conquered
  // blitzed is a subset of this
  @Getter private final Set<Territory> conquered = new HashSet<>();
  // blitzed territories
  private final Set<Territory> blitzed = new HashSet<>();
  // territories where a battle occurred
  private final Set<Territory> foughtBattles = new HashSet<>();

  // list of territory we have conquered in a FinishedBattle and where from and if amphibious
  @Getter
  private final Map<Territory, Map<Territory, Collection<Unit>>> finishedBattlesUnitAttackFromMap =
      new HashMap<>();

  // things like kamikaze suicide attacks disallow bombarding from that sea zone for that turn
  private final Set<Territory> noBombardAllowed = new HashSet<>();

  @Getter
  private final Map<Territory, Collection<Unit>> defendingAirThatCanNotLand = new HashMap<>();

  private BattleRecords battleRecords = null;
  // to keep track of all relationships that have changed this turn
  // (so we can validate things like transports loading in newly created hostile zones)
  private final Collection<
          Tuple<Tuple<GamePlayer, GamePlayer>, Tuple<RelationshipType, RelationshipType>>>
      relationshipChangesThisTurn = new ArrayList<>();

  void addToConquered(final Territory territory) {
    conquered.add(territory);
  }

  /**
   * Indicates whether territory was conquered.
   *
   * @param t referring territory.
   */
  public boolean wasConquered(final Territory t) {
    return conquered.contains(t);
  }

  /**
   * Indicates whether territory was conquered by blitz.
   *
   * @param t referring territory.
   */
  public boolean wasBlitzed(final Territory t) {
    return blitzed.contains(t);
  }

  public boolean wasBattleFought(final Territory t) {
    return foughtBattles.contains(t);
  }

  public boolean noBombardAllowedFromHere(final Territory t) {
    return noBombardAllowed.contains(t);
  }

  public void addNoBombardAllowedFromHere(final Territory t) {
    noBombardAllowed.add(t);
  }

  public void addRelationshipChangesThisTurn(
      final GamePlayer p1,
      final GamePlayer p2,
      final RelationshipType oldRelation,
      final RelationshipType newRelation) {
    relationshipChangesThisTurn.add(Tuple.of(Tuple.of(p1, p2), Tuple.of(oldRelation, newRelation)));
  }

  public boolean didAllThesePlayersJustGoToWarThisTurn(
      final GamePlayer p1, final Collection<Unit> enemyUnits) {
    final Set<GamePlayer> enemies = new HashSet<>();
    for (final Unit u : CollectionUtils.getMatches(enemyUnits, Matches.unitIsEnemyOf(p1))) {
      enemies.add(u.getOwner());
    }
    for (final GamePlayer e : enemies) {
      if (!didThesePlayersJustGoToWarThisTurn(p1, e)) {
        return false;
      }
    }
    return true;
  }

  private boolean didThesePlayersJustGoToWarThisTurn(final GamePlayer p1, final GamePlayer p2) {
    // check all relationship changes that are p1 and p2, to make sure that oldRelation is not war,
    // and newRelation is war
    for (final var tuple : relationshipChangesThisTurn) {
      final Tuple<GamePlayer, GamePlayer> players = tuple.getFirst();
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
      final Tuple<RelationshipType, RelationshipType> relations = tuple.getSecond();
      if (!Matches.relationshipTypeIsAtWar().test(relations.getFirst())
          && Matches.relationshipTypeIsAtWar().test(relations.getSecond())) {
        return true;
      }
    }
    return false;
  }

  @RemoveOnNextMajorRelease
  public void fixUpNullPlayers(GamePlayer nullPlayer) {
    for (var b : pendingBattles) {
      b.fixUpNullPlayer(nullPlayer);
    }
  }

  void clearFinishedBattles(final IDelegateBridge bridge) {
    for (final IBattle battle : List.copyOf(pendingBattles)) {
      if (FinishedBattle.class.isAssignableFrom(battle.getClass())) {
        final FinishedBattle finished = (FinishedBattle) battle;
        finishedBattlesUnitAttackFromMap.put(
            finished.getTerritory(), finished.getAttackingFromMap());
        finished.fight(bridge);
      }
    }
  }

  void clearEmptyAirBattleAttacks(final IDelegateBridge bridge) {
    for (final IBattle battle : List.copyOf(pendingBattles)) {
      if (AirBattle.class.isAssignableFrom(battle.getClass())) {
        final AirBattle airBattle = (AirBattle) battle;
        airBattle.updateDefendingUnits();
        if (airBattle.getDefendingUnits().isEmpty()) {
          airBattle.finishBattleAndRemoveFromTrackerHeadless(bridge);
        }
      }
    }
  }

  public void undoBattle(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer player,
      final IDelegateBridge bridge) {
    for (final IBattle battle : List.copyOf(pendingBattles)) {
      if (!battle.getTerritory().equals(route.getStart())) {
        battle.removeAttack(route, units);
        if (battle.isEmpty()) {
          removeBattleForUndo(player, battle);
        }
      }
    }
    final RelationshipTracker relationshipTracker = bridge.getData().getRelationshipTracker();
    // if we have no longer conquered it, clear the blitz state
    // We must look at all territories, because we could have conquered the end territory if there
    // are no units there
    for (final Territory current : route.getAllTerritories()) {
      if (!relationshipTracker.isAllied(current.getOwner(), player)
          && conquered.contains(current)) {
        conquered.remove(current);
        blitzed.remove(current);
      }
    }
    // say they weren't in combat
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(ChangeFactory.unitPropertyChange(unit, false, Unit.PropertyName.WAS_IN_COMBAT));
    }
    bridge.addChange(change);
  }

  private void removeBattleForUndo(final GamePlayer player, final IBattle battle) {
    if (battleRecords != null) {
      battleRecords.removeBattle(player, battle.getBattleId());
    }
    pendingBattles.remove(battle);
    dependencies.remove(battle);
    for (final Collection<IBattle> battles : dependencies.values()) {
      battles.remove(battle);
    }
  }

  private void addBombingBattle(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer attacker,
      final GameData data,
      final Map<Unit, Set<Unit>> targets) {
    IBattle battle = getPendingBattle(route.getEnd(), BattleType.BOMBING_RAID);
    if (battle == null) {
      battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, this);
      pendingBattles.add(battle);
      getBattleRecords()
          .addBattle(attacker, battle.getBattleId(), route.getEnd(), battle.getBattleType());
    }
    final Change change = battle.addAttackChange(route, units, targets);
    // when state is moved to the game data, this will change
    if (!change.isEmpty()) {
      throw new IllegalStateException("Non empty change");
    }
    // don't let land battles in the same territory occur before bombing battles
    final IBattle dependent = getPendingBattle(route.getEnd(), BattleType.NORMAL);
    if (dependent != null) {
      addDependency(dependent, battle);
    }
    final IBattle dependentAirBattle = getPendingBattle(route.getEnd(), BattleType.AIR_BATTLE);
    if (dependentAirBattle != null) {
      addDependency(dependentAirBattle, battle);
    }
  }

  public void addBattle(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer gamePlayer,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute) {
    this.addBattle(
        route,
        units,
        false,
        gamePlayer,
        bridge,
        changeTracker,
        unitsNotUnloadedTilEndOfRoute,
        null,
        false);
  }

  public void addBattle(
      final Route route,
      final Collection<Unit> units,
      final boolean bombing,
      final GamePlayer gamePlayer,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute,
      final Map<Unit, Set<Unit>> targets,
      final boolean airBattleCompleted) {
    final GameData data = bridge.getData();
    if (bombing) {
      // create only either an air battle OR a bombing battle.
      // (the air battle will create a bombing battle when done, if needed)
      if (!airBattleCompleted
          && Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
          && AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(
              route.getEnd(), gamePlayer, data, true)) {
        addAirBattle(route, units, gamePlayer, data, BattleType.AIR_RAID);
      } else {
        addBombingBattle(route, units, gamePlayer, data, targets);
      }
      // say they were in combat
      markWasInCombat(units, bridge, changeTracker);
    } else {
      // create both an air battle and a normal battle
      if (!airBattleCompleted
          && Properties.getBattlesMayBePreceededByAirBattles(data.getProperties())
          && AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(
              route.getEnd(), gamePlayer, data, false)) {
        addAirBattle(
            route,
            CollectionUtils.getMatches(units, AirBattle.attackingGroundSeaBattleEscorts()),
            gamePlayer,
            data,
            BattleType.AIR_BATTLE);
      }
      final Change change = addMustFightBattleChange(route, units, gamePlayer, data);
      addChange(bridge, changeTracker, change);
      if (units.stream().anyMatch(Matches.unitIsLand().or(Matches.unitIsSea()))) {
        addEmptyBattle(
            route, units, gamePlayer, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute);
      }
    }
  }

  private static void markWasInCombat(
      final Collection<Unit> units,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker) {
    if (units == null) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      change.add(ChangeFactory.unitPropertyChange(unit, true, Unit.PropertyName.WAS_IN_COMBAT));
    }
    addChange(bridge, changeTracker, change);
  }

  private void addAirBattle(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer attacker,
      final GameData data,
      final BattleType battleType) {
    if (units.isEmpty()) {
      return;
    }
    IBattle battle = getPendingBattle(route.getEnd(), battleType);
    if (battle == null) {
      battle = new AirBattle(route.getEnd(), battleType, data, attacker, this);
      pendingBattles.add(battle);
      getBattleRecords()
          .addBattle(attacker, battle.getBattleId(), route.getEnd(), battle.getBattleType());
    }
    final Change change = battle.addAttackChange(route, units, null);
    // when state is moved to the game data, this will change
    if (!change.isEmpty()) {
      throw new IllegalStateException("Non empty change");
    }
    // don't let land battles in the same territory occur before bombing battles
    if (battleType.isBombingRun()) {
      final IBattle dependentAirBattle = getPendingBattle(route.getEnd(), BattleType.AIR_BATTLE);
      if (dependentAirBattle != null) {
        addDependency(dependentAirBattle, battle);
      }
    } else {
      final IBattle airRaid = getPendingBattle(route.getEnd(), BattleType.AIR_RAID);
      if (airRaid != null) {
        addDependency(battle, airRaid);
      }
      final IBattle raid = getPendingBattle(route.getEnd(), BattleType.BOMBING_RAID);
      if (raid != null) {
        addDependency(battle, raid);
      }
    }
    final IBattle dependent = getPendingBattle(route.getEnd(), BattleType.NORMAL);
    if (dependent != null) {
      addDependency(dependent, battle);
    }
  }

  /** No enemies. */
  private void addEmptyBattle(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer gamePlayer,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker,
      final Collection<Unit> unitsNotUnloadedTilEndOfRoute) {
    final GameData data = bridge.getData();
    final Collection<Unit> canConquer =
        CollectionUtils.getMatches(
            units,
            Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
                    units, gamePlayer, false)
                .negate());
    if (canConquer.stream().noneMatch(Matches.unitIsNotAir())) {
      return;
    }
    final Collection<Unit> presentFromStartTilEnd = new ArrayList<>(canConquer);
    if (unitsNotUnloadedTilEndOfRoute != null) {
      presentFromStartTilEnd.removeAll(unitsNotUnloadedTilEndOfRoute);
    }
    final boolean canConquerMiddleSteps =
        presentFromStartTilEnd.stream().anyMatch(Matches.unitIsNotAir());
    final boolean scramblingEnabled = Properties.getScrambleRulesInEffect(data.getProperties());
    final var passableLandAndNotRestricted =
        Matches.isTerritoryNotUnownedWaterAndCanBeTakenOverBy(gamePlayer)
            .or(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(gamePlayer));
    final Predicate<Territory> conquerable =
        Matches.territoryIsEmptyOfCombatUnits(gamePlayer).and(passableLandAndNotRestricted);
    final Collection<Territory> conqueredTerritories = new ArrayList<>();
    if (canConquerMiddleSteps) {
      conqueredTerritories.addAll(route.getMatches(conquerable));
      // in case we begin in enemy territory, and blitz out of it, check the first territory
      if (!route.getStart().equals(route.getEnd()) && conquerable.test(route.getStart())) {
        conqueredTerritories.add(route.getStart());
      }
    }
    // we handle the end of the route later
    conqueredTerritories.remove(route.getEnd());
    final Collection<Territory> blitzedTerritories =
        CollectionUtils.getMatches(conqueredTerritories, Matches.territoryIsBlitzable(gamePlayer));
    this.blitzed.addAll(
        CollectionUtils.getMatches(blitzedTerritories, Matches.isTerritoryEnemy(gamePlayer)));
    this.conquered.addAll(
        CollectionUtils.getMatches(conqueredTerritories, Matches.isTerritoryEnemy(gamePlayer)));
    for (final Territory current : conqueredTerritories) {
      IBattle nonFight = getPendingBattle(current, BattleType.NORMAL);
      // TODO: if we ever want to scramble to a blitzed territory, then we need to fix this
      if (nonFight == null) {
        nonFight =
            new FinishedBattle(
                current,
                gamePlayer,
                this,
                BattleType.NORMAL,
                data,
                BattleRecord.BattleResultDescription.CONQUERED,
                WhoWon.ATTACKER);
        pendingBattles.add(nonFight);
        getBattleRecords()
            .addBattle(gamePlayer, nonFight.getBattleId(), current, nonFight.getBattleType());
      }
      final Change change = nonFight.addAttackChange(route, units, null);
      addChange(bridge, changeTracker, change);
      takeOver(current, gamePlayer, bridge, changeTracker, units);
    }
    // check the last territory
    if (conquerable.test(route.getEnd())) {
      IBattle precede = getDependentAmphibiousAssault(route);
      if (precede == null) {
        precede = getPendingBombingBattle(route.getEnd());
      }
      // if we have a preceding battle, then we must use a non-fighting-battle
      // if we have scrambling on, and this is an amphibious attack,
      // we may wish to scramble to kill the transports, so must use non-fighting-battle also
      if (precede != null || (scramblingEnabled && route.isUnload() && route.hasExactlyOneStep())) {
        IBattle nonFight = getPendingBattle(route.getEnd(), BattleType.NORMAL);
        if (nonFight == null) {
          nonFight = new NonFightingBattle(route.getEnd(), gamePlayer, this, data);
          pendingBattles.add(nonFight);
          getBattleRecords()
              .addBattle(
                  gamePlayer, nonFight.getBattleId(), route.getEnd(), nonFight.getBattleType());
        }
        final Change change = nonFight.addAttackChange(route, units, null);
        addChange(bridge, changeTracker, change);
        if (precede != null) {
          addDependency(nonFight, precede);
        }
      } else {
        if (Matches.isTerritoryEnemy(gamePlayer).test(route.getEnd())) {
          if (Matches.territoryIsBlitzable(gamePlayer).test(route.getEnd())) {
            this.blitzed.add(route.getEnd());
          }
          this.conquered.add(route.getEnd());
        }
        IBattle nonFight = getPendingBattle(route.getEnd(), BattleType.NORMAL);
        if (nonFight == null) {
          nonFight =
              new FinishedBattle(
                  route.getEnd(),
                  gamePlayer,
                  this,
                  BattleType.NORMAL,
                  data,
                  BattleRecord.BattleResultDescription.CONQUERED,
                  WhoWon.ATTACKER);
          pendingBattles.add(nonFight);
          getBattleRecords()
              .addBattle(
                  gamePlayer, nonFight.getBattleId(), route.getEnd(), nonFight.getBattleType());
        }
        final Change change = nonFight.addAttackChange(route, units, null);
        addChange(bridge, changeTracker, change);
        takeOver(route.getEnd(), gamePlayer, bridge, changeTracker, units);
      }
    }
    // TODO: else what?
  }

  /**
   * Changes ownership of the specified territory to the specified player as a result of the
   * territory being taken over after a battle.
   */
  public void takeOver(
      final Territory territory,
      final GamePlayer gamePlayer,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker,
      final @Nullable Collection<Unit> arrivingUnits) {
    // This could be NULL if unowned water
    final Optional<TerritoryAttachment> optionalTerritoryAttachment =
        TerritoryAttachment.get(territory);
    if (optionalTerritoryAttachment.isEmpty()) {
      // TODO: allow capture/destroy of infrastructure on unowned water
      return;
    }
    final TerritoryAttachment territoryAttachment = optionalTerritoryAttachment.get();
    final GameData data = bridge.getData();
    final Collection<Unit> arrivedUnits =
        (arrivingUnits == null ? null : new ArrayList<>(arrivingUnits));
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final boolean isTerritoryOwnerAnEnemy =
        relationshipTracker.canTakeOverOwnedTerritory(gamePlayer, territory.getOwner());
    // If this is a convoy (we wouldn't be in this method otherwise)
    // check to make sure attackers have more than just transports. If they don't, exit here.
    if (territory.isWater()
        && arrivedUnits != null
        && getAllAttachingSeaUnits(arrivedUnits, data) == 0) {
      return;
    }
    // If it was a Convoy Route - check ownership of the associated neighboring territory and set
    // message
    if (territoryAttachment.getConvoyRoute()) {
      writeHistoryOnTakeOverForConvoyRoute(territory, gamePlayer, bridge);
    }
    // if neutral, we may charge money to enter
    if (territory.getOwner().isNull()
        && !territory.isWater()
        && Properties.getNeutralCharge(data.getProperties()) >= 0) {
      addChangeChargeForEnteringNeutrals(territory, gamePlayer, bridge, changeTracker);
    }
    // if it's a capital we take the money
    // NOTE: this is not checking to see if it is an enemy.
    // instead it is relying on the fact that the capital should be owned by the person it is
    // attached to
    if (isTerritoryOwnerAnEnemy && territoryAttachment.isCapital()) {
      addChangesOnTakeOverCapitol(
          territory, territoryAttachment, gamePlayer, bridge, changeTracker);
    }
    // is this an allied territory? Revert to original owner if it is,
    // unless they don't own their capital
    final Optional<GamePlayer> optionalTerrOrigOwner =
        OriginalOwnerTracker.getOriginalOwner(territory);
    final boolean isTerritoryOrigOwnerAllied;
    isTerritoryOrigOwnerAllied =
        optionalTerrOrigOwner
            .filter(player -> relationshipTracker.isAllied(player, gamePlayer))
            .isPresent();
    GamePlayer newOwner =
        // if the original owner is the current owner, and the current owner is our enemy or
        // canTakeOver, then we do not worry about this.
        (isTerritoryOwnerAnEnemy
                && isTerritoryOrigOwnerAllied
                && !optionalTerrOrigOwner.get().equals(territory.getOwner()))
            ? getNewOwnerForTakeOver(territory, gamePlayer, optionalTerrOrigOwner.get(), data)
            : gamePlayer;
    // if we have specially set this territory to have whenCapturedByGoesTo,
    // then we set that here (except we don't set it if we are liberating allied owned territory)
    if (isTerritoryOwnerAnEnemy
        && newOwner.equals(gamePlayer)
        && Matches.territoryHasCaptureOwnershipChanges().test(territory)) {
      newOwner =
          territoryAttachment.getCaptureOwnershipChanges().stream()
              .filter(
                  captureOwnershipChange ->
                      !captureOwnershipChange.capturingPlayer.equals(
                          captureOwnershipChange.receivingPlayer))
              .filter(
                  captureOwnershipChange ->
                      captureOwnershipChange.capturingPlayer.equals(gamePlayer))
              .findFirst()
              .map(captureOwnershipChange -> captureOwnershipChange.receivingPlayer)
              .orElse(newOwner);
    }
    if (isTerritoryOwnerAnEnemy) {
      addChangeChangeOwnership(
          territory,
          newOwner,
          territoryAttachment,
          gamePlayer,
          bridge,
          changeTracker,
          arrivedUnits);
    }
    // Remove any bombing raids against captured territory
    if (territory.anyUnitsMatch(
        Matches.unitIsEnemyOf(gamePlayer).and(Matches.unitCanBeDamaged()))) {
      final IBattle bombingBattle = getPendingBombingBattle(territory);
      // Only throw an error if the battle is not empty. An empty one could legitimately happen when
      // you send a unit to bomb somewhere, but then move it again out of the battle site.
      if (bombingBattle != null && !bombingBattle.isEmpty()) {
        throw new IllegalStateException(BOMBING_DEPENDENCY_ERROR);
      }
    }
    captureOrDestroyUnits(territory, gamePlayer, newOwner, bridge, changeTracker);
    // is this territory our capitol or a capitol of our ally
    // Also check to make sure playerAttachment even HAS a capital to fix abend
    if (isTerritoryOwnerAnEnemy
        && isTerritoryOrigOwnerAllied
        && territoryAttachment.getCapital().isPresent()
        && TerritoryAttachment.getAllCapitals(optionalTerrOrigOwner.get(), data.getMap())
            .contains(territory)) {
      addChangesOnTakeOverAlliedCapitol(optionalTerrOrigOwner.get(), bridge, changeTracker);
    }
    // say they were in combat
    // if the territory being taken over is water, then do not say any land units were in combat
    // (they may want to unload from the transport and attack)
    if (Matches.territoryIsWater().test(territory) && arrivedUnits != null) {
      arrivedUnits.removeAll(CollectionUtils.getMatches(arrivedUnits, Matches.unitIsLand()));
    }
    markWasInCombat(arrivedUnits, bridge, changeTracker);
  }

  private static GamePlayer getNewOwnerForTakeOver(
      Territory territory, GamePlayer gamePlayer, GamePlayer terrOrigOwner, GameData data) {
    GamePlayer newOwner = gamePlayer;
    final List<Territory> capitalsListOwned =
        TerritoryAttachment.getAllCurrentlyOwnedCapitals(terrOrigOwner, data.getMap());
    if (!capitalsListOwned.isEmpty()) {
      newOwner = terrOrigOwner;
    } else { // hence newOwner = gamePlayer;
      for (final Territory current :
          TerritoryAttachment.getAllCapitals(terrOrigOwner, data.getMap())) {
        if (territory.equals(current) || current.getOwner().isNull()) {
          // if a neutral controls our capital, our territories get liberated (ie: china in ww2v3)
          newOwner = terrOrigOwner;
          break;
        }
      }
    }
    return newOwner;
  }

  private static void addChangesOnTakeOverCapitol(
      Territory territory,
      TerritoryAttachment territoryAttachment,
      GamePlayer gamePlayer,
      IDelegateBridge bridge,
      @Nullable UndoableMove changeTracker) {
    GameData data = bridge.getData();
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    // if the capital is owned by the capitols player take the money
    final GamePlayer whoseCapital =
        data.getPlayerList().getPlayerId(territoryAttachment.getCapitalOrThrow());
    final PlayerAttachment pa = PlayerAttachment.get(gamePlayer);
    final PlayerAttachment paWhoseCapital = PlayerAttachment.get(whoseCapital);
    final List<Territory> capitalsList =
        TerritoryAttachment.getAllCurrentlyOwnedCapitals(whoseCapital, data.getMap());
    // we are losing one right now, so it is < not <=
    if (paWhoseCapital != null && paWhoseCapital.getRetainCapitalNumber() < capitalsList.size()) {
      // do nothing, we keep our money since we still control enough capitals
      historyWriter.addChildToEvent(
          gamePlayer.getName() + " captures one of " + whoseCapital.getName() + " capitals");
    } else if (whoseCapital.equals(territory.getOwner())) {
      final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
      final int capturedPuCount = whoseCapital.getResources().getQuantity(pus);
      if (pa != null && Properties.getPacificTheater(data.getProperties())) {
        final Change changeVp =
            ChangeFactory.attachmentPropertyChange(
                pa, (capturedPuCount + pa.getCaptureVps()), "captureVps");
        addChange(bridge, changeTracker, changeVp);
      }
      final Change remove =
          ChangeFactory.changeResourcesChange(whoseCapital, pus, -capturedPuCount);
      addChange(bridge, changeTracker, remove);
      if (paWhoseCapital != null && paWhoseCapital.getDestroysPUs()) {
        historyWriter.addChildToEvent(
            gamePlayer.getName()
                + " destroys "
                + capturedPuCount
                + MyFormatter.pluralize("PU", capturedPuCount)
                + " while taking "
                + whoseCapital.getName()
                + " capital");
      } else {
        historyWriter.addChildToEvent(
            gamePlayer.getName()
                + " captures "
                + capturedPuCount
                + MyFormatter.pluralize("PU", capturedPuCount)
                + " while taking "
                + whoseCapital.getName()
                + " capital");
        final Change add = ChangeFactory.changeResourcesChange(gamePlayer, pus, capturedPuCount);
        addChange(bridge, changeTracker, add);
      }
      // remove all the tokens of the captured player if tokens are used
      if (data.getResourceList().getResourceOptional(Constants.TECH_TOKENS).isPresent()) {
        final Resource tokens = data.getResourceList().getResourceOrThrow(Constants.TECH_TOKENS);
        final int currTokens = whoseCapital.getResources().getQuantity(Constants.TECH_TOKENS);
        final Change removeTokens =
            ChangeFactory.changeResourcesChange(whoseCapital, tokens, -currTokens);
        addChange(bridge, changeTracker, removeTokens);
      }
    }
  }

  private static void addChangeChargeForEnteringNeutrals(
      Territory territory,
      GamePlayer gamePlayer,
      IDelegateBridge bridge,
      @Nullable UndoableMove changeTracker) {
    GameData data = bridge.getData();
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    final int puChargeIdeal = -Properties.getNeutralCharge(data.getProperties());
    final int puChargeReal =
        Math.min(0, Math.max(puChargeIdeal, -gamePlayer.getResources().getQuantity(pus)));
    final Change neutralFee = ChangeFactory.changeResourcesChange(gamePlayer, pus, puChargeReal);
    addChange(bridge, changeTracker, neutralFee);
    if (puChargeIdeal == puChargeReal) {
      historyWriter.addChildToEvent(
          gamePlayer.getName()
              + " loses "
              + -puChargeReal
              + " "
              + MyFormatter.pluralize("PU", -puChargeReal)
              + " for violating "
              + territory.getName()
              + "s neutrality.");
    } else {
      log.error(
          "Player, "
              + gamePlayer.getName()
              + " attacks a Neutral territory, and should have had to pay "
              + puChargeIdeal
              + ", but did not have enough PUs to pay! This is a bug.");
      historyWriter.addChildToEvent(
          gamePlayer.getName()
              + " loses "
              + -puChargeReal
              + " "
              + MyFormatter.pluralize("PU", -puChargeReal)
              + " for violating "
              + territory.getName()
              + "s neutrality.  Correct amount to charge is: "
              + puChargeIdeal
              + ".  Player should not have been able to make this attack!");
    }
  }

  private static void writeHistoryOnTakeOverForConvoyRoute(
      Territory territory, GamePlayer newOwner, final IDelegateBridge bridge) {
    GameData data = bridge.getData();
    RelationshipTracker relationshipTracker = bridge.getData().getRelationshipTracker();
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    // we could be part of a convoy route for another territory
    final Collection<Territory> attachedConvoyTo =
        TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data);
    attachedConvoyTo.forEach(
        convoy -> {
          final Optional<TerritoryAttachment> optionalConvoyTerritoryAttachment =
              TerritoryAttachment.get(convoy);
          if (optionalConvoyTerritoryAttachment.isEmpty()) {
            return;
          }
          final TerritoryAttachment cta = optionalConvoyTerritoryAttachment.get();
          if (!cta.getConvoyRoute()) {
            return;
          }
          final GamePlayer convoyOwner = convoy.getOwner();
          if (relationshipTracker.isAllied(newOwner, convoyOwner)) {
            if (cta.getConvoyAttached().stream()
                .noneMatch(Matches.isTerritoryAllied(convoyOwner))) {
              historyWriter.addChildToEvent(
                  convoyOwner.getName()
                      + " gains "
                      + cta.getProduction()
                      + " production in "
                      + convoy.getName()
                      + " for the liberation the convoy route in "
                      + territory.getName());
            }
          } else if (relationshipTracker.isAtWar(newOwner, convoyOwner)
              && CollectionUtils.countMatches(
                      cta.getConvoyAttached(), Matches.isTerritoryAllied(convoyOwner))
                  == 1) {
            historyWriter.addChildToEvent(
                convoyOwner.getName()
                    + " loses "
                    + cta.getProduction()
                    + " production in "
                    + convoy.getName()
                    + " due to the capture of the convoy route in "
                    + territory.getName());
          }
        });
  }

  private static int getAllAttachingSeaUnits(Collection<Unit> arrivedUnits, GameData data) {
    // Total Attacking Sea units = all units - land units - air units - submerged subs
    // Also subtract transports & subs (if they can't control sea zones)
    int totalMatches =
        arrivedUnits.size()
            - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsLand())
            - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsAir())
            - CollectionUtils.countMatches(arrivedUnits, Matches.unitIsSubmerged());
    // If transports are restricted from controlling sea zones, subtract them
    final Predicate<Unit> transportsCanNotControl =
        Matches.unitIsSeaTransportAndNotDestroyer()
            .and(Matches.unitIsSeaTransportButNotCombatSeaTransport());
    if (!Properties.getTransportControlSeaZone(data.getProperties())) {
      totalMatches -= CollectionUtils.countMatches(arrivedUnits, transportsCanNotControl);
    }
    // TODO check if istrn and NOT isDD
    // If subs are restricted from controlling sea zones, subtract them
    if (Properties.getSubControlSeaZoneRestricted(data.getProperties())) {
      totalMatches -=
          CollectionUtils.countMatches(arrivedUnits, Matches.unitCanBeMovedThroughByEnemies());
    }
    return totalMatches;
  }

  private static void addChangesOnTakeOverAlliedCapitol(
      GamePlayer terrOrigOwner, IDelegateBridge bridge, @Nullable UndoableMove changeTracker) {
    final GameData data = bridge.getData();
    // give it back to the original owner, if ally
    final Collection<Territory> originallyOwned =
        OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
    final List<Territory> alliedTerritories =
        CollectionUtils.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner));
    for (final Territory alliedTerritory : alliedTerritories) {
      if (alliedTerritory.isOwnedBy(terrOrigOwner)) {
        continue;
      }
      final Change takeOverFriendlyTerritories =
          ChangeFactory.changeOwner(alliedTerritory, terrOrigOwner);
      addChange(bridge, changeTracker, takeOverFriendlyTerritories);
      bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
      // give back the factories as well
      final Collection<Unit> infrastructureUnits =
          CollectionUtils.getMatches(alliedTerritory.getUnits(), Matches.unitIsInfrastructure());
      if (!infrastructureUnits.isEmpty()) {
        final Change takeOverNonComUnits =
            ChangeFactory.changeOwner(infrastructureUnits, terrOrigOwner, alliedTerritory);
        addChange(bridge, changeTracker, takeOverNonComUnits);
      }
    }
  }

  private void addChangeChangeOwnership(
      Territory territory,
      GamePlayer newOwner,
      TerritoryAttachment territoryAttachment,
      GamePlayer gamePlayer,
      IDelegateBridge bridge,
      @Nullable UndoableMove changeTracker,
      Collection<Unit> arrivedUnits) {
    final Change takeOver = ChangeFactory.changeOwner(territory, newOwner);
    bridge.getHistoryWriter().addChildToEvent(takeOver.toString());
    addChange(bridge, changeTracker, takeOver);
    territory.notifyChanged();
    if (changeTracker != null) {
      changeTracker.addToConquered(territory);
    }
    // play a sound
    ISound broadcaster = bridge.getSoundChannelBroadcaster();
    if (territory.isWater()) {
      // should probably see if there is something actually happening for water
      broadcaster.playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, gamePlayer);
    } else if (territoryAttachment.getCapital().isPresent()) {
      broadcaster.playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, gamePlayer);
    } else if (blitzed.contains(territory)
        && arrivedUnits != null
        && arrivedUnits.stream().anyMatch(Matches.unitCanBlitz())) {
      broadcaster.playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, gamePlayer);
    } else {
      broadcaster.playSoundForAll(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, gamePlayer);
    }
  }

  /**
   * Called when a territory is conquered to determine if remaining enemy units should be captured,
   * destroyed, or take damage.
   */
  public static void captureOrDestroyUnits(
      final Territory territory,
      final GamePlayer gamePlayer,
      final GamePlayer newOwner,
      final IDelegateBridge bridge,
      final @Nullable UndoableMove changeTracker) {
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    final GameState data = bridge.getData();
    // destroy any units that should be destroyed on capture
    if (Properties.getUnitsCanBeDestroyedInsteadOfCaptured(data.getProperties())) {
      final Predicate<Unit> enemyToBeDestroyed =
          Matches.enemyUnit(gamePlayer).and(Matches.unitDestroyedWhenCapturedByOrFrom(gamePlayer));
      final Collection<Unit> destroyed =
          territory.getUnitCollection().getMatches(enemyToBeDestroyed);
      if (!destroyed.isEmpty()) {
        historyWriter.addChildToEvent("Some non-combat units are destroyed: ", destroyed);
        addChange(bridge, changeTracker, ChangeFactory.removeUnits(territory, destroyed));
      }
    }
    // destroy any capture on entering units, IF the property to destroy them instead of capture is
    // turned on
    if (Properties.getOnEnteringUnitsDestroyedInsteadOfCaptured(data.getProperties())) {
      final Collection<Unit> destroyed =
          territory
              .getUnitCollection()
              .getMatches(Matches.unitCanBeCapturedOnEnteringThisTerritory(gamePlayer, territory));
      if (!destroyed.isEmpty()) {
        historyWriter.addChildToEvent(
            gamePlayer.getName() + " destroys some units instead of capturing them", destroyed);
        addChange(bridge, changeTracker, ChangeFactory.removeUnits(territory, destroyed));
      }
    }
    // destroy any disabled units owned by the enemy that are NOT infrastructure or factories
    final Predicate<Unit> enemyToBeDestroyed =
        Matches.enemyUnit(gamePlayer)
            .and(Matches.unitIsDisabled())
            .and(Matches.unitIsInfrastructure().negate());
    final Collection<Unit> destroyed = territory.getUnitCollection().getMatches(enemyToBeDestroyed);
    if (!destroyed.isEmpty()) {
      historyWriter.addChildToEvent(
          gamePlayer.getName() + " destroys some disabled combat units", destroyed);
      addChange(bridge, changeTracker, ChangeFactory.removeUnits(territory, destroyed));
    }
    // take over non-combatants
    final Predicate<Unit> enemyNonCom =
        Matches.enemyUnit(gamePlayer).and(Matches.unitIsInfrastructure());
    final Predicate<Unit> willBeCaptured =
        enemyNonCom.or(Matches.unitCanBeCapturedOnEnteringThisTerritory(gamePlayer, territory));
    final Collection<Unit> nonCom = territory.getUnitCollection().getMatches(willBeCaptured);
    // change any units that change unit types on capture
    if (Properties.getUnitsCanBeChangedOnCapture(data.getProperties())) {
      final Collection<Unit> toReplace =
          CollectionUtils.getMatches(
              nonCom, Matches.unitWhenCapturedChangesIntoDifferentUnitType());
      for (final Unit u : toReplace) {
        final Map<String, Tuple<String, IntegerMap<UnitType>>> map =
            u.getUnitAttachment().getWhenCapturedChangesInto();
        final GamePlayer currentOwner = u.getOwner();
        for (Map.Entry<String, Tuple<String, IntegerMap<UnitType>>> mapEntry :
            Collections.unmodifiableSet(map.entrySet())) {
          final String[] s =
              Iterables.toArray(Splitter.on(':').split(mapEntry.getKey()), String.class);
          if (!(s[0].equals("any")
              || data.getPlayerList().getPlayerId(s[0]).equals(currentOwner))) {
            continue;
          }
          // we could use "id" or "newOwner" here... not sure which to use
          if (!(s[1].equals("any") || data.getPlayerList().getPlayerId(s[1]).equals(gamePlayer))) {
            continue;
          }
          final CompositeChange changes = new CompositeChange();
          final Collection<Unit> toAdd = new ArrayList<>();
          final Tuple<String, IntegerMap<UnitType>> toCreate = mapEntry.getValue();
          final boolean translateAttributes = toCreate.getFirst().equalsIgnoreCase("true");
          for (final UnitType ut : toCreate.getSecond().keySet()) {
            toAdd.addAll(ut.create(toCreate.getSecond().getInt(ut), newOwner));
          }
          if (!toAdd.isEmpty()) {
            if (translateAttributes) {
              final Change translate =
                  UnitUtils.translateAttributesToOtherUnits(u, toAdd, territory);
              if (!translate.isEmpty()) {
                changes.add(translate);
              }
            }
            changes.add(ChangeFactory.removeUnits(territory, Set.of(u)));
            changes.add(ChangeFactory.addUnits(territory, toAdd));
            changes.add(ChangeFactory.markNoMovementChange(toAdd));
            historyWriter.addChildToEvent(
                gamePlayer.getName() + " converts " + u.toStringNoOwner() + " into different units",
                toAdd);
            addChange(bridge, changeTracker, changes);
            // don't forget to remove this unit from the list
            nonCom.remove(u);
            break;
          }
        }
      }
    }
    if (!nonCom.isEmpty()) {
      // FYI: a dummy delegate will not do anything with this change,
      // meaning that the battle calculator will think this unit lived, even though it died or was
      // captured, etc.!
      addChange(bridge, changeTracker, ChangeFactory.changeOwner(nonCom, newOwner, territory));
      addChange(bridge, changeTracker, ChangeFactory.markNoMovementChange(nonCom));
      final IntegerMap<Unit> damageMap = new IntegerMap<>();
      for (final Unit unit :
          CollectionUtils.getMatches(nonCom, Matches.unitWhenCapturedSustainsDamage())) {
        final int damageLimit = unit.getHowMuchMoreDamageCanThisUnitTake(territory);
        final int sustainedDamage = unit.getUnitAttachment().getWhenCapturedSustainsDamage();
        final int actualDamage = Math.max(0, Math.min(sustainedDamage, damageLimit));
        final int totalDamage = unit.getUnitDamage() + actualDamage;
        damageMap.put(unit, totalDamage);
      }
      if (!damageMap.isEmpty()) {
        final Change damageChange = ChangeFactory.bombingUnitDamage(damageMap, List.of(territory));
        addChange(bridge, changeTracker, damageChange);
        // Kill any units that can die if they have reached max damage
        final List<Unit> unitsCanDie =
            CollectionUtils.getMatches(
                damageMap.keySet(),
                Matches.unitCanDieFromReachingMaxDamage()
                    .and(Matches.unitIsAtMaxDamageOrNotCanBeDamaged(territory)));
        if (!unitsCanDie.isEmpty()) {
          addChange(bridge, changeTracker, ChangeFactory.removeUnits(territory, unitsCanDie));
        }
      }
    }
  }

  private static void addChange(
      IDelegateBridge bridge, @Nullable UndoableMove changeTracker, Change change) {
    bridge.addChange(change);
    if (changeTracker != null) {
      changeTracker.addChange(change);
    }
  }

  private Change addMustFightBattleChange(
      final Route route,
      final Collection<Unit> units,
      final GamePlayer gamePlayer,
      final GameData data) {
    // it is possible to add a battle with a route that is just the start territory, ie the units
    // did not move into the country they were there to start with
    // this happens when you have submerged subs emerging
    Territory site = route.getEnd();
    if (site == null) {
      site = route.getStart();
    }
    // this will be taken care of by the non fighting battle
    if (!Matches.territoryHasEnemyUnits(gamePlayer).test(site)) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    // if just an enemy factory &/or AA then no battle
    final Collection<Unit> enemyUnits =
        CollectionUtils.getMatches(site.getUnits(), Matches.enemyUnit(gamePlayer));
    if (route.getEnd() != null
        && !enemyUnits.isEmpty()
        && enemyUnits.stream().allMatch(Matches.unitIsInfrastructure())) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    IBattle battle = getPendingBattle(site, BattleType.NORMAL);
    // If there are no pending - add one for units already in the combat zone
    if (battle == null) {
      battle = new MustFightBattle(site, gamePlayer, data, this);
      pendingBattles.add(battle);
      getBattleRecords().addBattle(gamePlayer, battle.getBattleId(), site, battle.getBattleType());
    }
    // Add the units that moved into the battle
    final Change change = battle.addAttackChange(route, units, null);
    // make amphibious assaults dependent on possible naval invasions
    // it's only a dependency if we are unloading
    final IBattle precede = getDependentAmphibiousAssault(route);
    if (precede != null && units.stream().anyMatch(Matches.unitIsLand())) {
      addDependency(battle, precede);
    }
    // don't let land battles in the same territory occur before bombing battles
    final IBattle bombing = getPendingBombingBattle(route.getEnd());
    if (bombing != null) {
      addDependency(battle, bombing);
    }
    final IBattle airBattle = getPendingBattle(route.getEnd(), BattleType.AIR_BATTLE);
    if (airBattle != null) {
      addDependency(battle, airBattle);
    }
    return change;
  }

  private IBattle getDependentAmphibiousAssault(final Route route) {
    if (!route.isUnload()) {
      return null;
    }
    return getPendingBattle(route.getStart(), BattleType.NORMAL);
  }

  @Nullable
  public IBattle getPendingBombingBattle(final Territory t) {
    return BattleType.bombingBattleTypes().stream()
        .map(type -> getPendingBattle(t, type))
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);
  }

  public Collection<IBattle> getPendingBattles(BattleType type) {
    return CollectionUtils.getMatches(
        pendingBattles, b -> !b.isEmpty() && b.getBattleType() == type);
  }

  public Collection<IBattle> getPendingBattles(final Territory t) {
    return CollectionUtils.getMatches(pendingBattles, b -> b.getTerritory().equals(t));
  }

  public boolean hasPendingNonBombingBattle(final Territory t) {
    return getPendingNonBombingBattle(t) != null;
  }

  @Nullable
  public IBattle getPendingNonBombingBattle(final Territory t) {
    return BattleType.nonBombingBattleTypes().stream()
        .map(type -> getPendingBattle(t, type))
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);
  }

  @Nullable
  public IBattle getPendingBattle(final Territory t, final BattleType type) {
    return pendingBattles.stream()
        .filter(b -> b.getBattleType().equals(type) && b.getTerritory().equals(t))
        .findFirst()
        .orElse(null);
  }

  public IBattle getPendingBattle(final UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return pendingBattles.stream().filter(b -> b.getBattleId().equals(uuid)).findAny().orElse(null);
  }

  /** Returns a collection of territories where no bombing battles are pending. */
  public Collection<Territory> getPendingBattleSitesWithoutBombing() {
    return getPendingBattleSites(false);
  }

  /** Returns a collection of territories where bombing battles are pending. */
  public Collection<Territory> getPendingBattleSitesWithBombing() {
    return getPendingBattleSites(true);
  }

  /**
   * Returns a collection of territories where battles are pending.
   *
   * @param bombing whether only battles where there is bombing or where there is no bombing.
   */
  private Collection<Territory> getPendingBattleSites(final boolean bombing) {
    return pendingBattles.stream()
        .filter(b -> !b.isEmpty() && b.getBattleType().isBombingRun() == bombing)
        .map(IBattle::getTerritory)
        .collect(Collectors.toSet());
  }

  public BattleListing getBattleListingFromPendingBattles() {
    return new BattleListing(pendingBattles);
  }

  /**
   * Returns the battle that must occur before dependent can occur.
   *
   * @param blocked the battle that is blocked.
   */
  public Collection<IBattle> getDependentOn(final IBattle blocked) {
    final Collection<IBattle> dependent = dependencies.get(blocked);
    if (dependent == null) {
      return List.of();
    }
    return CollectionUtils.getMatches(dependent, Matches.battleIsEmpty().negate());
  }

  /**
   * Returns the battles that cannot occur until the given battle occurs.
   *
   * @param blocking the battle that is blocking the other battles.
   */
  public Collection<IBattle> getBlocked(final IBattle blocking) {
    return dependencies.keySet().stream()
        .filter(current -> getDependentOn(current).contains(blocking))
        .collect(Collectors.toList());
  }

  public void addDependency(final IBattle blocked, final IBattle blocking) {
    dependencies.computeIfAbsent(blocked, k -> new HashSet<>()).add(blocking);
  }

  private void removeDependency(final IBattle blocked, final IBattle blocking) {
    final Collection<IBattle> dependenciesOfBlocked = this.dependencies.get(blocked);
    dependenciesOfBlocked.remove(blocking);
    if (dependenciesOfBlocked.isEmpty()) {
      this.dependencies.remove(blocked);
    }
  }

  /** Remove battle from pending list, dependencies, and clear current battle. */
  public void removeBattle(final IBattle battle, final GameData data) {
    if (battle != null) {
      for (final IBattle current : getBlocked(battle)) {
        removeDependency(current, battle);
      }
      pendingBattles.remove(battle);
      foughtBattles.add(battle.getTerritory());
      try {
        data.getBattleDelegate().clearCurrentBattle(battle);
      } catch (final IllegalStateException e) {
        // ignore as can't find battle delegate
      }
    }
  }

  public void clear() {
    finishedBattlesUnitAttackFromMap.clear();
    pendingBattles.clear();
    blitzed.clear();
    foughtBattles.clear();
    conquered.clear();
    dependencies.clear();
    defendingAirThatCanNotLand.clear();
    noBombardAllowed.clear();
    relationshipChangesThisTurn.clear();
  }

  void addToDefendingAirThatCanNotLand(
      final Collection<Unit> units, final Territory szTerritoryTheyAreIn) {
    Collection<Unit> current = defendingAirThatCanNotLand.get(szTerritoryTheyAreIn);
    if (current == null) {
      current = new ArrayList<>();
    }
    current.addAll(units);
    defendingAirThatCanNotLand.put(szTerritoryTheyAreIn, current);
  }

  public void clearBattleRecords() {
    if (battleRecords != null) {
      battleRecords.clear();
      battleRecords = null;
    }
  }

  public BattleRecords getBattleRecords() {
    if (battleRecords == null) {
      battleRecords = new BattleRecords();
    }
    return battleRecords;
  }

  void sendBattleRecordsToGameData(final IDelegateBridge bridge) {
    if (battleRecords != null && !battleRecords.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Recording Battle Statistics");
      bridge.addChange(ChangeFactory.addBattleRecords(battleRecords, bridge.getData()));
    }
  }

  /**
   * 'Auto-fight' all the air battles and strategic bombing runs. Auto fight means we automatically
   * begin the fight without user action. This is to avoid clicks during the air battle and SBR
   * phase, and to enforce game rules that these phases are fought first before any other combat.
   */
  void fightAirRaidsAndStrategicBombing(final IDelegateBridge delegateBridge) {
    fightAirRaidsAndStrategicBombing(
        delegateBridge, this::getPendingBattleSitesWithBombing, this::getPendingBattle);
  }

  @VisibleForTesting
  void fightAirRaidsAndStrategicBombing(
      final IDelegateBridge delegateBridge,
      final Supplier<Collection<Territory>> pendingBattleSiteSupplier,
      final BiFunction<Territory, BattleType, IBattle> pendingBattleFunction) {
    // First we'll fight all the air battles (air raids)
    // Then we will have a wave of battles for the SBR. AA guns will shoot, and we'll roll for
    // damage.
    // CAUTION: air raid battles when completed will potentially spawn new bombing raids, hence
    // the user of a Supplier for the param. Would be good to refactor that out, in the meantime be
    // aware there are mass side effects in these calls...

    for (final Territory t : pendingBattleSiteSupplier.get()) {
      final IBattle airRaid = pendingBattleFunction.apply(t, BattleType.AIR_RAID);
      if (airRaid != null) {
        airRaid.fight(delegateBridge);
      }
    }

    // now that we've done all the air battles, do all the SBRs as a second wave.
    for (final Territory t : pendingBattleSiteSupplier.get()) {
      final IBattle bombingRaid = pendingBattleFunction.apply(t, BattleType.BOMBING_RAID);
      if (bombingRaid != null) {
        bombingRaid.fight(delegateBridge);
      }
    }
  }

  /**
   * Kill undefended transports. Done first to remove potentially dependent sea battles Which could
   * block amphibious assaults later
   */
  public void fightDefenselessBattles(final IDelegateBridge bridge) {
    // Here and below parameter "false" to getPendingBattleSites & getPendingBattle denote non-SBR
    // battles
    for (final IBattle battle : getPendingBattles(BattleType.NORMAL)) {
      final Territory territory = battle.getTerritory();
      final Collection<Unit> defenders = battle.getDefendingUnits();
      final List<Unit> possibleDefenders = getPossibleDefendingUnits(territory, defenders);
      if (getDependentOn(battle).isEmpty()
          && PowerStrengthAndRolls.build(
                      possibleDefenders,
                      CombatValueBuilder.mainCombatValue()
                          .enemyUnits(defenders)
                          .friendlyUnits(possibleDefenders)
                          .side(BattleState.Side.DEFENSE)
                          .gameSequence(bridge.getData().getSequence())
                          .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                          .lhtrHeavyBombers(
                              Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                          .gameDiceSides(bridge.getData().getDiceSides())
                          .territoryEffects(TerritoryEffectHelper.getEffects(territory))
                          .build())
                  .calculateTotalPower()
              == 0) {
        battle.fight(bridge);
      }
    }
    getPendingBattles(BattleType.NORMAL).stream()
        .filter(NonFightingBattle.class::isInstance)
        .filter(battle -> getDependentOn(battle).isEmpty())
        .forEach(battle -> battle.fight(bridge));
  }

  private static List<Unit> getPossibleDefendingUnits(
      final Territory territory, final Collection<Unit> defenders) {
    return CollectionUtils.getMatches(
        defenders, Matches.unitCanBeInBattle(false, !territory.isWater(), 1, true));
  }

  /** Fight battle automatically if there is only one left to pick from. */
  public void fightBattleIfOnlyOne(final IDelegateBridge bridge) {
    final Collection<IBattle> battles = getPendingBattles(BattleType.NORMAL);
    if (battles.size() == 1) {
      final var battle = CollectionUtils.getAny(battles);
      if (getDependentOn(battle).isEmpty()) {
        battle.fight(bridge);
      }
    }
  }

  @Override
  public String toString() {
    return "BattleTracker:"
        + "\n"
        + "Conquered:"
        + conquered
        + "\n"
        + "Blitzed:"
        + blitzed
        + "\n"
        + "Fought:"
        + foughtBattles
        + "\n"
        + "Pending:"
        + pendingBattles;
  }
}
