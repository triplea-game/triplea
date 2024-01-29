package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.AirBattle;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TransportUtils;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;

/** Used to move units and make changes to game state. */
public class MovePerformer implements Serializable {
  private static final long serialVersionUID = 3752242292777658310L;

  private transient AbstractMoveDelegate moveDelegate;
  private transient IDelegateBridge bridge;
  private transient GamePlayer player;
  private AaInMoveUtil aaInMoveUtil;
  private final ExecutionStack executionStack = new ExecutionStack();
  private UndoableMove currentMove;
  private Map<Unit, Collection<Unit>> airTransportDependents;
  private Collection<Unit> arrivingUnits;

  MovePerformer() {}

  private BattleTracker getBattleTracker() {
    return bridge.getData().getBattleDelegate().getBattleTracker();
  }

  void initialize(final AbstractMoveDelegate delegate) {
    this.moveDelegate = delegate;
    bridge = delegate.getBridge();
    player = bridge.getGamePlayer();
    if (aaInMoveUtil != null) {
      aaInMoveUtil.initialize(bridge);
    }
  }

  private Player getRemotePlayer(final GamePlayer gamePlayer) {
    return bridge.getRemotePlayer(gamePlayer);
  }

  private Player getRemotePlayer() {
    return getRemotePlayer(player);
  }

  void moveUnits(
      final MoveDescription move, final GamePlayer gamePlayer, final UndoableMove currentMove) {
    this.currentMove = currentMove;
    this.airTransportDependents = move.getAirTransportsDependents();
    populateStack(move.getUnits(), move.getRoute(), gamePlayer, move.getUnitsToSeaTransports());
    executionStack.execute(bridge);
  }

  public void resume() {
    executionStack.execute(bridge);
  }

  /** We assume that the move is valid. */
  private void populateStack(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer gamePlayer,
      final Map<Unit, Unit> unitsToTransports) {
    final IExecutable preAaFire =
        new IExecutable() {
          private static final long serialVersionUID = -7945930782650355037L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            // if we are moving out of a battle zone, mark it
            // this can happen for air units moving out of a battle zone
            for (final IBattle battle : getBattleTracker().getPendingBattles(route.getStart())) {
              for (final Unit unit : units) {
                final Route routeUnitUsedToMove =
                    moveDelegate.getRouteUsedToMoveInto(unit, route.getStart());
                if (battle != null) {
                  Change change = battle.removeAttack(routeUnitUsedToMove, Set.of(unit));
                  bridge.addChange(change);
                }
              }
            }
          }
        };
    // hack to allow the executables to share state
    final IExecutable fireAa =
        new IExecutable() {
          private static final long serialVersionUID = -3780228078499895244L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            final Collection<Unit> aaCasualties = fireAa(route, units);
            final Set<Unit> aaCasualtiesWithDependents = new HashSet<>();
            // need to remove any dependents here
            if (aaCasualties != null) {
              aaCasualtiesWithDependents.addAll(aaCasualties);
              final Map<Unit, Collection<Unit>> dependencies =
                  TransportTracker.transportingWithAllPossibleUnits(units);
              for (final Unit u : aaCasualties) {
                final Collection<Unit> dependents = dependencies.get(u);
                if (dependents != null) {
                  aaCasualtiesWithDependents.addAll(dependents);
                }
                // we might have new dependents too (ie: paratroopers)
                final Collection<Unit> airTransportDependents =
                    MovePerformer.this.airTransportDependents.get(u);
                if (airTransportDependents != null) {
                  aaCasualtiesWithDependents.addAll(airTransportDependents);
                }
              }
            }
            arrivingUnits = CollectionUtils.difference(units, aaCasualtiesWithDependents);
          }
        };
    final IExecutable postAaFire =
        new IExecutable() {
          private static final long serialVersionUID = 670783657414493643L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            // if any non-enemy territories on route or if any enemy units on route the battles
            // on (note water could have enemy, but it's not owned)
            final GameData data = bridge.getData();
            final Predicate<Territory> mustFightThrough = getMustFightThroughMatch(gamePlayer);
            final Collection<Unit> arrived =
                Collections.unmodifiableList(CollectionUtils.intersection(units, arrivingUnits));
            // Reset Optional
            arrivingUnits = new ArrayList<>();
            final Collection<Unit> arrivedCopyForBattles = new ArrayList<>(arrived);
            final Map<Unit, Unit> transporting =
                route.isLoad()
                    ? unitsToTransports
                    : TransportUtils.mapTransports(route, arrived, null);
            // If we have paratrooper land units being carried by air units, they should be dropped
            // off in the last territory. This means they are still dependent during the middle
            // steps of the route.
            final Collection<Unit> dependentOnSomethingTilTheEndOfRoute =
                TransportUtils.mapParatroopers(arrived).keySet();
            final Collection<Unit> presentFromStartTilEnd = new ArrayList<>(arrived);
            presentFromStartTilEnd.removeAll(dependentOnSomethingTilTheEndOfRoute);
            final CompositeChange change = new CompositeChange();

            // markFuelCostResourceChange must be done before we load/unload units
            change.add(Route.getFuelChanges(units, route, gamePlayer, data));

            markTransportsMovement(arrived, transporting, route);
            if (!arrived.isEmpty() && route.anyMatch(mustFightThrough)) {
              boolean ignoreBattle = false;
              // could it be a bombing raid
              final Collection<Unit> enemyUnits =
                  route.getEnd().getUnitCollection().getMatches(Matches.enemyUnit(gamePlayer));
              final Collection<Unit> enemyTargetsTotal =
                  CollectionUtils.getMatches(
                      enemyUnits,
                      Matches.unitCanBeDamaged().and(Matches.unitIsBeingTransported().negate()));
              final boolean canCreateAirBattle =
                  !enemyTargetsTotal.isEmpty()
                      && Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
                      && AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(
                          route.getEnd(), gamePlayer, data, true);
              final Predicate<Unit> allBombingRaid =
                  PredicateBuilder.of(Matches.unitIsStrategicBomber())
                      .orIf(canCreateAirBattle, Matches.unitCanEscort())
                      .build();
              final boolean allCanBomb = arrived.stream().allMatch(allBombingRaid);
              final Collection<Unit> enemyTargets =
                  CollectionUtils.getMatches(
                      enemyTargetsTotal,
                      Matches.unitIsOfTypes(
                          UnitAttachment.getAllowedBombingTargetsIntersection(
                              CollectionUtils.getMatches(arrived, Matches.unitIsStrategicBomber()),
                              data.getUnitTypeList())));
              final boolean targetsOrEscort =
                  !enemyTargets.isEmpty()
                      || (!enemyTargetsTotal.isEmpty()
                          && canCreateAirBattle
                          && arrived.stream().allMatch(Matches.unitCanEscort()));
              boolean targetedAttack = false;
              // if it's all bombers and there's something to bomb
              if (allCanBomb && targetsOrEscort && GameStepPropertiesHelper.isCombatMove(data)) {
                final boolean bombing = getRemotePlayer().shouldBomberBomb(route.getEnd());
                // if bombing and there's something to target - ask what to bomb
                if (bombing) {
                  // CompositeMatchOr<Unit> unitsToBeBombed = new
                  // CompositeMatchOr<Unit>(Matches.UnitIsFactory,
                  // Matches.UnitCanBeDamagedButIsNotFactory);
                  // determine which unit to bomb
                  final Unit target;
                  if (enemyTargets.size() > 1
                      && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
                          data.getProperties())
                      && !canCreateAirBattle) {
                    target =
                        getRemotePlayer()
                            .whatShouldBomberBomb(route.getEnd(), enemyTargets, arrived);
                  } else if (!enemyTargets.isEmpty()) {
                    target = CollectionUtils.getAny(enemyTargets);
                  } else {
                    // in case we are escorts only
                    target = CollectionUtils.getAny(enemyTargetsTotal);
                  }
                  if (target != null) {
                    targetedAttack = true;
                    final Map<Unit, Set<Unit>> targets = new HashMap<>();
                    targets.put(target, new HashSet<>(arrived));
                    // createdBattle = true;
                    getBattleTracker()
                        .addBattle(
                            route,
                            arrivedCopyForBattles,
                            bombing,
                            gamePlayer,
                            MovePerformer.this.bridge,
                            currentMove,
                            dependentOnSomethingTilTheEndOfRoute,
                            targets,
                            false);
                  }
                }
              }
              // Ignore Trn on Trn forces.
              if (Properties.getIgnoreTransportInMovement(bridge.getData().getProperties())) {
                final boolean allOwnedTransports =
                    arrived.stream().allMatch(Matches.unitIsSeaTransportButNotCombatSeaTransport());
                final boolean allEnemyTransports =
                    !enemyUnits.isEmpty()
                        && enemyUnits.stream()
                            .allMatch(Matches.unitIsSeaTransportButNotCombatSeaTransport());
                // If everybody is a transport, don't create a battle
                if (allOwnedTransports && allEnemyTransports) {
                  ignoreBattle = true;
                }
              }
              if (!ignoreBattle && GameStepPropertiesHelper.isCombatMove(data) && !targetedAttack) {
                getBattleTracker()
                    .addBattle(
                        route,
                        arrivedCopyForBattles,
                        gamePlayer,
                        MovePerformer.this.bridge,
                        currentMove,
                        dependentOnSomethingTilTheEndOfRoute);
              }
              if (!ignoreBattle
                  && GameStepPropertiesHelper.isNonCombatMove(data, false)
                  && !targetedAttack) {
                // We are in non-combat move phase, and we are taking over friendly territories. No
                // need for a battle. (This could get really difficult if we want these recorded in
                // battle records).
                for (final Territory t :
                    route.getMatches(
                        Matches.isTerritoryNotUnownedWaterAndCanBeTakenOverBy(gamePlayer)
                            .and(Matches.territoryIsBlitzable(gamePlayer)))) {
                  if (Matches.isTerritoryEnemy(gamePlayer).test(t)
                      || Matches.territoryHasEnemyUnits(gamePlayer).test(t)) {
                    continue;
                  }
                  if ((t.equals(route.getEnd())
                          && !arrivedCopyForBattles.isEmpty()
                          && arrivedCopyForBattles.stream().allMatch(Matches.unitIsAir()))
                      || (!t.equals(route.getEnd())
                          && !presentFromStartTilEnd.isEmpty()
                          && presentFromStartTilEnd.stream().allMatch(Matches.unitIsAir()))) {
                    continue;
                  }
                  // createdBattle = true;
                  getBattleTracker()
                      .takeOver(t, gamePlayer, bridge, currentMove, arrivedCopyForBattles);
                }
              }
            }
            // mark movement
            final Change moveChange = markMovementChange(arrived, route, gamePlayer);
            change.add(moveChange);
            // actually move the units
            final Change remove = ChangeFactory.removeUnits(route.getStart(), units);
            final Change add = ChangeFactory.addUnits(route.getEnd(), arrived);
            change.add(add, remove);
            MovePerformer.this.bridge.addChange(change);
            currentMove.addChange(change);
            currentMove.setDescription(
                MyFormatter.unitsToTextNoOwner(arrived)
                    + " moved from "
                    + route.getStart().getName()
                    + " to "
                    + route.getEnd().getName());
            moveDelegate.updateUndoableMoves(currentMove);
          }
        };
    executionStack.push(postAaFire);
    executionStack.push(fireAa);
    executionStack.push(preAaFire);
    executionStack.execute(bridge);
  }

  private static Predicate<Territory> getMustFightThroughMatch(final GamePlayer gamePlayer) {
    return Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(gamePlayer)
        .or(Matches.territoryHasNonSubmergedEnemyUnits(gamePlayer))
        .or(Matches.isTerritoryNotUnownedWaterAndCanBeTakenOverBy(gamePlayer));
  }

  private Change markMovementChange(
      final Collection<Unit> units, final Route route, final GamePlayer gamePlayer) {
    final GameData data = bridge.getData();
    final CompositeChange change = new CompositeChange();
    // only units owned by us need to be marked
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final Territory routeStart = route.getStart();
    final Territory routeEnd = route.getEnd();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(gamePlayer))) {
      BigDecimal moved = route.getMovementCost(unit);
      final UnitAttachment ua = unit.getUnitAttachment();
      if (ua.getIsAir()) {
        if (TerritoryAttachment.hasAirBase(routeStart)
            && relationshipTracker.isAllied(routeStart.getOwner(), unit.getOwner())) {
          moved = moved.subtract(BigDecimal.ONE);
        }
        if (routeEnd != null
            && TerritoryAttachment.hasAirBase(routeEnd)
            && relationshipTracker.isAllied(routeEnd.getOwner(), unit.getOwner())) {
          moved = moved.subtract(BigDecimal.ONE);
        }
      }
      change.add(
          ChangeFactory.unitPropertyChange(
              unit, moved.add(unit.getAlreadyMoved()), Unit.ALREADY_MOVED));
    }
    // if neutrals were taken over mark land units with 0 movement
    // if entered a non-blitzed conquered territory, mark with 0 movement
    if (GameStepPropertiesHelper.isCombatMove(data)
        && (!MoveDelegate.getEmptyNeutral(route).isEmpty() || hasConqueredNonBlitzed(route))) {
      for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsLand())) {
        change.add(ChangeFactory.markNoMovementChange(Set.of(unit)));
      }
    }
    if (routeEnd != null
        && Properties.getSubsCanEndNonCombatMoveWithEnemies(data.getProperties())
        && GameStepPropertiesHelper.isNonCombatMove(data, false)
        && routeEnd.anyUnitsMatch(
            Matches.unitIsEnemyOf(gamePlayer).and(Matches.unitIsDestroyer()))) {
      // if we are allowed to have our subs enter any sea zone with enemies during noncombat, we
      // want to make sure we
      // can't keep moving them if there is an enemy destroyer there
      for (final Unit unit :
          CollectionUtils.getMatches(units, Matches.unitCanMoveThroughEnemies())) {
        change.add(ChangeFactory.markNoMovementChange(Set.of(unit)));
      }
    }
    return change;
  }

  /** Marks transports and units involved in unloading with no movement left. */
  private void markTransportsMovement(
      final Collection<Unit> arrived, final Map<Unit, Unit> transporting, final Route route) {
    final GameData data = bridge.getData();
    final Predicate<Unit> paratroopNAirTransports =
        Matches.unitIsAirTransport().or(Matches.unitIsAirTransportable());
    final boolean paratroopsLanding =
        arrived.stream().anyMatch(paratroopNAirTransports)
            && MoveValidator.allLandUnitsAreBeingParatroopered(arrived);
    final Map<Unit, Collection<Unit>> dependentAirTransportableUnits = new HashMap<>();
    for (final Unit unit : arrived) {
      Unit transport = unit.getTransportedBy();
      if (transport != null) {
        dependentAirTransportableUnits.computeIfAbsent(transport, u -> new ArrayList<>()).add(unit);
      }
    }
    // add newly created dependents
    for (final Entry<Unit, Collection<Unit>> entry : airTransportDependents.entrySet()) {
      Collection<Unit> dependents = dependentAirTransportableUnits.get(entry.getKey());
      if (dependents != null) {
        dependents = new ArrayList<>(dependents);
        dependents.addAll(entry.getValue());
      } else {
        dependents = entry.getValue();
      }
      dependentAirTransportableUnits.put(entry.getKey(), dependents);
    }

    // load the transports
    if (route.isLoad() || paratroopsLanding) {
      // mark transports as having transported
      for (final Unit load : transporting.keySet()) {
        final Unit transport = transporting.get(load);
        if (!transport.equals(load.getTransportedBy())) {
          final Change change = TransportTracker.loadTransportChange(transport, load);
          currentMove.addChange(change);
          currentMove.load(transport);
          bridge.addChange(change);
        }
      }
      if (transporting.isEmpty()) {
        for (final Unit airTransport : dependentAirTransportableUnits.keySet()) {
          for (final Unit unit : dependentAirTransportableUnits.get(airTransport)) {
            final Change change = TransportTracker.loadTransportChange(airTransport, unit);
            currentMove.addChange(change);
            currentMove.load(airTransport);
            bridge.addChange(change);
          }
        }
      }
    }
    if (route.isUnload() || paratroopsLanding) {
      final Set<Unit> units = new HashSet<>();
      units.addAll(transporting.values());
      units.addAll(transporting.keySet());
      // if there are multiple units on a single transport, the transport will be in units list
      // multiple times
      if (transporting.isEmpty()) {
        units.addAll(dependentAirTransportableUnits.keySet());
        for (final Collection<Unit> airTransport : dependentAirTransportableUnits.values()) {
          units.addAll(airTransport);
        }
      }
      // any pending battles in the unloading zone?
      final BattleTracker tracker = getBattleTracker();
      final boolean pendingBattles =
          tracker.getPendingBattle(route.getStart(), BattleType.NORMAL) != null;
      for (final Unit unit : units) {
        if (Matches.unitIsAir().test(unit)) {
          continue;
        }
        final Unit transportedBy = unit.getTransportedBy();
        // we will unload our paratroopers after they land in battle (after aa guns fire)
        if (paratroopsLanding
            && transportedBy != null
            && Matches.unitIsAirTransport().test(transportedBy)
            && GameStepPropertiesHelper.isCombatMove(data)
            && Matches.territoryHasNonSubmergedEnemyUnits(player).test(route.getEnd())) {
          continue;
        }
        // unload the transports
        final Change change1 =
            TransportTracker.unloadTransportChange(
                unit, currentMove.getRoute().getEnd(), pendingBattles);
        currentMove.addChange(change1);
        currentMove.unload(unit);
        bridge.addChange(change1);
        // set noMovement
        final Change change2 = ChangeFactory.markNoMovementChange(Set.of(unit));
        currentMove.addChange(change2);
        bridge.addChange(change2);
      }
    }
  }

  private boolean hasConqueredNonBlitzed(final Route route) {
    final BattleTracker tracker = getBattleTracker();
    for (final Territory current : route.getSteps()) {
      if (tracker.wasConquered(current) && !tracker.wasBlitzed(current)) {
        return true;
      }
    }
    return false;
  }

  /** Fire aa guns. Returns units to remove. */
  private Collection<Unit> fireAa(final Route route, final Collection<Unit> units) {
    if (aaInMoveUtil == null) {
      aaInMoveUtil = new AaInMoveUtil();
    }
    aaInMoveUtil.initialize(bridge);
    final Collection<Unit> unitsToRemove =
        aaInMoveUtil.fireAa(
            route, units, UnitComparator.getLowestToHighestMovementComparator(), currentMove);
    aaInMoveUtil = null;
    return unitsToRemove;
  }
}
