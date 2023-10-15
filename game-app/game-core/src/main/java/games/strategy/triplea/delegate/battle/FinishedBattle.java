package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.BattleRecord.BattleResultDescription;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * A sort of scripted battle made for blitzed/conquered territories without a fight. TODO: expand to
 * cover all possible scripting battle needs.
 */
public class FinishedBattle extends AbstractBattle {
  private static final long serialVersionUID = -5852495231826940879L;

  private final Collection<Territory> amphibiousAttackFrom = new ArrayList<>();
  // maps Territory-> units (stores a collection of who is attacking from where, needed for undoing
  // moves)
  private final Map<Territory, Collection<Unit>> attackingFromMap = new HashMap<>();

  FinishedBattle(
      final Territory battleSite,
      final GamePlayer attacker,
      final BattleTracker battleTracker,
      final BattleType battleType,
      final GameData data,
      final BattleResultDescription battleResultDescription,
      final WhoWon whoWon) {
    super(battleSite, attacker, battleTracker, battleType, data);
    this.battleResultDescription = battleResultDescription;
    this.whoWon = whoWon;
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty();
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    isOver = true;
    battleTracker.removeBattle(this, bridge.getData());
    if (headless) {
      return;
    }
    clearTransportedBy(bridge);
    battleTracker
        .getBattleRecords()
        .addResultToBattle(
            attacker,
            battleId,
            defender,
            attackerLostTuv,
            defenderLostTuv,
            battleResultDescription,
            new BattleResults(this, gameData));
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    attackingUnits.addAll(units);
    final Collection<Unit> attackingFromMapUnits =
        attackingFromMap.computeIfAbsent(attackingFrom, k -> new ArrayList<>());
    attackingFromMapUnits.addAll(units);
    // are we amphibious
    if (route.getStart().isWater()
        && !route.getEnd().isWater()
        && units.stream().anyMatch(Matches.unitIsLand())) {
      amphibiousAttackFrom.add(route.getTerritoryBeforeEnd());
      isAmphibious = true;
    }
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public Change removeAttack(final Route route, final Collection<Unit> units) {
    // Note: This is the same code as in DependentBattle, but this is not a subclass.
    attackingUnits.removeAll(units);
    // the route could be null, in the case of a unit in a territory where a sub is submerged.
    if (route == null) {
      return new CompositeChange();
    }
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    final Collection<Unit> attackingFromMapUnits =
        attackingFromMap.getOrDefault(attackingFrom, new ArrayList<>());
    attackingFromMapUnits.removeAll(units);
    if (attackingFromMapUnits.isEmpty()) {
      this.attackingFromMap.remove(attackingFrom);
    }
    // deal with amphibious assaults
    if (attackingFrom.isWater()) {
      // if none of the units is a land unit, the attack from that territory is no longer an
      // amphibious assault
      if (attackingFromMapUnits.stream().noneMatch(Matches.unitIsLand())) {
        amphibiousAttackFrom.remove(attackingFrom);
        // do we have any amphibious attacks left?
        isAmphibious = !amphibiousAttackFrom.isEmpty();
      }
    }
    // Clear transportedBy for allied air on carriers as these are only set during the battle.
    return TransportTracker.clearTransportedByForAlliedAirOnCarrier(
        units, battleSite, attacker, gameData);
  }

  @Override
  public void unitsLostInPrecedingBattle(
      final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn) {
    final Collection<Unit> lost = getDependentUnits(units);
    lost.addAll(CollectionUtils.intersection(units, attackingUnits));
    if (!lost.isEmpty()) {
      attackingUnits.removeAll(lost);
      if (attackingUnits.isEmpty()) {
        final IntegerMap<UnitType> costs = bridge.getCostsForTuv(attacker);
        final int tuvLostAttacker =
            (withdrawn ? 0 : TuvUtils.getTuv(lost, attacker, costs, gameData));
        attackerLostTuv += tuvLostAttacker;
        // scripted?
        whoWon = WhoWon.DEFENDER;
        if (!headless) {
          battleTracker
              .getBattleRecords()
              .addResultToBattle(
                  attacker,
                  battleId,
                  defender,
                  attackerLostTuv,
                  defenderLostTuv,
                  BattleRecord.BattleResultDescription.LOST,
                  new BattleResults(this, gameData));
        }
        battleTracker.removeBattle(this, bridge.getData());
      }
    }
  }

  Map<Territory, Collection<Unit>> getAttackingFromMap() {
    return attackingFromMap;
  }
}
