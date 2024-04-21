package games.strategy.triplea.delegate;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Contains all the data to describe a move and to undo it. */
public class UndoableMove extends AbstractUndoableMove {
  private static final long serialVersionUID = 8490182214651531358L;

  private String reasonCantUndo;
  private String description;
  // this move is dependent on these moves
  // these moves cant be undone until this one has been
  private final Set<UndoableMove> dependencies = new HashSet<>();
  // these moves depend on me
  // we cant be undone until this is empty
  private final Set<UndoableMove> dependents = new HashSet<>();
  // list of countries we took over
  private final Set<Territory> conquered = new HashSet<>();
  // transports loaded by this move
  private final Set<Unit> loaded = new HashSet<>();
  // transports unloaded by this move
  private final Set<Unit> unloaded = new HashSet<>();
  @Getter private final Route route;

  public UndoableMove(final Collection<Unit> units, final Route route) {
    super(new CompositeChange(), units);
    this.route = route;
  }

  public void addToConquered(final Territory t) {
    conquered.add(t);
  }

  public boolean getCanUndo() {
    return reasonCantUndo == null && dependents.isEmpty();
  }

  String getReasonCantUndo() {
    if (reasonCantUndo != null) {
      return reasonCantUndo;
    } else if (!dependents.isEmpty()) {
      return "Move "
          + (CollectionUtils.getAny(dependents).getIndex() + 1)
          + " must be undone first";
    } else {
      throw new IllegalStateException("no reason");
    }
  }

  public void setCantUndo(final String reason) {
    reasonCantUndo = reason;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void load(final Unit transport) {
    loaded.add(transport);
  }

  public void unload(final Unit transport) {
    unloaded.add(transport);
  }

  @Override
  protected void undoSpecific(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    final BattleTracker battleTracker = data.getBattleDelegate().getBattleTracker();
    battleTracker.undoBattle(route, units, bridge.getGamePlayer(), bridge);
    // clean up dependencies
    for (final UndoableMove other : dependencies) {
      other.dependents.remove(this);
    }
    // if we are moving out of a battle zone, mark it
    // this can happen for air units moving out of a battle zone
    for (final IBattle battle : battleTracker.getPendingBattles(route.getStart())) {
      if (battle == null || battle.isOver()) {
        continue;
      }
      for (final Unit unit : units) {
        final Route routeUnitUsedToMove =
            data.getMoveDelegate().getRouteUsedToMoveInto(unit, route.getStart());
        if (!battle.getBattleType().isBombingRun()) {
          // route units used to move will be null in the case where an enemy sub is submerged in
          // the territory, and
          // another unit moved in to attack it, but some of the units in the original territory are
          // moved out. Undoing
          // this last move, the route used to move into the battle zone will be null
          if (routeUnitUsedToMove != null) {
            final Change change = battle.addAttackChange(routeUnitUsedToMove, Set.of(unit), null);
            bridge.addChange(change);
          }
        } else {
          Map<Unit, Set<Unit>> targets = null;
          Unit target = null;
          if (routeUnitUsedToMove != null) {
            final Territory end = routeUnitUsedToMove.getEnd();
            final Collection<Unit> enemyTargetsTotal =
                end.getMatches(
                    Matches.enemyUnit(bridge.getGamePlayer())
                        .and(Matches.unitCanBeDamaged())
                        .and(Matches.unitIsBeingTransported().negate()));
            final Collection<Unit> enemyTargets =
                CollectionUtils.getMatches(
                    enemyTargetsTotal,
                    Matches.unitIsOfTypes(
                        UnitAttachment.getAllowedBombingTargetsIntersection(
                            CollectionUtils.getMatches(
                                Set.of(unit), Matches.unitIsStrategicBomber()),
                            data.getUnitTypeList())));
            if (enemyTargets.size() > 1
                && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
                    data.getProperties())
                && !Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())) {
              Player player = bridge.getRemotePlayer(bridge.getGamePlayer());
              while (target == null) {
                target = player.whatShouldBomberBomb(end, enemyTargets, List.of(unit));
              }
            } else if (!enemyTargets.isEmpty()) {
              target = CollectionUtils.getAny(enemyTargets);
            }
            if (target != null) {
              targets = new HashMap<>();
              targets.put(target, new HashSet<>(Set.of(unit)));
            }
          }
          final Change change = battle.addAttackChange(routeUnitUsedToMove, Set.of(unit), targets);
          bridge.addChange(change);
        }
      }
    }
  }

  /**
   * Update the dependencies.
   *
   * @param undoableMoves list of moves that should be undone
   */
  public void initializeDependencies(final List<UndoableMove> undoableMoves) {
    for (final UndoableMove other : undoableMoves) {
      // TODO: verify we do not depend on IllegalStateException here before converting this to a
      // checkNotNull.
      Preconditions.checkState(other != null, "other should not be null: " + undoableMoves);

      // if the other move has moves that depend on this
      if (!CollectionUtils.intersection(other.getUnits(), this.getUnits()).isEmpty()
          // if the other move has transports that we are loading
          || !CollectionUtils.intersection(other.units, this.loaded).isEmpty()
          // or we are moving through a previously conquered territory
          // we should be able to take this out later
          // we need to add logic for this move to take over the same territories when the other
          // move is undone
          || !CollectionUtils.intersection(other.conquered, route.getAllTerritories()).isEmpty()
          // or we are unloading transports that have moved in another turn
          || !CollectionUtils.intersection(other.units, this.unloaded).isEmpty()
          || !CollectionUtils.intersection(other.unloaded, this.unloaded).isEmpty()) {
        dependencies.add(other);
        other.dependents.add(this);
      }
    }
  }

  // for use with airborne moving
  public void addDependency(final UndoableMove undoableMove) {
    dependencies.add(undoableMove);
    undoableMove.dependents.add(this);
  }

  public boolean wasTransportUnloaded(final Unit transport) {
    return unloaded.contains(transport);
  }

  public boolean wasTransportLoaded(final Unit transport) {
    return loaded.contains(transport);
  }

  @Override
  public String toString() {
    return "UndoableMove index;" + index + " description:" + description;
  }

  @Override
  public final String getMoveLabel() {
    return route.getStart() + " -> " + route.getEnd();
  }

  @Override
  public final Territory getEnd() {
    return route.getEnd();
  }

  public final Territory getStart() {
    return route.getStart();
  }

  @Override
  protected final MoveDescription getDescriptionObject() {
    return new MoveDescription(units, route);
  }
}
