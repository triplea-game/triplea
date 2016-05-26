package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

/**
 * Code to fire AA guns while in combat and non combat move.
 */
class AAInMoveUtil implements Serializable {
  private static final long serialVersionUID = 1787497998642717678L;
  private transient IDelegateBridge m_bridge;
  private transient PlayerID m_player;
  private Collection<Unit> m_casualties = new ArrayList<>();
  private final ExecutionStack m_executionStack = new ExecutionStack();

  AAInMoveUtil() {}

  public AAInMoveUtil initialize(final IDelegateBridge bridge) {
    m_bridge = bridge;
    m_player = bridge.getPlayerID();
    return this;
  }

  private GameData getData() {
    return m_bridge.getData();
  }

  private boolean isAlwaysONAAEnabled() {
    return games.strategy.triplea.Properties.getAlwaysOnAA(getData());
  }

  private boolean isAATerritoryRestricted() {
    return games.strategy.triplea.Properties.getAATerritoryRestricted(getData());
  }

  private ITripleAPlayer getRemotePlayer(final PlayerID id) {
    return (ITripleAPlayer) m_bridge.getRemotePlayer(id);
  }

  private ITripleAPlayer getRemotePlayer() {
    return getRemotePlayer(m_player);
  }

  /**
   * Fire aa guns. Returns units to remove.
   */
  Collection<Unit> fireAA(final Route route, final Collection<Unit> units, final Comparator<Unit> decreasingMovement,
      final UndoableMove currentMove) {
    if (m_executionStack.isEmpty()) {
      populateExecutionStack(route, units, decreasingMovement, currentMove);
    }
    m_executionStack.execute(m_bridge);
    return m_casualties;
  }

  private void populateExecutionStack(final Route route, final Collection<Unit> units,
      final Comparator<Unit> decreasingMovement, final UndoableMove currentMove) {
    final List<Unit> targets = new ArrayList<>(units);
    // select units with lowest movement first
    Collections.sort(targets, decreasingMovement);
    final List<IExecutable> executables = new ArrayList<>();
    final Iterator<Territory> iter = getTerritoriesWhereAAWillFire(route, units).iterator();
    while (iter.hasNext()) {
      final Territory location = iter.next();
      executables.add(new IExecutable() {
        private static final long serialVersionUID = -1545771595683434276L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireAA(location, targets, currentMove);
        }
      });
    }
    Collections.reverse(executables);
    m_executionStack.push(executables);
  }

  Collection<Territory> getTerritoriesWhereAAWillFire(final Route route, final Collection<Unit> units) {
    final boolean alwaysOnAA = isAlwaysONAAEnabled();
    // Just the attacked territory will have AA firing
    if (!alwaysOnAA && isAATerritoryRestricted()) {
      return Collections.emptyList();
    }
    final GameData data = getData();
    // No AA in nonCombat unless 'Always on AA'
    if (GameStepPropertiesHelper.isNonCombatMove(data, false) && !alwaysOnAA) {
      return Collections.emptyList();
    }
    // can't rely on m_player being the unit owner in Edit Mode
    // look at the units being moved to determine allies and enemies
    final PlayerID movingPlayer = movingPlayer(units);
    final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAA(movingPlayer, data);
    // don't iterate over the end
    // that will be a battle
    // and handled else where in this tangled mess
    final Match<Unit> hasAA = Matches.UnitIsAAthatCanFire(units, airborneTechTargetsAllowed, movingPlayer,
        Matches.UnitIsAAforFlyOverOnly, 1, true, data);
    // AA guns in transports shouldn't be able to fire
    final List<Territory> territoriesWhereAAWillFire = new ArrayList<>();
    for (final Territory current : route.getMiddleSteps()) {
      if (current.getUnits().someMatch(hasAA)) {
        territoriesWhereAAWillFire.add(current);
      }
    }
    if (games.strategy.triplea.Properties.getForceAAattacksForLastStepOfFlyOver(data)) {
      if (route.getEnd().getUnits().someMatch(hasAA)) {
        territoriesWhereAAWillFire.add(route.getEnd());
      }
    } else {
      // Since we are not firing on the last step, check the start as well, to prevent the user from moving to and from
      // AA sites one at a
      // time
      // if there was a battle fought there then don't fire, this covers the case where we fight, and "Always On AA"
      // wants to fire after the
      // battle.
      // TODO: there is a bug in which if you move an air unit to a battle site in the middle of non combat, it wont
      // fire
      if (route.getStart().getUnits().someMatch(hasAA) && !getBattleTracker().wasBattleFought(route.getStart())) {
        territoriesWhereAAWillFire.add(route.getStart());
      }
    }
    return territoriesWhereAAWillFire;
  }

  private BattleTracker getBattleTracker() {
    return DelegateFinder.battleDelegate(getData()).getBattleTracker();
  }

  private PlayerID movingPlayer(final Collection<Unit> units) {
    if (Match.someMatch(units, Matches.unitIsOwnedBy(m_player))) {
      return m_player;
    }
    if (units != null) {
      for (final Unit u : units) {
        if (u != null && u.getOwner() != null) {
          return u.getOwner();
        }
      }
    }
    return PlayerID.NULL_PLAYERID;
  }

  /**
   * Fire the aa units in the given territory, hits are removed from units
   */
  private void fireAA(final Territory territory, final Collection<Unit> units, final UndoableMove currentMove) {
    if (units.isEmpty()) {
      return;
    }
    final PlayerID movingPlayer = movingPlayer(units);
    final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAA(movingPlayer, getData());
    final List<Unit> defendingAA = territory.getUnits().getMatches(Matches.UnitIsAAthatCanFire(units,
        airborneTechTargetsAllowed, movingPlayer, Matches.UnitIsAAforFlyOverOnly, 1, true, getData()));
    // comes ordered alphabetically already
    final List<String> AAtypes = UnitAttachment.getAllOfTypeAAs(defendingAA);
    // stacks are backwards
    Collections.reverse(AAtypes);
    for (final String currentTypeAA : AAtypes) {
      final Collection<Unit> currentPossibleAA = Match.getMatches(defendingAA, Matches.UnitIsAAofTypeAA(currentTypeAA));
      final Set<UnitType> targetUnitTypesForThisTypeAA =
          UnitAttachment.get(currentPossibleAA.iterator().next().getType()).getTargetsAA(getData());
      final Set<UnitType> airborneTypesTargettedToo = airborneTechTargetsAllowed.get(currentTypeAA);
      final Collection<Unit> validTargetedUnitsForThisRoll =
          Match.getMatches(units, new CompositeMatchOr<>(Matches.unitIsOfTypes(targetUnitTypesForThisTypeAA),
              new CompositeMatchAnd<Unit>(Matches.UnitIsAirborne, Matches.unitIsOfTypes(airborneTypesTargettedToo))));
      // once we fire the AA guns, we can't undo
      // otherwise you could keep undoing and redoing
      // until you got the roll you wanted
      currentMove.setCantUndo("Move cannot be undone after " + currentTypeAA + " has fired.");
      final DiceRoll[] dice = new DiceRoll[1];
      final IExecutable rollDice = new IExecutable() {
        private static final long serialVersionUID = 4714364489659654758L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          // get rid of units already killed, so we don't target them twice
          validTargetedUnitsForThisRoll.removeAll(m_casualties);
          if (!validTargetedUnitsForThisRoll.isEmpty()) {
            dice[0] = DiceRoll.rollAA(validTargetedUnitsForThisRoll, currentPossibleAA, m_bridge, territory, true);
          }
        }
      };
      final IExecutable selectCasualties = new IExecutable() {
        private static final long serialVersionUID = -8633263235214834617L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!validTargetedUnitsForThisRoll.isEmpty()) {
            final int hitCount = dice[0].getHits();
            if (hitCount == 0) {
              if (currentTypeAA.equals("AA")) {
                m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS,
                    findDefender(currentPossibleAA, territory));
              } else {
                m_bridge.getSoundChannelBroadcaster().playSoundForAll(
                    SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS,
                    findDefender(currentPossibleAA, territory));
              }
              getRemotePlayer().reportMessage("No " + currentTypeAA + " hits in " + territory.getName(),
                  "No " + currentTypeAA + " hits in " + territory.getName());
            } else {
              if (currentTypeAA.equals("AA")) {
                m_bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT,
                    findDefender(currentPossibleAA, territory));
              } else {
                m_bridge.getSoundChannelBroadcaster().playSoundForAll(
                    SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT,
                    findDefender(currentPossibleAA, territory));
              }
              selectCasualties(dice[0], units, validTargetedUnitsForThisRoll, currentPossibleAA, defendingAA, territory,
                  currentTypeAA);
            }
          }
        }
      };
      // push in reverse order of execution
      m_executionStack.push(selectCasualties);
      m_executionStack.push(rollDice);
    }
  }

  private PlayerID findDefender(final Collection<Unit> defendingUnits, final Territory territory) {
    if (defendingUnits == null || defendingUnits.isEmpty()) {
      if (territory != null && territory.getOwner() != null && !territory.getOwner().isNull()) {
        return territory.getOwner();
      }
      return PlayerID.NULL_PLAYERID;
    } else if (territory != null && territory.getOwner() != null && !territory.getOwner().isNull()
        && Match.someMatch(defendingUnits, Matches.unitIsOwnedBy(territory.getOwner()))) {
      return territory.getOwner();
    }
    for (final Unit u : defendingUnits) {
      if (u != null && u.getOwner() != null) {
        return u.getOwner();
      }
    }
    return PlayerID.NULL_PLAYERID;
  }

  /**
   * hits are removed from units. Note that units are removed in the order
   * that the iterator will move through them.
   */
  private void selectCasualties(final DiceRoll dice, final Collection<Unit> allFriendlyUnits,
      final Collection<Unit> validTargetedUnitsForThisRoll, final Collection<Unit> defendingAA,
      final Collection<Unit> allEnemyUnits, final Territory territory, final String currentTypeAA) {
    final CasualtyDetails casualties = BattleCalculator.getAACasualties(false, validTargetedUnitsForThisRoll,
        allFriendlyUnits, defendingAA, allEnemyUnits, dice, m_bridge, territory.getOwner(), m_player, null, territory,
        TerritoryEffectHelper.getEffects(territory), false, new ArrayList<>());
    getRemotePlayer().reportMessage(casualties.size() + " " + currentTypeAA + " hits in " + territory.getName(),
        casualties.size() + " " + currentTypeAA + " hits in " + territory.getName());
    BattleDelegate.markDamaged(new ArrayList<>(casualties.getDamaged()), m_bridge, true);
    m_bridge.getHistoryWriter().addChildToEvent(
        MyFormatter.unitsToTextNoOwner(casualties.getKilled()) + " lost in " + territory.getName(),
        new ArrayList<>(casualties.getKilled()));
    allFriendlyUnits.removeAll(casualties.getKilled());
    if (m_casualties == null) {
      m_casualties = new ArrayList<>(casualties.getKilled());
    } else {
      m_casualties.addAll(casualties.getKilled());
    }
  }
}
