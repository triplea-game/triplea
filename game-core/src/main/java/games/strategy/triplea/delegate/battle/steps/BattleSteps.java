package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_DEFENSIVE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_DEFENSIVE_REGULAR;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_OFFENSIVE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_OFFENSIVE_REGULAR;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.steps.change.LandParatroopers;
import games.strategy.triplea.delegate.battle.steps.change.RemoveUnprotectedUnits;
import games.strategy.triplea.delegate.battle.steps.fire.NavalBombardment;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirAttackVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirDefendVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.ClearFirstStrikeCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.general.DefensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.fire.general.OffensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.retreat.DefensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/** Get the steps that will occurr in the battle */
@Builder
public class BattleSteps implements BattleStepStrings, BattleState {

  @Getter(onMethod = @__({@Override}))
  final int battleRound;

  @Getter(onMethod = @__({@Override}))
  final UUID battleId;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer attacker;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer defender;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> offensiveAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> attackingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> attackingWaitingToDie;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingWaitingToDie;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Territory battleSite;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GameData gameData;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> bombardingUnits;

  final @NonNull Function<Collection<Unit>, Collection<Unit>> getDependentUnits;
  final @NonNull Boolean isAmphibious;
  final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories;
  final @NonNull Function<Collection<Unit>, Collection<Territory>> getEmptyOrFriendlySeaNeighbors;
  final @NonNull BattleActions battleActions;

  final @NonNull Boolean isOver;

  @Override
  public Collection<Territory> getAttackerRetreatTerritories() {
    return getAttackerRetreatTerritories.get();
  }

  @Override
  public Collection<Territory> getEmptyOrFriendlySeaNeighbors(final Collection<Unit> units) {
    return getEmptyOrFriendlySeaNeighbors.apply(units);
  }

  @Override
  public Collection<Unit> getDependentUnits(final Collection<Unit> units) {
    return getDependentUnits.apply(units);
  }

  @Override
  public Collection<Unit> getUnits(final Side... sides) {
    final Collection<Unit> units = new ArrayList<>();
    for (final Side side : sides) {
      switch (side) {
        case OFFENSE:
          units.addAll(attackingUnits);
          break;
        case DEFENSE:
          units.addAll(defendingUnits);
          break;
        default:
          break;
      }
    }
    return units;
  }

  @Override
  public Collection<Unit> getWaitingToDie(final EnumSet<Side> sides) {
    final Collection<Unit> waitingToDie = new ArrayList<>();
    if (sides.contains(Side.OFFENSE)) {
      waitingToDie.addAll(attackingWaitingToDie);
    }
    if (sides.contains(Side.DEFENSE)) {
      waitingToDie.addAll(defendingWaitingToDie);
    }
    return waitingToDie;
  }

  @Override
  public void clearWaitingToDie(final EnumSet<Side> sides) {
    if (sides.contains(Side.OFFENSE)) {
      attackingWaitingToDie.clear();
    }
    if (sides.contains(Side.DEFENSE)) {
      defendingWaitingToDie.clear();
    }
  }

  @Override
  public boolean isOver() {
    return isOver;
  }

  @Override
  public boolean isAmphibious() {
    return isAmphibious;
  }

  public List<String> get() {
    final boolean isBattleSiteWater = battleSite.isWater();

    final BattleStep offensiveAaStep = new OffensiveAaFire(this, battleActions);
    final BattleStep defensiveAaStep = new DefensiveAaFire(this, battleActions);
    final BattleStep submergeSubsVsOnlyAir = new SubmergeSubsVsOnlyAirStep(this, battleActions);
    final BattleStep removeUnprotectedUnits = new RemoveUnprotectedUnits(this, battleActions);
    final BattleStep airAttackVsNonSubs = new AirAttackVsNonSubsStep(this);
    final BattleStep airDefendVsNonSubs = new AirDefendVsNonSubsStep(this);
    final BattleStep navalBombardment = new NavalBombardment(this, battleActions);
    final BattleStep landParatroopers = new LandParatroopers(this, battleActions);
    final BattleStep offensiveSubsSubmerge = new OffensiveSubsRetreat(this, battleActions);
    final BattleStep defensiveSubsSubmerge = new DefensiveSubsRetreat(this, battleActions);
    final BattleStep offensiveFirstStrike = new OffensiveFirstStrike(this, battleActions);
    final BattleStep defensiveFirstStrike = new DefensiveFirstStrike(this, battleActions);
    final BattleStep firstStrikeCasualties = new ClearFirstStrikeCasualties(this, battleActions);
    final BattleStep offensiveStandard = new OffensiveGeneral(this, battleActions);
    final BattleStep defensiveStandard = new DefensiveGeneral(this, battleActions);

    final List<String> steps = new ArrayList<>();
    steps.addAll(offensiveAaStep.getNames());
    steps.addAll(defensiveAaStep.getNames());

    steps.addAll(navalBombardment.getNames());
    steps.addAll(landParatroopers.getNames());

    if (offensiveSubsSubmerge.getOrder() == SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE) {
      steps.addAll(offensiveSubsSubmerge.getNames());
    }
    if (defensiveSubsSubmerge.getOrder() == SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE) {
      steps.addAll(defensiveSubsSubmerge.getNames());
    }
    steps.addAll(removeUnprotectedUnits.getNames());
    steps.addAll(submergeSubsVsOnlyAir.getNames());

    if (offensiveFirstStrike.getOrder() == FIRST_STRIKE_OFFENSIVE) {
      steps.addAll(offensiveFirstStrike.getNames());
    }
    if (defensiveFirstStrike.getOrder() == FIRST_STRIKE_DEFENSIVE) {
      steps.addAll(defensiveFirstStrike.getNames());
    }
    steps.addAll(firstStrikeCasualties.getNames());

    if (offensiveFirstStrike.getOrder() == FIRST_STRIKE_OFFENSIVE_REGULAR) {
      steps.addAll(offensiveFirstStrike.getNames());
    }
    steps.addAll(airAttackVsNonSubs.getNames());
    steps.addAll(offensiveStandard.getNames());

    if (defensiveFirstStrike.getOrder() == FIRST_STRIKE_DEFENSIVE_REGULAR) {
      steps.addAll(defensiveFirstStrike.getNames());
    }
    steps.addAll(airDefendVsNonSubs.getNames());
    steps.addAll(defensiveStandard.getNames());

    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat attacking subs
    if (offensiveSubsSubmerge.getOrder() == SUB_OFFENSIVE_RETREAT_AFTER_BATTLE) {
      steps.addAll(offensiveSubsSubmerge.getNames());
    }
    // if we are a sea zone, then we may not be able to retreat
    // (ie a sub traveled under another unit to get to the battle site)
    // or an enemy sub retreated to our sea zone
    // however, if all our sea units die, then the air units can still retreat, so if we have any
    // air units attacking in
    // a sea zone, we always have to have the retreat option shown
    // later, if our sea units die, we may ask the user to retreat
    final boolean someAirAtSea =
        isBattleSiteWater && attackingUnits.stream().anyMatch(Matches.unitIsAir());
    if (RetreatChecks.canAttackerRetreat(
            defendingUnits, gameData, getAttackerRetreatTerritories, isAmphibious)
        || someAirAtSea
        || RetreatChecks.canAttackerRetreatPartialAmphib(attackingUnits, gameData, isAmphibious)
        || RetreatChecks.canAttackerRetreatPlanes(attackingUnits, gameData, isAmphibious)) {
      steps.add(attacker.getName() + ATTACKER_WITHDRAW);
    }
    if (defensiveSubsSubmerge.getOrder() == SUB_DEFENSIVE_RETREAT_AFTER_BATTLE) {
      steps.addAll(defensiveSubsSubmerge.getNames());
    }
    return steps;
  }
}
