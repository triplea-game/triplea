package games.strategy.triplea.delegate;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractUserActionAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.DependentBattle;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Useful match interfaces.
 *
 * <p>Rather than writing code like,
 *
 * <pre>
 * boolean hasLand = false;
 * for (final Unit unit : someCollection) {
 *   UnitAttachment ua = UnitAttachment.get(unit.getType());
 *   if (ua.isAir) {
 *     hasAir = true;
 *     break;
 *   }
 * }
 * </pre>
 *
 * <p>You can write code like,
 *
 * <pre>
 * boolean hasLand = Match.anyMatch(someCollection, Matches.unitIsAir());
 * </pre>
 *
 * <p>The benefits should be obvious to any right minded person.
 */
public final class Matches {
  private Matches() {}

  public static <T> Predicate<T> always() {
    return it -> true;
  }

  public static <T> Predicate<T> never() {
    return it -> false;
  }

  public static Predicate<UnitType> unitTypeHasMoreThanOneHitPointTotal() {
    return ut -> UnitAttachment.get(ut).getHitPoints() > 1;
  }

  public static Predicate<Unit> unitHasMoreThanOneHitPointTotal() {
    return unit -> unitTypeHasMoreThanOneHitPointTotal().test(unit.getType());
  }

  public static Predicate<Unit> unitHasTakenSomeDamage() {
    return unit -> unit.getHits() > 0;
  }

  public static Predicate<Unit> unitHasNotTakenAnyDamage() {
    return unitHasTakenSomeDamage().negate();
  }

  public static Predicate<Unit> unitIsSea() {
    return unit -> UnitAttachment.get(unit.getType()).getIsSea();
  }

  public static Predicate<Unit> unitHasSubBattleAbilities() {
    return unitCanEvade().or(unitIsFirstStrike()).or(unitCanNotBeTargetedByAll());
  }

  public static Predicate<Unit> unitCanEvade() {
    return unit -> UnitAttachment.get(unit.getType()).getCanEvade();
  }

  public static Predicate<Unit> unitIsFirstStrike() {
    return unit -> UnitAttachment.get(unit.getType()).getIsFirstStrike();
  }

  public static Predicate<Unit> unitIsFirstStrikeOnDefense(final GameProperties properties) {
    Predicate<Unit> matcher = Matches.unitIsFirstStrike();

    // units with the deprecated isSuicide attribute automatically get isFirstStrike
    // but they shouldn't have first strike on defense if
    // DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE is true.
    if (Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(properties)) {
      matcher =
          matcher.and(
              // normal isFirstStrike units won't have suicideOnAttack
              Matches.unitIsSuicideOnAttack()
                  .negate()
                  // deprecated isSuicide units won't have suicideOnDefense
                  .or(Matches.unitIsSuicideOnDefense()));
    }
    return matcher;
  }

  public static Predicate<Unit> unitCanMoveThroughEnemies() {
    return unit -> UnitAttachment.get(unit.getType()).getCanMoveThroughEnemies();
  }

  public static Predicate<Unit> unitCanBeMovedThroughByEnemies() {
    return unit -> UnitAttachment.get(unit.getType()).getCanBeMovedThroughByEnemies();
  }

  public static Predicate<Unit> unitCanNotTargetAll() {
    return unit -> !UnitAttachment.get(unit.getType()).getCanNotTarget().isEmpty();
  }

  public static Predicate<Unit> unitCanNotBeTargetedByAll() {
    return unit -> !UnitAttachment.get(unit.getType()).getCanNotBeTargetedBy().isEmpty();
  }

  private static Predicate<Unit> unitIsCombatTransport() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsCombatTransport() && ua.getIsSea();
    };
  }

  public static Predicate<Unit> unitIsNotCombatTransport() {
    return unitIsCombatTransport().negate();
  }

  public static Predicate<Unit> unitIsTransportButNotCombatTransport() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport();
    };
  }

  public static Predicate<Unit> unitIsNotTransportButCouldBeCombatTransport() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() == -1 || (ua.getIsCombatTransport() && ua.getIsSea());
    };
  }

  public static Predicate<Unit> unitIsDestroyer() {
    return unit -> UnitAttachment.get(unit.getType()).getIsDestroyer();
  }

  public static Predicate<UnitType> unitTypeIsDestroyer() {
    return type -> UnitAttachment.get(type).getIsDestroyer();
  }

  public static Predicate<Unit> unitIsTransport() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea();
    };
  }

  public static Predicate<Unit> unitIsNotTransport() {
    return unitIsTransport().negate();
  }

  public static Predicate<Unit> unitIsTransportAndNotDestroyer() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !unitIsDestroyer().test(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea();
    };
  }

  public static Predicate<UnitType> unitTypeIsStrategicBomber() {
    return obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua != null && ua.getIsStrategicBomber();
    };
  }

  public static Predicate<Unit> unitIsStrategicBomber() {
    return obj -> unitTypeIsStrategicBomber().test(obj.getType());
  }

  public static Predicate<Unit> unitIsNotStrategicBomber() {
    return unitIsStrategicBomber().negate();
  }

  public static Predicate<Unit> unitHasMoved() {
    return unit -> unit.hasMoved();
  }

  public static Predicate<Unit> unitHasNotMoved() {
    return unitHasMoved().negate();
  }

  public static Predicate<Unit> unitHasNotBeenChargedFlatFuelCost() {
    return unit -> !unit.getChargedFlatFuelCost();
  }

  // TODO: this should really be improved to check more properties like support attachments
  public static Predicate<Unit> unitCanAttack(final GamePlayer gamePlayer) {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getMovement(gamePlayer) > 0
          && (ua.getAttack(gamePlayer) > 0 || ua.getOffensiveAttackAa(gamePlayer) > 0);
    };
  }

  public static Predicate<Unit> unitHasAttackValueOfAtLeast(final int attackValue) {
    return unit -> UnitAttachment.get(unit.getType()).getAttack(unit.getOwner()) >= attackValue;
  }

  public static Predicate<Unit> unitHasDefendValueOfAtLeast(final int defendValue) {
    return unit -> UnitAttachment.get(unit.getType()).getDefense(unit.getOwner()) >= defendValue;
  }

  public static Predicate<Unit> unitIsEnemyOf(
      final RelationshipTracker relationshipTracker, final GamePlayer player) {
    return unit -> relationshipTracker.isAtWar(unit.getOwner(), player);
  }

  public static Predicate<Unit> unitIsNotSea() {
    return unit -> !UnitAttachment.get(unit.getType()).getIsSea();
  }

  public static Predicate<UnitType> unitTypeIsSea() {
    return type -> UnitAttachment.get(type).getIsSea();
  }

  public static Predicate<UnitType> unitTypeIsNotSea() {
    return type -> !UnitAttachment.get(type).getIsSea();
  }

  public static Predicate<UnitType> unitTypeIsSeaOrAir() {
    return type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsSea() || ua.getIsAir();
    };
  }

  public static Predicate<Unit> unitIsAir() {
    return unit -> UnitAttachment.get(unit.getType()).getIsAir();
  }

  public static Predicate<Unit> unitIsNotAir() {
    return unit -> !UnitAttachment.get(unit.getType()).getIsAir();
  }

  public static Predicate<UnitType> unitTypeCanBombard(final GamePlayer gamePlayer) {
    return type -> UnitAttachment.get(type).getCanBombard(gamePlayer);
  }

  static Predicate<Unit> unitCanBeGivenByTerritoryTo(final GamePlayer player) {
    return unit -> UnitAttachment.get(unit.getType()).getCanBeGivenByTerritoryTo().contains(player);
  }

  public static Predicate<Unit> unitCanBeCapturedOnEnteringToInThisTerritory(
      final GamePlayer player, final Territory terr, final GameProperties properties) {
    return unit -> {
      if (!Properties.getCaptureUnitsOnEnteringTerritory(properties)) {
        return false;
      }
      final GamePlayer unitOwner = unit.getOwner();
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final boolean unitCanBeCapturedByPlayer = ua.getCanBeCapturedOnEnteringBy().contains(player);
      final TerritoryAttachment ta = TerritoryAttachment.get(terr);
      if (ta == null) {
        return false;
      }
      if (ta.getCaptureUnitOnEnteringBy() == null) {
        return false;
      }
      final boolean territoryCanHaveUnitsThatCanBeCapturedByPlayer =
          ta.getCaptureUnitOnEnteringBy().contains(player);
      final PlayerAttachment pa = PlayerAttachment.get(unitOwner);
      if (pa == null) {
        return false;
      }
      if (pa.getCaptureUnitOnEnteringBy() == null) {
        return false;
      }
      final boolean unitOwnerCanLetUnitsBeCapturedByPlayer =
          pa.getCaptureUnitOnEnteringBy().contains(player);
      return (unitCanBeCapturedByPlayer
          && territoryCanHaveUnitsThatCanBeCapturedByPlayer
          && unitOwnerCanLetUnitsBeCapturedByPlayer);
    };
  }

  public static Predicate<Unit> unitDestroyedWhenCapturedByOrFrom(final GamePlayer playerBy) {
    return unitDestroyedWhenCapturedBy(playerBy).or(unitDestroyedWhenCapturedFrom());
  }

  private static Predicate<Unit> unitDestroyedWhenCapturedBy(final GamePlayer playerBy) {
    return u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (ua.getDestroyedWhenCapturedBy().isEmpty()) {
        return false;
      }
      for (final Tuple<String, GamePlayer> tuple : ua.getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("BY") && tuple.getSecond().equals(playerBy)) {
          return true;
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitDestroyedWhenCapturedFrom() {
    return u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (ua.getDestroyedWhenCapturedBy().isEmpty()) {
        return false;
      }
      for (final Tuple<String, GamePlayer> tuple : ua.getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("FROM") && tuple.getSecond().equals(u.getOwner())) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Unit> unitIsAirBase() {
    return unit -> UnitAttachment.get(unit.getType()).getIsAirBase();
  }

  public static Predicate<UnitType> unitTypeCanBeDamaged() {
    return ut -> UnitAttachment.get(ut).getCanBeDamaged();
  }

  public static Predicate<Unit> unitCanBeDamaged() {
    return unit -> unitTypeCanBeDamaged().test(unit.getType());
  }

  public static Predicate<Unit> unitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return true;
      }
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
          unit.getData().getProperties())) {
        return unit.getUnitDamage() >= unit.getHowMuchDamageCanThisUnitTakeTotal(t);
      }
      return false;
    };
  }

  public static Predicate<Unit> unitIsLegalBombingTargetBy(final Unit bomberOrRocket) {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(bomberOrRocket.getType());
      final Set<UnitType> allowedTargets =
          ua.getBombingTargets(bomberOrRocket.getData().getUnitTypeList());
      return allowedTargets == null || allowedTargets.contains(unit.getType());
    };
  }

  public static Predicate<Unit> unitHasTakenSomeBombingUnitDamage() {
    return unit -> unit.getUnitDamage() > 0;
  }

  public static Predicate<Unit> unitHasNotTakenAnyBombingUnitDamage() {
    return unitHasTakenSomeBombingUnitDamage().negate();
  }

  public static Predicate<Unit> unitIsDisabled() {
    return unit -> {
      if (!unitCanBeDamaged().test(unit)) {
        return false;
      }
      if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
          unit.getData().getProperties())) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getMaxOperationalDamage() < 0) {
        // factories may or may not have max operational damage set, so we must still determine here
        // assume that if maxOperationalDamage < 0, then the max damage must be based on the
        // territory value (if the
        // damage >= production of territory, then we are disabled)
        // TerritoryAttachment ta = TerritoryAttachment.get(t);
        // return taUnit.getUnitDamage() >= ta.getProduction();
        return false;
      }
      // only greater than. if == then we can still operate
      return unit.getUnitDamage() > ua.getMaxOperationalDamage();
    };
  }

  public static Predicate<Unit> unitIsNotDisabled() {
    return unitIsDisabled().negate();
  }

  public static Predicate<Unit> unitCanDieFromReachingMaxDamage() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getCanBeDamaged() && ua.getCanDieFromReachingMaxDamage();
    };
  }

  public static Predicate<UnitType> unitTypeIsInfrastructure() {
    return ut -> UnitAttachment.get(ut).getIsInfrastructure();
  }

  public static Predicate<Unit> unitIsInfrastructure() {
    return unit -> unitTypeIsInfrastructure().test(unit.getType());
  }

  public static Predicate<Unit> unitIsNotInfrastructure() {
    return unitIsInfrastructure().negate();
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA
   * ability.
   */
  public static Predicate<Unit> unitIsSupporterOrHasCombatAbility(final boolean attack) {
    return unit ->
        unitTypeIsSupporterOrHasCombatAbility(attack, unit.getOwner()).test(unit.getType());
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA
   * ability.
   */
  private static Predicate<UnitType> unitTypeIsSupporterOrHasCombatAbility(
      final boolean attack, final GamePlayer player) {
    return ut -> {
      // if unit has attack or defense, return true
      final UnitAttachment ua = UnitAttachment.get(ut);
      if (attack && ua.getAttack(player) > 0) {
        return true;
      }
      if (!attack && ua.getDefense(player) > 0) {
        return true;
      }
      // if unit can support other units, return true
      return !UnitSupportAttachment.get(ut).isEmpty();
    };
  }

  public static Predicate<UnitSupportAttachment> unitSupportAttachmentCanBeUsedByPlayer(
      final GamePlayer player) {
    return usa -> usa.getPlayers().contains(player);
  }

  public static Predicate<Unit> unitCanScramble() {
    return unit -> UnitAttachment.get(unit.getType()).getCanScramble();
  }

  public static Predicate<Unit> unitWasScrambled() {
    return Unit::getWasScrambled;
  }

  public static Predicate<Unit> unitWasInAirBattle() {
    return Unit::getWasInAirBattle;
  }

  public static Predicate<Unit> unitCanBombard(final GamePlayer gamePlayer) {
    return unit -> UnitAttachment.get(unit.getType()).getCanBombard(gamePlayer);
  }

  public static Predicate<Unit> unitCanBlitz() {
    return unit -> UnitAttachment.get(unit.getType()).getCanBlitz(unit.getOwner());
  }

  public static Predicate<Unit> unitIsLandTransport() {
    return unit -> UnitAttachment.get(unit.getType()).getIsLandTransport();
  }

  public static Predicate<Unit> unitIsLandTransportWithCapacity() {
    return unit -> unitIsLandTransport().and(unitCanTransport()).test(unit);
  }

  public static Predicate<Unit> unitIsLandTransportWithoutCapacity() {
    return unit -> unitIsLandTransport().and(unitCanTransport().negate()).test(unit);
  }

  public static Predicate<Unit> unitIsNotInfrastructureAndNotCapturedOnEntering(
      final GamePlayer player, final Territory terr, final GameProperties properties) {
    return unit ->
        !UnitAttachment.get(unit.getType()).getIsInfrastructure()
            && !unitCanBeCapturedOnEnteringToInThisTerritory(player, terr, properties).test(unit);
  }

  public static Predicate<UnitType> unitTypeIsSuicideOnAttack() {
    return type -> UnitAttachment.get(type).getIsSuicideOnAttack();
  }

  public static Predicate<UnitType> unitTypeIsSuicideOnDefense() {
    return type -> UnitAttachment.get(type).getIsSuicideOnDefense();
  }

  public static Predicate<Unit> unitIsSuicideOnAttack() {
    return unit -> UnitAttachment.get(unit.getType()).getIsSuicideOnAttack();
  }

  public static Predicate<Unit> unitIsSuicideOnDefense() {
    return unit -> UnitAttachment.get(unit.getType()).getIsSuicideOnDefense();
  }

  public static Predicate<Unit> unitIsSuicideOnHit() {
    return unit -> UnitAttachment.get(unit.getType()).getIsSuicideOnHit();
  }

  public static Predicate<Unit> unitIsKamikaze() {
    return unit -> UnitAttachment.get(unit.getType()).getIsKamikaze();
  }

  public static Predicate<UnitType> unitTypeIsAir() {
    return type -> UnitAttachment.get(type).getIsAir();
  }

  private static Predicate<UnitType> unitTypeIsNotAir() {
    return type -> !UnitAttachment.get(type).getIsAir();
  }

  public static Predicate<Unit> unitCanLandOnCarrier() {
    return unit -> UnitAttachment.get(unit.getType()).getCarrierCost() != -1;
  }

  public static Predicate<Unit> unitIsCarrier() {
    return unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1;
  }

  public static Predicate<Territory> territoryHasOwnedCarrier(final GamePlayer player) {
    return t -> t.getUnitCollection().anyMatch(unitIsOwnedBy(player).and(unitIsCarrier()));
  }

  public static Predicate<Unit> unitIsAlliedCarrier(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return unit ->
        UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
            && relationshipTracker.isAllied(player, unit.getOwner());
  }

  public static Predicate<Unit> unitCanBeTransported() {
    return unit -> UnitAttachment.get(unit.getType()).getTransportCost() != -1;
  }

  public static Predicate<Unit> unitWasAmphibious() {
    return Unit::getWasAmphibious;
  }

  public static Predicate<Unit> unitWasNotAmphibious() {
    return unitWasAmphibious().negate();
  }

  public static Predicate<Unit> unitWasInCombat() {
    return Unit::getWasInCombat;
  }

  public static Predicate<Unit> unitWasUnloadedThisTurn() {
    return obj -> obj.getUnloadedTo() != null;
  }

  private static Predicate<Unit> unitWasLoadedThisTurn() {
    return Unit::getWasLoadedThisTurn;
  }

  static Predicate<Unit> unitWasNotLoadedThisTurn() {
    return unitWasLoadedThisTurn().negate();
  }

  public static Predicate<Unit> unitCanTransport() {
    return unit -> UnitAttachment.get(unit.getType()).getTransportCapacity() != -1;
  }

  public static Predicate<UnitType> unitTypeCanProduceUnits() {
    return obj -> UnitAttachment.get(obj).getCanProduceUnits();
  }

  public static Predicate<Unit> unitCanProduceUnits() {
    return obj -> unitTypeCanProduceUnits().test(obj.getType());
  }

  public static Predicate<UnitType> unitTypeHasMaxBuildRestrictions() {
    return type -> UnitAttachment.get(type).getMaxBuiltPerPlayer() >= 0;
  }

  public static Predicate<UnitType> unitTypeIsRocket() {
    return obj -> UnitAttachment.get(obj).getIsRocket();
  }

  static Predicate<Unit> unitIsRocket() {
    return obj -> unitTypeIsRocket().test(obj.getType());
  }

  public static Predicate<Unit> unitHasMovementLimit() {
    return obj -> UnitAttachment.get(obj.getType()).getMovementLimit() != null;
  }

  public static Predicate<Unit> unitHasAttackingLimit() {
    return obj -> UnitAttachment.get(obj.getType()).getAttackingLimit() != null;
  }

  public static Predicate<UnitType> unitTypeCanNotMoveDuringCombatMove() {
    return type -> UnitAttachment.get(type).getCanNotMoveDuringCombatMove();
  }

  public static Predicate<Unit> unitCanNotMoveDuringCombatMove() {
    return obj -> unitTypeCanNotMoveDuringCombatMove().test(obj.getType());
  }

  public static Predicate<Unit> unitCanMoveDuringCombatMove() {
    return unitCanNotMoveDuringCombatMove().negate();
  }

  private static Predicate<Unit> unitIsAaThatCanHitTheseUnits(
      final Collection<Unit> targets,
      final Predicate<Unit> typeOfAa,
      final Map<String, Set<UnitType>> airborneTechTargetsAllowed) {
    return obj -> {
      if (!typeOfAa.test(obj)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      final Set<UnitType> targetsAa = ua.getTargetsAa(obj.getData().getUnitTypeList());
      for (final Unit u : targets) {
        if (targetsAa.contains(u.getType())) {
          return true;
        }
      }
      return targets.stream()
          .anyMatch(
              unitIsAirborne().and(unitIsOfTypes(airborneTechTargetsAllowed.get(ua.getTypeAa()))));
    };
  }

  /** Checks if the unit type can be hit with AA fire by one of the firingUnits */
  private static Predicate<UnitType> unitTypeCanBeHitByAaFire(
      final Collection<UnitType> firingUnits,
      final UnitTypeList unitTypeList,
      final int battleRound) {
    // make sure the aa firing units are valid for combat and during this round
    final Collection<UnitType> aaFiringUnits =
        CollectionUtils.getMatches(
            firingUnits,
            unitTypeIsAaForCombatOnly().and(unitTypeIsAaThatCanFireOnRound(battleRound)));
    return unitType ->
        aaFiringUnits.stream()
            .anyMatch(
                type -> {
                  final UnitAttachment attachment = UnitAttachment.get(type);
                  return attachment.getTargetsAa(unitTypeList).contains(unitType);
                });
  }

  public static Predicate<Unit> unitIsAaOfTypeAa(final String typeAa) {
    return obj -> UnitAttachment.get(obj.getType()).getTypeAa().matches(typeAa);
  }

  public static Predicate<Unit> unitAaShotDamageableInsteadOfKillingInstantly() {
    return obj -> UnitAttachment.get(obj.getType()).getDamageableAa();
  }

  private static Predicate<Unit> unitIsAaThatWillNotFireIfPresentEnemyUnits(
      final Collection<Unit> enemyUnitsPresent) {
    return obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      for (final Unit u : enemyUnitsPresent) {
        if (ua.getWillNotFireIfPresent().contains(u.getType())) {
          return true;
        }
      }
      return false;
    };
  }

  private static Predicate<UnitType> unitTypeIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return obj -> {
      final int maxRoundsAa = UnitAttachment.get(obj).getMaxRoundsAa();
      return maxRoundsAa < 0 || maxRoundsAa >= battleRoundNumber;
    };
  }

  private static Predicate<Unit> unitIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return obj -> unitTypeIsAaThatCanFireOnRound(battleRoundNumber).test(obj.getType());
  }

  public static Predicate<Unit> unitIsAaThatCanFire(
      final Collection<Unit> unitsMovingOrAttacking,
      final Map<String, Set<UnitType>> airborneTechTargetsAllowed,
      final GamePlayer playerMovingOrAttacking,
      final Predicate<Unit> typeOfAa,
      final int battleRoundNumber,
      final boolean defending,
      final RelationshipTracker relationshipTracker) {
    return enemyUnit(playerMovingOrAttacking, relationshipTracker)
        .and(unitIsBeingTransported().negate())
        .and(
            unitIsAaThatCanHitTheseUnits(
                unitsMovingOrAttacking, typeOfAa, airborneTechTargetsAllowed))
        .and(unitIsAaThatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).negate())
        .and(unitIsAaThatCanFireOnRound(battleRoundNumber))
        .and(
            defending
                ? unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()
                : unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero());
  }

  private static Predicate<UnitType> unitTypeIsAaForCombatOnly() {
    return obj -> UnitAttachment.get(obj).getIsAaForCombatOnly();
  }

  public static Predicate<Unit> unitIsAaForCombatOnly() {
    return obj -> unitTypeIsAaForCombatOnly().test(obj.getType());
  }

  public static Predicate<UnitType> unitTypeIsAaForBombingThisUnitOnly() {
    return obj -> UnitAttachment.get(obj).getIsAaForBombingThisUnitOnly();
  }

  public static Predicate<Unit> unitIsAaForBombingThisUnitOnly() {
    return obj -> unitTypeIsAaForBombingThisUnitOnly().test(obj.getType());
  }

  private static Predicate<UnitType> unitTypeIsAaForFlyOverOnly() {
    return obj -> UnitAttachment.get(obj).getIsAaForFlyOverOnly();
  }

  static Predicate<Unit> unitIsAaForFlyOverOnly() {
    return obj -> unitTypeIsAaForFlyOverOnly().test(obj.getType());
  }

  public static Predicate<UnitType> unitTypeIsAaForAnything() {
    return obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAaForBombingThisUnitOnly()
          || ua.getIsAaForCombatOnly()
          || ua.getIsAaForFlyOverOnly();
    };
  }

  public static Predicate<Unit> unitIsAaForAnything() {
    return obj -> unitTypeIsAaForAnything().test(obj.getType());
  }

  public static Predicate<Unit> unitIsNotAa() {
    return unitIsAaForAnything().negate();
  }

  private static Predicate<UnitType> unitTypeMaxAaAttacksIsInfinite() {
    return obj -> UnitAttachment.get(obj).getMaxAaAttacks() == -1;
  }

  public static Predicate<Unit> unitMaxAaAttacksIsInfinite() {
    return obj -> unitTypeMaxAaAttacksIsInfinite().test(obj.getType());
  }

  private static Predicate<UnitType> unitTypeMayOverStackAa() {
    return obj -> UnitAttachment.get(obj).getMayOverStackAa();
  }

  public static Predicate<Unit> unitMayOverStackAa() {
    return obj -> unitTypeMayOverStackAa().test(obj.getType());
  }

  static Predicate<Unit> unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getAttackAa(obj.getOwner()) > 0 && ua.getMaxAaAttacks() != 0;
    };
  }

  static Predicate<Unit> unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getOffensiveAttackAa(obj.getOwner()) > 0 && ua.getMaxAaAttacks() != 0;
    };
  }

  public static Predicate<Unit> unitIsLandTransportable() {
    return unit -> UnitAttachment.get(unit.getType()).getIsLandTransportable();
  }

  public static Predicate<Unit> unitIsNotLandTransportable() {
    return unitIsLandTransportable().negate();
  }

  public static Predicate<Unit> unitIsAirTransportable() {
    return obj -> {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (!ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransportable();
    };
  }

  public static Predicate<Unit> unitIsNotAirTransportable() {
    return unitIsAirTransportable().negate();
  }

  public static Predicate<Unit> unitIsAirTransport() {
    return obj -> {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (!ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransport();
    };
  }

  public static Predicate<Unit> unitIsArtillery() {
    return obj -> UnitAttachment.get(obj.getType()).getArtillery();
  }

  public static Predicate<Unit> unitIsArtillerySupportable() {
    return obj -> UnitAttachment.get(obj.getType()).getArtillerySupportable();
  }

  public static Predicate<Territory> territoryIsWater() {
    return Territory::isWater;
  }

  public static Predicate<Territory> territoryIsIsland() {
    return t -> {
      final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
      return neighbors.size() == 1 && territoryIsWater().test(neighbors.iterator().next());
    };
  }

  public static Predicate<Territory> territoryIsVictoryCity() {
    return t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getVictoryCity() != 0;
    };
  }

  public static Predicate<Territory> territoryIsLand() {
    return territoryIsWater().negate();
  }

  public static Predicate<Territory> territoryIsEmpty() {
    return t -> t.getUnitCollection().isEmpty();
  }

  /**
   * Tests for Land, Convoys Centers and Convoy Routes, and Contested Territories. Assumes player is
   * either the owner of the territory we are testing, or about to become the owner (ie: this
   * doesn't test ownership). If the game option for contested territories not producing is on, then
   * will also remove any contested territories.
   */
  public static Predicate<Territory> territoryCanCollectIncomeFrom(
      final GamePlayer player,
      final GameProperties properties,
      final RelationshipTracker relationshipTracker) {
    final boolean contestedDoNotProduce =
        Properties.getContestedTerritoriesProduceNoIncome(properties);
    return t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      final @Nullable GamePlayer origOwner = OriginalOwnerTracker.getOriginalOwner(t);
      // if it's water, it is a Convoy Center
      // Can't get PUs for capturing a CC, only original owner can get them. (Except capturing
      // null player CCs)
      if (t.isWater()
          && !(origOwner == null
              || origOwner.equals(GamePlayer.NULL_PLAYERID)
              || origOwner.equals(player))) {
        return false;
      }
      if (ta.getConvoyRoute() && !ta.getConvoyAttached().isEmpty()) {
        // Determine if at least one part of the convoy route is owned by us or an ally
        boolean atLeastOne = false;
        for (final Territory convoy : ta.getConvoyAttached()) {
          if (relationshipTracker.isAllied(convoy.getOwner(), player)
              && TerritoryAttachment.get(convoy).getConvoyRoute()) {
            atLeastOne = true;
          }
        }
        if (!atLeastOne) {
          return false;
        }
      }
      return !(contestedDoNotProduce
          && !territoryHasNoEnemyUnits(player, relationshipTracker).test(t));
    };
  }

  public static Predicate<Territory> territoryHasNeighborMatching(
      final GameMap gameMap, final Predicate<Territory> match) {
    return t -> !gameMap.getNeighbors(t, match).isEmpty();
  }

  public static Predicate<Territory> territoryIsInList(final Collection<Territory> list) {
    return list::contains;
  }

  public static Predicate<Territory> territoryIsNotInList(final Collection<Territory> list) {
    return not(list::contains);
  }

  public static Predicate<Territory>
      territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(
          final GameData data, final GamePlayer player) {
    return t ->
        !data.getMap()
            .getNeighbors(
                t, territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(data, player))
            .isEmpty();
  }

  public static Predicate<Territory> territoryHasWaterNeighbor(final GameMap gameMap) {
    return t -> !gameMap.getNeighbors(t, territoryIsWater()).isEmpty();
  }

  public static Predicate<Territory> territoryIsOwnedAndHasOwnedUnitMatching(
      final GamePlayer player, final Predicate<Unit> unitMatch) {
    return t ->
        t.getOwner().equals(player)
            && t.getUnitCollection().anyMatch(unitIsOwnedBy(player).and(unitMatch));
  }

  public static Predicate<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(
      final GamePlayer player) {
    return t ->
        t.getOwner().equals(player) && t.getUnitCollection().anyMatch(unitCanProduceUnits());
  }

  private static Predicate<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(
      final GameData data, final GamePlayer player) {
    return t -> {
      if (!GameStepPropertiesHelper.getCombinedTurns(data, player).contains(t.getOwner())) {
        return false;
      }
      if (!t.getUnitCollection().anyMatch(unitCanProduceUnits())) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
      return !(bt == null || bt.wasConquered(t));
    };
  }

  static Predicate<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(
      final RelationshipTracker relationshipTracker, final GamePlayer player) {
    return t ->
        isTerritoryAllied(player, relationshipTracker).test(t)
            && t.getUnitCollection().anyMatch(unitCanProduceUnits());
  }

  public static Predicate<Territory> territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(
      final RelationshipTracker relationshipTracker,
      final GamePlayer player,
      final Predicate<Unit> unitMatch) {
    return t -> {
      if (!relationshipTracker.isAtWar(player, t.getOwner())) {
        return false;
      }
      return !t.getOwner().isNull()
          && t.getUnitCollection().anyMatch(enemyUnit(player, relationshipTracker).and(unitMatch));
    };
  }

  public static Predicate<Territory> territoryIsEmptyOfCombatUnits(
      final RelationshipTracker relationshipTracker, final GamePlayer player) {
    return t ->
        t.getUnitCollection()
            .allMatch(unitIsInfrastructure().or(enemyUnit(player, relationshipTracker).negate()));
  }

  public static Predicate<Territory> territoryIsNeutralButNotWater() {
    return t -> !t.isWater() && t.getOwner().equals(GamePlayer.NULL_PLAYERID);
  }

  public static Predicate<Territory> territoryIsImpassable() {
    return t -> {
      if (t.isWater()) {
        return false;
      }
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getIsImpassable();
    };
  }

  public static Predicate<Territory> territoryEffectsAllowUnits(final Collection<Unit> units) {
    return t ->
        units.stream()
            .noneMatch(
                Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)));
  }

  public static Predicate<Territory> territoryIsNotImpassable() {
    return territoryIsImpassable().negate();
  }

  public static Predicate<Territory> seaCanMoveOver(
      final GamePlayer player, final GameProperties properties) {
    return t ->
        territoryIsWater().test(t)
            && territoryIsPassableAndNotRestricted(player, properties).test(t);
  }

  public static Predicate<Territory> airCanFlyOver(
      final GamePlayer player,
      final GameProperties properties,
      final RelationshipTracker relationshipTracker,
      final boolean areNeutralsPassableByAir) {
    return t -> {
      if (!areNeutralsPassableByAir && territoryIsNeutralButNotWater().test(t)) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player, properties).test(t)
          && !(territoryIsLand().test(t)
              && !relationshipTracker.canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
    };
  }

  public static Predicate<Territory> territoryIsPassableAndNotRestricted(
      final GamePlayer player, final GameProperties properties) {
    return t -> {
      if (territoryIsImpassable().test(t)) {
        return false;
      }
      if (!Properties.getMovementByTerritoryRestricted(properties)) {
        return true;
      }
      final RulesAttachment ra =
          (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      if (ra == null || ra.getMovementRestrictionTerritories() == null) {
        return true;
      }
      final String movementRestrictionType = ra.getMovementRestrictionType();
      final Collection<Territory> listedTerritories =
          ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
      return (movementRestrictionType.equals("allowed") == listedTerritories.contains(t));
    };
  }

  private static Predicate<Territory> territoryIsImpassableToLandUnits(
      final GamePlayer player, final GameProperties properties) {
    return t ->
        t.isWater() || territoryIsPassableAndNotRestricted(player, properties).negate().test(t);
  }

  public static Predicate<Territory> territoryIsNotImpassableToLandUnits(
      final GamePlayer player, final GameProperties properties) {
    return t -> territoryIsImpassableToLandUnits(player, properties).negate().test(t);
  }

  /**
   * Does NOT check for: Canals, Blitzing, Loading units on transports, TerritoryEffects that
   * disallow units, Stacking Limits, Unit movement left, Fuel available, etc. <br>
   * <br>
   * Does check for: Impassable, ImpassableNeutrals, ImpassableToAirNeutrals, RestrictedTerritories,
   * requiresUnitToMove, Land units moving on water, Sea units moving on land, and territories that
   * are disallowed due to a relationship attachment (canMoveLandUnitsOverOwnedLand,
   * canMoveAirUnitsOverOwnedLand, canLandAirUnitsOnOwnedLand, canMoveIntoDuringCombatMove, etc).
   */
  public static Predicate<Territory> territoryIsPassableAndNotRestrictedAndOkByRelationships(
      final GamePlayer playerWhoOwnsAllTheUnitsMoving,
      final GameProperties properties,
      final RelationshipTracker relationshipTracker,
      final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded,
      final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported,
      final boolean isLandingZoneOnLandForAirUnits) {
    final boolean neutralsPassable = !Properties.getNeutralsImpassable(properties);
    final boolean areNeutralsPassableByAir =
        neutralsPassable && Properties.getNeutralFlyoverAllowed(properties);
    return t -> {
      if (territoryIsImpassable().test(t)) {
        return false;
      }
      if ((!neutralsPassable || (hasAirUnitsNotBeingTransported && !areNeutralsPassableByAir))
          && territoryIsNeutralButNotWater().test(t)) {
        return false;
      }
      if (Properties.getMovementByTerritoryRestricted(properties)) {
        final RulesAttachment ra =
            (RulesAttachment)
                playerWhoOwnsAllTheUnitsMoving.getAttachment(Constants.RULES_ATTACHMENT_NAME);
        if (ra != null && ra.getMovementRestrictionTerritories() != null) {
          final String movementRestrictionType = ra.getMovementRestrictionType();
          final Collection<Territory> listedTerritories =
              ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
          if (!(movementRestrictionType.equals("allowed") == listedTerritories.contains(t))) {
            return false;
          }
        }
      }
      final boolean isWater = territoryIsWater().test(t);
      final boolean isLand = territoryIsLand().test(t);
      if (hasLandUnitsNotBeingTransportedOrBeingLoaded && !isLand) {
        return false;
      }
      if (hasSeaUnitsNotBeingTransported && !isWater) {
        return false;
      }
      if (isLand) {
        if (hasLandUnitsNotBeingTransportedOrBeingLoaded
            && !relationshipTracker.canMoveLandUnitsOverOwnedLand(
                playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
        if (hasAirUnitsNotBeingTransported
            && !relationshipTracker.canMoveAirUnitsOverOwnedLand(
                playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
      }
      return (!isLandingZoneOnLandForAirUnits
              || relationshipTracker.canLandAirUnitsOnOwnedLand(
                  playerWhoOwnsAllTheUnitsMoving, t.getOwner()))
          && !(isCombatMovePhase
              && !relationshipTracker.canMoveIntoDuringCombatMove(
                  playerWhoOwnsAllTheUnitsMoving, t.getOwner()));
    };
  }

  public static Predicate<IBattle> battleIsEmpty() {
    return IBattle::isEmpty;
  }

  public static Predicate<IBattle> battleIsAmphibious() {
    return IBattle::isAmphibious;
  }

  public static Predicate<IBattle> battleIsAmphibiousWithUnitsAttackingFrom(final Territory from) {
    return battleIsAmphibious()
        .and(
            b ->
                (b instanceof DependentBattle)
                    && ((DependentBattle) b).getAmphibiousAttackTerritories().contains(from));
  }

  public static Predicate<Unit> unitHasEnoughMovementForRoute(final Route route) {
    return unit -> {
      BigDecimal left = unit.getMovementLeft();
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final GamePlayer player = unit.getOwner();
      if (ua.getIsAir()) {
        final TerritoryAttachment taStart = TerritoryAttachment.get(route.getStart());
        final TerritoryAttachment taEnd = TerritoryAttachment.get(route.getEnd());
        if (taStart != null && taStart.getAirBase()) {
          left = left.add(BigDecimal.ONE);
        }
        if (taEnd != null && taEnd.getAirBase()) {
          left = left.add(BigDecimal.ONE);
        }
      }
      final GameStep stepName = unit.getData().getSequence().getStep();
      if (ua.getIsSea() && stepName.getDisplayName().equals("Non Combat Move")) {
        // If a zone adjacent to the starting and ending sea zones are allied naval bases, increase
        // the range.
        // TODO Still need to be able to handle stops on the way
        // (history to get route.getStart()
        for (final Territory terrNext : unit.getData().getMap().getNeighbors(route.getStart(), 1)) {
          final TerritoryAttachment taNeighbor = TerritoryAttachment.get(terrNext);
          if (taNeighbor != null
              && taNeighbor.getNavalBase()
              && unit.getData().getRelationshipTracker().isAllied(terrNext.getOwner(), player)) {
            for (final Territory terrEnd :
                unit.getData().getMap().getNeighbors(route.getEnd(), 1)) {
              final TerritoryAttachment taEndNeighbor = TerritoryAttachment.get(terrEnd);
              if (taEndNeighbor != null
                  && taEndNeighbor.getNavalBase()
                  && unit.getData().getRelationshipTracker().isAllied(terrEnd.getOwner(), player)) {
                left = left.add(BigDecimal.ONE);
                break;
              }
            }
          }
        }
      }
      if (left.compareTo(BigDecimal.ZERO) < 0) {
        return false;
      }
      final boolean hasMovementForRoute = left.compareTo(route.getMovementCost(unit)) >= 0;
      if (Properties.getEnterTerritoriesWithHigherMovementCostsThenRemainingMovement(
          unit.getData().getProperties())) {
        return hasMovementForRoute || left.compareTo(route.getMovementCostIgnoreEnd(unit)) > 0;
      }
      return hasMovementForRoute;
    };
  }

  public static Predicate<Unit> unitHasMovementLeft() {
    return Unit::hasMovementLeft;
  }

  public static Predicate<Unit> unitCanMove() {
    return u -> unitTypeCanMove(u.getOwner()).test(u.getType());
  }

  public static Predicate<UnitType> unitTypeCanMove(final GamePlayer player) {
    return obj -> UnitAttachment.get(obj).getMovement(player) > 0;
  }

  public static Predicate<UnitType> unitTypeIsStatic(final GamePlayer gamePlayer) {
    return unitType -> !unitTypeCanMove(gamePlayer).test(unitType);
  }

  public static Predicate<Unit> unitIsLandAndOwnedBy(final GamePlayer player) {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !ua.getIsSea() && !ua.getIsAir() && unit.getOwner().equals(player);
    };
  }

  public static Predicate<Unit> unitIsOwnedBy(final GamePlayer player) {
    return unit -> unit.getOwner().equals(player);
  }

  public static Predicate<Unit> unitIsOwnedByOfAnyOfThesePlayers(
      final Collection<GamePlayer> players) {
    return unit -> players.contains(unit.getOwner());
  }

  public static Predicate<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units) {
    final Collection<UnitCategory> unitCategories = UnitSeparator.categorize(units);
    return unit -> {
      final Collection<Unit> transporting = unit.getTransporting();
      return !Collections.disjoint(UnitSeparator.categorize(transporting), unitCategories);
    };
  }

  public static Predicate<Territory> isTerritoryAllied(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> relationshipTracker.isAllied(player, t.getOwner());
  }

  public static Predicate<Territory> isTerritoryOwnedBy(final GamePlayer player) {
    return t -> t.getOwner().equals(player);
  }

  public static Predicate<Territory> isTerritoryOwnedBy(final Collection<GamePlayer> players) {
    return t -> {
      for (final GamePlayer player : players) {
        if (t.getOwner().equals(player)) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Unit> isUnitAllied(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> relationshipTracker.isAllied(player, t.getOwner());
  }

  public static Predicate<Territory> isTerritoryFriendly(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t ->
        t.isWater()
            || t.getOwner().equals(player)
            || relationshipTracker.isAllied(player, t.getOwner());
  }

  private static Predicate<Unit> unitIsEnemyAaForFlyOver(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return unitIsAaForFlyOverOnly().and(enemyUnit(player, relationshipTracker));
  }

  public static Predicate<Unit> unitIsInTerritory(final Territory territory) {
    return o -> territory.getUnits().contains(o);
  }

  public static Predicate<Territory> isTerritoryEnemy(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> !t.getOwner().equals(player) && relationshipTracker.isAtWar(player, t.getOwner());
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWater(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    // if we look at territory attachments, may have funny results for blockades or other things
    // that are passable
    // and not owned. better to check them by alliance. (veqryn)
    return t ->
        !t.getOwner().equals(player)
            && ((!t.getOwner().equals(GamePlayer.NULL_PLAYERID) || !t.isWater())
                && relationshipTracker.isAtWar(player, t.getOwner()));
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(
      final GamePlayer player,
      final GameProperties properties,
      final RelationshipTracker relationshipTracker) {
    return t -> {
      if (t.getOwner().equals(player)) {
        return false;
      }
      // if we look at territory attachments, may have funny results for blockades or other things
      // that are passable
      // and not owned. better to check them by alliance. (veqryn)
      if (t.getOwner().equals(GamePlayer.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player, properties).test(t)
          && relationshipTracker.isAtWar(player, t.getOwner());
    };
  }

  public static Predicate<Territory> territoryIsBlitzable(
      final GamePlayer player, final GameData data) {
    return t -> {
      // cant blitz water
      if (t.isWater()) {
        return false;
      }
      // cant blitz on neutrals
      if (t.getOwner().equals(GamePlayer.NULL_PLAYERID)
          && !Properties.getNeutralsBlitzable(data.getProperties())) {
        return false;
      }
      // was conquered but not blitzed
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
          && !AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(t)) {
        return false;
      }
      // we ignore neutral units
      final Predicate<Unit> blitzableUnits =
          PredicateBuilder.of(enemyUnit(player, data.getRelationshipTracker()).negate())
              // WW2V2, cant blitz through factories and aa guns
              // WW2V1, you can
              .orIf(
                  !Properties.getWW2V2(data.getProperties())
                      && !Properties.getBlitzThroughFactoriesAndAaRestricted(data.getProperties()),
                  unitIsInfrastructure())
              .build();
      return t.getUnitCollection().allMatch(blitzableUnits);
    };
  }

  public static Predicate<Territory> isTerritoryFreeNeutral(final GameProperties properties) {
    return t ->
        t.getOwner().equals(GamePlayer.NULL_PLAYERID)
            && Properties.getNeutralCharge(properties) <= 0;
  }

  public static Predicate<Territory> territoryDoesNotCostMoneyToEnter(
      final GameProperties properties) {
    return t ->
        territoryIsLand().negate().test(t)
            || !t.getOwner().equals(GamePlayer.NULL_PLAYERID)
            || Properties.getNeutralCharge(properties) <= 0;
  }

  public static Predicate<Unit> enemyUnit(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return unit -> relationshipTracker.isAtWar(player, unit.getOwner());
  }

  public static Predicate<Unit> enemyUnitOfAnyOfThesePlayers(
      final Collection<GamePlayer> players, final RelationshipTracker relationshipTracker) {
    return unit -> relationshipTracker.isAtWarWithAnyOfThesePlayers(unit.getOwner(), players);
  }

  public static Predicate<Unit> unitOwnedBy(final GamePlayer player) {
    return unit -> unit.getOwner().equals(player);
  }

  public static Predicate<Unit> unitOwnedBy(final List<GamePlayer> players) {
    return o -> {
      for (final GamePlayer p : players) {
        if (o.getOwner().equals(p)) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Unit> alliedUnit(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return unit ->
        unit.getOwner().equals(player) || relationshipTracker.isAllied(player, unit.getOwner());
  }

  public static Predicate<Unit> alliedUnitOfAnyOfThesePlayers(
      final Collection<GamePlayer> players, final RelationshipTracker relationshipTracker) {
    return unit ->
        unitIsOwnedByOfAnyOfThesePlayers(players).test(unit)
            || relationshipTracker.isAlliedWithAnyOfThesePlayers(unit.getOwner(), players);
  }

  public static Predicate<Territory> territoryIs(final Territory test) {
    return t -> t.equals(test);
  }

  public static Predicate<Territory> territoryHasLandUnitsOwnedBy(final GamePlayer player) {
    return t -> t.getUnitCollection().anyMatch(unitIsOwnedBy(player).and(unitIsLand()));
  }

  public static Predicate<Territory> territoryHasUnitsOwnedBy(final GamePlayer player) {
    final Predicate<Unit> unitOwnedBy = unitIsOwnedBy(player);
    return t -> t.getUnitCollection().anyMatch(unitOwnedBy);
  }

  public static Predicate<Territory> territoryHasUnitsThatMatch(final Predicate<Unit> cond) {
    return t -> t.getUnitCollection().anyMatch(cond);
  }

  public static Predicate<Territory> territoryHasEnemyAaForFlyOver(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t ->
        t.getUnitCollection().anyMatch(unitIsEnemyAaForFlyOver(player, relationshipTracker));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> !t.getUnitCollection().anyMatch(enemyUnit(player, relationshipTracker));
  }

  public static Predicate<Territory> territoryHasAlliedUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> t.getUnitCollection().anyMatch(alliedUnit(player, relationshipTracker));
  }

  static Predicate<Territory> territoryHasNonSubmergedEnemyUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    final Predicate<Unit> match =
        enemyUnit(player, relationshipTracker).and(unitIsSubmerged().negate());
    return t -> t.getUnitCollection().anyMatch(match);
  }

  public static Predicate<Territory> territoryHasEnemyLandUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t ->
        t.getUnitCollection().anyMatch(enemyUnit(player, relationshipTracker).and(unitIsLand()));
  }

  public static Predicate<Territory> territoryHasEnemySeaUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t ->
        t.getUnitCollection().anyMatch(enemyUnit(player, relationshipTracker).and(unitIsSea()));
  }

  public static Predicate<Territory> territoryHasEnemyUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> t.getUnitCollection().anyMatch(enemyUnit(player, relationshipTracker));
  }

  public static Predicate<Territory> territoryIsNotUnownedWater() {
    return t -> !(t.isWater() && TerritoryAttachment.get(t) == null && t.getOwner().isNull());
  }

  /**
   * The territory is owned by the enemy of those enemy units (i.e. probably owned by you or your
   * ally, but not necessarily so in an FFA type game).
   */
  public static Predicate<Territory> territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> {
      final List<Unit> enemyUnits =
          t.getUnitCollection()
              .getMatches(
                  enemyUnit(player, relationshipTracker)
                      .and(unitIsNotAir())
                      .and(unitIsNotInfrastructure()));
      final Collection<GamePlayer> enemyPlayers =
          enemyUnits.stream().map(Unit::getOwner).collect(Collectors.toSet());
      return isAtWarWithAnyOfThesePlayers(enemyPlayers, relationshipTracker).test(t.getOwner());
    };
  }

  public static Predicate<Unit> transportCannotUnload(final Territory territory) {
    return transport -> {
      if (TransportTracker.hasTransportUnloadedInPreviousPhase(transport)) {
        return true;
      }
      return TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, territory)
          || TransportTracker.isTransportUnloadRestrictedInNonCombat(transport);
    };
  }

  public static Predicate<Unit> transportIsNotTransporting() {
    return transport -> !TransportTracker.isTransporting(transport);
  }

  /**
   * Tests the TripleAUnit getTransportedBy value which is normally set for sea transport movement
   * of land units, and sometimes set for other things like para-troopers and dependent allied
   * fighters sitting as cargo on a ship. (Not sure if set for mech inf or not.)
   */
  public static Predicate<Unit> unitIsBeingTransported() {
    return dependent -> dependent.getTransportedBy() != null;
  }

  /**
   * Returns a predicate that tests the TripleAUnit getTransportedBy value (also tests for
   * para-troopers, and for dependent allied fighters sitting as cargo on a ship).
   *
   * @param units Referring unit.
   * @param currentPlayer Current player
   * @param data Game data.
   * @param forceLoadParatroopersIfPossible Should we load paratroopers? (if not, we assume they are
   *     already loaded).
   */
  public static Predicate<Unit> unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
      final Collection<Unit> units,
      final GamePlayer currentPlayer,
      final GameState data,
      final boolean forceLoadParatroopersIfPossible) {
    final Map<Unit, Unit> paratrooperMap =
        forceLoadParatroopersIfPossible ? TransportUtils.mapParatroopers(units) : Map.of();
    return dependent -> {
      // transported on a sea transport
      final Unit transportedBy = dependent.getTransportedBy();
      if (transportedBy != null && units.contains(transportedBy)) {
        return true;
      }
      // cargo on a carrier
      final Map<Unit, Collection<Unit>> carrierMustMoveWith =
          MoveValidator.carrierMustMoveWith(units, units, data, currentPlayer);
      if (carrierMustMoveWith.values().stream().anyMatch(c -> c.contains(dependent))) {
        return true;
      }

      return paratrooperMap.containsKey(dependent);
    };
  }

  public static Predicate<Unit> unitIsLand() {
    return unitIsNotSea().and(unitIsNotAir());
  }

  public static Predicate<UnitType> unitTypeIsLand() {
    return unitTypeIsNotSea().and(unitTypeIsNotAir());
  }

  public static Predicate<Unit> unitIsNotLand() {
    return unitIsLand().negate();
  }

  public static Predicate<Unit> unitIsOfType(final UnitType type) {
    return unit -> unit.getType().equals(type);
  }

  public static Predicate<Unit> unitIsOfTypes(final Set<UnitType> types) {
    return unit -> types != null && !types.isEmpty() && types.contains(unit.getType());
  }

  public static Predicate<Unit> unitIsOfTypes(final UnitType... types) {
    return unitIsOfTypes(new HashSet<>(Arrays.asList(types)));
  }

  public static Predicate<Territory> territoryWasFoughtOver(final BattleTracker tracker) {
    return t -> tracker.wasBattleFought(t) || tracker.wasBlitzed(t);
  }

  public static Predicate<Unit> unitIsSubmerged() {
    return Unit::getSubmerged;
  }

  public static Predicate<UnitType> unitTypeIsFirstStrike() {
    return type -> UnitAttachment.get(type).getIsFirstStrike();
  }

  public static Predicate<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return u -> TechTracker.hasImprovedArtillerySupport(u.getOwner());
  }

  public static Predicate<Territory> territoryHasNonAllowedCanal(
      final GamePlayer player, final GameData gameData) {
    return t -> new MoveValidator(gameData).validateCanal(new Route(t), null, player) != null;
  }

  public static Predicate<Territory> territoryIsBlockedSea(
      final GamePlayer player,
      final GameProperties properties,
      final RelationshipTracker relationshipTracker) {
    final Predicate<Unit> transport =
        unitIsTransportButNotCombatTransport().negate().and(unitIsLand().negate());
    final Predicate<Unit> unitCond =
        PredicateBuilder.of(unitIsInfrastructure().negate())
            .and(alliedUnit(player, relationshipTracker).negate())
            .and(unitCanBeMovedThroughByEnemies().negate())
            .andIf(Properties.getIgnoreTransportInMovement(properties), transport)
            .build();
    return territoryHasUnitsThatMatch(unitCond).negate().and(territoryIsWater());
  }

  static Predicate<Unit> unitCanRepairOthers() {
    return unit -> {
      if (unitIsDisabled().test(unit) || unitIsBeingTransported().test(unit)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getRepairsUnits() != null && !ua.getRepairsUnits().isEmpty();
    };
  }

  static Predicate<Unit> unitCanRepairThisUnit(
      final Unit damagedUnit, final Territory territoryOfRepairUnit) {
    return unitCanRepair -> {
      final Set<GamePlayer> players =
          GameStepPropertiesHelper.getCombinedTurns(damagedUnit.getData(), damagedUnit.getOwner());
      if (players.size() > 1) {

        // If combined turns then can repair as long as at least 1 capital is owned except at
        // territories that a
        // combined capital isn't owned
        boolean atLeastOnePlayerOwnsCapital = false;
        for (final GamePlayer player : players) {
          final boolean ownCapital =
              TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(
                  player, damagedUnit.getData().getMap());
          atLeastOnePlayerOwnsCapital = atLeastOnePlayerOwnsCapital || ownCapital;
          if (!ownCapital && territoryOfRepairUnit.getOwner().equals(player)) {
            return false;
          }
        }
        if (!atLeastOnePlayerOwnsCapital) {
          return false;
        }
      } else {

        // Damaged units can only be repaired by facilities if the unit owner controls their capital
        if (!TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(
            damagedUnit.getOwner(), damagedUnit.getData().getMap())) {
          return false;
        }
      }
      final UnitAttachment ua = UnitAttachment.get(unitCanRepair.getType());
      return ua.getRepairsUnits() != null
          && ua.getRepairsUnits().keySet().contains(damagedUnit.getType());
    };
  }

  /**
   * Returns a predicate that will return true if the territory contains a unit that can repair this
   * unit (It will also return true if this unit is Sea and an adjacent land territory has a land
   * unit that can repair this unit.)
   *
   * @param territory referring territory
   * @param player referring player
   */
  public static Predicate<Unit> unitCanBeRepairedByFacilitiesInItsTerritory(
      final Territory territory,
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final GameMap gameMap) {
    return damagedUnit -> {
      final Predicate<Unit> damaged =
          unitHasMoreThanOneHitPointTotal().and(unitHasTakenSomeDamage());
      if (!damaged.test(damagedUnit)) {
        return false;
      }
      final Predicate<Unit> repairUnit =
          alliedUnit(player, relationshipTracker)
              .and(unitCanRepairOthers())
              .and(unitCanRepairThisUnit(damagedUnit, territory));
      if (territory.getUnitCollection().anyMatch(repairUnit)) {
        return true;
      }
      if (unitIsSea().test(damagedUnit)) {
        final List<Territory> neighbors =
            new ArrayList<>(gameMap.getNeighbors(territory, territoryIsLand()));
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitLand =
              alliedUnit(player, relationshipTracker)
                  .and(unitCanRepairOthers())
                  .and(unitCanRepairThisUnit(damagedUnit, current))
                  .and(unitIsLand());
          if (current.getUnitCollection().anyMatch(repairUnitLand)) {
            return true;
          }
        }
      } else if (unitIsLand().test(damagedUnit)) {
        final List<Territory> neighbors =
            new ArrayList<>(gameMap.getNeighbors(territory, territoryIsWater()));
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitSea =
              alliedUnit(player, relationshipTracker)
                  .and(unitCanRepairOthers())
                  .and(unitCanRepairThisUnit(damagedUnit, current))
                  .and(unitIsSea());
          if (current.getUnitCollection().anyMatch(repairUnitSea)) {
            return true;
          }
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitCanGiveBonusMovement() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null
          && !ua.getGivesMovement().isEmpty()
          && unitIsBeingTransported().negate().test(unit);
    };
  }

  static Predicate<Unit> unitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return unitWhichCanGiveBonusMovement -> {
      if (unitIsDisabled().test(unitWhichCanGiveBonusMovement)) {
        return false;
      }
      final UnitType type = unitWhichCanGiveBonusMovement.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      // TODO: make sure the unit is operational
      return unitCanGiveBonusMovement().test(unitWhichCanGiveBonusMovement)
          && ua.getGivesMovement().getInt(unitWhichWillGetBonus.getType()) != 0;
    };
  }

  /**
   * Returns a predicate that will return true if the territory contains a unit that can give bonus
   * movement to this unit (It will also return true if this unit is Sea and an adjacent land
   * territory has a land unit that can give bonus movement to this unit.)
   *
   * @param territory referring territory
   * @param player referring player
   */
  public static Predicate<Unit> unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(
      final Territory territory,
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final GameMap gameMap) {
    return unitWhichWillGetBonus -> {
      final Predicate<Unit> givesBonusUnit =
          alliedUnit(player, relationshipTracker)
              .and(unitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
      if (territory.getUnitCollection().anyMatch(givesBonusUnit)) {
        return true;
      }
      if (unitIsSea().test(unitWhichWillGetBonus)) {
        final Predicate<Unit> givesBonusUnitLand = givesBonusUnit.and(unitIsLand());
        final List<Territory> neighbors =
            new ArrayList<>(gameMap.getNeighbors(territory, territoryIsLand()));
        for (final Territory current : neighbors) {
          if (current.getUnitCollection().anyMatch(givesBonusUnitLand)) {
            return true;
          }
        }
      }
      return false;
    };
  }

  static Predicate<Unit> unitCreatesUnits() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getCreatesUnitsList() != null && !ua.getCreatesUnitsList().isEmpty();
    };
  }

  static Predicate<Unit> unitCreatesResources() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null
          && ua.getCreatesResourcesList() != null
          && !ua.getCreatesResourcesList().isEmpty();
    };
  }

  public static Predicate<UnitType> unitTypeConsumesUnitsOnCreation() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit);
      return ua != null && ua.getConsumesUnits() != null && !ua.getConsumesUnits().isEmpty();
    };
  }

  static Predicate<Unit> unitConsumesUnitsOnCreation() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getConsumesUnits() != null && !ua.getConsumesUnits().isEmpty();
    };
  }

  static Predicate<Unit> unitWhichConsumesUnitsHasRequiredUnits(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return unitWhichRequiresUnits -> {
      if (!unitConsumesUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      boolean canBuild = true;
      for (final UnitType ut : requiredUnits) {
        final Predicate<Unit> unitIsOwnedByAndOfTypeAndNotDamaged =
            unitIsOwnedBy(unitWhichRequiresUnits.getOwner())
                .and(unitIsOfType(ut))
                .and(unitHasNotTakenAnyBombingUnitDamage())
                .and(unitHasNotTakenAnyDamage())
                .and(unitIsNotDisabled());
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final int numberInTerritory =
            CollectionUtils.countMatches(
                unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndOfTypeAndNotDamaged);
        if (numberInTerritory < requiredNumber) {
          canBuild = false;
        }
        if (!canBuild) {
          break;
        }
      }
      return canBuild;
    };
  }

  public static Predicate<Unit> unitRequiresUnitsOnCreation() {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getRequiresUnits() != null && !ua.getRequiresUnits().isEmpty();
    };
  }

  /**
   * Checks if requiresUnits criteria allows placement in territory based on units there at the
   * start of turn.
   */
  public static Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnitsInList(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return unitWhichRequiresUnits -> {
      if (!unitRequiresUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final Predicate<Unit> unitIsOwnedByAndNotDisabled =
          unitIsOwnedBy(unitWhichRequiresUnits.getOwner()).and(unitIsNotDisabled());
      unitsInTerritoryAtStartOfTurn.retainAll(
          CollectionUtils.getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled));
      boolean canBuild = false;
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final List<String[]> unitComboPossibilities = ua.getRequiresUnits();
      for (final String[] combo : unitComboPossibilities) {
        if (combo != null) {
          boolean haveAll = true;
          final Collection<UnitType> requiredUnits = ua.getListedUnits(combo);
          for (final UnitType ut : requiredUnits) {
            if (CollectionUtils.countMatches(unitsInTerritoryAtStartOfTurn, unitIsOfType(ut)) < 1) {
              haveAll = false;
            }
            if (!haveAll) {
              break;
            }
          }
          if (haveAll) {
            canBuild = true;
          }
        }
        if (canBuild) {
          break;
        }
      }
      return canBuild;
    };
  }

  /** Check if unit meets requiredUnitsToMove criteria and can move into territory. */
  public static Predicate<Unit> unitHasRequiredUnitsToMove(
      final Territory t, final RelationshipTracker relationshipTracker) {
    return unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null
          || ua.getRequiresUnitsToMove() == null
          || ua.getRequiresUnitsToMove().isEmpty()) {
        return true;
      }

      final Predicate<Unit> unitIsOwnedByAndNotDisabled =
          isUnitAllied(unit.getOwner(), relationshipTracker).and(unitIsNotDisabled());
      final List<Unit> units =
          CollectionUtils.getMatches(t.getUnits(), unitIsOwnedByAndNotDisabled);
      for (final String[] array : ua.getRequiresUnitsToMove()) {
        boolean haveAll = true;
        for (final UnitType ut : ua.getListedUnits(array)) {
          if (units.stream().noneMatch(unitIsOfType(ut))) {
            haveAll = false;
            break;
          }
        }
        if (haveAll) {
          return true;
        }
      }

      return false;
    };
  }

  public static Predicate<Territory> territoryHasRequiredUnitsToMove(
      final Collection<Unit> units, final RelationshipTracker relationshipTracker) {
    return t -> units.stream().allMatch(unitHasRequiredUnitsToMove(t, relationshipTracker));
  }

  static Predicate<Territory> territoryIsBlockadeZone() {
    return t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getBlockadeZone();
    };
  }

  public static Predicate<UnitType> unitTypeIsConstruction() {
    return type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua != null && ua.getIsConstruction();
    };
  }

  public static Predicate<Unit> unitIsConstruction() {
    return obj -> unitTypeIsConstruction().test(obj.getType());
  }

  public static Predicate<Unit> unitIsNotConstruction() {
    return unitIsConstruction().negate();
  }

  public static Predicate<Unit> unitCanProduceUnitsAndIsInfrastructure() {
    return unitCanProduceUnits().and(unitIsInfrastructure());
  }

  public static Predicate<Unit> unitCanProduceUnitsAndCanBeDamaged() {
    return unitCanProduceUnits().and(unitCanBeDamaged());
  }

  /**
   * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from
   * any other unit. Otherwise, units must have a specific unit in this list to be able to invade
   * from that unit.
   */
  public static Predicate<Unit> unitCanInvade() {
    return unit -> {
      // is the unit being transported?
      final Unit transport = unit.getTransportedBy();
      if (transport == null) {
        // Unit isn't transported so can Invade
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.canInvadeFrom(transport);
    };
  }

  public static Predicate<RelationshipType> relationshipTypeIsAllied() {
    return relationship -> relationship.getRelationshipTypeAttachment().isAllied();
  }

  public static Predicate<RelationshipType> relationshipTypeIsNeutral() {
    return relationship -> relationship.getRelationshipTypeAttachment().isNeutral();
  }

  public static Predicate<RelationshipType> relationshipTypeIsAtWar() {
    return relationship -> relationship.getRelationshipTypeAttachment().isWar();
  }

  public static Predicate<Relationship> relationshipIsAtWar() {
    return relationship ->
        relationship.getRelationshipType().getRelationshipTypeAttachment().isWar();
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveLandUnitsOverOwnedLand() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().canMoveLandUnitsOverOwnedLand();
  }

  /** If the territory is not land, returns true. Else, tests relationship of the owners. */
  public static Predicate<Territory> territoryAllowsCanMoveLandUnitsOverOwnedLand(
      final GamePlayer ownerOfUnitsMoving, final RelationshipTracker relationshipTracker) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final GamePlayer territoryOwner = t.getOwner();
      return territoryOwner == null
          || relationshipTracker.canMoveLandUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    };
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveAirUnitsOverOwnedLand() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().canMoveAirUnitsOverOwnedLand();
  }

  /** If the territory is not land, returns true. Else, tests relationship of the owners. */
  public static Predicate<Territory> territoryAllowsCanMoveAirUnitsOverOwnedLand(
      final GamePlayer ownerOfUnitsMoving, final RelationshipTracker relationshipTracker) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final GamePlayer territoryOwner = t.getOwner();
      return territoryOwner == null
          || relationshipTracker.canMoveAirUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    };
  }

  public static Predicate<RelationshipType> relationshipTypeCanLandAirUnitsOnOwnedLand() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().canLandAirUnitsOnOwnedLand();
  }

  public static Predicate<RelationshipType> relationshipTypeCanTakeOverOwnedTerritory() {
    return relationship -> relationship.getRelationshipTypeAttachment().canTakeOverOwnedTerritory();
  }

  public static Predicate<RelationshipType> relationshipTypeGivesBackOriginalTerritories() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().givesBackOriginalTerritories();
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveIntoDuringCombatMove() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().canMoveIntoDuringCombatMove();
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveThroughCanals() {
    return relationship -> relationship.getRelationshipTypeAttachment().canMoveThroughCanals();
  }

  public static Predicate<RelationshipType> relationshipTypeRocketsCanFlyOver() {
    return relationship -> relationship.getRelationshipTypeAttachment().canRocketsFlyOver();
  }

  public static Predicate<String> isValidRelationshipName(
      final RelationshipTypeList relationshipTypeList) {
    return relationshipName -> relationshipTypeList.getRelationshipType(relationshipName) != null;
  }

  public static Predicate<GamePlayer> isAtWar(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return player2 ->
        relationshipTypeIsAtWar().test(relationshipTracker.getRelationshipType(player, player2));
  }

  public static Predicate<GamePlayer> isAtWarWithAnyOfThesePlayers(
      final Collection<GamePlayer> players, final RelationshipTracker relationshipTracker) {
    return player2 -> relationshipTracker.isAtWarWithAnyOfThesePlayers(player2, players);
  }

  public static Predicate<GamePlayer> isAllied(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return player2 ->
        relationshipTypeIsAllied().test(relationshipTracker.getRelationshipType(player, player2));
  }

  public static Predicate<GamePlayer> isAlliedWithAnyOfThesePlayers(
      final Collection<GamePlayer> players, final RelationshipTracker relationshipTracker) {
    return player2 -> relationshipTracker.isAlliedWithAnyOfThesePlayers(player2, players);
  }

  public static Predicate<Unit> unitIsOwnedAndIsFactoryOrCanProduceUnits(final GamePlayer player) {
    return unit -> unitCanProduceUnits().test(unit) && unitIsOwnedBy(player).test(unit);
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith() {
    return unit -> !UnitAttachment.get(unit.getType()).getReceivesAbilityWhenWith().isEmpty();
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith(
      final String filterForAbility, final String filterForUnitType) {
    return u -> {
      for (final String receives : UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith()) {
        final String[] s = receives.split(":", 2);
        if (s[0].equals(filterForAbility) && s[1].equals(filterForUnitType)) {
          return true;
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitHasWhenCombatDamagedEffect() {
    return u -> !UnitAttachment.get(u.getType()).getWhenCombatDamaged().isEmpty();
  }

  public static Predicate<Unit> unitHasWhenCombatDamagedEffect(final String filterForEffect) {
    return unitHasWhenCombatDamagedEffect()
        .and(
            unit -> {
              final int currentDamage = unit.getHits();
              final List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>>
                  whenCombatDamagedList = UnitAttachment.get(unit.getType()).getWhenCombatDamaged();
              for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key :
                  whenCombatDamagedList) {
                final String effect = key.getSecond().getFirst();
                if (!effect.equals(filterForEffect)) {
                  continue;
                }
                final int damagedFrom = key.getFirst().getFirst();
                final int damagedTo = key.getFirst().getSecond();
                if (currentDamage >= damagedFrom && currentDamage <= damagedTo) {
                  return true;
                }
              }
              return false;
            });
  }

  public static Predicate<Territory> territoryHasCaptureOwnershipChanges() {
    return t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return (ta != null) && !ta.getCaptureOwnershipChanges().isEmpty();
    };
  }

  public static Predicate<Unit> unitWhenHitPointsDamagedChangesInto() {
    return u -> !UnitAttachment.get(u.getType()).getWhenHitPointsDamagedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitAtMaxHitPointDamageChangesInto() {
    return u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      return ua.getWhenHitPointsDamagedChangesInto().containsKey(ua.getHitPoints());
    };
  }

  static Predicate<Unit> unitWhenHitPointsRepairedChangesInto() {
    return u -> !UnitAttachment.get(u.getType()).getWhenHitPointsRepairedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitWhenCapturedChangesIntoDifferentUnitType() {
    return u -> !UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitWhenCapturedSustainsDamage() {
    return u -> UnitAttachment.get(u.getType()).getWhenCapturedSustainsDamage() > 0;
  }

  public static <T extends AbstractUserActionAttachment>
      Predicate<T> abstractUserActionAttachmentCanBeAttempted(
          final Map<ICondition, Boolean> testedConditions) {
    return uaa -> uaa.hasAttemptsLeft() && uaa.canPerform(testedConditions);
  }

  static Predicate<Unit> unitCanOnlyPlaceInOriginalTerritories() {
    return u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      final Set<String> specialOptions = ua.getSpecial();
      for (final String option : specialOptions) {
        if (option.equals("canOnlyPlaceInOriginalTerritories")) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * Accounts for OccupiedTerrOf. Returns false if there is no territory attachment (like if it is
   * water).
   */
  public static Predicate<Territory> territoryIsOriginallyOwnedBy(final GamePlayer player) {
    return t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      final GamePlayer originalOwner = ta.getOriginalOwner();
      if (originalOwner == null) {
        return player == null;
      }
      return originalOwner.equals(player);
    };
  }

  static Predicate<GamePlayer> isAlliedAndAlliancesCanChainTogether(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return player2 ->
        relationshipTypeIsAlliedAndAlliancesCanChainTogether()
            .test(relationshipTracker.getRelationshipType(player, player2));
  }

  public static Predicate<RelationshipType> relationshipTypeIsAlliedAndAlliancesCanChainTogether() {
    return rt ->
        relationshipTypeIsAllied().test(rt)
            && rt.getRelationshipTypeAttachment().canAlliancesChainTogether();
  }

  /**
   * If player is null, this predicate will return true if ANY of the relationship changes match the
   * conditions. (since paa's can have more than 1 change).
   *
   * @param player CAN be null
   * @param currentRelation cannot be null
   * @param newRelation cannot be null
   * @param relationshipTracker cannot be null
   */
  public static Predicate<PoliticalActionAttachment> politicalActionIsRelationshipChangeOf(
      @Nullable final GamePlayer player,
      final Predicate<RelationshipType> currentRelation,
      final Predicate<RelationshipType> newRelation,
      final RelationshipTracker relationshipTracker) {
    return paa -> {
      for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
          paa.getRelationshipChanges()) {
        final GamePlayer p1 = relationshipChange.player1;
        final GamePlayer p2 = relationshipChange.player2;
        if (player != null && !(p1.equals(player) || p2.equals(player))) {
          continue;
        }
        final RelationshipType currentType = relationshipTracker.getRelationshipType(p1, p2);
        final RelationshipType newType = relationshipChange.relationshipType;
        if (currentRelation.test(currentType) && newRelation.test(newType)) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<PoliticalActionAttachment> politicalActionAffectsAtLeastOneAlivePlayer(
      final GamePlayer currentPlayer, final GameMap gameMap) {
    return paa -> {
      for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
          paa.getRelationshipChanges()) {
        final GamePlayer p1 = relationshipChange.player1;
        final GamePlayer p2 = relationshipChange.player2;
        if (!currentPlayer.equals(p1) && p1.amNotDeadYet(gameMap)) {
          return true;
        }
        if (!currentPlayer.equals(p2) && p2.amNotDeadYet(gameMap)) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Territory> airCanLandOnThisAlliedNonConqueredLandTerritory(
      final GamePlayer player, final GameData data) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
      if (bt.wasConquered(t)) {
        return false;
      }
      final GamePlayer owner = t.getOwner();
      if (owner == null || owner.isNull()) {
        return false;
      }
      final RelationshipTracker rt = data.getRelationshipTracker();
      return !(!rt.canMoveAirUnitsOverOwnedLand(player, owner)
          || !rt.canLandAirUnitsOnOwnedLand(player, owner));
    };
  }

  static Predicate<Territory> territoryAllowsRocketsCanFlyOver(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final GamePlayer owner = t.getOwner();
      if (owner == null || owner.isNull()) {
        return true;
      }
      return relationshipTracker.rocketsCanFlyOver(player, owner);
    };
  }

  // TODO: update scrambling to consider movement cost
  public static Predicate<Unit> unitCanScrambleOnRouteDistance(final Route route) {
    return unit ->
        UnitAttachment.get(unit.getType()).getMaxScrambleDistance() >= route.numberOfSteps();
  }

  public static Predicate<Unit> unitCanIntercept() {
    return u -> UnitAttachment.get(u.getType()).getCanIntercept();
  }

  public static Predicate<Unit> unitRequiresAirBaseToIntercept() {
    return u -> UnitAttachment.get(u.getType()).getRequiresAirBaseToIntercept();
  }

  static Predicate<Unit> unitCanEscort() {
    return u -> UnitAttachment.get(u.getType()).getCanEscort();
  }

  public static Predicate<Unit> unitCanAirBattle() {
    return u -> UnitAttachment.get(u.getType()).getCanAirBattle();
  }

  public static Predicate<Territory> //
      terrIsOwnedByPlayerRelationshipCanTakeOwnedTerrAndPassableAndNotWater(
      final GamePlayer attacker) {
    return t -> {
      if (t.getOwner().equals(attacker)) {
        return false;
      }
      if (t.getOwner().equals(GamePlayer.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(attacker, t.getData().getProperties()).test(t)
          && relationshipTypeCanTakeOverOwnedTerritory()
              .test(
                  t.getData().getRelationshipTracker().getRelationshipType(attacker, t.getOwner()));
    };
  }

  public static Predicate<Territory> territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(
      final GamePlayer movingPlayer) {
    return t ->
        t.getOwner().equals(movingPlayer)
            || ((t.getOwner().equals(GamePlayer.NULL_PLAYERID) && t.isWater())
                || t.getData()
                    .getRelationshipTracker()
                    .canMoveIntoDuringCombatMove(movingPlayer, t.getOwner()));
  }

  public static Predicate<Unit> unitCanBeInBattle(
      final boolean attack,
      final boolean isLandBattle,
      final int battleRound,
      final boolean doNotIncludeBombardingSeaUnits) {
    return unitCanBeInBattle(
        attack, isLandBattle, battleRound, doNotIncludeBombardingSeaUnits, List.of());
  }

  public static Predicate<Unit> unitCanBeInBattle(
      final boolean attack,
      final boolean isLandBattle,
      final int battleRound,
      final boolean doNotIncludeBombardingSeaUnits,
      final Collection<UnitType> firingUnits) {
    return unitCanBeInBattle(
        attack, isLandBattle, battleRound, true, doNotIncludeBombardingSeaUnits, firingUnits);
  }

  public static Predicate<Unit> unitCanBeInBattle(
      final boolean attack,
      final boolean isLandBattle,
      final int battleRound,
      final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeBombardingSeaUnits,
      final Collection<UnitType> firingUnits) {
    return unit ->
        unitTypeCanBeInBattle(
                attack,
                isLandBattle,
                unit.getOwner(),
                battleRound,
                includeAttackersThatCanNotMove,
                doNotIncludeBombardingSeaUnits,
                firingUnits)
            .test(unit.getType());
  }

  public static Predicate<UnitType> unitTypeCanBeInBattle(
      final boolean attack,
      final boolean isLandBattle,
      final GamePlayer player,
      final int battleRound,
      final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeBombardingSeaUnits,
      final Collection<UnitType> firingUnits) {

    // remove infrastructure units unless it can support or fight
    // or it is AA that can fire this round
    // or it can be shot at by AA
    final PredicateBuilder<UnitType> canBeInBattleBuilder =
        PredicateBuilder.of(unitTypeIsInfrastructure().negate())
            .or(unitTypeIsSupporterOrHasCombatAbility(attack, player))
            .or(unitTypeIsAaForCombatOnly().and(unitTypeIsAaThatCanFireOnRound(battleRound)))
            .or(
                unitTypeCanBeHitByAaFire(
                    firingUnits, player.getData().getUnitTypeList(), battleRound));

    if (attack) {
      if (!includeAttackersThatCanNotMove) {
        canBeInBattleBuilder
            .and(unitTypeCanNotMoveDuringCombatMove().negate())
            .and(unitTypeCanMove(player));
      }
      if (isLandBattle) {
        if (doNotIncludeBombardingSeaUnits) {
          canBeInBattleBuilder.and(unitTypeIsSea().negate());
        }
      } else { // is sea battle
        canBeInBattleBuilder.and(unitTypeIsLand().negate());
      }
    } else { // defense
      canBeInBattleBuilder.and((isLandBattle ? unitTypeIsSea() : unitTypeIsLand()).negate());
    }

    return canBeInBattleBuilder.build();
  }

  public static Predicate<Unit> unitIsAirborne() {
    return Unit::getAirborne;
  }

  public static <T> Predicate<T> isNotInList(final List<T> list) {
    return not(list::contains);
  }

  /** Ignores units that are submerged or not valid for the specific battle site */
  public static Predicate<Unit> unitIsActiveInTerritory(final Territory battleSite) {
    return Matches.unitIsSubmerged()
        .negate()
        .and(
            Matches.territoryIsLand().test(battleSite)
                ? Matches.unitIsSea().negate()
                : Matches.unitIsLand().negate());
  }
}
