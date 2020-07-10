package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.battle.steps.change.LandParatroopers;
import games.strategy.triplea.delegate.battle.steps.fire.NavalBombardment;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirAttackVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirDefendVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.retreat.DefensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    final BattleStep airAttackVsNonSubs = new AirAttackVsNonSubsStep(this);
    final BattleStep airDefendVsNonSubs = new AirDefendVsNonSubsStep(this);
    final BattleStep navalBombardment = new NavalBombardment(this, battleActions);
    final BattleStep landParatroopers = new LandParatroopers(this, battleActions);
    final BattleStep offensiveSubsSubmerge = new OffensiveSubsRetreat(this, battleActions);
    final BattleStep defensiveSubsSubmerge = new DefensiveSubsRetreat(this, battleActions);

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
    // See if there any unescorted transports
    if (isBattleSiteWater
        && Properties.getTransportCasualtiesRestricted(gameData)
        && (attackingUnits.stream().anyMatch(Matches.unitIsTransport())
            || defendingUnits.stream().anyMatch(Matches.unitIsTransport()))) {
      steps.add(REMOVE_UNESCORTED_TRANSPORTS);
    }
    steps.addAll(submergeSubsVsOnlyAir.getNames());

    final boolean defenderSubsFireFirst =
        SubsChecks.defenderSubsFireFirst(attackingUnits, defendingUnits, gameData);
    final ReturnFire returnFireAgainstAttackingSubs =
        SubsChecks.returnFireAgainstAttackingSubs(attackingUnits, defendingUnits, gameData);
    final ReturnFire returnFireAgainstDefendingSubs =
        SubsChecks.returnFireAgainstDefendingSubs(attackingUnits, defendingUnits, gameData);
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove
    // casualties
    if (defenderSubsFireFirst
        && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(gameData))) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    final boolean onlyAttackerSneakAttack =
        !defenderSubsFireFirst
            && returnFireAgainstAttackingSubs == ReturnFire.NONE
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;
    // attacker subs sneak attack, no sneak attack if destroyers are present
    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(attacker.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      if (onlyAttackerSneakAttack) {
        steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
      }
    }
    // ww2v2 rules, all subs fire FIRST in combat, regardless of presence of destroyers.
    final boolean defendingSubsFireWithAllDefenders =
        !defenderSubsFireFirst
            && !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;
    // defender subs sneak attack, no sneak attack in Pacific/Europe Theaters or if destroyers are
    // present
    final boolean defendingSubsFireWithAllDefendersAlways =
        !SubsChecks.defendingSubsSneakAttack(gameData);
    if (!defendingSubsFireWithAllDefendersAlways
        && !defendingSubsFireWithAllDefenders
        && !defenderSubsFireFirst
        && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(gameData))) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    if ((attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())
            || defendingUnits.stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(gameData)))
        && !defenderSubsFireFirst
        && !onlyAttackerSneakAttack
        && (returnFireAgainstDefendingSubs != ReturnFire.ALL
            || returnFireAgainstAttackingSubs != ReturnFire.ALL)) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }

    steps.addAll(airAttackVsNonSubs.getNames());

    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(attacker.getName() + FIRE);
      steps.add(defender.getName() + SELECT_CASUALTIES);
    }
    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    final Collection<Unit> defendingUnitsAliveAndDamaged = new ArrayList<>(defendingUnits);
    defendingUnitsAliveAndDamaged.addAll(defendingWaitingToDie);
    if (defendingUnitsAliveAndDamaged.stream()
            .anyMatch(Matches.unitIsFirstStrikeOnDefense(gameData))
        && !defenderSubsFireFirst
        && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    steps.addAll(airDefendVsNonSubs.getNames());
    if (defendingUnits.stream().anyMatch(Matches.unitIsFirstStrikeOnDefense(gameData).negate())) {
      steps.add(defender.getName() + FIRE);
      steps.add(attacker.getName() + SELECT_CASUALTIES);
    }
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
