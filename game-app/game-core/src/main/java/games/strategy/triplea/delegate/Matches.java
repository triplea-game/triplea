package games.strategy.triplea.delegate;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
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
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
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
@UtilityClass
public final class Matches {
  public static Predicate<UnitType> unitTypeHasMoreThanOneHitPointTotal() {
    return ut -> ut.getUnitAttachment().getHitPoints() > 1;
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
    return unit -> unit.getUnitAttachment().isSea();
  }

  public static Predicate<Unit> unitHasSubBattleAbilities() {
    return unitCanEvade().or(unitIsFirstStrike()).or(unitCanNotBeTargetedByAll());
  }

  public static Predicate<Unit> unitCanEvade() {
    return unit -> unit.getUnitAttachment().getCanEvade();
  }

  public static Predicate<Unit> unitIsFirstStrike() {
    return unit -> unit.getUnitAttachment().getIsFirstStrike();
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
              not(Matches.unitIsSuicideOnAttack())
                  // deprecated isSuicide units won't have suicideOnDefense
                  .or(Matches.unitIsSuicideOnDefense()));
    }
    return matcher;
  }

  public static Predicate<Unit> unitCanMoveThroughEnemies() {
    return unit -> unit.getUnitAttachment().getCanMoveThroughEnemies();
  }

  public static Predicate<Unit> unitCanBeMovedThroughByEnemies() {
    return unit -> unit.getUnitAttachment().getCanBeMovedThroughByEnemies();
  }

  public static Predicate<Unit> unitCanNotBeTargetedByAll() {
    return unit -> !unit.getUnitAttachment().getCanNotBeTargetedBy().isEmpty();
  }

  private static Predicate<Unit> unitIsCombatSeaTransport() {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.isCombatTransport() && ua.isSea();
    };
  }

  public static Predicate<Unit> unitIsNotCombatSeaTransport() {
    return unitIsCombatSeaTransport().negate();
  }

  public static Predicate<Unit> unitIsSeaTransportButNotCombatSeaTransport() {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.getTransportCapacity() != -1 && ua.isSea() && !ua.isCombatTransport();
    };
  }

  public static Predicate<Unit> unitIsNotSeaTransportButCouldBeCombatSeaTransport() {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.getTransportCapacity() == -1 || (ua.isCombatTransport() && ua.isSea());
    };
  }

  public static Predicate<Unit> unitIsDestroyer() {
    return unit -> unit.getUnitAttachment().isDestroyer();
  }

  public static Predicate<UnitType> unitTypeIsDestroyer() {
    return type -> type.getUnitAttachment().isDestroyer();
  }

  public static Predicate<Unit> unitIsSeaTransport() {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.getTransportCapacity() != -1 && ua.isSea();
    };
  }

  public static Predicate<Unit> unitIsNotSeaTransport() {
    return unitIsSeaTransport().negate();
  }

  public static Predicate<Unit> unitIsSeaTransportAndNotDestroyer() {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return !unitIsDestroyer().test(unit) && ua.getTransportCapacity() != -1 && ua.isSea();
    };
  }

  public static Predicate<UnitType> unitTypeIsStrategicBomber() {
    return unitType -> unitType.getUnitAttachment().isStrategicBomber();
  }

  public static Predicate<Unit> unitIsStrategicBomber() {
    return u -> unitTypeIsStrategicBomber().test(u.getType());
  }

  public static Predicate<Unit> unitHasMoved() {
    return Unit::hasMoved;
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
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.getMovement(gamePlayer) > 0
          && (ua.getAttack(gamePlayer) > 0 || ua.getOffensiveAttackAa(gamePlayer) > 0);
    };
  }

  public static Predicate<Unit> unitCanParticipateInCombat(
      boolean attack,
      GamePlayer attacker,
      Territory battleSite,
      int battleRound,
      Collection<Unit> enemyUnits) {
    final Collection<UnitType> enemyUnitTypes = UnitUtils.getUnitTypesFromUnitList(enemyUnits);
    return u -> {
      final boolean landBattle = !battleSite.isWater();
      if (!landBattle && Matches.unitIsLand().test(u)) {
        return false;
      }
      // still allow infrastructure type units that can provide support have combat abilities
      // remove infrastructure units that can't take part in combat (air/naval bases, etc...)
      if (!Matches.unitCanBeInBattle(attack, landBattle, battleRound, false, enemyUnitTypes)
          .test(u)) {
        return false;
      }
      // remove capturableOnEntering units (veqryn)
      if (Matches.unitCanBeCapturedOnEnteringThisTerritory(attacker, battleSite).test(u)) {
        return false;
      }
      // remove any allied air units that are stuck on damaged carriers (veqryn)
      if (Matches.unitIsBeingTransported()
          .and(Matches.unitIsAir())
          .and(Matches.unitCanLandOnCarrier())
          .test(u)) {
        return false;
      }
      // remove any units that were in air combat (veqryn)
      return !Matches.unitWasInAirBattle().test(u);
    };
  }

  public static Predicate<Unit> unitHasAttackValueOfAtLeast(final int attackValue) {
    return unit -> unit.getUnitAttachment().getAttack(unit.getOwner()) >= attackValue;
  }

  public static Predicate<Unit> unitHasDefendValueOfAtLeast(final int defendValue) {
    return unit -> unit.getUnitAttachment().getDefense(unit.getOwner()) >= defendValue;
  }

  public static Predicate<Unit> unitIsEnemyOf(final GamePlayer player) {
    return unit -> player.isAtWar(unit.getOwner());
  }

  public static Predicate<Unit> unitIsNotSea() {
    return unit -> !unit.getUnitAttachment().isSea();
  }

  public static Predicate<UnitType> unitTypeIsSea() {
    return type -> type.getUnitAttachment().isSea();
  }

  public static Predicate<UnitType> unitTypeIsNotSea() {
    return type -> !type.getUnitAttachment().isSea();
  }

  public static Predicate<UnitType> unitTypeIsSeaOrAir() {
    return type -> {
      final UnitAttachment ua = type.getUnitAttachment();
      return ua.isSea() || ua.isAir();
    };
  }

  public static Predicate<Unit> unitIsAir() {
    return unit -> unit.getUnitAttachment().isAir();
  }

  public static Predicate<Unit> unitIsNotAir() {
    return unit -> !unit.getUnitAttachment().isAir();
  }

  public static Predicate<UnitType> unitTypeCanBombard(final GamePlayer gamePlayer) {
    return type -> type.getUnitAttachment().getCanBombard(gamePlayer);
  }

  static Predicate<Unit> unitCanBeGivenByTerritoryTo(final GamePlayer player) {
    return unit -> unit.getUnitAttachment().getCanBeGivenByTerritoryTo().contains(player);
  }

  public static Predicate<Unit> unitCanBeCapturedOnEnteringThisTerritory(
      final GamePlayer player, final Territory t) {
    return unit -> {
      if (!Properties.getCaptureUnitsOnEnteringTerritory(player.getData().getProperties())) {
        return false;
      }
      final GamePlayer unitOwner = unit.getOwner();
      final UnitAttachment ua = unit.getUnitAttachment();
      final boolean unitCanBeCapturedByPlayer = ua.getCanBeCapturedOnEnteringBy().contains(player);
      final Optional<TerritoryAttachment> optionalTerritoryAttachment = TerritoryAttachment.get(t);
      if (optionalTerritoryAttachment.isEmpty()) {
        return false;
      }
      final boolean territoryCanHaveUnitsThatCanBeCapturedByPlayer =
          optionalTerritoryAttachment.get().getCaptureUnitOnEnteringBy().contains(player);
      final PlayerAttachment pa = PlayerAttachment.get(unitOwner);
      if (pa == null) {
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
      for (Tuple<String, GamePlayer> tuple : u.getUnitAttachment().getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("BY") && tuple.getSecond().equals(playerBy)) {
          return true;
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitDestroyedWhenCapturedFrom() {
    return u -> {
      for (Tuple<String, GamePlayer> tuple : u.getUnitAttachment().getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("FROM") && tuple.getSecond().equals(u.getOwner())) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Unit> unitIsAirBase() {
    return unit -> unit.getUnitAttachment().isAirBase();
  }

  public static Predicate<UnitType> unitTypeCanBeDamaged() {
    return ut -> ut.getUnitAttachment().canBeDamaged();
  }

  public static Predicate<Unit> unitCanBeDamaged() {
    return unit -> unitTypeCanBeDamaged().test(unit.getType());
  }

  public static Predicate<Unit> unitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      if (!ua.canBeDamaged()) {
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
      final UnitAttachment ua = bomberOrRocket.getUnitAttachment();
      final Set<UnitType> allowedTargets =
          ua.getBombingTargets(bomberOrRocket.getData().getUnitTypeList());
      return allowedTargets.contains(unit.getType());
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
      final UnitAttachment ua = unit.getUnitAttachment();
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
      final UnitAttachment ua = unit.getUnitAttachment();
      return ua.canBeDamaged() && ua.canDieFromReachingMaxDamage();
    };
  }

  public static Predicate<UnitType> unitTypeIsInfrastructure() {
    return ut -> ut.getUnitAttachment().isInfrastructure();
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
    return u -> unitTypeIsSupporterOrHasCombatAbility(attack, u.getOwner()).test(u.getType());
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA
   * ability.
   */
  private static Predicate<UnitType> unitTypeIsSupporterOrHasCombatAbility(
      final boolean attack, final GamePlayer player) {
    return ut -> {
      // if unit has attack or defense, return true
      final UnitAttachment ua = ut.getUnitAttachment();
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
    return unit -> unit.getUnitAttachment().canScramble();
  }

  public static Predicate<Unit> unitWasScrambled() {
    return Unit::getWasScrambled;
  }

  public static Predicate<Unit> unitWasInAirBattle() {
    return Unit::getWasInAirBattle;
  }

  public static Predicate<Unit> unitCanBombard(final GamePlayer gamePlayer) {
    return unit -> unit.getUnitAttachment().getCanBombard(gamePlayer);
  }

  public static Predicate<Unit> unitCanBlitz() {
    return unit -> unit.getUnitAttachment().getCanBlitz(unit.getOwner());
  }

  public static Predicate<Unit> unitIsLandTransport() {
    return unit -> unit.getUnitAttachment().isLandTransport();
  }

  public static Predicate<Unit> unitIsLandTransportWithCapacity() {
    return unit -> unitIsLandTransport().and(unitCanTransport()).test(unit);
  }

  public static Predicate<Unit> unitIsLandTransportWithoutCapacity() {
    return unit -> unitIsLandTransport().and(unitCanTransport().negate()).test(unit);
  }

  public static Predicate<Unit> unitIsNotInfrastructureAndNotCapturedOnEntering(
      final GamePlayer player, final Territory territory) {
    return unit ->
        !unit.getUnitAttachment().isInfrastructure()
            && !unitCanBeCapturedOnEnteringThisTerritory(player, territory).test(unit);
  }

  public static Predicate<UnitType> unitTypeIsSuicideOnAttack() {
    return type -> type.getUnitAttachment().getIsSuicideOnAttack();
  }

  public static Predicate<UnitType> unitTypeIsSuicideOnDefense() {
    return type -> type.getUnitAttachment().getIsSuicideOnDefense();
  }

  public static Predicate<Unit> unitIsSuicideOnAttack() {
    return unit -> unit.getUnitAttachment().getIsSuicideOnAttack();
  }

  public static Predicate<Unit> unitIsSuicideOnDefense() {
    return unit -> unit.getUnitAttachment().getIsSuicideOnDefense();
  }

  public static Predicate<Unit> unitIsSuicideOnHit() {
    return unit -> unit.getUnitAttachment().isSuicideOnHit();
  }

  public static Predicate<Unit> unitIsKamikaze() {
    return unit -> unit.getUnitAttachment().isKamikaze();
  }

  public static Predicate<UnitType> unitTypeIsAir() {
    return type -> type.getUnitAttachment().isAir();
  }

  private static Predicate<UnitType> unitTypeIsNotAir() {
    return type -> !type.getUnitAttachment().isAir();
  }

  public static Predicate<Unit> unitCanLandOnCarrier() {
    return unit -> unit.getUnitAttachment().getCarrierCost() != -1;
  }

  public static Predicate<Unit> unitIsCarrier() {
    return unit -> unit.getUnitAttachment().getCarrierCapacity() != -1;
  }

  public static Predicate<Territory> territoryHasOwnedCarrier(final GamePlayer player) {
    return t -> t.anyUnitsMatch(unitIsOwnedBy(player).and(unitIsCarrier()));
  }

  public static Predicate<Unit> unitIsAlliedCarrier(final GamePlayer player) {
    return u -> u.getUnitAttachment().getCarrierCapacity() != -1 && player.isAllied(u.getOwner());
  }

  public static Predicate<Unit> unitCanBeTransported() {
    return unit -> unit.getUnitAttachment().getTransportCost() != -1;
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
    return u -> u.getUnloadedTo() != null;
  }

  private static Predicate<Unit> unitWasLoadedThisTurn() {
    return Unit::getWasLoadedThisTurn;
  }

  static Predicate<Unit> unitWasNotLoadedThisTurn() {
    return unitWasLoadedThisTurn().negate();
  }

  public static Predicate<Unit> unitCanTransport() {
    return unit -> unit.getUnitAttachment().getTransportCapacity() != -1;
  }

  public static Predicate<UnitType> unitTypeCanProduceUnits() {
    return ut -> ut.getUnitAttachment().canProduceUnits();
  }

  public static Predicate<Unit> unitCanProduceUnits() {
    return u -> unitTypeCanProduceUnits().test(u.getType());
  }

  public static Predicate<UnitType> unitTypeHasMaxBuildRestrictions() {
    return ut -> ut.getUnitAttachment().getMaxBuiltPerPlayer() >= 0;
  }

  public static Predicate<UnitType> unitTypeIsRocket() {
    return ut -> ut.getUnitAttachment().isRocket();
  }

  static Predicate<Unit> unitIsRocket() {
    return obj -> unitTypeIsRocket().test(obj.getType());
  }

  public static Predicate<UnitType> unitTypeCanNotMoveDuringCombatMove() {
    return u -> u.getUnitAttachment().canNotMoveDuringCombatMove();
  }

  public static Predicate<Unit> unitCanNotMoveDuringCombatMove() {
    return u -> unitTypeCanNotMoveDuringCombatMove().test(u.getType());
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
      final UnitAttachment ua = obj.getUnitAttachment();
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
            .anyMatch(ut -> ut.getUnitAttachment().getTargetsAa(unitTypeList).contains(unitType));
  }

  public static Predicate<Unit> unitIsAaOfTypeAa(final String typeAa) {
    return u -> u.getUnitAttachment().getTypeAa().equals(typeAa);
  }

  public static Predicate<Unit> unitAaShotDamageableInsteadOfKillingInstantly() {
    return u -> u.getUnitAttachment().getDamageableAa();
  }

  private static Predicate<Unit> unitIsAaThatWillNotFireIfPresentEnemyUnits(
      final Collection<Unit> enemyUnitsPresent) {
    return obj -> {
      final UnitAttachment ua = obj.getUnitAttachment();
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
      final int maxRoundsAa = obj.getUnitAttachment().getMaxRoundsAa();
      return maxRoundsAa < 0 || maxRoundsAa >= battleRoundNumber;
    };
  }

  private static Predicate<Unit> unitIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return u -> unitTypeIsAaThatCanFireOnRound(battleRoundNumber).test(u.getType());
  }

  public static Predicate<Unit> unitIsAaThatCanFire(
      final Collection<Unit> unitsMovingOrAttacking,
      final Map<String, Set<UnitType>> airborneTechTargetsAllowed,
      final GamePlayer playerMovingOrAttacking,
      final Predicate<Unit> typeOfAa,
      final int battleRoundNumber,
      final boolean defending) {
    return enemyUnit(playerMovingOrAttacking)
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
    return ut -> ut.getUnitAttachment().isAaForCombatOnly();
  }

  public static Predicate<Unit> unitIsAaForCombatOnly() {
    return ut -> unitTypeIsAaForCombatOnly().test(ut.getType());
  }

  public static Predicate<UnitType> unitTypeIsAaForBombingThisUnitOnly() {
    return ut -> ut.getUnitAttachment().isAaForBombingThisUnitOnly();
  }

  public static Predicate<Unit> unitIsAaForBombingThisUnitOnly() {
    return u -> unitTypeIsAaForBombingThisUnitOnly().test(u.getType());
  }

  static Predicate<Unit> unitIsAaForFlyOverOnly() {
    return u -> u.getUnitAttachment().isAaForFlyOverOnly();
  }

  public static Predicate<UnitType> unitTypeIsAaForAnything() {
    return ut -> {
      final UnitAttachment ua = ut.getUnitAttachment();
      return ua.isAaForBombingThisUnitOnly() || ua.isAaForCombatOnly() || ua.isAaForFlyOverOnly();
    };
  }

  public static Predicate<Unit> unitIsAaForAnything() {
    return u -> unitTypeIsAaForAnything().test(u.getType());
  }

  public static Predicate<Unit> unitIsNotAa() {
    return unitIsAaForAnything().negate();
  }

  public static Predicate<Unit> unitMaxAaAttacksIsInfinite() {
    return u -> u.getUnitAttachment().getMaxAaAttacks() == -1;
  }

  public static Predicate<Unit> unitMayOverStackAa() {
    return u -> u.getUnitAttachment().getMayOverStackAa();
  }

  static Predicate<Unit> unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return u -> {
      final UnitAttachment ua = u.getUnitAttachment();
      return ua.getAttackAa(u.getOwner()) > 0 && ua.getMaxAaAttacks() != 0;
    };
  }

  static Predicate<Unit> unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return u -> {
      final UnitAttachment ua = u.getUnitAttachment();
      return ua.getOffensiveAttackAa(u.getOwner()) > 0 && ua.getMaxAaAttacks() != 0;
    };
  }

  public static Predicate<Unit> unitIsLandTransportable() {
    return unit -> unit.getUnitAttachment().isLandTransportable();
  }

  public static Predicate<Unit> unitIsAirTransportable() {
    return u -> {
      final TechAttachment ta = u.getOwner().getTechAttachment();
      if (!ta.getParatroopers()) {
        return false;
      }
      return u.getUnitAttachment().isAirTransportable();
    };
  }

  public static Predicate<Unit> unitIsAirTransport() {
    return u -> {
      final TechAttachment ta = u.getOwner().getTechAttachment();
      if (!ta.getParatroopers()) {
        return false;
      }
      return u.getUnitAttachment().isAirTransport();
    };
  }

  public static Predicate<Unit> unitIsArtillery() {
    return u -> u.getUnitAttachment().getArtillery();
  }

  public static Predicate<Unit> unitIsArtillerySupportable() {
    return u -> u.getUnitAttachment().getArtillerySupportable();
  }

  public static Predicate<Territory> territoryIsWater() {
    return Territory::isWater;
  }

  public static Predicate<Territory> territoryIsLand() {
    return territoryIsWater().negate();
  }

  public static Predicate<Territory> territoryIsIsland() {
    return t -> {
      final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
      return neighbors.size() == 1 && CollectionUtils.getAny(neighbors).isWater();
    };
  }

  public static Predicate<Territory> territoryIsVictoryCity() {
    return t -> {
      return 0 != TerritoryAttachment.get(t).map(TerritoryAttachment::getVictoryCity).orElse(0);
    };
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
  public static Predicate<Territory> territoryCanCollectIncomeFrom(final GamePlayer player) {
    final boolean contestedDoNotProduce =
        Properties.getContestedTerritoriesProduceNoIncome(player.getData().getProperties());
    return t -> {
      final Optional<TerritoryAttachment> optionalTerritoryAttachment = TerritoryAttachment.get(t);
      if (optionalTerritoryAttachment.isEmpty()) {
        return false;
      }
      final Optional<GamePlayer> optionalOriginalOwner = OriginalOwnerTracker.getOriginalOwner(t);
      // if it's water, it is a Convoy Center
      // Can't get PUs for capturing a CC, only original owner can get them. (Except capturing
      // null player CCs)
      if (t.isWater()
          && !(optionalOriginalOwner.isEmpty()
              || optionalOriginalOwner.get().isNull()
              || optionalOriginalOwner.get().equals(player))) {
        return false;
      }
      final TerritoryAttachment ta = optionalTerritoryAttachment.get();
      if (ta.getConvoyRoute() && !ta.getConvoyAttached().isEmpty()) {
        // Determine if at least one part of the convoy route is owned by us or an ally
        boolean atLeastOne = false;
        for (final Territory convoy : ta.getConvoyAttached()) {
          if (player.isAllied(convoy.getOwner())
              && TerritoryAttachment.getOrThrow(convoy).getConvoyRoute()) {
            atLeastOne = true;
          }
        }
        if (!atLeastOne) {
          return false;
        }
      }
      return !(contestedDoNotProduce && !territoryHasNoEnemyUnits(player).test(t));
    };
  }

  public static Predicate<Territory> territoryHasNeighborMatching(
      final GameMap gameMap, final Predicate<Territory> match) {
    return t -> !gameMap.getNeighbors(t, match).isEmpty();
  }

  public static Predicate<Territory>
      territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(
          final GamePlayer player) {
    return t ->
        !player
            .getData()
            .getMap()
            .getNeighbors(t, territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(player))
            .isEmpty();
  }

  public static Predicate<Territory> territoryHasWaterNeighbor(final GameMap gameMap) {
    return t -> !gameMap.getNeighbors(t, territoryIsWater()).isEmpty();
  }

  public static Predicate<Territory> territoryIsOwnedAndHasOwnedUnitMatching(
      final GamePlayer player, final Predicate<Unit> unitMatch) {
    return t -> t.isOwnedBy(player) && t.anyUnitsMatch(unitIsOwnedBy(player).and(unitMatch));
  }

  public static Predicate<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(
      final GamePlayer player) {
    return t -> t.isOwnedBy(player) && t.anyUnitsMatch(unitCanProduceUnits());
  }

  private static Predicate<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(
      final GamePlayer player) {
    return t -> {
      final GameData data = player.getData();
      if (!GameStepPropertiesHelper.getCombinedTurns(data, player).contains(t.getOwner())) {
        return false;
      }
      if (!t.anyUnitsMatch(unitCanProduceUnits())) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
      return !(bt == null || bt.wasConquered(t));
    };
  }

  static Predicate<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(
      final GamePlayer player) {
    return t -> isTerritoryAllied(player).test(t) && t.anyUnitsMatch(unitCanProduceUnits());
  }

  public static Predicate<Territory> territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(
      final GamePlayer player, final Predicate<Unit> unitMatch) {
    return t -> {
      if (!player.isAtWar(t.getOwner())) {
        return false;
      }
      return !t.getOwner().isNull() && t.anyUnitsMatch(enemyUnit(player).and(unitMatch));
    };
  }

  public static Predicate<Territory> territoryIsEmptyOfCombatUnits(final GamePlayer player) {
    return t ->
        t.getUnitCollection().allMatch(unitIsInfrastructure().or(enemyUnit(player).negate()));
  }

  public static Predicate<Territory> territoryIsNeutralButNotWater() {
    return isTerritoryNeutral().and(territoryIsWater().negate());
  }

  public static Predicate<Territory> territoryIsUnownedWater() {
    return isTerritoryNeutral().and(territoryIsWater());
  }

  public static Predicate<Territory> territoryIsImpassable() {
    return t -> {
      if (t.isWater()) {
        return false;
      }
      return TerritoryAttachment.get(t).map(TerritoryAttachment::getIsImpassable).orElse(false);
    };
  }

  public static Predicate<Territory> territoryEffectsAllowUnits(final Collection<Unit> units) {
    return t -> {
      Set<UnitType> types = TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t);
      return units.stream().noneMatch(Matches.unitIsOfTypes(types));
    };
  }

  public static Predicate<Territory> territoryIsNotImpassable() {
    return territoryIsImpassable().negate();
  }

  public static Predicate<Territory> seaCanMoveOver(final GamePlayer player) {
    return t -> t.isWater() && territoryIsPassableAndNotRestricted(player).test(t);
  }

  public static Predicate<Territory> airCanFlyOver(
      final GamePlayer player, final boolean areNeutralsPassableByAir) {
    return t -> {
      if (!areNeutralsPassableByAir && territoryIsNeutralButNotWater().test(t)) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player).test(t)
          && !(territoryIsLand().test(t)
              && !player
                  .getData()
                  .getRelationshipTracker()
                  .canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
    };
  }

  public static Predicate<Territory> territoryIsPassableAndNotRestricted(final GamePlayer player) {
    return t -> {
      if (territoryIsImpassable().test(t)) {
        return false;
      }
      if (!Properties.getMovementByTerritoryRestricted(player.getData().getProperties())) {
        return true;
      }
      final RulesAttachment ra = player.getRulesAttachment();
      if (ra == null || ra.getMovementRestrictionTerritories() == null) {
        return true;
      }
      final Collection<Territory> listedTerritories =
          ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
      return (ra.isMovementRestrictionTypeAllowed() == listedTerritories.contains(t));
    };
  }

  private static Predicate<Territory> territoryIsImpassableToLandUnits(final GamePlayer player) {
    return t -> t.isWater() || territoryIsPassableAndNotRestricted(player).negate().test(t);
  }

  public static Predicate<Territory> territoryIsNotImpassableToLandUnits(final GamePlayer player) {
    return t -> territoryIsImpassableToLandUnits(player).negate().test(t);
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
      final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded,
      final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported,
      final boolean isLandingZoneOnLandForAirUnits) {
    final GameProperties properties = playerWhoOwnsAllTheUnitsMoving.getData().getProperties();
    final RelationshipTracker rt =
        playerWhoOwnsAllTheUnitsMoving.getData().getRelationshipTracker();
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
        final RulesAttachment ra = playerWhoOwnsAllTheUnitsMoving.getRulesAttachment();
        if (ra != null && ra.getMovementRestrictionTerritories() != null) {
          final Collection<Territory> listedTerritories =
              ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
          if (!(ra.isMovementRestrictionTypeAllowed() == listedTerritories.contains(t))) {
            return false;
          }
        }
      }
      if (hasLandUnitsNotBeingTransportedOrBeingLoaded && t.isWater()) {
        return false;
      }
      if (hasSeaUnitsNotBeingTransported && !t.isWater()) {
        return false;
      }
      if (!t.isWater()) {
        if (hasLandUnitsNotBeingTransportedOrBeingLoaded
            && !rt.canMoveLandUnitsOverOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
        if (hasAirUnitsNotBeingTransported
            && !rt.canMoveAirUnitsOverOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
      }
      return (!isLandingZoneOnLandForAirUnits
              || rt.canLandAirUnitsOnOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner()))
          && !(isCombatMovePhase
              && !rt.canMoveIntoDuringCombatMove(playerWhoOwnsAllTheUnitsMoving, t.getOwner()));
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
      final UnitAttachment ua = unit.getUnitAttachment();
      if (ua.isAir()) {
        if (TerritoryAttachment.hasAirBase(route.getStart())) {
          left = left.add(BigDecimal.ONE);
        }
        if (TerritoryAttachment.hasAirBase(route.getEnd())) {
          left = left.add(BigDecimal.ONE);
        }
      }
      // Apply "AAP" (Pacific) Naval base bonus. Note: In "AAG40" (1940) naval bases are implemented
      // differently: via a naval base _unit_ that boosts movement rather than territory attachment.
      if (ua.isSea() && unit.getData().getSequence().getStep().isNonCombat()) {
        // If a zone adjacent to the starting and ending sea zones are allied naval bases, increase
        // the range.
        // TODO Still need to be able to handle stops on the way
        if (hasNeighboringAlliedNavalBase(route.getStart(), unit.getOwner())
            && hasNeighboringAlliedNavalBase(route.getEnd(), unit.getOwner())) {
          left = left.add(BigDecimal.ONE);
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

  private static boolean hasNeighboringAlliedNavalBase(Territory t, GamePlayer player) {
    return t.getData().getMap().getNeighbors(t).stream()
        .anyMatch(t2 -> hasAlliedNavalBase(t2, player));
  }

  private static boolean hasAlliedNavalBase(Territory t, GamePlayer player) {
    return TerritoryAttachment.hasNavalBase(t) && t.getOwner().isAllied(player);
  }

  public static Predicate<Unit> unitHasMovementLeft() {
    return Unit::hasMovementLeft;
  }

  public static Predicate<Unit> unitCanMove() {
    return u -> unitTypeCanMove(u.getOwner()).test(u.getType());
  }

  public static Predicate<UnitType> unitTypeCanMove(final GamePlayer player) {
    return unitType -> unitType.getUnitAttachment().getMovement(player) > 0;
  }

  public static Predicate<UnitType> unitTypeIsStatic(final GamePlayer gamePlayer) {
    return unitType -> !unitTypeCanMove(gamePlayer).test(unitType);
  }

  public static Predicate<Unit> unitIsLandAndOwnedBy(final GamePlayer player) {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      return !ua.isSea() && !ua.isAir() && unit.isOwnedBy(player);
    };
  }

  public static Predicate<Unit> unitIsOwnedBy(final GamePlayer player) {
    return unit -> unit.isOwnedBy(player);
  }

  public static Predicate<Unit> unitIsOwnedByAnyOf(final Collection<GamePlayer> players) {
    return unit -> players.contains(unit.getOwner());
  }

  public static Predicate<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units) {
    final Collection<UnitCategory> unitCategories = UnitSeparator.categorize(units);
    return unit -> {
      final Collection<Unit> transporting = unit.getTransporting();
      return !Collections.disjoint(UnitSeparator.categorize(transporting), unitCategories);
    };
  }

  public static Predicate<Territory> isTerritoryAllied(final GamePlayer player) {
    return t -> player.isAllied(t.getOwner());
  }

  public static Predicate<Territory> isTerritoryOwnedBy(final GamePlayer player) {
    return t -> t.isOwnedBy(player);
  }

  public static Predicate<Territory> isTerritoryNeutral() {
    return t -> t.getOwner().isNull();
  }

  public static Predicate<Territory> isTerritoryOwnedByAnyOf(final Collection<GamePlayer> players) {
    return t -> players.contains(t.getOwner());
  }

  public static Predicate<Unit> isUnitAllied(final GamePlayer player) {
    return u -> player.isAllied(u.getOwner());
  }

  public static Predicate<Territory> isTerritoryFriendly(final GamePlayer player) {
    return t -> t.isWater() || t.isOwnedBy(player) || player.isAllied(t.getOwner());
  }

  private static Predicate<Unit> unitIsEnemyAaForFlyOver(final GamePlayer player) {
    return unitIsAaForFlyOverOnly().and(enemyUnit(player));
  }

  public static Predicate<Unit> unitIsInTerritory(final Territory territory) {
    return u -> territory.getUnits().contains(u);
  }

  public static Predicate<Territory> isTerritoryEnemy(final GamePlayer player) {
    return t -> !t.isOwnedBy(player) && player.isAtWar(t.getOwner());
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWater(final GamePlayer player) {
    // if we look at territory attachments, may have funny results for blockades or other things
    // that are passable and not owned. better to check them by alliance. (veqryn)
    return t ->
        !t.isOwnedBy(player)
            && ((!t.getOwner().isNull() || !t.isWater()) && player.isAtWar(t.getOwner()));
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(
      final GamePlayer player) {
    return territoryNotImpassibleOrRestrictedOrNeutralWaterAndNotOwnedBy(player)
        .and(t -> player.isAtWar(t.getOwner()));
  }

  public static Predicate<Territory> isTerritoryNotUnownedWaterAndCanBeTakenOverBy(
      final GamePlayer player) {
    final RelationshipTracker relationshipTracker = player.getData().getRelationshipTracker();
    return territoryNotImpassibleOrRestrictedOrNeutralWaterAndNotOwnedBy(player)
        .and(t -> relationshipTracker.canTakeOverOwnedTerritory(player, t.getOwner()));
  }

  private static Predicate<Territory> territoryNotImpassibleOrRestrictedOrNeutralWaterAndNotOwnedBy(
      final GamePlayer player) {
    return not(isTerritoryOwnedBy(player))
        .and(not(territoryIsUnownedWater()))
        .and(territoryIsPassableAndNotRestricted(player));
  }

  public static Predicate<Territory> territoryIsBlitzable(final GamePlayer player) {
    return t -> {
      // cant blitz water
      if (t.isWater()) {
        return false;
      }
      // cant blitz on neutrals
      GameData data = player.getData();
      if (t.getOwner().isNull() && !Properties.getNeutralsBlitzable(data.getProperties())) {
        return false;
      }
      // was conquered but not blitzed
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
          && !AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(t)) {
        return false;
      }
      // we ignore neutral units
      final Predicate<Unit> blitzableUnits =
          PredicateBuilder.of(enemyUnit(player).negate())
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
    return isTerritoryNeutral().and(t -> Properties.getNeutralCharge(properties) <= 0);
  }

  public static Predicate<Territory> territoryDoesNotCostMoneyToEnter(
      final GameProperties properties) {
    return t ->
        t.isWater() || !t.getOwner().isNull() || Properties.getNeutralCharge(properties) <= 0;
  }

  public static Predicate<Unit> enemyUnit(final GamePlayer player) {
    return unit -> player.isAtWar(unit.getOwner());
  }

  public static Predicate<Unit> enemyUnitOfAnyOfThesePlayers(final Collection<GamePlayer> players) {
    return unit -> unit.getOwner().isAtWarWithAnyOfThesePlayers(players);
  }

  public static Predicate<Unit> alliedUnit(final GamePlayer player) {
    return unit -> unit.isOwnedBy(player) || player.isAllied(unit.getOwner());
  }

  public static Predicate<Unit> alliedUnitOfAnyOfThesePlayers(
      final Collection<GamePlayer> players) {
    return unit ->
        unitIsOwnedByAnyOf(players).test(unit)
            || unit.getOwner().isAlliedWithAnyOfThesePlayers(players);
  }

  public static Predicate<Territory> territoryIs(final Territory test) {
    return t -> t.equals(test);
  }

  public static Predicate<Territory> territoryHasLandUnitsOwnedBy(final GamePlayer player) {
    return t -> t.anyUnitsMatch(unitIsOwnedBy(player).and(unitIsLand()));
  }

  public static Predicate<Territory> territoryHasUnitsOwnedBy(final GamePlayer player) {
    return t -> t.anyUnitsMatch(unitIsOwnedBy(player));
  }

  public static Predicate<Territory> territoryHasUnitsThatMatch(final Predicate<Unit> cond) {
    return t -> t.anyUnitsMatch(cond);
  }

  public static Predicate<Territory> territoryHasEnemyAaForFlyOver(final GamePlayer player) {
    return t -> t.anyUnitsMatch(unitIsEnemyAaForFlyOver(player));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnits(final GamePlayer player) {
    return t -> !t.anyUnitsMatch(enemyUnit(player));
  }

  public static Predicate<Territory> territoryHasAlliedUnits(final GamePlayer player) {
    return t -> t.anyUnitsMatch(alliedUnit(player));
  }

  static Predicate<Territory> territoryHasNonSubmergedEnemyUnits(final GamePlayer player) {
    return t -> t.anyUnitsMatch(enemyUnit(player).and(not(unitIsSubmerged())));
  }

  public static Predicate<Territory> territoryHasEnemyLandUnits(final GamePlayer player) {
    return t -> t.anyUnitsMatch(enemyUnit(player).and(unitIsLand()));
  }

  public static Predicate<Territory> territoryHasEnemySeaUnits(final GamePlayer player) {
    return t -> t.anyUnitsMatch(enemyUnit(player).and(unitIsSea()));
  }

  public static Predicate<Territory> territoryHasEnemyUnits(final GamePlayer player) {
    return t -> t.anyUnitsMatch(enemyUnit(player));
  }

  public static Predicate<Territory> territoryIsNotUnownedWater() {
    return t -> !(t.isWater() && TerritoryAttachment.get(t).isEmpty() && t.getOwner().isNull());
  }

  /**
   * The territory is owned by the enemy of those enemy units (i.e. probably owned by you or your
   * ally, but not necessarily so in an FFA type game).
   */
  public static Predicate<Territory> territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(
      final GamePlayer player) {
    return t -> {
      final List<Unit> enemyUnits =
          t.getUnitCollection()
              .getMatches(enemyUnit(player).and(unitIsNotAir()).and(unitIsNotInfrastructure()));
      final Collection<GamePlayer> enemyPlayers =
          enemyUnits.stream().map(Unit::getOwner).collect(Collectors.toSet());
      return isAtWarWithAnyOfThesePlayers(enemyPlayers).test(t.getOwner());
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
   * @param forceLoadParatroopersIfPossible Should we load paratroopers? (if not, we assume they are
   */
  public static Predicate<Unit> unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
      final Collection<Unit> units,
      final GamePlayer currentPlayer,
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
          MoveValidator.carrierMustMoveWith(units, units, currentPlayer);
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
    return unitIsOfTypes(Set.of(types));
  }

  public static Predicate<Territory> territoryWasFoughtOver(final BattleTracker tracker) {
    return t -> tracker.wasBattleFought(t) || tracker.wasBlitzed(t);
  }

  public static Predicate<Unit> unitIsSubmerged() {
    return Unit::getSubmerged;
  }

  public static Predicate<UnitType> unitTypeIsFirstStrike() {
    return type -> type.getUnitAttachment().getIsFirstStrike();
  }

  public static Predicate<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return u -> TechTracker.hasImprovedArtillerySupport(u.getOwner());
  }

  static Predicate<Unit> unitCanRepairOthers() {
    return unit -> {
      if (unitIsDisabled().test(unit) || unitIsBeingTransported().test(unit)) {
        return false;
      }
      return !unit.getUnitAttachment().getRepairsUnits().isEmpty();
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
          if (!ownCapital && territoryOfRepairUnit.isOwnedBy(player)) {
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
      final UnitAttachment ua = unitCanRepair.getUnitAttachment();
      return ua.getRepairsUnits().keySet().contains(damagedUnit.getType());
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
      final Territory territory, final GamePlayer player) {
    return damagedUnit -> {
      final Predicate<Unit> damaged =
          unitHasMoreThanOneHitPointTotal().and(unitHasTakenSomeDamage());
      if (!damaged.test(damagedUnit)) {
        return false;
      }
      final Predicate<Unit> repairUnit =
          alliedUnit(player)
              .and(unitCanRepairOthers())
              .and(unitCanRepairThisUnit(damagedUnit, territory));
      if (territory.anyUnitsMatch(repairUnit)) {
        return true;
      }
      if (unitIsSea().test(damagedUnit)) {
        final Collection<Territory> neighbors =
            player.getData().getMap().getNeighbors(territory, territoryIsLand());
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitLand =
              alliedUnit(player)
                  .and(unitCanRepairOthers())
                  .and(unitCanRepairThisUnit(damagedUnit, current))
                  .and(unitIsLand());
          if (current.anyUnitsMatch(repairUnitLand)) {
            return true;
          }
        }
      } else if (unitIsLand().test(damagedUnit)) {
        final Collection<Territory> neighbors =
            player.getData().getMap().getNeighbors(territory, territoryIsWater());
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitSea =
              alliedUnit(player)
                  .and(unitCanRepairOthers())
                  .and(unitCanRepairThisUnit(damagedUnit, current))
                  .and(unitIsSea());
          if (current.anyUnitsMatch(repairUnitSea)) {
            return true;
          }
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitCanGiveBonusMovement() {
    return unit ->
        !unit.getUnitAttachment().getGivesMovement().isEmpty()
            && unitIsBeingTransported().negate().test(unit);
  }

  static Predicate<Unit> unitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return unitWhichCanGiveBonusMovement -> {
      if (unitIsDisabled().test(unitWhichCanGiveBonusMovement)) {
        return false;
      }
      final UnitType type = unitWhichCanGiveBonusMovement.getType();
      final UnitAttachment ua = type.getUnitAttachment();
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
      final Territory territory, final GamePlayer player) {
    return unitWhichWillGetBonus -> {
      final Predicate<Unit> givesBonusUnit =
          alliedUnit(player).and(unitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
      if (territory.anyUnitsMatch(givesBonusUnit)) {
        return true;
      }
      if (unitIsSea().test(unitWhichWillGetBonus)) {
        final Predicate<Unit> givesBonusUnitLand = givesBonusUnit.and(unitIsLand());
        final Collection<Territory> neighbors =
            player.getData().getMap().getNeighbors(territory, territoryIsLand());
        return neighbors.stream().anyMatch(t -> t.anyUnitsMatch(givesBonusUnitLand));
      }
      return false;
    };
  }

  static Predicate<Unit> unitCreatesUnits() {
    return unit -> !unit.getUnitAttachment().getCreatesUnitsList().isEmpty();
  }

  static Predicate<Unit> unitCreatesResources() {
    return unit -> !unit.getUnitAttachment().getCreatesResourcesList().isEmpty();
  }

  public static Predicate<UnitType> unitTypeConsumesUnitsOnCreation() {
    return unit -> !unit.getUnitAttachment().getConsumesUnits().isEmpty();
  }

  public static Predicate<Unit> unitConsumesUnitsOnCreation() {
    return unit -> !unit.getUnitAttachment().getConsumesUnits().isEmpty();
  }

  static Predicate<Unit> unitWhichConsumesUnitsHasRequiredUnits(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return unitWhichRequiresUnits -> {
      if (!unitConsumesUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final UnitAttachment ua = unitWhichRequiresUnits.getUnitAttachment();
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      boolean canBuild = true;
      for (final UnitType ut : requiredUnits) {
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final int numberInTerritory =
            CollectionUtils.countMatches(
                unitsInTerritoryAtStartOfTurn,
                eligibleUnitToConsume(unitWhichRequiresUnits.getOwner(), ut));
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

  public static Predicate<Unit> eligibleUnitToConsume(GamePlayer owner, UnitType ut) {
    return unitIsOwnedBy(owner)
        .and(unitIsOfType(ut))
        .and(unitHasNotTakenAnyBombingUnitDamage())
        .and(unitHasNotTakenAnyDamage())
        .and(unitIsNotDisabled());
  }

  public static Predicate<Unit> unitRequiresUnitsOnCreation() {
    return unit -> !unit.getUnitAttachment().getRequiresUnits().isEmpty();
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
      final Collection<Unit> unitsInTerritoryAtStartOfTurnWithSameOwnerAndNotDisabled =
          CollectionUtils.getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled);
      final UnitAttachment ua = unitWhichRequiresUnits.getUnitAttachment();
      for (final String[] combo : ua.getRequiresUnits()) {
        boolean haveAll = true;
        for (final UnitType ut : ua.getListedUnits(combo)) {
          if (unitsInTerritoryAtStartOfTurnWithSameOwnerAndNotDisabled.stream()
              .noneMatch(unitIsOfType(ut))) {
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

  /** Check if unit meets requiredUnitsToMove criteria and can move into territory. */
  public static Predicate<Unit> unitHasRequiredUnitsToMove(final Territory t) {
    return unit -> {
      final UnitAttachment ua = unit.getUnitAttachment();
      if (ua.getRequiresUnitsToMove().isEmpty()) {
        return true;
      }

      final Predicate<Unit> unitIsOwnedByAndNotDisabled =
          isUnitAllied(unit.getOwner()).and(unitIsNotDisabled());
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

  public static Predicate<Territory> territoryHasRequiredUnitsToMove(final Collection<Unit> units) {
    return t -> units.stream().allMatch(unitHasRequiredUnitsToMove(t));
  }

  static Predicate<Territory> territoryIsBlockadeZone() {
    return t -> TerritoryAttachment.get(t).map(TerritoryAttachment::getBlockadeZone).orElse(false);
  }

  public static Predicate<UnitType> unitTypeIsConstruction() {
    return type -> {
      final UnitAttachment ua = type.getUnitAttachment();
      return ua.isConstruction();
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
      final UnitAttachment ua = unit.getUnitAttachment();
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
      final GamePlayer ownerOfUnitsMoving) {
    return t -> {
      if (t.isWater()) {
        return true;
      }
      final var relationshipTracker = ownerOfUnitsMoving.getData().getRelationshipTracker();
      return relationshipTracker.canMoveLandUnitsOverOwnedLand(t.getOwner(), ownerOfUnitsMoving);
    };
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveAirUnitsOverOwnedLand() {
    return relationship ->
        relationship.getRelationshipTypeAttachment().canMoveAirUnitsOverOwnedLand();
  }

  /** If the territory is not land, returns true. Else, tests relationship of the owners. */
  public static Predicate<Territory> territoryAllowsCanMoveAirUnitsOverOwnedLand(
      final GamePlayer ownerOfUnitsMoving) {
    return t -> {
      if (t.isWater()) {
        return true;
      }
      final var relationshipTracker = t.getOwner().getData().getRelationshipTracker();
      return relationshipTracker.canMoveAirUnitsOverOwnedLand(t.getOwner(), ownerOfUnitsMoving);
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

  public static Predicate<GamePlayer> isAtWar(final GamePlayer player) {
    return player::isAtWar;
  }

  public static Predicate<GamePlayer> isAtWarWithAnyOfThesePlayers(
      final Collection<GamePlayer> players) {
    return player2 -> player2.isAtWarWithAnyOfThesePlayers(players);
  }

  public static Predicate<GamePlayer> isAllied(final GamePlayer player) {
    return player::isAllied;
  }

  public static Predicate<GamePlayer> isAlliedWithAnyOfThesePlayers(
      final Collection<GamePlayer> players) {
    return player2 -> player2.isAlliedWithAnyOfThesePlayers(players);
  }

  public static Predicate<Unit> unitIsOwnedAndIsFactoryOrCanProduceUnits(final GamePlayer player) {
    return unit -> unitCanProduceUnits().test(unit) && unitIsOwnedBy(player).test(unit);
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith() {
    return unit -> !unit.getUnitAttachment().getReceivesAbilityWhenWith().isEmpty();
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith(
      final String filterForAbility, final String filterForUnitType) {
    return u -> {
      for (final String receives : u.getUnitAttachment().getReceivesAbilityWhenWith()) {
        final String[] s = receives.split(":", 2);
        if (s[0].equals(filterForAbility) && s[1].equals(filterForUnitType)) {
          return true;
        }
      }
      return false;
    };
  }

  private static Predicate<Unit> unitHasWhenCombatDamagedEffect() {
    return u -> !u.getUnitAttachment().getWhenCombatDamaged().isEmpty();
  }

  public static Predicate<Unit> unitHasWhenCombatDamagedEffect(final String filterForEffect) {
    return unitHasWhenCombatDamagedEffect()
        .and(
            unit -> {
              final int currentDamage = unit.getHits();
              final List<UnitAttachment.WhenCombatDamaged> whenCombatDamagedList =
                  unit.getUnitAttachment().getWhenCombatDamaged();
              for (final UnitAttachment.WhenCombatDamaged key : whenCombatDamagedList) {
                if (!key.getEffect().equals(filterForEffect)) {
                  continue;
                }
                final int damagedMin = key.getDamageMin();
                final int damagedMax = key.getDamageMax();
                if (currentDamage >= damagedMin && currentDamage <= damagedMax) {
                  return true;
                }
              }
              return false;
            });
  }

  public static Predicate<Territory> territoryHasCaptureOwnershipChanges() {
    return t ->
        !TerritoryAttachment.get(t)
            .map(TerritoryAttachment::getCaptureOwnershipChanges)
            .orElse(List.of())
            .isEmpty();
  }

  public static Predicate<Unit> unitWhenHitPointsDamagedChangesInto() {
    return u -> !u.getUnitAttachment().getWhenHitPointsDamagedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitAtMaxHitPointDamageChangesInto() {
    return u -> {
      final UnitAttachment ua = u.getUnitAttachment();
      return ua.getWhenHitPointsDamagedChangesInto().containsKey(ua.getHitPoints());
    };
  }

  static Predicate<Unit> unitWhenHitPointsRepairedChangesInto() {
    return u -> !u.getUnitAttachment().getWhenHitPointsRepairedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitWhenCapturedChangesIntoDifferentUnitType() {
    return u -> !u.getUnitAttachment().getWhenCapturedChangesInto().isEmpty();
  }

  public static Predicate<Unit> unitWhenCapturedSustainsDamage() {
    return u -> u.getUnitAttachment().getWhenCapturedSustainsDamage() > 0;
  }

  public static <T extends AbstractUserActionAttachment>
      Predicate<T> abstractUserActionAttachmentCanBeAttempted(
          final Map<ICondition, Boolean> testedConditions) {
    return uaa -> uaa.hasAttemptsLeft() && uaa.canPerform(testedConditions);
  }

  static Predicate<Unit> unitCanOnlyPlaceInOriginalTerritories() {
    return u -> {
      final UnitAttachment ua = u.getUnitAttachment();
      return ua.getSpecial().contains("canOnlyPlaceInOriginalTerritories");
    };
  }

  /**
   * Accounts for OccupiedTerrOf. Returns false if there is no territory attachment (like if it is
   * water).
   */
  public static Predicate<Territory> territoryIsOriginallyOwnedBy(final GamePlayer player) {
    return t -> {
      final Optional<TerritoryAttachment> optionalTerritoryAttachment = TerritoryAttachment.get(t);
      if (optionalTerritoryAttachment.isEmpty()) {
        return false;
      }
      final Optional<GamePlayer> optionalOriginalOwner =
          optionalTerritoryAttachment.get().getOriginalOwner();
      return optionalOriginalOwner
          .map(gamePlayer -> gamePlayer.equals(player))
          .orElseGet(() -> player == null);
    };
  }

  static Predicate<GamePlayer> isAlliedAndAlliancesCanChainTogether(final GamePlayer player) {
    return player2 ->
        relationshipTypeIsAlliedAndAlliancesCanChainTogether()
            .test(player.getData().getRelationshipTracker().getRelationshipType(player, player2));
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
      final GamePlayer currentPlayer) {
    return paa -> {
      for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
          paa.getRelationshipChanges()) {
        final GamePlayer p1 = relationshipChange.player1;
        final GamePlayer p2 = relationshipChange.player2;
        if (!currentPlayer.equals(p1) && p1.amNotDeadYet()) {
          return true;
        }
        if (!currentPlayer.equals(p2) && p2.amNotDeadYet()) {
          return true;
        }
      }
      return false;
    };
  }

  public static Predicate<Territory> airCanLandOnThisAlliedNonConqueredLandTerritory(
      final GamePlayer player) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(player.getData());
      if (bt.wasConquered(t)) {
        return false;
      }
      final GamePlayer owner = t.getOwner();
      if (owner.isNull()) {
        return false;
      }
      final RelationshipTracker rt = player.getData().getRelationshipTracker();
      return !(!rt.canMoveAirUnitsOverOwnedLand(player, owner)
          || !rt.canLandAirUnitsOnOwnedLand(player, owner));
    };
  }

  static Predicate<Territory> territoryAllowsRocketsCanFlyOver(final GamePlayer player) {
    return t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final GamePlayer owner = t.getOwner();
      if (owner.isNull()) {
        return true;
      }
      return player.getData().getRelationshipTracker().rocketsCanFlyOver(player, owner);
    };
  }

  // TODO: update scrambling to consider movement cost
  public static Predicate<Unit> unitCanScrambleOnRouteDistance(final Route route) {
    return unit -> unit.getUnitAttachment().getMaxScrambleDistance() >= route.numberOfSteps();
  }

  public static Predicate<Unit> unitCanIntercept() {
    return u -> u.getUnitAttachment().canIntercept();
  }

  public static Predicate<Unit> unitRequiresAirBaseToIntercept() {
    return u -> u.getUnitAttachment().getRequiresAirBaseToIntercept();
  }

  static Predicate<Unit> unitCanEscort() {
    return u -> u.getUnitAttachment().canEscort();
  }

  public static Predicate<Unit> unitCanAirBattle() {
    return u -> u.getUnitAttachment().canAirBattle();
  }

  public static Predicate<Territory> territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(
      final GamePlayer movingPlayer) {
    return t ->
        t.isOwnedBy(movingPlayer)
            || ((t.getOwner().isNull() && t.isWater())
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
                unitType ->
                    unitTypeCanBeHitByAaFire(
                            firingUnits, unitType.getData().getUnitTypeList(), battleRound)
                        .test(unitType));

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
