package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class NoUnitsWithRolls {
  private final boolean isAttacker;
  private final boolean hasRetreatTerritories;
  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final Collection<Unit> defendingUnits;
  private @NonNull final Territory battleSite;

  public Collection<Unit> check() {
    if (isRetreatPossible() || attackingUnits.isEmpty() || defendingUnits.isEmpty()) {
      return List.of();
    }
    final Collection<Unit> units;
    final Collection<Unit> enemyUnits;
    if (isAttacker) {
      units = attackingUnits;
      enemyUnits = defendingUnits;
    } else {
      units = defendingUnits;
      enemyUnits = attackingUnits;
    }
    final Predicate<Unit> notSubmergedAndType = getSubmergedAndTerritoryTypePredicate();
    final boolean hasUnitsThatCanRollLeft =
        getUnitsThatCanRoll(notSubmergedAndType, units, isAttacker);
    final boolean enemyHasUnitsThatCanRollLeft =
        getUnitsThatCanRoll(notSubmergedAndType, enemyUnits, !isAttacker);

    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      return CollectionUtils.getMatches(
          units, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    }
    return List.of();
  }

  private boolean getUnitsThatCanRoll(
      final Predicate<Unit> notSubmergedAndType,
      final Collection<Unit> units,
      final boolean isAttacker) {
    return units.stream()
        .anyMatch(notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(isAttacker)));
  }

  private boolean isRetreatPossible() {
    // if we are the attacker, we can retreat instead of dying
    return isAttacker
        && (hasRetreatTerritories || attackingUnits.stream().anyMatch(Matches.unitIsAir()));
  }

  private Predicate<Unit> getSubmergedAndTerritoryTypePredicate() {
    return Matches.unitIsSubmerged()
        .negate()
        .and(
            Matches.territoryIsLand().test(battleSite)
                ? Matches.unitIsSea().negate()
                : Matches.unitIsLand().negate());
  }
}
