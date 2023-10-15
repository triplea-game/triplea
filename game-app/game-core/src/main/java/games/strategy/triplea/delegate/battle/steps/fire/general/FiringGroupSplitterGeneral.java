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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;

/**
 * Create firing groups using units
 *
 * <p>The canNotTarget and canNotBeTargetedBy attributes will define what the firing groups are. If
 * there is a target unit that can be targeted by one unit but not by the other, then two firing
 * groups will be created. Both will have the same target unit but the firing unit will be
 * different.
 *
 * <p>If there are multiple isSuicideOnHit unit types in the same group, then there will be one
 * firing group for each of the isSuicideOnHit unit types and one firing group for all the other
 * unit types.
 *
 * <p>See {@link FiringGroup} for why isSuicideOnHit needs to be separated by unit type.
 */
@Value(staticConstructor = "of")
public class FiringGroupSplitterGeneral
    implements Function<BattleState, Collection<FiringGroup>>, Serializable {
  private static final long serialVersionUID = -8563922735558824822L;

  public enum Type {
    NORMAL,
    FIRST_STRIKE
  }

  BattleState.Side side;

  Type type;

  /** Name displayed in the Battle UI */
  String groupName;

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    final Collection<Unit> ourUnits = battleState.filterUnits(ACTIVE, side);
    final Collection<Unit> enemyUnits = battleState.filterUnits(ALIVE, side.getOpposite());
    final Collection<Unit> enemyCombatants =
        CollectionUtils.getMatches(
            getCombatParticipants(battleState, side.getOpposite(), enemyUnits, ourUnits),
            PredicateBuilder.of(Matches.unitIsNotInfrastructure())
                .andIf(side == DEFENSE, Matches.unitIsSuicideOnAttack().negate())
                .andIf(side == OFFENSE, Matches.unitIsSuicideOnDefense().negate())
                .build());
    final Collection<Unit> canFire =
        CollectionUtils.getMatches(
            getCombatParticipants(battleState, side, ourUnits, enemyUnits),
            PredicateBuilder.of(getFiringUnitPredicate(battleState))
                // Remove offense allied units if allied air can not participate
                .andIf(
                    side == OFFENSE
                        && !Properties.getAlliedAirIndependent(
                            battleState.getGameData().getProperties()),
                    Matches.unitIsOwnedBy(battleState.getPlayer(side)))
                .build());

    final List<FiringGroup> firingGroups = new ArrayList<>();
    final List<TargetGroup> targetGroups = TargetGroup.newTargetGroups(canFire, enemyCombatants);
    if (targetGroups.size() == 1) {
      firingGroups.addAll(
          buildFiringGroups(groupName, canFire, enemyCombatants, targetGroups.get(0)));
    } else {
      // General firing groups don't have individual names so find commonly used groups and
      // give them unique names
      final List<TargetGroup> airVsSubGroups =
          CollectionUtils.getMatches(targetGroups, filterAirVsSubTargetGroups(enemyUnits));
      generateNamedGroups(
          AIR_FIRE_NON_SUBS, firingGroups, airVsSubGroups, canFire, enemyCombatants);
      targetGroups.removeAll(airVsSubGroups);
      generateNamedGroups(groupName, firingGroups, targetGroups, canFire, enemyCombatants);
    }
    return firingGroups;
  }

  private Collection<Unit> getCombatParticipants(
      BattleState battleState,
      BattleState.Side side,
      Collection<Unit> units,
      Collection<Unit> enemyUnits) {
    // Filter participants (same as is done in MustFightBattle.removeNonCombatants()), so that we
    // don't end up generating combat step names for units that will be excluded.
    return CollectionUtils.getMatches(
        units,
        Matches.unitCanParticipateInCombat(
            side == OFFENSE,
            battleState.getPlayer(side),
            battleState.getBattleSite(),
            1,
            enemyUnits));
  }

  private Predicate<Unit> getFiringUnitPredicate(final BattleState battleState) {
    final Predicate<Unit> predicate =
        (side == OFFENSE)
            ? Matches.unitIsFirstStrike()
            : Matches.unitIsFirstStrikeOnDefense(battleState.getGameData().getProperties());
    return type == Type.NORMAL ? predicate.negate() : predicate;
  }

  private List<FiringGroup> buildFiringGroups(
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
      final Collection<FiringGroup> firingGroups,
      final Collection<TargetGroup> targetGroups,
      final Collection<Unit> canFire,
      final Collection<Unit> enemyUnits) {
    if (targetGroups.size() == 1) {
      firingGroups.addAll(
          buildFiringGroups(name, canFire, enemyUnits, CollectionUtils.getAny(targetGroups)));
    } else {
      // use the first unitType name of each TargetGroup as a suffix for the FiringGroup name
      for (final TargetGroup targetGroup : targetGroups) {
        // Sort units first, before getting the unit type to use for the name, so that we choose
        // the same name regardless of the order of units provided.
        List<Unit> firingUnits = targetGroup.getFiringUnits(canFire);
        firingUnits.sort(Comparator.comparing(u -> u.getType().getName()));
        UnitType type = CollectionUtils.getAny(firingUnits).getType();
        firingGroups.addAll(
            buildFiringGroups(name + " " + type.getName(), canFire, enemyUnits, targetGroup));
      }
    }
  }
}
