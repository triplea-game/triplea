package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.MovePanel;
import games.strategy.util.CollectionUtils;

/**
 * Contains all the data to describe a move and to undo it.
 */
public class UndoableMove extends AbstractUndoableMove {
  private static final long serialVersionUID = 8490182214651531358L;
  private String m_reasonCantUndo;
  private String m_description;
  // this move is dependent on these moves
  // these moves cant be undone until this one has been
  private final Set<UndoableMove> m_iDependOn = new HashSet<>();
  // these moves depend on me
  // we cant be undone until this is empty
  private final Set<UndoableMove> m_dependOnMe = new HashSet<>();
  // list of countries we took over
  private final Set<Territory> m_conquered = new HashSet<>();
  // transports loaded by this move
  private final Set<Unit> m_loaded = new HashSet<>();
  // transports unloaded by this move
  private final Set<Unit> m_unloaded = new HashSet<>();
  private final Route m_route;

  public void addToConquered(final Territory t) {
    m_conquered.add(t);
  }

  public Route getRoute() {
    return m_route;
  }

  public boolean getcanUndo() {
    return (m_reasonCantUndo == null) && m_dependOnMe.isEmpty();
  }

  String getReasonCantUndo() {
    if (m_reasonCantUndo != null) {
      return m_reasonCantUndo;
    } else if (!m_dependOnMe.isEmpty()) {
      return "Move " + (m_dependOnMe.iterator().next().getIndex() + 1) + " must be undone first";
    } else {
      throw new IllegalStateException("no reason");
    }
  }

  public void setCantUndo(final String reason) {
    m_reasonCantUndo = reason;
  }

  public String getDescription() {
    return m_description;
  }

  public void setDescription(final String description) {
    m_description = description;
  }

  public UndoableMove(final Collection<Unit> units, final Route route) {
    super(new CompositeChange(), units);
    m_route = route;
  }

  public void load(final Unit transport) {
    m_loaded.add(transport);
  }

  public void unload(final Unit transport) {
    m_unloaded.add(transport);
  }

  @Override
  protected void undoSpecific(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    final BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
    battleTracker.undoBattle(m_route, m_units, bridge.getPlayerId(), bridge);
    // clean up dependencies
    for (final UndoableMove other : m_iDependOn) {
      other.m_dependOnMe.remove(this);
    }
    // if we are moving out of a battle zone, mark it
    // this can happen for air units moving out of a battle zone
    for (final IBattle battle : battleTracker.getPendingBattles(m_route.getStart(), null)) {
      if ((battle == null) || battle.isOver()) {
        continue;
      }
      for (final Unit unit : m_units) {
        final Route routeUnitUsedToMove =
            DelegateFinder.moveDelegate(data).getRouteUsedToMoveInto(unit, m_route.getStart());
        if (!battle.getBattleType().isBombingRun()) {
          // route units used to move will be null in the case
          // where an enemy sub is submerged in the territory, and another unit
          // moved in to attack it, but some of the units in the original
          // territory are moved out. Undoing this last move, the route used to move
          // into the battle zone will be null
          if (routeUnitUsedToMove != null) {
            final Change change = battle.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit), null);
            bridge.addChange(change);
          }
        } else {
          HashMap<Unit, HashSet<Unit>> targets = null;
          Unit target = null;
          if ((routeUnitUsedToMove != null) && (routeUnitUsedToMove.getEnd() != null)) {
            final Territory end = routeUnitUsedToMove.getEnd();
            final Collection<Unit> enemyTargetsTotal = end.getUnits().getMatches(
                Matches.enemyUnit(bridge.getPlayerId(), data)
                    .and(Matches.unitCanBeDamaged())
                    .and(Matches.unitIsBeingTransported().negate()));
            final Collection<Unit> enemyTargets = CollectionUtils.getMatches(enemyTargetsTotal,
                Matches.unitIsOfTypes(UnitAttachment.getAllowedBombingTargetsIntersection(
                    CollectionUtils.getMatches(Collections.singleton(unit), Matches.unitIsStrategicBomber()), data)));
            if ((enemyTargets.size() > 1)
                && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)
                && !Properties.getRaidsMayBePreceededByAirBattles(data)) {
              while (target == null) {
                target = ((ITripleAPlayer) bridge.getRemotePlayer(bridge.getPlayerId())).whatShouldBomberBomb(end,
                    enemyTargets, Collections.singletonList(unit));
              }
            } else if (!enemyTargets.isEmpty()) {
              target = enemyTargets.iterator().next();
            }
            if (target != null) {
              targets = new HashMap<>();
              targets.put(target, new HashSet<>(Collections.singleton(unit)));
            }
          }
          final Change change = battle.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit), targets);
          bridge.addChange(change);
        }
      }
    }
    // Clear any temporary dependents
    MovePanel.clearDependents(m_units);
  }

  /**
   * Update the dependencies.
   *
   * @param undoableMoves
   *        list of moves that should be undone
   */
  public void initializeDependencies(final List<UndoableMove> undoableMoves) {
    for (final UndoableMove other : undoableMoves) {
      if (other == null) {
        System.err.println(undoableMoves);
        throw new IllegalStateException("other should not be null");
      }
      // if the other move has moves that depend on this
      if (!CollectionUtils.intersection(other.getUnits(), this.getUnits()).isEmpty()
          // if the other move has transports that we are loading
          || !CollectionUtils.intersection(other.m_units, this.m_loaded).isEmpty()
          // or we are moving through a previously conqueured territory
          // we should be able to take this out later
          // we need to add logic for this move to take over the same territories
          // when the other move is undone
          || !CollectionUtils.intersection(other.m_conquered, m_route.getAllTerritories()).isEmpty()
          // or we are unloading transports that have moved in another turn
          || !CollectionUtils.intersection(other.m_units, this.m_unloaded).isEmpty()
          || !CollectionUtils.intersection(other.m_unloaded, this.m_unloaded).isEmpty()) {
        m_iDependOn.add(other);
        other.m_dependOnMe.add(this);
      }
    }
  }

  // for use with airborne moving
  public void addDependency(final UndoableMove undoableMove) {
    m_iDependOn.add(undoableMove);
    undoableMove.m_dependOnMe.add(this);
  }

  public boolean wasTransportUnloaded(final Unit transport) {
    return m_unloaded.contains(transport);
  }

  public boolean wasTransportLoaded(final Unit transport) {
    return m_loaded.contains(transport);
  }

  @Override
  public String toString() {
    return "UndoableMove index;" + m_index + " description:" + m_description;
  }

  @Override
  public final String getMoveLabel() {
    return m_route.getStart() + " -> " + m_route.getEnd();
  }

  @Override
  public final Territory getEnd() {
    return m_route.getEnd();
  }

  public final Territory getStart() {
    return m_route.getStart();
  }

  @Override
  protected final MoveDescription getDescriptionObject() {
    return new MoveDescription(m_units, m_route);
  }
}
