package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Battle with possible dependencies Includes MustFightBattle and NonFightingBattle. */
public abstract class DependentBattle extends AbstractBattle {
  private static final long serialVersionUID = 9119442509652443015L;
  protected Map<Territory, Collection<Unit>> attackingFromMap;
  @RemoveOnNextMajorRelease protected Set<Territory> attackingFrom;
  private final Collection<Territory> amphibiousAttackFrom;

  DependentBattle(
      final Territory battleSite,
      final GamePlayer attacker,
      final BattleTracker battleTracker,
      final GameData data) {
    super(battleSite, attacker, battleTracker, BattleType.NORMAL, data);
    attackingFromMap = new HashMap<>();
    amphibiousAttackFrom = new ArrayList<>();
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    attackingUnits.removeAll(units);
    // the route could be null, in the case of a unit in a territory where a sub is submerged.
    if (route == null) {
      return;
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
        getAmphibiousAttackTerritories().remove(attackingFrom);
        // do we have any amphibious attacks left?
        isAmphibious = !getAmphibiousAttackTerritories().isEmpty();
      }
    }
  }

  /** Return attacking from Collection. */
  public Collection<Territory> getAttackingFrom() {
    return attackingFromMap.keySet();
  }

  /** Return attacking from Map. */
  public Map<Territory, Collection<Unit>> getAttackingFromMap() {
    return attackingFromMap;
  }

  /** Returns territories where there are amphibious attacks. */
  public Collection<Territory> getAmphibiousAttackTerritories() {
    return amphibiousAttackFrom;
  }
}
