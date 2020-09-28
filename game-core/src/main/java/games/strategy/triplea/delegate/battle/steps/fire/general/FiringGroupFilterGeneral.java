package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_FIRE_NON_SUBS;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroupFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;

/**
 * Create firing groups with units that match {@link #firingUnitPredicate}
 *
 * <p>The firing groups are separated by canNotTarget, canNotBeTargetedBy, and isSuicideOnHit
 */
@Value(staticConstructor = "of")
public class FiringGroupFilterGeneral implements FiringGroupFilter {

  BattleState.Side side;

  Predicate<Unit> firingUnitPredicate;

  /** Name displayed in the Battle UI */
  String groupName;

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    Collection<Unit> canFire = battleState.filterUnits(ACTIVE, side);

    canFire = CollectionUtils.getMatches(canFire, firingUnitPredicate);

    // Remove offense allied units if allied air can not participate
    if (side == OFFENSE && !Properties.getAlliedAirIndependent(battleState.getGameData())) {
      canFire =
          CollectionUtils.getMatches(canFire, Matches.unitIsOwnedBy(battleState.getPlayer(side)));
    }
    final Collection<Unit> enemyUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, side.getOpposite()),
            PredicateBuilder.of(Matches.unitIsNotInfrastructure())
                .andIf(side == DEFENSE, Matches.unitIsSuicideOnAttack().negate())
                .andIf(side == OFFENSE, Matches.unitIsSuicideOnDefense().negate())
                .build());

    final List<FiringGroup> firingGroups = new ArrayList<>();

    final List<TargetGroup> targetGroups = TargetGroup.newTargetGroups(canFire, enemyUnits);

    if (targetGroups.size() == 1) {
      firingGroups.addAll(getFiringGroups(groupName, canFire, enemyUnits, targetGroups.get(0)));
    } else {
      // General firing groups don't have individual names so find commonly groups and give them
      // unique names
      final List<TargetGroup> airVsSubGroups =
          targetGroups.stream()
              .filter(this.filterAirVsSubTargetGroups(enemyUnits))
              .collect(Collectors.toList());
      generateNamedGroups(AIR_FIRE_NON_SUBS, firingGroups, airVsSubGroups, canFire, enemyUnits);
      targetGroups.removeAll(airVsSubGroups);

      generateNamedGroups(groupName, firingGroups, targetGroups, canFire, enemyUnits);
    }
    return firingGroups;
  }

  private List<FiringGroup> getFiringGroups(
      final String name,
      final Collection<Unit> canFire,
      final Collection<Unit> enemyUnits,
      final TargetGroup targetGroup) {
    final Collection<Unit> firingUnits = targetGroup.getFiringUnits(canFire);
    final Collection<Unit> targetUnits = targetGroup.getTargetUnits(enemyUnits);
    return FiringGroup.groupBySuicideOnHit(name, firingUnits, targetUnits);
  }

  private Predicate<TargetGroup> filterAirVsSubTargetGroups(final Collection<Unit> enemyUnits) {
    return (targetGroup) -> {
      final boolean allAir =
          targetGroup.getFiringUnitTypes().stream().allMatch(Matches.unitTypeIsAir());
      if (!allAir) {
        return false;
      }

      // check to see if any possible targeted sea unit type has "canNotBeTargetedByAll"
      // if that unit is not a target of this group, then this is most likely air vs subs
      final List<UnitType> canNotBeTargetedUnitTypes =
          enemyUnits.stream()
              .filter(Matches.unitCanNotBeTargetedByAll())
              .map(Unit::getType)
              .filter(Matches.unitTypeIsSea())
              .collect(Collectors.toList());
      final List<UnitType> targetTypes = new ArrayList<>(targetGroup.getTargetUnitTypes());
      targetTypes.retainAll(canNotBeTargetedUnitTypes);

      return targetTypes.isEmpty();
    };
  }

  private void generateNamedGroups(
      final String name,
      final List<FiringGroup> firingGroups,
      final List<TargetGroup> airVsSubGroups,
      final Collection<Unit> canFire,
      final Collection<Unit> enemyUnits) {
    if (airVsSubGroups.size() == 1) {
      firingGroups.addAll(getFiringGroups(name, canFire, enemyUnits, airVsSubGroups.get(0)));
    } else {
      for (final TargetGroup airVsSubGroup : airVsSubGroups) {
        final Optional<UnitType> unitType =
            airVsSubGroup.getFiringUnits(canFire).stream().map(Unit::getType).findFirst();
        if (unitType.isPresent()) {
          firingGroups.addAll(
              getFiringGroups(
                  name + " " + unitType.get().getName(), canFire, enemyUnits, airVsSubGroup));
        }
      }
    }
  }
}
