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

/**
 * Detects units that can be killed off because they have no supporting units that can
 * attack/defend.
 */
@Builder
public class FindUnitsWithNoRollsLeft {
  private @NonNull final Boolean isAttacker;
  private @NonNull final Boolean hasRetreatTerritories;
  private @NonNull final Collection<Unit> friendlyUnits;
  private @NonNull final Collection<Unit> enemyUnits;
  private @NonNull final Territory battleSite;

  public Collection<Unit> find() {
    if (isRetreatPossible() || friendlyUnits.isEmpty() || enemyUnits.isEmpty()) {
      return List.of();
    }
    final Predicate<Unit> notSubmergedAndType = getSubmergedAndTerritoryTypePredicate();
    final boolean hasUnitsThatCanRollLeft =
        getUnitsThatCanRoll(notSubmergedAndType, friendlyUnits, isAttacker);
    final boolean enemyHasUnitsThatCanRollLeft =
        getUnitsThatCanRoll(notSubmergedAndType, enemyUnits, !isAttacker);

    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      return CollectionUtils.getMatches(
          friendlyUnits, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
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
        && (hasRetreatTerritories || friendlyUnits.stream().anyMatch(Matches.unitIsAir()));
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
