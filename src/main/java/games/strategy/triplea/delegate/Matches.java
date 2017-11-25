package games.strategy.triplea.delegate;

import static games.strategy.util.Util.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.AbstractUserActionAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.PredicateBuilder;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

/**
 * Useful match interfaces.
 *
 * <p>
 * Rather than writing code like,
 * </p>
 *
 * <pre>
 * boolean hasLand = false;
 * Iterator iter = someCollection.iterator();
 * while (iter.hasNext()) {
 *   Unit unit = (Unit) iter.next();
 *   UnitAttachment ua = UnitAttachment.get(unit.getType());
 *   if (ua.isAir) {
 *     hasAir = true;
 *     break;
 *   }
 * }
 * </pre>
 *
 * <p>
 * You can write code like,
 * </p>
 *
 * <pre>
 * boolean hasLand = Match.anyMatch(someCollection, Matches.unitIsAir());
 * </pre>
 *
 * <p>
 * The benefits should be obvious to any right minded person.
 * </p>
 */
public final class Matches {
  private Matches() {}

  /**
   * Returns a match whose condition is always satisfied.
   *
   * @return A match; never {@code null}.
   */
  public static <T> Predicate<T> always() {
    return Match.of(it -> true);
  }

  /**
   * Returns a match whose condition is never satisfied.
   *
   * @return A match; never {@code null}.
   */
  public static <T> Predicate<T> never() {
    return Match.of(it -> false);
  }

  /**
   * Returns the number of matches found.
   */
  public static <T> int countMatches(final Collection<T> collection, final Predicate<T> match) {
    return (int) collection.stream().filter(match).count();
  }

  /**
   * Returns the elements of the collection that match.
   */
  public static <T> List<T> getMatches(final Collection<T> collection, final Predicate<T> match) {
    return collection.stream().filter(match).collect(Collectors.toList());
  }

  /**
   * Only returns the first n matches.
   * If n matches cannot be found will return all matches that
   * can be found.
   */
  public static <T> List<T> getNMatches(final Collection<T> collection, final int max, final Predicate<T> match) {
    return collection.stream().filter(match).limit(max).collect(Collectors.toList());
  }

  public static Predicate<UnitType> unitTypeHasMoreThanOneHitPointTotal() {
    return Match.of(ut -> UnitAttachment.get(ut).getHitPoints() > 1);
  }

  public static Predicate<Unit> unitHasMoreThanOneHitPointTotal() {
    return Match.of(unit -> unitTypeHasMoreThanOneHitPointTotal().test(unit.getType()));
  }

  public static Predicate<Unit> unitHasTakenSomeDamage() {
    return Match.of(unit -> unit.getHits() > 0);
  }

  public static Predicate<Unit> unitHasNotTakenAnyDamage() {
    return unitHasTakenSomeDamage().negate();
  }

  public static Predicate<Unit> unitIsSea() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSea());
  }

  public static Predicate<Unit> unitIsSub() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSub());
  }

  public static Predicate<Unit> unitIsNotSub() {
    return unitIsSub().negate();
  }

  private static Predicate<Unit> unitIsCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsCombatTransport() && ua.getIsSea();
    });
  }

  static Predicate<Unit> unitIsNotCombatTransport() {
    return unitIsCombatTransport().negate();
  }

  /**
   * Returns a match indicating the specified unit is a transport but not a combat transport.
   */
  public static Predicate<Unit> unitIsTransportButNotCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport();
    });
  }

  /**
   * Returns a match indicating the specified unit is not a transport but may be a combat transport.
   */
  public static Predicate<Unit> unitIsNotTransportButCouldBeCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getTransportCapacity() == -1) {
        return true;
      }
      return ua.getIsCombatTransport() && ua.getIsSea();
    });
  }

  public static Predicate<Unit> unitIsDestroyer() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsDestroyer());
  }

  public static Predicate<UnitType> unitTypeIsDestroyer() {
    return Match.of(type -> UnitAttachment.get(type).getIsDestroyer());
  }

  /**
   * Returns a match indicating the specified unit can transport other units by sea.
   */
  public static Predicate<Unit> unitIsTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea();
    });
  }

  public static Predicate<Unit> unitIsNotTransport() {
    return unitIsTransport().negate();
  }

  static Predicate<Unit> unitIsTransportAndNotDestroyer() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !unitIsDestroyer().test(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea();
    });
  }

  /**
   * Returns a match indicating the specified unit type is a strategic bomber.
   */
  public static Predicate<UnitType> unitTypeIsStrategicBomber() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getIsStrategicBomber();
    });
  }

  public static Predicate<Unit> unitIsStrategicBomber() {
    return Match.of(obj -> unitTypeIsStrategicBomber().test(obj.getType()));
  }

  static Predicate<Unit> unitIsNotStrategicBomber() {
    return unitIsStrategicBomber().negate();
  }

  static final Predicate<UnitType> unitTypeCanLandOnCarrier() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getCarrierCost() != -1;
    });
  }

  static Predicate<Unit> unitHasMoved() {
    return Match.of(unit -> TripleAUnit.get(unit).getAlreadyMoved() > 0);
  }

  public static Predicate<Unit> unitHasNotMoved() {
    return unitHasMoved().negate();
  }

  static Predicate<Unit> unitCanAttack(final PlayerID id) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getMovement(id) <= 0) {
        return false;
      }
      return ua.getAttack(id) > 0;
    });
  }

  public static Predicate<Unit> unitHasAttackValueOfAtLeast(final int attackValue) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getAttack(unit.getOwner()) >= attackValue);
  }

  public static Predicate<Unit> unitHasDefendValueOfAtLeast(final int defendValue) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getDefense(unit.getOwner()) >= defendValue);
  }

  public static Predicate<Unit> unitIsEnemyOf(final GameData data, final PlayerID player) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWar(unit.getOwner(), player));
  }

  public static Predicate<Unit> unitIsNotSea() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsSea());
  }

  public static Predicate<UnitType> unitTypeIsSea() {
    return Match.of(type -> UnitAttachment.get(type).getIsSea());
  }

  public static Predicate<UnitType> unitTypeIsNotSea() {
    return Match.of(type -> !UnitAttachment.get(type).getIsSea());
  }

  /**
   * Returns a match indicating the specified unit type is for sea or air units.
   */
  public static Predicate<UnitType> unitTypeIsSeaOrAir() {
    return Match.of(type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsSea() || ua.getIsAir();
    });
  }

  public static Predicate<Unit> unitIsAir() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAir());
  }

  public static Predicate<Unit> unitIsNotAir() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsAir());
  }

  public static Predicate<UnitType> unitTypeCanBombard(final PlayerID id) {
    return Match.of(type -> UnitAttachment.get(type).getCanBombard(id));
  }

  static Predicate<Unit> unitCanBeGivenByTerritoryTo(final PlayerID player) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBeGivenByTerritoryTo().contains(player));
  }

  static Predicate<Unit> unitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr,
      final GameData data) {
    return Match.of(unit -> {
      if (!Properties.getCaptureUnitsOnEnteringTerritory(data)) {
        return false;
      }
      final PlayerID unitOwner = unit.getOwner();
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final boolean unitCanBeCapturedByPlayer = ua.getCanBeCapturedOnEnteringBy().contains(player);
      final TerritoryAttachment ta = TerritoryAttachment.get(terr);
      if (ta == null) {
        return false;
      }
      if (ta.getCaptureUnitOnEnteringBy() == null) {
        return false;
      }
      final boolean territoryCanHaveUnitsThatCanBeCapturedByPlayer = ta.getCaptureUnitOnEnteringBy().contains(player);
      final PlayerAttachment pa = PlayerAttachment.get(unitOwner);
      if (pa == null) {
        return false;
      }
      if (pa.getCaptureUnitOnEnteringBy() == null) {
        return false;
      }
      final boolean unitOwnerCanLetUnitsBeCapturedByPlayer = pa.getCaptureUnitOnEnteringBy().contains(player);
      return (unitCanBeCapturedByPlayer && territoryCanHaveUnitsThatCanBeCapturedByPlayer
          && unitOwnerCanLetUnitsBeCapturedByPlayer);
    });
  }

  static Predicate<Unit> unitDestroyedWhenCapturedByOrFrom(final PlayerID playerBy) {
    return unitDestroyedWhenCapturedBy(playerBy).or(unitDestroyedWhenCapturedFrom());
  }

  private static Predicate<Unit> unitDestroyedWhenCapturedBy(final PlayerID playerBy) {
    return Match.of(u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (ua.getDestroyedWhenCapturedBy().isEmpty()) {
        return false;
      }
      for (final Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("BY") && tuple.getSecond().equals(playerBy)) {
          return true;
        }
      }
      return false;
    });
  }

  private static Predicate<Unit> unitDestroyedWhenCapturedFrom() {
    return Match.of(u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (ua.getDestroyedWhenCapturedBy().isEmpty()) {
        return false;
      }
      for (final Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy()) {
        if (tuple.getFirst().equals("FROM") && tuple.getSecond().equals(u.getOwner())) {
          return true;
        }
      }
      return false;
    });
  }

  public static Predicate<Unit> unitIsAirBase() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAirBase());
  }

  public static Predicate<UnitType> unitTypeCanBeDamaged() {
    return Match.of(ut -> UnitAttachment.get(ut).getCanBeDamaged());
  }

  public static Predicate<Unit> unitCanBeDamaged() {
    return Match.of(unit -> unitTypeCanBeDamaged().test(unit.getType()));
  }

  static Predicate<Unit> unitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return true;
      }
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())) {
        final TripleAUnit taUnit = (TripleAUnit) unit;
        return taUnit.getUnitDamage() >= taUnit.getHowMuchDamageCanThisUnitTakeTotal(unit, t);
      } else {
        return false;
      }
    });
  }

  static Predicate<Unit> unitIsLegalBombingTargetBy(final Unit bomberOrRocket) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(bomberOrRocket.getType());
      final Set<UnitType> allowedTargets = ua.getBombingTargets(bomberOrRocket.getData());
      return allowedTargets == null || allowedTargets.contains(unit.getType());
    });
  }

  public static Predicate<Unit> unitHasTakenSomeBombingUnitDamage() {
    return Match.of(unit -> ((TripleAUnit) unit).getUnitDamage() > 0);
  }

  public static Predicate<Unit> unitHasNotTakenAnyBombingUnitDamage() {
    return unitHasTakenSomeBombingUnitDamage().negate();
  }

  /**
   * Returns a match indicating the specified unit is disabled.
   */
  public static Predicate<Unit> unitIsDisabled() {
    return Match.of(unit -> {
      if (!unitCanBeDamaged().test(unit)) {
        return false;
      }
      if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final TripleAUnit taUnit = (TripleAUnit) unit;
      if (ua.getMaxOperationalDamage() < 0) {
        // factories may or may not have max operational damage set, so we must still determine here
        // assume that if maxOperationalDamage < 0, then the max damage must be based on the territory value (if the
        // damage >= production of
        // territory, then we are disabled)
        // TerritoryAttachment ta = TerritoryAttachment.get(t);
        // return taUnit.getUnitDamage() >= ta.getProduction();
        return false;
      }
      // only greater than. if == then we can still operate
      return taUnit.getUnitDamage() > ua.getMaxOperationalDamage();
    });
  }

  public static Predicate<Unit> unitIsNotDisabled() {
    return unitIsDisabled().negate();
  }

  static Predicate<Unit> unitCanDieFromReachingMaxDamage() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return false;
      }
      return ua.getCanDieFromReachingMaxDamage();
    });
  }

  public static Predicate<UnitType> unitTypeIsInfrastructure() {
    return Match.of(ut -> UnitAttachment.get(ut).getIsInfrastructure());
  }

  public static Predicate<Unit> unitIsInfrastructure() {
    return Match.of(unit -> unitTypeIsInfrastructure().test(unit.getType()));
  }

  public static Predicate<Unit> unitIsNotInfrastructure() {
    return unitIsInfrastructure().negate();
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  public static Predicate<Unit> unitIsSupporterOrHasCombatAbility(final boolean attack) {
    return Match.of(unit -> unitTypeIsSupporterOrHasCombatAbility(attack, unit.getOwner()).test(unit.getType()));
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  private static Predicate<UnitType> unitTypeIsSupporterOrHasCombatAbility(final boolean attack,
      final PlayerID player) {
    return Match.of(ut -> {
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
    });
  }

  public static Predicate<UnitSupportAttachment> unitSupportAttachmentCanBeUsedByPlayer(final PlayerID player) {
    return Match.of(usa -> usa.getPlayers().contains(player));
  }

  public static Predicate<Unit> unitCanScramble() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanScramble());
  }

  public static Predicate<Unit> unitWasScrambled() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasScrambled());
  }

  static Predicate<Unit> unitWasInAirBattle() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasInAirBattle());
  }

  public static Predicate<Unit> unitCanBombard(final PlayerID id) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBombard(id));
  }

  public static Predicate<Unit> unitCanBlitz() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBlitz(unit.getOwner()));
  }

  static Predicate<Unit> unitIsLandTransport() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsLandTransport());
  }

  static Predicate<Unit> unitIsNotInfrastructureAndNotCapturedOnEntering(final PlayerID player,
      final Territory terr, final GameData data) {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsInfrastructure()
        && !unitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).test(unit));
  }

  static Predicate<Unit> unitIsSuicide() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSuicide());
  }

  static Predicate<Unit> unitIsSuicideOnHit() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSuicideOnHit());
  }

  static Predicate<Unit> unitIsKamikaze() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsKamikaze());
  }

  public static Predicate<UnitType> unitTypeIsAir() {
    return Match.of(type -> UnitAttachment.get(type).getIsAir());
  }

  private static Predicate<UnitType> unitTypeIsNotAir() {
    return Match.of(type -> !UnitAttachment.get(type).getIsAir());
  }

  public static Predicate<Unit> unitCanLandOnCarrier() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCost() != -1);
  }

  public static Predicate<Unit> unitIsCarrier() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1);
  }

  static Predicate<Territory> territoryHasOwnedCarrier(final PlayerID player) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsOwnedBy(player).and(unitIsCarrier())));
  }

  public static Predicate<Unit> unitIsAlliedCarrier(final PlayerID player, final GameData data) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
        && data.getRelationshipTracker().isAllied(player, unit.getOwner()));
  }

  public static Predicate<Unit> unitCanBeTransported() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCost() != -1);
  }

  static Predicate<Unit> unitWasAmphibious() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasAmphibious());
  }

  static Predicate<Unit> unitWasNotAmphibious() {
    return unitWasAmphibious().negate();
  }

  static Predicate<Unit> unitWasInCombat() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasInCombat());
  }

  static Predicate<Unit> unitWasUnloadedThisTurn() {
    return Match.of(obj -> ((TripleAUnit) obj).getUnloadedTo() != null);
  }

  private static Predicate<Unit> unitWasLoadedThisTurn() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasLoadedThisTurn());
  }

  static Predicate<Unit> unitWasNotLoadedThisTurn() {
    return unitWasLoadedThisTurn().negate();
  }

  public static Predicate<Unit> unitCanTransport() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCapacity() != -1);
  }

  public static Predicate<UnitType> unitTypeCanProduceUnits() {
    return Match.of(obj -> UnitAttachment.get(obj).getCanProduceUnits());
  }

  public static Predicate<Unit> unitCanProduceUnits() {
    return Match.of(obj -> unitTypeCanProduceUnits().test(obj.getType()));
  }

  public static Predicate<UnitType> unitTypeHasMaxBuildRestrictions() {
    return Match.of(type -> UnitAttachment.get(type).getMaxBuiltPerPlayer() >= 0);
  }

  public static Predicate<UnitType> unitTypeIsRocket() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsRocket());
  }

  static Predicate<Unit> unitIsRocket() {
    return Match.of(obj -> unitTypeIsRocket().test(obj.getType()));
  }

  static Predicate<Unit> unitHasMovementLimit() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getMovementLimit() != null);
  }

  static final Predicate<Unit> unitHasAttackingLimit() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getAttackingLimit() != null);
  }

  private static Predicate<UnitType> unitTypeCanNotMoveDuringCombatMove() {
    return Match.of(type -> UnitAttachment.get(type).getCanNotMoveDuringCombatMove());
  }

  public static Predicate<Unit> unitCanNotMoveDuringCombatMove() {
    return Match.of(obj -> unitTypeCanNotMoveDuringCombatMove().test(obj.getType()));
  }

  private static Predicate<Unit> unitIsAaThatCanHitTheseUnits(final Collection<Unit> targets,
      final Predicate<Unit> typeOfAa, final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed) {
    return Match.of(obj -> {
      if (!typeOfAa.test(obj)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      final Set<UnitType> targetsAa = ua.getTargetsAA(obj.getData());
      for (final Unit u : targets) {
        if (targetsAa.contains(u.getType())) {
          return true;
        }
      }
      return targets.stream()
          .anyMatch(unitIsAirborne().and(unitIsOfTypes(airborneTechTargetsAllowed.get(ua.getTypeAA()))));
    });
  }

  static Predicate<Unit> unitIsAaOfTypeAa(final String typeAa) {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getTypeAA().matches(typeAa));
  }

  static Predicate<Unit> unitAaShotDamageableInsteadOfKillingInstantly() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getDamageableAA());
  }

  private static Predicate<Unit> unitIsAaThatWillNotFireIfPresentEnemyUnits(final Collection<Unit> enemyUnitsPresent) {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      for (final Unit u : enemyUnitsPresent) {
        if (ua.getWillNotFireIfPresent().contains(u.getType())) {
          return true;
        }
      }
      return false;
    });
  }

  private static Predicate<UnitType> unitTypeIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return Match.of(obj -> {
      final int maxRoundsAa = UnitAttachment.get(obj).getMaxRoundsAA();
      return maxRoundsAa < 0 || maxRoundsAa >= battleRoundNumber;
    });
  }

  private static Predicate<Unit> unitIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return Match.of(obj -> unitTypeIsAaThatCanFireOnRound(battleRoundNumber).test(obj.getType()));
  }

  static Predicate<Unit> unitIsAaThatCanFire(final Collection<Unit> unitsMovingOrAttacking,
      final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed, final PlayerID playerMovingOrAttacking,
      final Predicate<Unit> typeOfAa, final int battleRoundNumber, final boolean defending, final GameData data) {
    return enemyUnit(playerMovingOrAttacking, data)
        .and(unitIsBeingTransported().negate())
        .and(unitIsAaThatCanHitTheseUnits(unitsMovingOrAttacking, typeOfAa, airborneTechTargetsAllowed))
        .and(unitIsAaThatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).negate())
        .and(unitIsAaThatCanFireOnRound(battleRoundNumber))
        .and(defending
            ? unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()
            : unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero());
  }

  private static Predicate<UnitType> unitTypeIsAaForCombatOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforCombatOnly());
  }

  static Predicate<Unit> unitIsAaForCombatOnly() {
    return Match.of(obj -> unitTypeIsAaForCombatOnly().test(obj.getType()));
  }

  public static Predicate<UnitType> unitTypeIsAaForBombingThisUnitOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforBombingThisUnitOnly());
  }

  public static Predicate<Unit> unitIsAaForBombingThisUnitOnly() {
    return Match.of(obj -> unitTypeIsAaForBombingThisUnitOnly().test(obj.getType()));
  }

  private static Predicate<UnitType> unitTypeIsAaForFlyOverOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforFlyOverOnly());
  }

  static Predicate<Unit> unitIsAaForFlyOverOnly() {
    return Match.of(obj -> unitTypeIsAaForFlyOverOnly().test(obj.getType()));
  }

  /**
   * Returns a match indicating the specified unit type is anti-aircraft for any condition.
   */
  public static Predicate<UnitType> unitTypeIsAaForAnything() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly() || ua.getIsAAforFlyOverOnly();
    });
  }

  public static Predicate<Unit> unitIsAaForAnything() {
    return Match.of(obj -> unitTypeIsAaForAnything().test(obj.getType()));
  }

  public static Predicate<Unit> unitIsNotAa() {
    return unitIsAaForAnything().negate();
  }

  private static Predicate<UnitType> unitTypeMaxAaAttacksIsInfinite() {
    return Match.of(obj -> UnitAttachment.get(obj).getMaxAAattacks() == -1);
  }

  static Predicate<Unit> unitMaxAaAttacksIsInfinite() {
    return Match.of(obj -> unitTypeMaxAaAttacksIsInfinite().test(obj.getType()));
  }

  private static Predicate<UnitType> unitTypeMayOverStackAa() {
    return Match.of(obj -> UnitAttachment.get(obj).getMayOverStackAA());
  }

  static Predicate<Unit> unitMayOverStackAa() {
    return Match.of(obj -> unitTypeMayOverStackAa().test(obj.getType()));
  }

  static Predicate<Unit> unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    });
  }

  static Predicate<Unit> unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getOffensiveAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    });
  }

  public static Predicate<Unit> unitIsInfantry() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getIsInfantry());
  }

  public static Predicate<Unit> unitIsNotInfantry() {
    return unitIsInfantry().negate();
  }

  /**
   * Returns a match indicating the specified unit can be transported by air.
   */
  public static Predicate<Unit> unitIsAirTransportable() {
    return Match.of(obj -> {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (ta == null || !ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransportable();
    });
  }

  static Predicate<Unit> unitIsNotAirTransportable() {
    return unitIsAirTransportable().negate();
  }

  /**
   * Returns a match indicating the specified unit can transport other units by air.
   */
  public static Predicate<Unit> unitIsAirTransport() {
    return Match.of(obj -> {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (ta == null || !ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransport();
    });
  }

  public static Predicate<Unit> unitIsArtillery() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getArtillery());
  }

  public static Predicate<Unit> unitIsArtillerySupportable() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getArtillerySupportable());
  }

  // TODO: CHECK whether this makes any sense
  public static Predicate<Territory> territoryIsLandOrWater() {
    return Match.of(Objects::nonNull);
  }

  public static Predicate<Territory> territoryIsWater() {
    return Match.of(Territory::isWater);
  }

  /**
   * Returns a match indicating the specified territory is an island.
   */
  public static Predicate<Territory> territoryIsIsland() {
    return Match.of(t -> {
      final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
      return neighbors.size() == 1 && territoryIsWater().test(neighbors.iterator().next());
    });
  }

  /**
   * Returns a match indicating the specified territory is a victory city.
   */
  public static Predicate<Territory> territoryIsVictoryCity() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      return ta.getVictoryCity() != 0;
    });
  }

  public static Predicate<Territory> territoryIsLand() {
    return territoryIsWater().negate();
  }

  public static Predicate<Territory> territoryIsEmpty() {
    return Match.of(t -> t.getUnits().size() == 0);
  }

  /**
   * Tests for Land, Convoys Centers and Convoy Routes, and Contested Territories.
   * Assumes player is either the owner of the territory we are testing, or about to become the owner (ie: this doesn't
   * test ownership).
   * If the game option for contested territories not producing is on, then will also remove any contested territories.
   */
  public static Predicate<Territory> territoryCanCollectIncomeFrom(final PlayerID player, final GameData data) {
    final boolean contestedDoNotProduce =
        Properties.getContestedTerritoriesProduceNoIncome(data);
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      final PlayerID origOwner = OriginalOwnerTracker.getOriginalOwner(t);
      if (t.isWater()) {
        // if it's water, it is a Convoy Center
        // Can't get PUs for capturing a CC, only original owner can get them. (Except capturing null player CCs)
        if (!(origOwner == null || origOwner == PlayerID.NULL_PLAYERID || origOwner == player)) {
          return false;
        }
      }
      if (ta.getConvoyRoute() && !ta.getConvoyAttached().isEmpty()) {
        // Determine if at least one part of the convoy route is owned by us or an ally
        boolean atLeastOne = false;
        for (final Territory convoy : ta.getConvoyAttached()) {
          if (data.getRelationshipTracker().isAllied(convoy.getOwner(), player)
              && TerritoryAttachment.get(convoy).getConvoyRoute()) {
            atLeastOne = true;
          }
        }
        if (!atLeastOne) {
          return false;
        }
      }
      return !(contestedDoNotProduce && !territoryHasNoEnemyUnits(player, data).test(t));
    });
  }

  public static Predicate<Territory> territoryHasNeighborMatching(final GameData data,
      final Predicate<Territory> match) {
    return Match.of(t -> data.getMap().getNeighbors(t, match).size() > 0);
  }

  public static Predicate<Territory> territoryHasAlliedNeighborWithAlliedUnitMatching(final GameData data,
      final PlayerID player, final Predicate<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsAlliedAndHasAlliedUnitMatching(data, player, unitMatch)).size() > 0);
  }

  public static Predicate<Territory> territoryIsInList(final Collection<Territory> list) {
    return Match.of(list::contains);
  }

  public static Predicate<Territory> territoryIsNotInList(final Collection<Territory> list) {
    return Match.of(not(list::contains));
  }

  /**
   * @param data
   *        game data
   * @return Match&lt;Territory> that tests if there is a route to an enemy capital from the given territory.
   */
  public static Predicate<Territory> territoryHasRouteToEnemyCapital(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      for (final PlayerID otherPlayer : data.getPlayerList().getPlayers()) {
        final List<Territory> capitalsListOwned =
            new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
        for (final Territory current : capitalsListOwned) {
          if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
            continue;
          }
          if (data.getMap().getDistance(t, current, territoryIsPassableAndNotRestricted(player, data)) != -1) {
            return true;
          }
        }
      }
      return false;
    });
  }

  /**
   * @param data
   *        game data.
   * @return true only if the route is land
   */
  public static Predicate<Territory> territoryHasLandRouteToEnemyCapital(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      for (final PlayerID otherPlayer : data.getPlayerList().getPlayers()) {
        final List<Territory> capitalsListOwned =
            new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
        for (final Territory current : capitalsListOwned) {
          if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
            continue;
          }
          if (data.getMap().getDistance(t, current, territoryIsNotImpassableToLandUnits(player, data)) != -1) {
            return true;
          }
        }
      }
      return false;
    });
  }

  public static Predicate<Territory> territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(final GameData data,
      final PlayerID player, final Predicate<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, unitMatch)).size() > 0);
  }

  public static Predicate<Territory> territoryHasOwnedNeighborWithOwnedUnitMatching(final GameData data,
      final PlayerID player, final Predicate<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsOwnedAndHasOwnedUnitMatching(player, unitMatch)).size() > 0);
  }

  static Predicate<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(
      final GameData data, final PlayerID player) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(data, player)).size() > 0);
  }

  public static Predicate<Territory> territoryHasWaterNeighbor(final GameData data) {
    return Match.of(t -> data.getMap().getNeighbors(t, territoryIsWater()).size() > 0);
  }

  private static Predicate<Territory> territoryIsAlliedAndHasAlliedUnitMatching(final GameData data,
      final PlayerID player,
      final Predicate<Unit> unitMatch) {
    return Match.of(t -> {
      if (!data.getRelationshipTracker().isAllied(t.getOwner(), player)) {
        return false;
      }
      return t.getUnits().anyMatch(alliedUnit(player, data).and(unitMatch));
    });
  }

  public static Predicate<Territory> territoryIsOwnedAndHasOwnedUnitMatching(final PlayerID player,
      final Predicate<Unit> unitMatch) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().anyMatch(unitIsOwnedBy(player).and(unitMatch));
    });
  }

  public static Predicate<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().anyMatch(unitCanProduceUnits());
    });
  }

  private static Predicate<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(final GameData data,
      final PlayerID player) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      if (!t.getUnits().anyMatch(unitCanProduceUnits())) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
      return !(bt == null || bt.wasConquered(t));
    });
  }

  static Predicate<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      if (!isTerritoryAllied(player, data).test(t)) {
        return false;
      }
      return t.getUnits().anyMatch(unitCanProduceUnits());
    });
  }

  public static Predicate<Territory> territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(final GameData data,
      final PlayerID player, final Predicate<Unit> unitMatch) {
    return Match.of(t -> {
      if (!data.getRelationshipTracker().isAtWar(player, t.getOwner())) {
        return false;
      }
      if (t.getOwner().isNull()) {
        return false;
      }
      return t.getUnits().anyMatch(enemyUnit(player, data).and(unitMatch));
    });
  }

  static Predicate<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player) {
    return t -> t.getUnits().allMatch(unitIsInfrastructure().or(enemyUnit(player, data).negate()));
  }

  /**
   * Returns a match indicating the specified territory is neutral and not water.
   */
  public static Predicate<Territory> territoryIsNeutralButNotWater() {
    return Match.of(t -> {
      if (t.isWater()) {
        return false;
      }
      return t.getOwner().equals(PlayerID.NULL_PLAYERID);
    });
  }

  /**
   * Returns a match indicating the specified territory is impassable.
   */
  public static Predicate<Territory> territoryIsImpassable() {
    return Match.of(t -> {
      if (t.isWater()) {
        return false;
      }
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getIsImpassable();
    });
  }

  public static Predicate<Territory> territoryIsNotImpassable() {
    return territoryIsImpassable().negate();
  }

  static Predicate<Territory> seaCanMoveOver(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (!territoryIsWater().test(t)) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player, data).test(t);
    });
  }

  static Predicate<Territory> airCanFlyOver(final PlayerID player, final GameData data,
      final boolean areNeutralsPassableByAir) {
    return Match.of(t -> {
      if (!areNeutralsPassableByAir && territoryIsNeutralButNotWater().test(t)) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(player, data).test(t)) {
        return false;
      }
      return !(territoryIsLand().test(t)
          && !data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
    });
  }

  public static Predicate<Territory> territoryIsPassableAndNotRestricted(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (territoryIsImpassable().test(t)) {
        return false;
      }
      if (!Properties.getMovementByTerritoryRestricted(data)) {
        return true;
      }
      final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      if (ra == null || ra.getMovementRestrictionTerritories() == null) {
        return true;
      }
      final String movementRestrictionType = ra.getMovementRestrictionType();
      final Collection<Territory> listedTerritories =
          ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
      return (movementRestrictionType.equals("allowed") == listedTerritories.contains(t));
    });
  }

  private static Predicate<Territory> territoryIsImpassableToLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.isWater()) {
        return true;
      } else if (territoryIsPassableAndNotRestricted(player, data).negate().test(t)) {
        return true;
      }
      return false;
    });
  }

  public static Predicate<Territory> territoryIsNotImpassableToLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> territoryIsImpassableToLandUnits(player, data).negate().test(t));
  }

  /**
   * Does NOT check for: Canals, Blitzing, Loading units on transports, TerritoryEffects that disallow units, Stacking
   * Limits, Unit movement left, Fuel available, etc.
   * <br>
   * <br>
   * Does check for: Impassable, ImpassableNeutrals, ImpassableToAirNeutrals, RestrictedTerritories, requiresUnitToMove,
   * Land units moving on water, Sea units moving on land, and territories that are disallowed due to a relationship
   * attachment (canMoveLandUnitsOverOwnedLand, canMoveAirUnitsOverOwnedLand, canLandAirUnitsOnOwnedLand,
   * canMoveIntoDuringCombatMove, etc).
   */
  public static Predicate<Territory> territoryIsPassableAndNotRestrictedAndOkByRelationships(
      final PlayerID playerWhoOwnsAllTheUnitsMoving, final GameData data, final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded, final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported, final boolean isLandingZoneOnLandForAirUnits) {
    final boolean neutralsPassable = !Properties.getNeutralsImpassable(data);
    final boolean areNeutralsPassableByAir =
        neutralsPassable && Properties.getNeutralFlyoverAllowed(data);
    return Match.of(t -> {
      if (territoryIsImpassable().test(t)) {
        return false;
      }
      if ((!neutralsPassable || (hasAirUnitsNotBeingTransported && !areNeutralsPassableByAir))
          && territoryIsNeutralButNotWater().test(t)) {
        return false;
      }
      if (Properties.getMovementByTerritoryRestricted(data)) {
        final RulesAttachment ra =
            (RulesAttachment) playerWhoOwnsAllTheUnitsMoving.getAttachment(Constants.RULES_ATTACHMENT_NAME);
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
        if (hasLandUnitsNotBeingTransportedOrBeingLoaded && !data.getRelationshipTracker()
            .canMoveLandUnitsOverOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
        if (hasAirUnitsNotBeingTransported && !data.getRelationshipTracker()
            .canMoveAirUnitsOverOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
          return false;
        }
      }
      if (isLandingZoneOnLandForAirUnits && !data.getRelationshipTracker()
          .canLandAirUnitsOnOwnedLand(playerWhoOwnsAllTheUnitsMoving, t.getOwner())) {
        return false;
      }
      return !(isCombatMovePhase && !data.getRelationshipTracker()
          .canMoveIntoDuringCombatMove(playerWhoOwnsAllTheUnitsMoving, t.getOwner()));
    });
  }

  static Predicate<IBattle> battleIsEmpty() {
    return Match.of(IBattle::isEmpty);
  }

  static Predicate<IBattle> battleIsAmphibious() {
    return Match.of(IBattle::isAmphibious);
  }

  public static Predicate<Unit> unitHasEnoughMovementForRoutes(final List<Route> route) {
    return unitHasEnoughMovementForRoute(Route.create(route));
  }

  public static Predicate<Unit> unitHasEnoughMovementForRoute(final List<Territory> territories) {
    return unitHasEnoughMovementForRoute(new Route(territories));
  }

  public static Predicate<Unit> unitHasEnoughMovementForRoute(final Route route) {
    return Match.of(unit -> {
      int left = TripleAUnit.get(unit).getMovementLeft();
      int movementcost = route.getMovementCost(unit);
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      final PlayerID player = unit.getOwner();
      if (ua.getIsAir()) {
        TerritoryAttachment taStart = null;
        TerritoryAttachment taEnd = null;
        if (route.getStart() != null) {
          taStart = TerritoryAttachment.get(route.getStart());
        }
        if (route.getEnd() != null) {
          taEnd = TerritoryAttachment.get(route.getEnd());
        }
        movementcost = route.getMovementCost(unit);
        if (taStart != null && taStart.getAirBase()) {
          left++;
        }
        if (taEnd != null && taEnd.getAirBase()) {
          left++;
        }
      }
      final GameStep stepName = unit.getData().getSequence().getStep();
      if (ua.getIsSea() && stepName.getDisplayName().equals("Non Combat Move")) {
        movementcost = route.getMovementCost(unit);
        // If a zone adjacent to the starting and ending sea zones
        // are allied navalbases, increase the range.
        // TODO Still need to be able to handle stops on the way
        // (history to get route.getStart()
        for (final Territory terrNext : unit.getData().getMap().getNeighbors(route.getStart(), 1)) {
          final TerritoryAttachment taNeighbor = TerritoryAttachment.get(terrNext);
          if (taNeighbor != null && taNeighbor.getNavalBase()
              && unit.getData().getRelationshipTracker().isAllied(terrNext.getOwner(), player)) {
            for (final Territory terrEnd : unit.getData().getMap().getNeighbors(route.getEnd(), 1)) {
              final TerritoryAttachment taEndNeighbor = TerritoryAttachment.get(terrEnd);
              if (taEndNeighbor != null && taEndNeighbor.getNavalBase()
                  && unit.getData().getRelationshipTracker().isAllied(terrEnd.getOwner(), player)) {
                left++;
                break;
              }
            }
          }
        }
      }
      return !(left < 0 || left < movementcost);
    });
  }

  /**
   * Match units that have at least 1 movement left.
   */
  public static Predicate<Unit> unitHasMovementLeft() {
    return Match.of(o -> TripleAUnit.get(o).getMovementLeft() >= 1);
  }

  public static Predicate<Unit> unitCanMove() {
    return Match.of(u -> unitTypeCanMove(u.getOwner()).test(u.getType()));
  }

  private static Predicate<UnitType> unitTypeCanMove(final PlayerID player) {
    return Match.of(obj -> UnitAttachment.get(obj).getMovement(player) > 0);
  }

  public static Predicate<UnitType> unitTypeIsStatic(final PlayerID id) {
    return Match.of(unitType -> !unitTypeCanMove(id).test(unitType));
  }

  public static Predicate<Unit> unitIsLandAndOwnedBy(final PlayerID player) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !ua.getIsSea() && !ua.getIsAir() && unit.getOwner().equals(player);
    });
  }

  public static Predicate<Unit> unitIsOwnedBy(final PlayerID player) {
    return Match.of(unit -> unit.getOwner().equals(player));
  }

  public static Predicate<Unit> unitIsOwnedByOfAnyOfThesePlayers(final Collection<PlayerID> players) {
    return Match.of(unit -> players.contains(unit.getOwner()));
  }

  public static Predicate<Unit> unitIsTransporting() {
    return Match.of(unit -> {
      final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
      return !(transporting == null || transporting.isEmpty());
    });
  }

  public static Predicate<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units) {
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units);
    return Match.of(unit -> {
      final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
      if (transporting == null) {
        return false;
      }
      return Util.someIntersect(UnitSeperator.categorize(transporting), unitCategories);
    });
  }

  public static Predicate<Territory> isTerritoryAllied(final PlayerID player, final GameData data) {
    return Match.of(t -> data.getRelationshipTracker().isAllied(player, t.getOwner()));
  }

  public static Predicate<Territory> isTerritoryOwnedBy(final PlayerID player) {
    return Match.of(t -> t.getOwner().equals(player));
  }

  public static Predicate<Territory> isTerritoryOwnedBy(final Collection<PlayerID> players) {
    return Match.of(t -> {
      for (final PlayerID player : players) {
        if (t.getOwner().equals(player)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Predicate<Unit> isUnitAllied(final PlayerID player, final GameData data) {
    return Match.of(t -> data.getRelationshipTracker().isAllied(player, t.getOwner()));
  }

  public static Predicate<Territory> isTerritoryFriendly(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.isWater()) {
        return true;
      }
      if (t.getOwner().equals(player)) {
        return true;
      }
      return data.getRelationshipTracker().isAllied(player, t.getOwner());
    });
  }

  private static Predicate<Unit> unitIsEnemyAaForAnything(final PlayerID player, final GameData data) {
    return unitIsAaForAnything().and(enemyUnit(player, data));
  }

  private static Predicate<Unit> unitIsEnemyAaForCombat(final PlayerID player, final GameData data) {
    return unitIsAaForCombatOnly().and(enemyUnit(player, data));
  }

  static Predicate<Unit> unitIsInTerritory(final Territory territory) {
    return Match.of(o -> territory.getUnits().getUnits().contains(o));
  }

  public static Predicate<Territory> isTerritoryEnemy(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.getOwner().equals(player)) {
        return false;
      }
      return data.getRelationshipTracker().isAtWar(player, t.getOwner());
    });
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWater(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.getOwner().equals(player)) {
        return false;
      }
      // if we look at territory attachments, may have funny results for blockades or other things that are passable
      // and not owned. better
      // to check them by alliance. (veqryn)
      // OLD code included: if(t.isWater() && t.getOwner().isNull() && TerritoryAttachment.get(t) == null){return
      // false;}
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      return data.getRelationshipTracker().isAtWar(player, t.getOwner());
    });
  }

  public static Predicate<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(final PlayerID player,
      final GameData data) {
    return Match.of(t -> {
      if (t.getOwner().equals(player)) {
        return false;
      }
      // if we look at territory attachments, may have funny results for blockades or other things that are passable
      // and not owned. better
      // to check them by alliance. (veqryn)
      // OLD code included: if(t.isWater() && t.getOwner().isNull() && TerritoryAttachment.get(t) == null){return
      // false;}
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(player, data).test(t)) {
        return false;
      }
      return data.getRelationshipTracker().isAtWar(player, t.getOwner());
    });
  }

  public static Predicate<Territory> territoryIsBlitzable(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      // cant blitz water
      if (t.isWater()) {
        return false;
      }
      // cant blitz on neutrals
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID)
          && !Properties.getNeutralsBlitzable(data)) {
        return false;
      }
      // was conquered but not blitzed
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
          && !AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(t)) {
        return false;
      }
      // we ignore neutral units
      final Predicate<Unit> blitzableUnits = PredicateBuilder
          .of(enemyUnit(player, data).negate())
          // WW2V2, cant blitz through factories and aa guns
          // WW2V1, you can
          .orIf(!Properties.getWW2V2(data) && !Properties.getBlitzThroughFactoriesAndAARestricted(data),
              unitIsInfrastructure())
          .build();
      return t.getUnits().allMatch(blitzableUnits);
    });
  }

  public static Predicate<Territory> isTerritoryFreeNeutral(final GameData data) {
    return Match.of(t -> t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) <= 0);
  }

  public static Predicate<Territory> territoryDoesNotCostMoneyToEnter(final GameData data) {
    return Match.of(t -> territoryIsLand().negate().test(t) || !t.getOwner().equals(PlayerID.NULL_PLAYERID)
        || Properties.getNeutralCharge(data) <= 0);
  }

  public static Predicate<Unit> enemyUnit(final PlayerID player, final GameData data) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWar(player, unit.getOwner()));
  }

  public static Predicate<Unit> enemyUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(unit.getOwner(), players));
  }

  public static Predicate<Unit> unitOwnedBy(final PlayerID player) {
    return Match.of(unit -> unit.getOwner().equals(player));
  }

  public static Predicate<Unit> unitOwnedBy(final List<PlayerID> players) {
    return Match.of(o -> {
      for (final PlayerID p : players) {
        if (o.getOwner().equals(p)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Predicate<Unit> alliedUnit(final PlayerID player, final GameData data) {
    return Match.of(unit -> {
      if (unit.getOwner().equals(player)) {
        return true;
      }
      return data.getRelationshipTracker().isAllied(player, unit.getOwner());
    });
  }

  public static Predicate<Unit> alliedUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return Match.of(unit -> {
      if (unitIsOwnedByOfAnyOfThesePlayers(players).test(unit)) {
        return true;
      }
      return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(unit.getOwner(), players);
    });
  }

  public static Predicate<Territory> territoryIs(final Territory test) {
    return Match.of(t -> t.equals(test));
  }

  public static Predicate<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsOwnedBy(player).and(unitIsLand())));
  }

  public static Predicate<Territory> territoryHasUnitsOwnedBy(final PlayerID player) {
    final Predicate<Unit> unitOwnedBy = unitIsOwnedBy(player);
    return Match.of(t -> t.getUnits().anyMatch(unitOwnedBy));
  }

  public static Predicate<Territory> territoryHasUnitsThatMatch(final Predicate<Unit> cond) {
    return Match.of(t -> t.getUnits().anyMatch(cond));
  }

  public static Predicate<Territory> territoryHasEnemyAaForAnything(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsEnemyAaForAnything(player, data)));
  }

  public static Predicate<Territory> territoryHasEnemyAaForCombatOnly(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsEnemyAaForCombat(player, data)));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> !t.getUnits().anyMatch(enemyUnit(player, data)));
  }

  public static Predicate<Territory> territoryHasAlliedUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(alliedUnit(player, data)));
  }

  static Predicate<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data) {
    final Predicate<Unit> match = enemyUnit(player, data).and(unitIsSubmerged().negate());
    return Match.of(t -> t.getUnits().anyMatch(match));
  }

  public static Predicate<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(enemyUnit(player, data).and(unitIsLand())));
  }

  public static Predicate<Territory> territoryHasEnemySeaUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(enemyUnit(player, data).and(unitIsSea())));
  }

  public static Predicate<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(enemyUnit(player, data)));
  }

  static Predicate<Territory> territoryIsNotUnownedWater() {
    return Match.of(t -> !(t.isWater() && TerritoryAttachment.get(t) == null && t.getOwner().isNull()));
  }

  /**
   * The territory is owned by the enemy of those enemy units (i.e. probably owned by you or your ally, but not
   * necessarily so in an FFA type game).
   */
  static Predicate<Territory> territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(
      final PlayerID player, final GameData gameData) {
    return Match.of(t -> {
      final List<Unit> enemyUnits = t.getUnits().getMatches(enemyUnit(player, gameData)
          .and(unitIsNotAir())
          .and(unitIsNotInfrastructure()));
      final Collection<PlayerID> enemyPlayers = enemyUnits.stream()
          .map(Unit::getOwner)
          .collect(Collectors.toSet());
      return isAtWarWithAnyOfThesePlayers(enemyPlayers, gameData).test(t.getOwner());
    });
  }

  public static Predicate<Unit> transportCannotUnload(final Territory territory) {
    return Match.of(transport -> {
      if (TransportTracker.hasTransportUnloadedInPreviousPhase(transport)) {
        return true;
      }
      if (TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, territory)) {
        return true;
      }
      return TransportTracker.isTransportUnloadRestrictedInNonCombat(transport);
    });
  }

  public static Predicate<Unit> transportIsNotTransporting() {
    return Match.of(transport -> !TransportTracker.isTransporting(transport));
  }

  static Predicate<Unit> transportIsTransporting() {
    return Match.of(transport -> TransportTracker.isTransporting(transport));
  }

  /**
   * @return Match that tests the TripleAUnit getTransportedBy value
   *         which is normally set for sea transport movement of land units,
   *         and sometimes set for other things like para-troopers and dependent allied fighters sitting as cargo on a
   *         ship. (not sure if
   *         set for mech inf or not)
   */
  public static Predicate<Unit> unitIsBeingTransported() {
    return Match.of(dependent -> ((TripleAUnit) dependent).getTransportedBy() != null);
  }

  /**
   * @param units
   *        referring unit.
   * @param route
   *        referring route
   * @param currentPlayer
   *        current player
   * @param data
   *        game data
   * @param forceLoadParatroopersIfPossible
   *        should we load paratroopers? (if not, we assume they are already loaded)
   * @return Match that tests the TripleAUnit getTransportedBy value
   *         (also tests for para-troopers, and for dependent allied fighters sitting as cargo on a ship)
   */
  public static Predicate<Unit> unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(final Collection<Unit> units,
      final Route route, final PlayerID currentPlayer, final GameData data,
      final boolean forceLoadParatroopersIfPossible) {
    return Match.of(dependent -> {
      // transported on a sea transport
      final Unit transportedBy = ((TripleAUnit) dependent).getTransportedBy();
      if (transportedBy != null && units.contains(transportedBy)) {
        return true;
      }
      // cargo on a carrier
      final Map<Unit, Collection<Unit>> carrierMustMoveWith =
          MoveValidator.carrierMustMoveWith(units, units, data, currentPlayer);
      if (carrierMustMoveWith != null) {
        for (final Unit unit : carrierMustMoveWith.keySet()) {
          if (carrierMustMoveWith.get(unit).contains(dependent)) {
            return true;
          }
        }
      }
      // paratrooper on an air transport
      if (forceLoadParatroopersIfPossible) {
        final Collection<Unit> airTransports = getMatches(units, unitIsAirTransport());
        final Collection<Unit> paratroops = getMatches(units, unitIsAirTransportable());
        if (!airTransports.isEmpty() && !paratroops.isEmpty()) {
          if (TransportUtils.mapTransportsToLoad(paratroops, airTransports)
              .containsKey(dependent)) {
            return true;
          }
        }
      }
      return false;
    });
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
    return Match.of(unit -> unit.getType().equals(type));
  }

  public static Predicate<Unit> unitIsOfTypes(final Set<UnitType> types) {
    return Match.of(unit -> {
      if (types == null || types.isEmpty()) {
        return false;
      }
      return types.contains(unit.getType());
    });
  }

  static Predicate<Territory> territoryWasFoughOver(final BattleTracker tracker) {
    return Match.of(t -> tracker.wasBattleFought(t) || tracker.wasBlitzed(t));
  }

  static Predicate<Unit> unitIsSubmerged() {
    return Match.of(u -> TripleAUnit.get(u).getSubmerged());
  }

  public static Predicate<UnitType> unitTypeIsSub() {
    return Match.of(type -> UnitAttachment.get(type).getIsSub());
  }

  static Predicate<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return Match.of(u -> TechTracker.hasImprovedArtillerySupport(u.getOwner()));
  }

  public static Predicate<Territory> territoryHasNonAllowedCanal(final PlayerID player,
      final Collection<Unit> unitsMoving,
      final GameData data) {
    return Match.of(t -> MoveValidator.validateCanal(t, null, unitsMoving, player, data).isPresent());
  }

  public static Predicate<Territory> territoryIsBlockedSea(final PlayerID player, final GameData data) {
    final Predicate<Unit> transport = unitIsTransportButNotCombatTransport().negate().and(unitIsLand().negate());
    final Predicate<Unit> unitCond = Match.of(PredicateBuilder
        .of(unitIsInfrastructure().negate())
        .and(alliedUnit(player, data).negate())
        .andIf(Properties.getIgnoreTransportInMovement(data), transport)
        .andIf(Properties.getIgnoreSubInMovement(data), unitIsSub().negate())
        .build());
    return territoryHasUnitsThatMatch(unitCond).negate().and(territoryIsWater());
  }

  static Predicate<Unit> unitCanRepairOthers() {
    return Match.of(unit -> {
      if (unitIsDisabled().test(unit) || unitIsBeingTransported().test(unit)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getRepairsUnits() == null) {
        return false;
      }
      return !ua.getRepairsUnits().isEmpty();
    });
  }

  static Predicate<Unit> unitCanRepairThisUnit(final Unit damagedUnit, final Territory territoryOfRepairUnit) {
    return Match.of(unitCanRepair -> {
      final Set<PlayerID> players =
          GameStepPropertiesHelper.getCombinedTurns(damagedUnit.getData(), damagedUnit.getOwner());
      if (players.size() > 1) {

        // If combined turns then can repair as long as at least 1 capital is owned except at territories that a
        // combined capital isn't owned
        boolean atLeastOnePlayerOwnsCapital = false;
        for (final PlayerID player : players) {
          final boolean ownCapital =
              TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(player, damagedUnit.getData());
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
        if (!TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(damagedUnit.getOwner(), damagedUnit.getData())) {
          return false;
        }
      }
      final UnitAttachment ua = UnitAttachment.get(unitCanRepair.getType());
      return ua.getRepairsUnits() != null && ua.getRepairsUnits().keySet().contains(damagedUnit.getType());
    });
  }

  /**
   * @param territory
   *        referring territory
   * @param player
   *        referring player
   * @param data
   *        game data
   * @return Match that will return true if the territory contains a unit that can repair this unit
   *         (It will also return true if this unit is Sea and an adjacent land territory has a land unit that can
   *         repair this unit.)
   */
  public static Predicate<Unit> unitCanBeRepairedByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return Match.of(damagedUnit -> {
      final Predicate<Unit> damaged = unitHasMoreThanOneHitPointTotal().and(unitHasTakenSomeDamage());
      if (!damaged.test(damagedUnit)) {
        return false;
      }
      final Predicate<Unit> repairUnit = alliedUnit(player, data)
          .and(unitCanRepairOthers())
          .and(unitCanRepairThisUnit(damagedUnit, territory));
      if (territory.getUnits().anyMatch(repairUnit)) {
        return true;
      }
      if (unitIsSea().test(damagedUnit)) {
        final List<Territory> neighbors =
            new ArrayList<>(data.getMap().getNeighbors(territory, territoryIsLand()));
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitLand = alliedUnit(player, data)
              .and(unitCanRepairOthers())
              .and(unitCanRepairThisUnit(damagedUnit, current))
              .and(unitIsLand());
          if (current.getUnits().anyMatch(repairUnitLand)) {
            return true;
          }
        }
      } else if (unitIsLand().test(damagedUnit)) {
        final List<Territory> neighbors = new ArrayList<>(data.getMap().getNeighbors(territory, territoryIsWater()));
        for (final Territory current : neighbors) {
          final Predicate<Unit> repairUnitSea = alliedUnit(player, data)
              .and(unitCanRepairOthers())
              .and(unitCanRepairThisUnit(damagedUnit, current))
              .and(unitIsSea());
          if (current.getUnits().anyMatch(repairUnitSea)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  private static Predicate<Unit> unitCanGiveBonusMovement() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getGivesMovement().size() > 0 && unitIsBeingTransported().negate().test(unit);
    });
  }

  static Predicate<Unit> unitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return Match.of(unitWhichCanGiveBonusMovement -> {
      if (unitIsDisabled().test(unitWhichCanGiveBonusMovement)) {
        return false;
      }
      final UnitType type = unitWhichCanGiveBonusMovement.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      // TODO: make sure the unit is operational
      return unitCanGiveBonusMovement().test(unitWhichCanGiveBonusMovement)
          && ua.getGivesMovement().getInt(unitWhichWillGetBonus.getType()) != 0;
    });
  }

  /**
   * @param territory
   *        referring territory
   * @param player
   *        referring player
   * @param data
   *        game data
   * @return Match that will return true if the territory contains a unit that can give bonus movement to this unit
   *         (It will also return true if this unit is Sea and an adjacent land territory has a land unit that can give
   *         bonus movement to
   *         this unit.)
   */
  public static Predicate<Unit> unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return Match.of(unitWhichWillGetBonus -> {
      final Predicate<Unit> givesBonusUnit = alliedUnit(player, data)
          .and(unitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
      if (territory.getUnits().anyMatch(givesBonusUnit)) {
        return true;
      }
      if (unitIsSea().test(unitWhichWillGetBonus)) {
        final Predicate<Unit> givesBonusUnitLand = givesBonusUnit.and(unitIsLand());
        final List<Territory> neighbors = new ArrayList<>(data.getMap().getNeighbors(territory, territoryIsLand()));
        for (final Territory current : neighbors) {
          if (current.getUnits().anyMatch(givesBonusUnitLand)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  static Predicate<Unit> unitCreatesUnits() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0;
    });
  }

  static Predicate<Unit> unitCreatesResources() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0;
    });
  }

  /**
   * Returns a match indicating the specified unit type consumes at least one type of unit upon creation.
   */
  public static Predicate<UnitType> unitTypeConsumesUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit);
      return ua != null && ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0;
    });
  }

  static Predicate<Unit> unitConsumesUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0;
    });
  }

  static Predicate<Unit> unitWhichConsumesUnitsHasRequiredUnits(final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!unitConsumesUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      boolean canBuild = true;
      for (final UnitType ut : requiredUnits) {
        final Predicate<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = unitIsOwnedBy(unitWhichRequiresUnits.getOwner())
            .and(unitIsOfType(ut))
            .and(unitHasNotTakenAnyBombingUnitDamage())
            .and(unitHasNotTakenAnyDamage())
            .and(unitIsNotDisabled());
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final int numberInTerritory = countMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndOfTypeAndNotDamaged);
        if (numberInTerritory < requiredNumber) {
          canBuild = false;
        }
        if (!canBuild) {
          break;
        }
      }
      return canBuild;
    });
  }

  /**
   * Returns a match indicating the specified unit requires at least one type of unit upon creation.
   */
  public static Predicate<Unit> unitRequiresUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getRequiresUnits() != null && ua.getRequiresUnits().size() > 0;
    });
  }

  /**
   * Checks if requiresUnits criteria allows placement in territory based on units there at the start of turn.
   */
  public static Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnitsInList(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!unitRequiresUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final Predicate<Unit> unitIsOwnedByAndNotDisabled = unitIsOwnedBy(unitWhichRequiresUnits.getOwner())
          .and(unitIsNotDisabled());
      unitsInTerritoryAtStartOfTurn.retainAll(getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled));
      boolean canBuild = false;
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final List<String[]> unitComboPossibilities = ua.getRequiresUnits();
      for (final String[] combo : unitComboPossibilities) {
        if (combo != null) {
          boolean haveAll = true;
          final Collection<UnitType> requiredUnits = ua.getListedUnits(combo);
          for (final UnitType ut : requiredUnits) {
            if (countMatches(unitsInTerritoryAtStartOfTurn, unitIsOfType(ut)) < 1) {
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
    });
  }

  /**
   * Check if unit meets requiredUnitsToMove criteria and can move into territory.
   */
  public static Predicate<Unit> unitHasRequiredUnitsToMove(final Territory t, final GameData data) {
    return Match.of(unit -> {

      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null || ua.getRequiresUnitsToMove() == null || ua.getRequiresUnitsToMove().isEmpty()) {
        return true;
      }

      final Predicate<Unit> unitIsOwnedByAndNotDisabled = isUnitAllied(unit.getOwner(), data).and(unitIsNotDisabled());
      final List<Unit> units = getMatches(t.getUnits().getUnits(), unitIsOwnedByAndNotDisabled);
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
    });
  }

  static Predicate<Territory> territoryIsBlockadeZone() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getBlockadeZone();
    });
  }

  /**
   * Returns a match indicating the specified unit type is a construction unit type.
   */
  public static Predicate<UnitType> unitTypeIsConstruction() {
    return Match.of(type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua != null && ua.getIsConstruction();
    });
  }

  public static Predicate<Unit> unitIsConstruction() {
    return Match.of(obj -> unitTypeIsConstruction().test(obj.getType()));
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
   * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from any other unit.
   * Otherwise, units must have a specific unit in this list to be able to invade from that unit.
   */
  public static Predicate<Unit> unitCanInvade() {
    return Match.of(unit -> {
      // is the unit being transported?
      final Unit transport = TripleAUnit.get(unit).getTransportedBy();
      if (transport == null) {
        // Unit isn't transported so can Invade
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.canInvadeFrom(transport);
    });
  }

  public static Predicate<RelationshipType> relationshipTypeIsAllied() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isAllied());
  }

  public static Predicate<RelationshipType> relationshipTypeIsNeutral() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isNeutral());
  }

  public static Predicate<RelationshipType> relationshipTypeIsAtWar() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isWar());
  }

  public static Predicate<Relationship> relationshipIsAtWar() {
    return Match.of(relationship -> relationship.getRelationshipType().getRelationshipTypeAttachment().isWar());
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveLandUnitsOverOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveLandUnitsOverOwnedLand());
  }

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Predicate<Territory> territoryAllowsCanMoveLandUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final PlayerID territoryOwner = t.getOwner();
      if (territoryOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    });
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveAirUnitsOverOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveAirUnitsOverOwnedLand());
  }

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Predicate<Territory> territoryAllowsCanMoveAirUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final PlayerID territoryOwner = t.getOwner();
      if (territoryOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    });
  }

  public static Predicate<RelationshipType> relationshipTypeCanLandAirUnitsOnOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canLandAirUnitsOnOwnedLand());
  }

  public static Predicate<RelationshipType> relationshipTypeCanTakeOverOwnedTerritory() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canTakeOverOwnedTerritory());
  }

  public static Predicate<RelationshipType> relationshipTypeGivesBackOriginalTerritories() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().givesBackOriginalTerritories());
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveIntoDuringCombatMove() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveIntoDuringCombatMove());
  }

  public static Predicate<RelationshipType> relationshipTypeCanMoveThroughCanals() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveThroughCanals());
  }

  public static Predicate<RelationshipType> relationshipTypeRocketsCanFlyOver() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canRocketsFlyOver());
  }

  public static Predicate<String> isValidRelationshipName(final GameData data) {
    return Match.of(relationshipName -> data.getRelationshipTypeList().getRelationshipType(relationshipName) != null);
  }

  public static Predicate<PlayerID> isAtWar(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAtWar()
        .test(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Predicate<PlayerID> isAtWarWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(player2, players));
  }

  public static Predicate<PlayerID> isAllied(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAllied()
        .test(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Predicate<PlayerID> isAlliedWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(player2, players));
  }

  public static Predicate<Unit> unitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(unit -> unitCanProduceUnits().test(unit) && unitIsOwnedBy(player).test(unit));
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getReceivesAbilityWhenWith().isEmpty());
  }

  public static Predicate<Unit> unitCanReceiveAbilityWhenWith(final String filterForAbility,
      final String filterForUnitType) {
    return Match.of(u -> {
      for (final String receives : UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith()) {
        final String[] s = receives.split(":");
        if (s[0].equals(filterForAbility) && s[1].equals(filterForUnitType)) {
          return true;
        }
      }
      return false;
    });
  }

  private static Predicate<Unit> unitHasWhenCombatDamagedEffect() {
    return Match.of(u -> !UnitAttachment.get(u.getType()).getWhenCombatDamaged().isEmpty());
  }

  static Predicate<Unit> unitHasWhenCombatDamagedEffect(final String filterForEffect) {
    return Match.of(u -> {
      if (!unitHasWhenCombatDamagedEffect().test(u)) {
        return false;
      }
      final TripleAUnit taUnit = (TripleAUnit) u;
      final int currentDamage = taUnit.getHits();
      final List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamagedList =
          UnitAttachment.get(u.getType()).getWhenCombatDamaged();
      for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : whenCombatDamagedList) {
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

  static Predicate<Territory> territoryHasWhenCapturedByGoesTo() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      return !ta.getWhenCapturedByGoesTo().isEmpty();
    });
  }

  static Predicate<Unit> unitWhenCapturedChangesIntoDifferentUnitType() {
    return Match.of(u -> !UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty());
  }

  public static <T extends AbstractUserActionAttachment> Predicate<T> abstractUserActionAttachmentCanBeAttempted(
      final HashMap<ICondition, Boolean> testedConditions) {
    return Match.of(uaa -> uaa.hasAttemptsLeft() && uaa.canPerform(testedConditions));
  }

  public static Predicate<PoliticalActionAttachment> politicalActionHasCostBetween(final int greaterThanEqualTo,
      final int lessThanEqualTo) {
    return Match.of(paa -> paa.getCostPU() >= greaterThanEqualTo && paa.getCostPU() <= lessThanEqualTo);
  }

  static Predicate<Unit> unitCanOnlyPlaceInOriginalTerritories() {
    return Match.of(u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      final Set<String> specialOptions = ua.getSpecial();
      for (final String option : specialOptions) {
        if (option.equals("canOnlyPlaceInOriginalTerritories")) {
          return true;
        }
      }
      return false;
    });
  }

  /**
   * Accounts for OccupiedTerrOf. Returns false if there is no territory attachment (like if it is water).
   */
  public static Predicate<Territory> territoryIsOriginallyOwnedBy(final PlayerID player) {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      final PlayerID originalOwner = ta.getOriginalOwner();
      if (originalOwner == null) {
        return player == null;
      }
      return originalOwner.equals(player);
    });
  }

  static Predicate<PlayerID> isAlliedAndAlliancesCanChainTogether(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAlliedAndAlliancesCanChainTogether()
        .test(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Predicate<RelationshipType> relationshipTypeIsAlliedAndAlliancesCanChainTogether() {
    return Match.of(rt -> relationshipTypeIsAllied().test(rt)
        && rt.getRelationshipTypeAttachment().canAlliancesChainTogether());
  }

  public static Predicate<RelationshipType> relationshipTypeIsDefaultWarPosition() {
    return Match.of(rt -> rt.getRelationshipTypeAttachment().isDefaultWarPosition());
  }

  /**
   * If player is null, this match Will return true if ANY of the relationship changes match the conditions. (since
   * paa's can have more than
   * 1 change).
   *
   * @param player
   *        CAN be null
   * @param currentRelation
   *        cannot be null
   * @param newRelation
   *        cannot be null
   * @param data
   *        cannot be null
   */
  public static Predicate<PoliticalActionAttachment> politicalActionIsRelationshipChangeOf(final PlayerID player,
      final Predicate<RelationshipType> currentRelation, final Predicate<RelationshipType> newRelation,
      final GameData data) {
    return Match.of(paa -> {
      for (final String relationshipChangeString : paa.getRelationshipChange()) {
        final String[] relationshipChange = relationshipChangeString.split(":");
        final PlayerID p1 = data.getPlayerList().getPlayerId(relationshipChange[0]);
        final PlayerID p2 = data.getPlayerList().getPlayerId(relationshipChange[1]);
        if (player != null && !(p1.equals(player) || p2.equals(player))) {
          continue;
        }
        final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
        final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
        if (currentRelation.test(currentType) && newRelation.test(newType)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Predicate<PoliticalActionAttachment> politicalActionAffectsAtLeastOneAlivePlayer(
      final PlayerID currentPlayer, final GameData data) {
    return Match.of(paa -> {
      for (final String relationshipChangeString : paa.getRelationshipChange()) {
        final String[] relationshipChange = relationshipChangeString.split(":");
        final PlayerID p1 = data.getPlayerList().getPlayerId(relationshipChange[0]);
        final PlayerID p2 = data.getPlayerList().getPlayerId(relationshipChange[1]);
        if (!currentPlayer.equals(p1)) {
          if (p1.amNotDeadYet(data)) {
            return true;
          }
        }
        if (!currentPlayer.equals(p2)) {
          if (p2.amNotDeadYet(data)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  public static Predicate<Territory> airCanLandOnThisAlliedNonConqueredLandTerritory(final PlayerID player,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().test(t)) {
        return false;
      }
      final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
      if (bt.wasConquered(t)) {
        return false;
      }
      final PlayerID owner = t.getOwner();
      if (owner == null || owner.isNull()) {
        return false;
      }
      final RelationshipTracker rt = data.getRelationshipTracker();
      return !(!rt.canMoveAirUnitsOverOwnedLand(player, owner) || !rt.canLandAirUnitsOnOwnedLand(player, owner));
    });
  }

  static Predicate<Territory> territoryAllowsRocketsCanFlyOver(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().test(t)) {
        return true;
      }
      final PlayerID owner = t.getOwner();
      if (owner == null || owner.isNull()) {
        return true;
      }
      final RelationshipTracker rt = data.getRelationshipTracker();
      return rt.rocketsCanFlyOver(player, owner);
    });
  }

  public static Predicate<Unit> unitCanScrambleOnRouteDistance(final Route route) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getMaxScrambleDistance() >= route.getMovementCost(unit));
  }

  static Predicate<Unit> unitCanIntercept() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanIntercept());
  }

  static Predicate<Unit> unitCanEscort() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanEscort());
  }

  static Predicate<Unit> unitCanAirBattle() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanAirBattle());
  }

  static Predicate<Territory> //
      territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(
          final PlayerID attacker) {
    return Match.of(t -> {
      if (t.getOwner().equals(attacker)) {
        return false;
      }
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(attacker, t.getData()).test(t)) {
        return false;
      }
      return relationshipTypeCanTakeOverOwnedTerritory()
          .test(t.getData().getRelationshipTracker().getRelationshipType(attacker, t.getOwner()));
    });
  }

  static Predicate<Territory> territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(final PlayerID movingPlayer) {
    return Match.of(t -> {
      if (t.getOwner().equals(movingPlayer)) {
        return true;
      }
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return true;
      }
      return t.getData().getRelationshipTracker().canMoveIntoDuringCombatMove(movingPlayer, t.getOwner());
    });
  }

  public static Predicate<Unit> unitCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAa, final boolean doNotIncludeBombardingSeaUnits) {
    return Match.of(unit -> unitTypeCanBeInBattle(attack, isLandBattle, unit.getOwner(), battleRound,
        includeAttackersThatCanNotMove, doNotIncludeAa, doNotIncludeBombardingSeaUnits).test(unit.getType()));
  }

  public static Predicate<UnitType> unitTypeCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final PlayerID player, final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAa, final boolean doNotIncludeBombardingSeaUnits) {

    // Filter out anything like factories, or units that have no combat ability AND cannot be taken casualty
    final PredicateBuilder<UnitType> canBeInBattleBuilder = PredicateBuilder.of(unitTypeIsInfrastructure().negate())
        .or(unitTypeIsSupporterOrHasCombatAbility(attack, player))
        .orIf(!doNotIncludeAa, unitTypeIsAaForCombatOnly().and(unitTypeIsAaThatCanFireOnRound(battleRound)));

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

    return Match.of(canBeInBattleBuilder.build());
  }

  static Predicate<Unit> unitIsAirborne() {
    return Match.of(obj -> ((TripleAUnit) obj).getAirborne());
  }

  public static <T> Predicate<T> isNotInList(final List<T> list) {
    return Match.of(not(list::contains));
  }
}
