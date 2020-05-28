package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.triplea.java.collections.CollectionUtils;

public class StepStrings implements BattleStepStrings {

  public static List<String> determineStepStrings(
      final boolean canFireOffensiveAa,
      final boolean canFireDefendingAa,
      final boolean showFirstRun,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Collection<Unit> offensiveAa,
      final Collection<Unit> defendingAa,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> defendingWaitingToDie,
      final Territory battleSite,
      final GameData gameData,
      final Collection<Unit> bombardingUnits,
      final Function<Collection<Unit>, Collection<Unit>> getDependentUnits,
      final boolean canAttackerRetreatSubs,
      final boolean canAttackerRetreat,
      final boolean canAttackerRetreatPartialAmphib,
      final boolean canAttackerRetreatPlanes,
      final boolean canDefenderRetreatSubs,
      final boolean isBattleSiteWater) {
    final List<String> steps = new ArrayList<>();
    if (canFireOffensiveAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(offensiveAa)) {
        steps.add(attacker.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(defender.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(defender.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (canFireDefendingAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(defendingAa)) {
        steps.add(defender.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(attacker.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(attacker.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (showFirstRun) {
      if (!isBattleSiteWater && !bombardingUnits.isEmpty()) {
        steps.add(NAVAL_BOMBARDMENT);
        steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
      }
      if (!isBattleSiteWater && TechAttachment.isAirTransportable(attacker)) {
        final Collection<Unit> bombers =
            CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransport());
        if (!bombers.isEmpty()) {
          final Collection<Unit> dependents = getDependentUnits.apply(bombers);
          if (!dependents.isEmpty()) {
            steps.add(LAND_PARATROOPS);
          }
        }
      }
    }
    // Check if defending subs can submerge before battle
    if (Properties.getSubRetreatBeforeBattle(gameData)) {
      if (defendingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(attacker.getName() + SUBS_SUBMERGE);
      }
      if (attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(defender.getName() + SUBS_SUBMERGE);
      }
    }
    // See if there any unescorted transports
    if (isBattleSiteWater && Properties.getTransportCasualtiesRestricted(gameData)) {
      if (attackingUnits.stream().anyMatch(Matches.unitIsTransport())
          || defendingUnits.stream().anyMatch(Matches.unitIsTransport())) {
        steps.add(REMOVE_UNESCORTED_TRANSPORTS);
      }
    }
    final boolean defenderSubsFireFirst =
        defenderSubsFireFirst();
    final ReturnFire returnFireAgainstAttackingSubs =
        returnFireAgainstAttackingSubs();
    final ReturnFire returnFireAgainstDefendingSubs =
        returnFireAgainstDefendingSubs();
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove
    // casualties
    if (defenderSubsFireFirst && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
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
        !defendingSubsSneakAttack();
    if (!defendingSubsFireWithAllDefendersAlways
        && !defendingSubsFireWithAllDefenders
        && !defenderSubsFireFirst
        && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    if ((attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())
        || defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike()))
        && !defenderSubsFireFirst
        && !onlyAttackerSneakAttack
        && (returnFireAgainstDefendingSubs != ReturnFire.ALL
        || returnFireAgainstAttackingSubs != ReturnFire.ALL)) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    // Air units can't attack subs without Destroyers present
    if (attackingUnits.stream().anyMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(defendingUnits, attackingUnits)) {
      steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
      steps.add(AIR_ATTACK_NON_SUBS);
    }
    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(attacker.getName() + FIRE);
      steps.add(defender.getName() + SELECT_CASUALTIES);
    }
    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    final Collection<Unit> units = new ArrayList<>(defendingUnits);
    units.addAll(defendingWaitingToDie);
    if (units.stream().anyMatch(Matches.unitCanNotTargetAll())
        && !defenderSubsFireFirst
        && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    // Air Units can't attack subs without Destroyers present
    if (defendingUnits.stream().anyMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(attackingUnits, units)) {
      steps.add(AIR_DEFEND_NON_SUBS);
    }
    if (defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(defender.getName() + FIRE);
      steps.add(attacker.getName() + SELECT_CASUALTIES);
    }
    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat attacking subs
    if (attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (Properties.getSubmersibleSubs(gameData)) {
        if (!Properties.getSubRetreatBeforeBattle(gameData)) {
          steps.add(attacker.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (canAttackerRetreatSubs) {
          steps.add(attacker.getName() + SUBS_WITHDRAW);
        }
      }
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
    if (canAttackerRetreat
        || someAirAtSea
        || canAttackerRetreatPartialAmphib
        || canAttackerRetreatPlanes) {
      steps.add(attacker.getName() + ATTACKER_WITHDRAW);
    }
    // retreat defending subs
    if (defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (Properties.getSubmersibleSubs(gameData)) {
        if (!Properties.getSubRetreatBeforeBattle(gameData)) {
          steps.add(defender.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (canDefenderRetreatSubs) {
          steps.add(defender.getName() + SUBS_WITHDRAW);
        }
      }
    }
    return steps;
  }

  private static boolean canAirAttackSubs(
      final Collection<Unit> firedAt, final Collection<Unit> firing) {
    return firedAt.stream().noneMatch(Matches.unitCanNotBeTargetedByAll())
        || firing.stream().anyMatch(Matches.unitIsDestroyer());
  }

  private boolean canAttackerRetreatSubs() {
    if (defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return defendingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (canAttackerRetreat() || Properties.getSubmersibleSubs(gameData));
  }

  private boolean canDefenderRetreatSubs() {
    if (attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return attackingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (getEmptyOrFriendlySeaNeighbors.apply(
                        defender,
                        CollectionUtils.getMatches(defendingUnits, Matches.unitCanEvade()))
                    .size()
                != 0
            || Properties.getSubmersibleSubs(gameData));
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (isAmphibious && Properties.getPartialAmphibiousRetreat(gameData)) {
      // Only include land units when checking for allow amphibious retreat
      final List<Unit> landUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsLand());
      for (final Unit unit : landUnits) {
        if (!unit.getWasAmphibious()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean canAttackerRetreatPlanes() {
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && isAmphibious
        && attackingUnits.stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreat() {
    if (onlyDefenselessDefendingTransportsLeft()) {
      return false;
    }
    if (isAmphibious) {
      return false;
    }
    return !getAttackerRetreatTerritories.get().isEmpty();
  }

  private boolean onlyDefenselessDefendingTransportsLeft() {
    return Properties.getTransportCasualtiesRestricted(gameData)
        && !defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsTransportButNotCombatTransport());
  }

  private boolean defenderSubsFireFirst() {
    return returnFireAgainstAttackingSubs() == ReturnFire.ALL
        && returnFireAgainstDefendingSubs() == ReturnFire.NONE;
  }

  private ReturnFire returnFireAgainstAttackingSubs() {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttackAndNoAttackingDestroyers();
    final ReturnFire returnFireAgainstAttackingSubs;
    if (!attackingSubsSneakAttack) {
      returnFireAgainstAttackingSubs = ReturnFire.ALL;
    } else if (defendingSubsSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstAttackingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  private ReturnFire returnFireAgainstDefendingSubs() {
    final boolean attackingSubsSneakAttack =
        defendingUnits.stream().noneMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttackAndNoAttackingDestroyers();
    final ReturnFire returnFireAgainstDefendingSubs;
    if (!defendingSubsSneakAttack) {
      returnFireAgainstDefendingSubs = ReturnFire.ALL;
    } else if (attackingSubsSneakAttack || Properties.getWW2V2(gameData)) {
      returnFireAgainstDefendingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstDefendingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstDefendingSubs;
  }

  private boolean defendingSubsSneakAttackAndNoAttackingDestroyers() {
    return attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && defendingSubsSneakAttack();
  }

  private boolean defendingSubsSneakAttack() {
    return Properties.getWW2V2(gameData) || Properties.getDefendingSubsSneakAttack(gameData);
  }
}
