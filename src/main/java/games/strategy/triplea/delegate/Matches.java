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
  public static <T> Match<T> always() {
    return Match.of(it -> true);
  }

  /**
   * Returns a match whose condition is never satisfied.
   *
   * @return A match; never {@code null}.
   */
  public static <T> Match<T> never() {
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

  public static Match<UnitType> unitTypeHasMoreThanOneHitPointTotal() {
    return Match.of(ut -> UnitAttachment.get(ut).getHitPoints() > 1);
  }

  public static Match<Unit> unitHasMoreThanOneHitPointTotal() {
    return Match.of(unit -> unitTypeHasMoreThanOneHitPointTotal().match(unit.getType()));
  }

  public static Match<Unit> unitHasTakenSomeDamage() {
    return Match.of(unit -> unit.getHits() > 0);
  }

  public static Match<Unit> unitHasNotTakenAnyDamage() {
    return unitHasTakenSomeDamage().invert();
  }

  public static Match<Unit> unitIsSea() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSea());
  }

  public static Match<Unit> unitIsSub() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSub());
  }

  public static Match<Unit> unitIsNotSub() {
    return unitIsSub().invert();
  }

  private static Match<Unit> unitIsCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsCombatTransport() && ua.getIsSea();
    });
  }

  static Match<Unit> unitIsNotCombatTransport() {
    return unitIsCombatTransport().invert();
  }

  /**
   * Returns a match indicating the specified unit is a transport but not a combat transport.
   */
  public static Match<Unit> unitIsTransportButNotCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport();
    });
  }

  /**
   * Returns a match indicating the specified unit is not a transport but may be a combat transport.
   */
  public static Match<Unit> unitIsNotTransportButCouldBeCombatTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getTransportCapacity() == -1) {
        return true;
      }
      return ua.getIsCombatTransport() && ua.getIsSea();
    });
  }

  public static Match<Unit> unitIsDestroyer() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsDestroyer());
  }

  public static Match<UnitType> unitTypeIsDestroyer() {
    return Match.of(type -> UnitAttachment.get(type).getIsDestroyer());
  }

  /**
   * Returns a match indicating the specified unit can transport other units by sea.
   */
  public static Match<Unit> unitIsTransport() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1 && ua.getIsSea();
    });
  }

  public static Match<Unit> unitIsNotTransport() {
    return unitIsTransport().invert();
  }

  static Match<Unit> unitIsTransportAndNotDestroyer() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !unitIsDestroyer().match(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea();
    });
  }

  /**
   * Returns a match indicating the specified unit type is a strategic bomber.
   */
  public static Match<UnitType> unitTypeIsStrategicBomber() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getIsStrategicBomber();
    });
  }

  public static Match<Unit> unitIsStrategicBomber() {
    return Match.of(obj -> unitTypeIsStrategicBomber().match(obj.getType()));
  }

  static Match<Unit> unitIsNotStrategicBomber() {
    return unitIsStrategicBomber().invert();
  }

  static final Match<UnitType> unitTypeCanLandOnCarrier() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getCarrierCost() != -1;
    });
  }

  static Match<Unit> unitHasMoved() {
    return Match.of(unit -> TripleAUnit.get(unit).getAlreadyMoved() > 0);
  }

  public static Match<Unit> unitHasNotMoved() {
    return unitHasMoved().invert();
  }

  static Match<Unit> unitCanAttack(final PlayerID id) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getMovement(id) <= 0) {
        return false;
      }
      return ua.getAttack(id) > 0;
    });
  }

  public static Match<Unit> unitHasAttackValueOfAtLeast(final int attackValue) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getAttack(unit.getOwner()) >= attackValue);
  }

  public static Match<Unit> unitHasDefendValueOfAtLeast(final int defendValue) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getDefense(unit.getOwner()) >= defendValue);
  }

  public static Match<Unit> unitIsEnemyOf(final GameData data, final PlayerID player) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWar(unit.getOwner(), player));
  }

  public static Match<Unit> unitIsNotSea() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsSea());
  }

  public static Match<UnitType> unitTypeIsSea() {
    return Match.of(type -> UnitAttachment.get(type).getIsSea());
  }

  public static Match<UnitType> unitTypeIsNotSea() {
    return Match.of(type -> !UnitAttachment.get(type).getIsSea());
  }

  /**
   * Returns a match indicating the specified unit type is for sea or air units.
   */
  public static Match<UnitType> unitTypeIsSeaOrAir() {
    return Match.of(type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsSea() || ua.getIsAir();
    });
  }

  public static Match<Unit> unitIsAir() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAir());
  }

  public static Match<Unit> unitIsNotAir() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsAir());
  }

  public static Match<UnitType> unitTypeCanBombard(final PlayerID id) {
    return Match.of(type -> UnitAttachment.get(type).getCanBombard(id));
  }

  static Match<Unit> unitCanBeGivenByTerritoryTo(final PlayerID player) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBeGivenByTerritoryTo().contains(player));
  }

  static Match<Unit> unitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr,
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

  static Match<Unit> unitDestroyedWhenCapturedByOrFrom(final PlayerID playerBy) {
    return Match.anyOf(unitDestroyedWhenCapturedBy(playerBy), unitDestroyedWhenCapturedFrom());
  }

  private static Match<Unit> unitDestroyedWhenCapturedBy(final PlayerID playerBy) {
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

  private static Match<Unit> unitDestroyedWhenCapturedFrom() {
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

  public static Match<Unit> unitIsAirBase() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAirBase());
  }

  public static Match<UnitType> unitTypeCanBeDamaged() {
    return Match.of(ut -> UnitAttachment.get(ut).getCanBeDamaged());
  }

  public static Match<Unit> unitCanBeDamaged() {
    return Match.of(unit -> unitTypeCanBeDamaged().match(unit.getType()));
  }

  static Match<Unit> unitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
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

  static Match<Unit> unitIsLegalBombingTargetBy(final Unit bomberOrRocket) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(bomberOrRocket.getType());
      final Set<UnitType> allowedTargets = ua.getBombingTargets(bomberOrRocket.getData());
      return allowedTargets == null || allowedTargets.contains(unit.getType());
    });
  }

  public static Match<Unit> unitHasTakenSomeBombingUnitDamage() {
    return Match.of(unit -> ((TripleAUnit) unit).getUnitDamage() > 0);
  }

  public static Match<Unit> unitHasNotTakenAnyBombingUnitDamage() {
    return unitHasTakenSomeBombingUnitDamage().invert();
  }

  /**
   * Returns a match indicating the specified unit is disabled.
   */
  public static Match<Unit> unitIsDisabled() {
    return Match.of(unit -> {
      if (!unitCanBeDamaged().match(unit)) {
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

  public static Match<Unit> unitIsNotDisabled() {
    return unitIsDisabled().invert();
  }

  static Match<Unit> unitCanDieFromReachingMaxDamage() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return false;
      }
      return ua.getCanDieFromReachingMaxDamage();
    });
  }

  public static Match<UnitType> unitTypeIsInfrastructure() {
    return Match.of(ut -> UnitAttachment.get(ut).getIsInfrastructure());
  }

  public static Match<Unit> unitIsInfrastructure() {
    return Match.of(unit -> unitTypeIsInfrastructure().match(unit.getType()));
  }

  public static Match<Unit> unitIsNotInfrastructure() {
    return unitIsInfrastructure().invert();
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  public static Match<Unit> unitIsSupporterOrHasCombatAbility(final boolean attack) {
    return Match.of(unit -> unitTypeIsSupporterOrHasCombatAbility(attack, unit.getOwner()).match(unit.getType()));
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  private static Match<UnitType> unitTypeIsSupporterOrHasCombatAbility(final boolean attack, final PlayerID player) {
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

  public static Match<UnitSupportAttachment> unitSupportAttachmentCanBeUsedByPlayer(final PlayerID player) {
    return Match.of(usa -> usa.getPlayers().contains(player));
  }

  public static Match<Unit> unitCanScramble() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanScramble());
  }

  public static Match<Unit> unitWasScrambled() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasScrambled());
  }

  static Match<Unit> unitWasInAirBattle() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasInAirBattle());
  }

  public static Match<Unit> unitCanBombard(final PlayerID id) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBombard(id));
  }

  public static Match<Unit> unitCanBlitz() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBlitz(unit.getOwner()));
  }

  static Match<Unit> unitIsLandTransport() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsLandTransport());
  }

  static Match<Unit> unitIsNotInfrastructureAndNotCapturedOnEntering(final PlayerID player,
      final Territory terr, final GameData data) {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsInfrastructure()
        && !unitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).match(unit));
  }

  static Match<Unit> unitIsSuicide() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSuicide());
  }

  static Match<Unit> unitIsSuicideOnHit() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSuicideOnHit());
  }

  static Match<Unit> unitIsKamikaze() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getIsKamikaze());
  }

  public static Match<UnitType> unitTypeIsAir() {
    return Match.of(type -> UnitAttachment.get(type).getIsAir());
  }

  private static Match<UnitType> unitTypeIsNotAir() {
    return Match.of(type -> !UnitAttachment.get(type).getIsAir());
  }

  public static Match<Unit> unitCanLandOnCarrier() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCost() != -1);
  }

  public static Match<Unit> unitIsCarrier() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1);
  }

  static Match<Territory> territoryHasOwnedCarrier(final PlayerID player) {
    return Match.of(t -> t.getUnits().anyMatch(Match.allOf(unitIsOwnedBy(player), unitIsCarrier())));
  }

  public static Match<Unit> unitIsAlliedCarrier(final PlayerID player, final GameData data) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
        && data.getRelationshipTracker().isAllied(player, unit.getOwner()));
  }

  public static Match<Unit> unitCanBeTransported() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCost() != -1);
  }

  static Match<Unit> unitWasAmphibious() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasAmphibious());
  }

  static Match<Unit> unitWasNotAmphibious() {
    return unitWasAmphibious().invert();
  }

  static Match<Unit> unitWasInCombat() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasInCombat());
  }

  static Match<Unit> unitWasUnloadedThisTurn() {
    return Match.of(obj -> ((TripleAUnit) obj).getUnloadedTo() != null);
  }

  private static Match<Unit> unitWasLoadedThisTurn() {
    return Match.of(obj -> ((TripleAUnit) obj).getWasLoadedThisTurn());
  }

  static Match<Unit> unitWasNotLoadedThisTurn() {
    return unitWasLoadedThisTurn().invert();
  }

  public static Match<Unit> unitCanTransport() {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCapacity() != -1);
  }

  public static Match<UnitType> unitTypeCanProduceUnits() {
    return Match.of(obj -> UnitAttachment.get(obj).getCanProduceUnits());
  }

  public static Match<Unit> unitCanProduceUnits() {
    return Match.of(obj -> unitTypeCanProduceUnits().match(obj.getType()));
  }

  public static Match<UnitType> unitTypeHasMaxBuildRestrictions() {
    return Match.of(type -> UnitAttachment.get(type).getMaxBuiltPerPlayer() >= 0);
  }

  public static Match<UnitType> unitTypeIsRocket() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsRocket());
  }

  static Match<Unit> unitIsRocket() {
    return Match.of(obj -> unitTypeIsRocket().match(obj.getType()));
  }

  static Match<Unit> unitHasMovementLimit() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getMovementLimit() != null);
  }

  static final Match<Unit> unitHasAttackingLimit() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getAttackingLimit() != null);
  }

  private static Match<UnitType> unitTypeCanNotMoveDuringCombatMove() {
    return Match.of(type -> UnitAttachment.get(type).getCanNotMoveDuringCombatMove());
  }

  public static Match<Unit> unitCanNotMoveDuringCombatMove() {
    return Match.of(obj -> unitTypeCanNotMoveDuringCombatMove().match(obj.getType()));
  }

  private static Match<Unit> unitIsAaThatCanHitTheseUnits(final Collection<Unit> targets,
      final Match<Unit> typeOfAa, final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed) {
    return Match.of(obj -> {
      if (!typeOfAa.match(obj)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      final Set<UnitType> targetsAa = ua.getTargetsAA(obj.getData());
      for (final Unit u : targets) {
        if (targetsAa.contains(u.getType())) {
          return true;
        }
      }
      return targets.stream().anyMatch(Match.allOf(unitIsAirborne(),
          unitIsOfTypes(airborneTechTargetsAllowed.get(ua.getTypeAA()))));
    });
  }

  static Match<Unit> unitIsAaOfTypeAa(final String typeAa) {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getTypeAA().matches(typeAa));
  }

  static Match<Unit> unitAaShotDamageableInsteadOfKillingInstantly() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getDamageableAA());
  }

  private static Match<Unit> unitIsAaThatWillNotFireIfPresentEnemyUnits(final Collection<Unit> enemyUnitsPresent) {
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

  private static Match<UnitType> unitTypeIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return Match.of(obj -> {
      final int maxRoundsAa = UnitAttachment.get(obj).getMaxRoundsAA();
      return maxRoundsAa < 0 || maxRoundsAa >= battleRoundNumber;
    });
  }

  private static Match<Unit> unitIsAaThatCanFireOnRound(final int battleRoundNumber) {
    return Match.of(obj -> unitTypeIsAaThatCanFireOnRound(battleRoundNumber).match(obj.getType()));
  }

  static Match<Unit> unitIsAaThatCanFire(final Collection<Unit> unitsMovingOrAttacking,
      final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed, final PlayerID playerMovingOrAttacking,
      final Match<Unit> typeOfAa, final int battleRoundNumber, final boolean defending, final GameData data) {
    return Match.allOf(enemyUnit(playerMovingOrAttacking, data),
        unitIsBeingTransported().invert(),
        unitIsAaThatCanHitTheseUnits(unitsMovingOrAttacking, typeOfAa, airborneTechTargetsAllowed),
        unitIsAaThatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).invert(),
        unitIsAaThatCanFireOnRound(battleRoundNumber),
        (defending ? unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()
            : unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero()));
  }

  private static Match<UnitType> unitTypeIsAaForCombatOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforCombatOnly());
  }

  static Match<Unit> unitIsAaForCombatOnly() {
    return Match.of(obj -> unitTypeIsAaForCombatOnly().match(obj.getType()));
  }

  public static Match<UnitType> unitTypeIsAaForBombingThisUnitOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforBombingThisUnitOnly());
  }

  public static Match<Unit> unitIsAaForBombingThisUnitOnly() {
    return Match.of(obj -> unitTypeIsAaForBombingThisUnitOnly().match(obj.getType()));
  }

  private static Match<UnitType> unitTypeIsAaForFlyOverOnly() {
    return Match.of(obj -> UnitAttachment.get(obj).getIsAAforFlyOverOnly());
  }

  static Match<Unit> unitIsAaForFlyOverOnly() {
    return Match.of(obj -> unitTypeIsAaForFlyOverOnly().match(obj.getType()));
  }

  /**
   * Returns a match indicating the specified unit type is anti-aircraft for any condition.
   */
  public static Match<UnitType> unitTypeIsAaForAnything() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly() || ua.getIsAAforFlyOverOnly();
    });
  }

  public static Match<Unit> unitIsAaForAnything() {
    return Match.of(obj -> unitTypeIsAaForAnything().match(obj.getType()));
  }

  public static Match<Unit> unitIsNotAa() {
    return unitIsAaForAnything().invert();
  }

  private static Match<UnitType> unitTypeMaxAaAttacksIsInfinite() {
    return Match.of(obj -> UnitAttachment.get(obj).getMaxAAattacks() == -1);
  }

  static Match<Unit> unitMaxAaAttacksIsInfinite() {
    return Match.of(obj -> unitTypeMaxAaAttacksIsInfinite().match(obj.getType()));
  }

  private static Match<UnitType> unitTypeMayOverStackAa() {
    return Match.of(obj -> UnitAttachment.get(obj).getMayOverStackAA());
  }

  static Match<Unit> unitMayOverStackAa() {
    return Match.of(obj -> unitTypeMayOverStackAa().match(obj.getType()));
  }

  static Match<Unit> unitAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    });
  }

  static Match<Unit> unitOffensiveAttackAaIsGreaterThanZeroAndMaxAaAttacksIsNotZero() {
    return Match.of(obj -> {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getOffensiveAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    });
  }

  public static Match<Unit> unitIsInfantry() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getIsInfantry());
  }

  public static Match<Unit> unitIsNotInfantry() {
    return unitIsInfantry().invert();
  }

  /**
   * Returns a match indicating the specified unit can be transported by air.
   */
  public static Match<Unit> unitIsAirTransportable() {
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

  static Match<Unit> unitIsNotAirTransportable() {
    return unitIsAirTransportable().invert();
  }

  /**
   * Returns a match indicating the specified unit can transport other units by air.
   */
  public static Match<Unit> unitIsAirTransport() {
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

  public static Match<Unit> unitIsArtillery() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getArtillery());
  }

  public static Match<Unit> unitIsArtillerySupportable() {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getArtillerySupportable());
  }

  // TODO: CHECK whether this makes any sense
  public static Match<Territory> territoryIsLandOrWater() {
    return Match.of(Objects::nonNull);
  }

  public static Match<Territory> territoryIsWater() {
    return Match.of(Territory::isWater);
  }

  /**
   * Returns a match indicating the specified territory is an island.
   */
  public static Match<Territory> territoryIsIsland() {
    return Match.of(t -> {
      final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
      return neighbors.size() == 1 && territoryIsWater().match(neighbors.iterator().next());
    });
  }

  /**
   * Returns a match indicating the specified territory is a victory city.
   */
  public static Match<Territory> territoryIsVictoryCity() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      return ta.getVictoryCity() != 0;
    });
  }

  public static Match<Territory> territoryIsLand() {
    return territoryIsWater().invert();
  }

  public static Match<Territory> territoryIsEmpty() {
    return Match.of(t -> t.getUnits().size() == 0);
  }

  /**
   * Tests for Land, Convoys Centers and Convoy Routes, and Contested Territories.
   * Assumes player is either the owner of the territory we are testing, or about to become the owner (ie: this doesn't
   * test ownership).
   * If the game option for contested territories not producing is on, then will also remove any contested territories.
   */
  public static Match<Territory> territoryCanCollectIncomeFrom(final PlayerID player, final GameData data) {
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
      return !(contestedDoNotProduce && !territoryHasNoEnemyUnits(player, data).match(t));
    });
  }

  public static Match<Territory> territoryHasNeighborMatching(final GameData data, final Match<Territory> match) {
    return Match.of(t -> data.getMap().getNeighbors(t, match).size() > 0);
  }

  public static Match<Territory> territoryHasAlliedNeighborWithAlliedUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsAlliedAndHasAlliedUnitMatching(data, player, unitMatch)).size() > 0);
  }

  public static Match<Territory> territoryIsInList(final Collection<Territory> list) {
    return Match.of(list::contains);
  }

  public static Match<Territory> territoryIsNotInList(final Collection<Territory> list) {
    return Match.of(not(list::contains));
  }

  /**
   * @param data
   *        game data
   * @return Match&lt;Territory> that tests if there is a route to an enemy capital from the given territory.
   */
  public static Match<Territory> territoryHasRouteToEnemyCapital(final GameData data, final PlayerID player) {
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
  public static Match<Territory> territoryHasLandRouteToEnemyCapital(final GameData data, final PlayerID player) {
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

  public static Match<Territory> territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, unitMatch)).size() > 0);
  }

  public static Match<Territory> territoryHasOwnedNeighborWithOwnedUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryIsOwnedAndHasOwnedUnitMatching(player, unitMatch)).size() > 0);
  }

  static Match<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(
      final GameData data, final PlayerID player) {
    return Match.of(t -> data.getMap()
        .getNeighbors(t, territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(data, player)).size() > 0);
  }

  public static Match<Territory> territoryHasWaterNeighbor(final GameData data) {
    return Match.of(t -> data.getMap().getNeighbors(t, territoryIsWater()).size() > 0);
  }

  private static Match<Territory> territoryIsAlliedAndHasAlliedUnitMatching(final GameData data, final PlayerID player,
      final Match<Unit> unitMatch) {
    return Match.of(t -> {
      if (!data.getRelationshipTracker().isAllied(t.getOwner(), player)) {
        return false;
      }
      return t.getUnits().anyMatch(Match.allOf(alliedUnit(player, data), unitMatch));
    });
  }

  public static Match<Territory> territoryIsOwnedAndHasOwnedUnitMatching(final PlayerID player,
      final Match<Unit> unitMatch) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().anyMatch(Match.allOf(unitIsOwnedBy(player), unitMatch));
    });
  }

  public static Match<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().anyMatch(unitCanProduceUnits());
    });
  }

  private static Match<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(final GameData data,
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

  static Match<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      if (!isTerritoryAllied(player, data).match(t)) {
        return false;
      }
      return t.getUnits().anyMatch(unitCanProduceUnits());
    });
  }

  public static Match<Territory> territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return Match.of(t -> {
      if (!data.getRelationshipTracker().isAtWar(player, t.getOwner())) {
        return false;
      }
      if (t.getOwner().isNull()) {
        return false;
      }
      return t.getUnits().anyMatch(Match.allOf(enemyUnit(player, data), unitMatch));
    });
  }

  static Match<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      final Match<Unit> nonCom = Match.anyOf(unitIsInfrastructure(), enemyUnit(player, data).invert());
      return t.getUnits().allMatch(nonCom);
    });
  }

  /**
   * Returns a match indicating the specified territory is neutral and not water.
   */
  public static Match<Territory> territoryIsNeutralButNotWater() {
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
  public static Match<Territory> territoryIsImpassable() {
    return Match.of(t -> {
      if (t.isWater()) {
        return false;
      }
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getIsImpassable();
    });
  }

  public static Match<Territory> territoryIsNotImpassable() {
    return territoryIsImpassable().invert();
  }

  static Match<Territory> seaCanMoveOver(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (!territoryIsWater().match(t)) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player, data).match(t);
    });
  }

  static Match<Territory> airCanFlyOver(final PlayerID player, final GameData data,
      final boolean areNeutralsPassableByAir) {
    return Match.of(t -> {
      if (!areNeutralsPassableByAir && territoryIsNeutralButNotWater().match(t)) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(player, data).match(t)) {
        return false;
      }
      return !(territoryIsLand().match(t)
          && !data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
    });
  }

  public static Match<Territory> territoryIsPassableAndNotRestricted(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (territoryIsImpassable().match(t)) {
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

  private static Match<Territory> territoryIsImpassableToLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.isWater()) {
        return true;
      } else if (territoryIsPassableAndNotRestricted(player, data).invert().match(t)) {
        return true;
      }
      return false;
    });
  }

  public static Match<Territory> territoryIsNotImpassableToLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> territoryIsImpassableToLandUnits(player, data).invert().match(t));
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
  public static Match<Territory> territoryIsPassableAndNotRestrictedAndOkByRelationships(
      final PlayerID playerWhoOwnsAllTheUnitsMoving, final GameData data, final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded, final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported, final boolean isLandingZoneOnLandForAirUnits) {
    final boolean neutralsPassable = !Properties.getNeutralsImpassable(data);
    final boolean areNeutralsPassableByAir =
        neutralsPassable && Properties.getNeutralFlyoverAllowed(data);
    return Match.of(t -> {
      if (territoryIsImpassable().match(t)) {
        return false;
      }
      if ((!neutralsPassable || (hasAirUnitsNotBeingTransported && !areNeutralsPassableByAir))
          && territoryIsNeutralButNotWater().match(t)) {
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
      final boolean isWater = territoryIsWater().match(t);
      final boolean isLand = territoryIsLand().match(t);
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

  static Match<IBattle> battleIsEmpty() {
    return Match.of(IBattle::isEmpty);
  }

  static Match<IBattle> battleIsAmphibious() {
    return Match.of(IBattle::isAmphibious);
  }

  public static Match<Unit> unitHasEnoughMovementForRoutes(final List<Route> route) {
    return unitHasEnoughMovementForRoute(Route.create(route));
  }

  public static Match<Unit> unitHasEnoughMovementForRoute(final List<Territory> territories) {
    return unitHasEnoughMovementForRoute(new Route(territories));
  }

  public static Match<Unit> unitHasEnoughMovementForRoute(final Route route) {
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
  public static Match<Unit> unitHasMovementLeft() {
    return Match.of(o -> TripleAUnit.get(o).getMovementLeft() >= 1);
  }

  public static Match<Unit> unitCanMove() {
    return Match.of(u -> unitTypeCanMove(u.getOwner()).match(u.getType()));
  }

  private static Match<UnitType> unitTypeCanMove(final PlayerID player) {
    return Match.of(obj -> UnitAttachment.get(obj).getMovement(player) > 0);
  }

  public static Match<UnitType> unitTypeIsStatic(final PlayerID id) {
    return Match.of(unitType -> !unitTypeCanMove(id).match(unitType));
  }

  public static Match<Unit> unitIsLandAndOwnedBy(final PlayerID player) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !ua.getIsSea() && !ua.getIsAir() && unit.getOwner().equals(player);
    });
  }

  public static Match<Unit> unitIsOwnedBy(final PlayerID player) {
    return Match.of(unit -> unit.getOwner().equals(player));
  }

  public static Match<Unit> unitIsOwnedByOfAnyOfThesePlayers(final Collection<PlayerID> players) {
    return Match.of(unit -> players.contains(unit.getOwner()));
  }

  public static Match<Unit> unitIsTransporting() {
    return Match.of(unit -> {
      final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
      return !(transporting == null || transporting.isEmpty());
    });
  }

  public static Match<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units) {
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units);
    return Match.of(unit -> {
      final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
      if (transporting == null) {
        return false;
      }
      return Util.someIntersect(UnitSeperator.categorize(transporting), unitCategories);
    });
  }

  public static Match<Territory> isTerritoryAllied(final PlayerID player, final GameData data) {
    return Match.of(t -> data.getRelationshipTracker().isAllied(player, t.getOwner()));
  }

  public static Match<Territory> isTerritoryOwnedBy(final PlayerID player) {
    return Match.of(t -> t.getOwner().equals(player));
  }

  public static Match<Territory> isTerritoryOwnedBy(final Collection<PlayerID> players) {
    return Match.of(t -> {
      for (final PlayerID player : players) {
        if (t.getOwner().equals(player)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Match<Unit> isUnitAllied(final PlayerID player, final GameData data) {
    return Match.of(t -> data.getRelationshipTracker().isAllied(player, t.getOwner()));
  }

  public static Match<Territory> isTerritoryFriendly(final PlayerID player, final GameData data) {
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

  private static Match<Unit> unitIsEnemyAaForAnything(final PlayerID player, final GameData data) {
    return Match.allOf(unitIsAaForAnything(), enemyUnit(player, data));
  }

  private static Match<Unit> unitIsEnemyAaForCombat(final PlayerID player, final GameData data) {
    return Match.allOf(unitIsAaForCombatOnly(), enemyUnit(player, data));
  }

  static Match<Unit> unitIsInTerritory(final Territory territory) {
    return Match.of(o -> territory.getUnits().getUnits().contains(o));
  }

  public static Match<Territory> isTerritoryEnemy(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.getOwner().equals(player)) {
        return false;
      }
      return data.getRelationshipTracker().isAtWar(player, t.getOwner());
    });
  }

  public static Match<Territory> isTerritoryEnemyAndNotUnownedWater(final PlayerID player, final GameData data) {
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

  public static Match<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(final PlayerID player,
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
      if (!territoryIsPassableAndNotRestricted(player, data).match(t)) {
        return false;
      }
      return data.getRelationshipTracker().isAtWar(player, t.getOwner());
    });
  }

  public static Match<Territory> territoryIsBlitzable(final PlayerID player, final GameData data) {
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
          .of(enemyUnit(player, data).invert())
          // WW2V2, cant blitz through factories and aa guns
          // WW2V1, you can
          .orIf(!Properties.getWW2V2(data) && !Properties.getBlitzThroughFactoriesAndAARestricted(data),
              unitIsInfrastructure())
          .build();
      return t.getUnits().allMatch(blitzableUnits);
    });
  }

  public static Match<Territory> isTerritoryFreeNeutral(final GameData data) {
    return Match.of(t -> t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) <= 0);
  }

  public static Match<Territory> territoryDoesNotCostMoneyToEnter(final GameData data) {
    return Match.of(t -> territoryIsLand().invert().match(t) || !t.getOwner().equals(PlayerID.NULL_PLAYERID)
        || Properties.getNeutralCharge(data) <= 0);
  }

  public static Match<Unit> enemyUnit(final PlayerID player, final GameData data) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWar(player, unit.getOwner()));
  }

  public static Match<Unit> enemyUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return Match.of(unit -> data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(unit.getOwner(), players));
  }

  public static Match<Unit> unitOwnedBy(final PlayerID player) {
    return Match.of(unit -> unit.getOwner().equals(player));
  }

  public static Match<Unit> unitOwnedBy(final List<PlayerID> players) {
    return Match.of(o -> {
      for (final PlayerID p : players) {
        if (o.getOwner().equals(p)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Match<Unit> alliedUnit(final PlayerID player, final GameData data) {
    return Match.of(unit -> {
      if (unit.getOwner().equals(player)) {
        return true;
      }
      return data.getRelationshipTracker().isAllied(player, unit.getOwner());
    });
  }

  public static Match<Unit> alliedUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return Match.of(unit -> {
      if (unitIsOwnedByOfAnyOfThesePlayers(players).match(unit)) {
        return true;
      }
      return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(unit.getOwner(), players);
    });
  }

  public static Match<Territory> territoryIs(final Territory test) {
    return Match.of(t -> t.equals(test));
  }

  public static Match<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player) {
    return Match.of(t -> t.getUnits().anyMatch(Match.allOf(unitIsOwnedBy(player), unitIsLand())));
  }

  public static Match<Territory> territoryHasUnitsOwnedBy(final PlayerID player) {
    final Match<Unit> unitOwnedBy = unitIsOwnedBy(player);
    return Match.of(t -> t.getUnits().anyMatch(unitOwnedBy));
  }

  public static Match<Territory> territoryHasUnitsThatMatch(final Match<Unit> cond) {
    return Match.of(t -> t.getUnits().anyMatch(cond));
  }

  public static Match<Territory> territoryHasEnemyAaForAnything(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsEnemyAaForAnything(player, data)));
  }

  public static Match<Territory> territoryHasEnemyAaForCombatOnly(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(unitIsEnemyAaForCombat(player, data)));
  }

  public static Match<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> !t.getUnits().anyMatch(enemyUnit(player, data)));
  }

  public static Match<Territory> territoryHasAlliedUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(alliedUnit(player, data)));
  }

  static Match<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data) {
    final Match<Unit> match = Match.allOf(enemyUnit(player, data), unitIsSubmerged().invert());
    return Match.of(t -> t.getUnits().anyMatch(match));
  }

  public static Match<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(Match.allOf(enemyUnit(player, data), unitIsLand())));
  }

  public static Match<Territory> territoryHasEnemySeaUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(Match.allOf(enemyUnit(player, data), unitIsSea())));
  }

  public static Match<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().anyMatch(enemyUnit(player, data)));
  }

  static Match<Territory> territoryIsNotUnownedWater() {
    return Match.of(t -> !(t.isWater() && TerritoryAttachment.get(t) == null && t.getOwner().isNull()));
  }

  /**
   * The territory is owned by the enemy of those enemy units (i.e. probably owned by you or your ally, but not
   * necessarily so in an FFA type game).
   */
  static Match<Territory> territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(
      final PlayerID player, final GameData gameData) {
    return Match.of(t -> {
      final List<Unit> enemyUnits = t.getUnits().getMatches(Match.allOf(
          enemyUnit(player, gameData),
          unitIsNotAir(),
          unitIsNotInfrastructure()));
      final Collection<PlayerID> enemyPlayers = enemyUnits.stream()
          .map(Unit::getOwner)
          .collect(Collectors.toSet());
      return isAtWarWithAnyOfThesePlayers(enemyPlayers, gameData).match(t.getOwner());
    });
  }

  public static Match<Unit> transportCannotUnload(final Territory territory) {
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

  public static Match<Unit> transportIsNotTransporting() {
    return Match.of(transport -> !TransportTracker.isTransporting(transport));
  }

  static Match<Unit> transportIsTransporting() {
    return Match.of(transport -> TransportTracker.isTransporting(transport));
  }

  /**
   * @return Match that tests the TripleAUnit getTransportedBy value
   *         which is normally set for sea transport movement of land units,
   *         and sometimes set for other things like para-troopers and dependent allied fighters sitting as cargo on a
   *         ship. (not sure if
   *         set for mech inf or not)
   */
  public static Match<Unit> unitIsBeingTransported() {
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
  public static Match<Unit> unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(final Collection<Unit> units,
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

  public static Match<Unit> unitIsLand() {
    return Match.allOf(unitIsNotSea(), unitIsNotAir());
  }

  public static Match<UnitType> unitTypeIsLand() {
    return Match.allOf(unitTypeIsNotSea(), unitTypeIsNotAir());
  }

  public static Match<Unit> unitIsNotLand() {
    return unitIsLand().invert();
  }

  public static Match<Unit> unitIsOfType(final UnitType type) {
    return Match.of(unit -> unit.getType().equals(type));
  }

  public static Match<Unit> unitIsOfTypes(final Set<UnitType> types) {
    return Match.of(unit -> {
      if (types == null || types.isEmpty()) {
        return false;
      }
      return types.contains(unit.getType());
    });
  }

  static Match<Territory> territoryWasFoughOver(final BattleTracker tracker) {
    return Match.of(t -> tracker.wasBattleFought(t) || tracker.wasBlitzed(t));
  }

  static Match<Unit> unitIsSubmerged() {
    return Match.of(u -> TripleAUnit.get(u).getSubmerged());
  }

  public static Match<UnitType> unitTypeIsSub() {
    return Match.of(type -> UnitAttachment.get(type).getIsSub());
  }

  static Match<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return Match.of(u -> TechTracker.hasImprovedArtillerySupport(u.getOwner()));
  }

  public static Match<Territory> territoryHasNonAllowedCanal(final PlayerID player, final Collection<Unit> unitsMoving,
      final GameData data) {
    return Match.of(t -> MoveValidator.validateCanal(t, null, unitsMoving, player, data).isPresent());
  }

  public static Match<Territory> territoryIsBlockedSea(final PlayerID player, final GameData data) {
    final Match<Unit> sub = Match.allOf(unitIsSub().invert());
    final Match<Unit> transport = Match.allOf(unitIsTransportButNotCombatTransport().invert(), unitIsLand().invert());
    final Match<Unit> unitCond = Match.of(PredicateBuilder
        .of(unitIsInfrastructure().invert())
        .and(alliedUnit(player, data).invert())
        .andIf(Properties.getIgnoreTransportInMovement(data), transport)
        .andIf(Properties.getIgnoreSubInMovement(data), sub)
        .build());
    return Match.allOf(territoryHasUnitsThatMatch(unitCond).invert(), territoryIsWater());
  }

  static Match<Unit> unitCanRepairOthers() {
    return Match.of(unit -> {
      if (unitIsDisabled().match(unit) || unitIsBeingTransported().match(unit)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getRepairsUnits() == null) {
        return false;
      }
      return !ua.getRepairsUnits().isEmpty();
    });
  }

  static Match<Unit> unitCanRepairThisUnit(final Unit damagedUnit, final Territory territoryOfRepairUnit) {
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
  public static Match<Unit> unitCanBeRepairedByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return Match.of(damagedUnit -> {
      final Match<Unit> damaged = Match.allOf(unitHasMoreThanOneHitPointTotal(), unitHasTakenSomeDamage());
      if (!damaged.match(damagedUnit)) {
        return false;
      }
      final Match<Unit> repairUnit = Match.allOf(alliedUnit(player, data),
          unitCanRepairOthers(), unitCanRepairThisUnit(damagedUnit, territory));
      if (territory.getUnits().anyMatch(repairUnit)) {
        return true;
      }
      if (unitIsSea().match(damagedUnit)) {
        final List<Territory> neighbors =
            new ArrayList<>(data.getMap().getNeighbors(territory, territoryIsLand()));
        for (final Territory current : neighbors) {
          final Match<Unit> repairUnitLand = Match.allOf(alliedUnit(player, data),
              unitCanRepairOthers(), unitCanRepairThisUnit(damagedUnit, current), unitIsLand());
          if (current.getUnits().anyMatch(repairUnitLand)) {
            return true;
          }
        }
      } else if (unitIsLand().match(damagedUnit)) {
        final List<Territory> neighbors = new ArrayList<>(data.getMap().getNeighbors(territory, territoryIsWater()));
        for (final Territory current : neighbors) {
          final Match<Unit> repairUnitSea = Match.allOf(alliedUnit(player, data),
              unitCanRepairOthers(), unitCanRepairThisUnit(damagedUnit, current), unitIsSea());
          if (current.getUnits().anyMatch(repairUnitSea)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  private static Match<Unit> unitCanGiveBonusMovement() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getGivesMovement().size() > 0 && unitIsBeingTransported().invert().match(unit);
    });
  }

  static Match<Unit> unitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return Match.of(unitWhichCanGiveBonusMovement -> {
      if (unitIsDisabled().match(unitWhichCanGiveBonusMovement)) {
        return false;
      }
      final UnitType type = unitWhichCanGiveBonusMovement.getType();
      final UnitAttachment ua = UnitAttachment.get(type);
      // TODO: make sure the unit is operational
      return unitCanGiveBonusMovement().match(unitWhichCanGiveBonusMovement)
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
  public static Match<Unit> unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return Match.of(unitWhichWillGetBonus -> {
      final Match<Unit> givesBonusUnit = Match.allOf(alliedUnit(player, data),
          unitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
      if (territory.getUnits().anyMatch(givesBonusUnit)) {
        return true;
      }
      if (unitIsSea().match(unitWhichWillGetBonus)) {
        final Match<Unit> givesBonusUnitLand = Match.allOf(givesBonusUnit, unitIsLand());
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

  static Match<Unit> unitCreatesUnits() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0;
    });
  }

  static Match<Unit> unitCreatesResources() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0;
    });
  }

  /**
   * Returns a match indicating the specified unit type consumes at least one type of unit upon creation.
   */
  public static Match<UnitType> unitTypeConsumesUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit);
      return ua != null && ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0;
    });
  }

  static Match<Unit> unitConsumesUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0;
    });
  }

  static Match<Unit> unitWhichConsumesUnitsHasRequiredUnits(final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!unitConsumesUnitsOnCreation().match(unitWhichRequiresUnits)) {
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      boolean canBuild = true;
      for (final UnitType ut : requiredUnits) {
        final Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = Match.allOf(
            unitIsOwnedBy(unitWhichRequiresUnits.getOwner()),
            unitIsOfType(ut),
            unitHasNotTakenAnyBombingUnitDamage(),
            unitHasNotTakenAnyDamage(),
            unitIsNotDisabled());
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
  public static Match<Unit> unitRequiresUnitsOnCreation() {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua != null && ua.getRequiresUnits() != null && ua.getRequiresUnits().size() > 0;
    });
  }

  /**
   * Checks if requiresUnits criteria allows placement in territory based on units there at the start of turn.
   */
  public static Match<Unit> unitWhichRequiresUnitsHasRequiredUnitsInList(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!unitRequiresUnitsOnCreation().match(unitWhichRequiresUnits)) {
        return true;
      }
      final Match<Unit> unitIsOwnedByAndNotDisabled = Match.allOf(
          unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), unitIsNotDisabled());
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
  public static Match<Unit> unitHasRequiredUnitsToMove(final Territory t, final GameData data) {
    return Match.of(unit -> {

      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null || ua.getRequiresUnitsToMove() == null || ua.getRequiresUnitsToMove().isEmpty()) {
        return true;
      }

      final Match<Unit> unitIsOwnedByAndNotDisabled = Match.allOf(
          isUnitAllied(unit.getOwner(), data), unitIsNotDisabled());
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

  static Match<Territory> territoryIsBlockadeZone() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getBlockadeZone();
    });
  }

  /**
   * Returns a match indicating the specified unit type is a construction unit type.
   */
  public static Match<UnitType> unitTypeIsConstruction() {
    return Match.of(type -> {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua != null && ua.getIsConstruction();
    });
  }

  public static Match<Unit> unitIsConstruction() {
    return Match.of(obj -> unitTypeIsConstruction().match(obj.getType()));
  }

  public static Match<Unit> unitIsNotConstruction() {
    return unitIsConstruction().invert();
  }

  public static Match<Unit> unitCanProduceUnitsAndIsInfrastructure() {
    return Match.allOf(unitCanProduceUnits(), unitIsInfrastructure());
  }

  public static Match<Unit> unitCanProduceUnitsAndCanBeDamaged() {
    return Match.allOf(unitCanProduceUnits(), unitCanBeDamaged());
  }

  /**
   * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from any other unit.
   * Otherwise, units must have a specific unit in this list to be able to invade from that unit.
   */
  public static Match<Unit> unitCanInvade() {
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

  public static Match<RelationshipType> relationshipTypeIsAllied() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isAllied());
  }

  public static Match<RelationshipType> relationshipTypeIsNeutral() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isNeutral());
  }

  public static Match<RelationshipType> relationshipTypeIsAtWar() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().isWar());
  }

  public static Match<Relationship> relationshipIsAtWar() {
    return Match.of(relationship -> relationship.getRelationshipType().getRelationshipTypeAttachment().isWar());
  }

  public static Match<RelationshipType> relationshipTypeCanMoveLandUnitsOverOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveLandUnitsOverOwnedLand());
  }

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> territoryAllowsCanMoveLandUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().match(t)) {
        return true;
      }
      final PlayerID territoryOwner = t.getOwner();
      if (territoryOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    });
  }

  public static Match<RelationshipType> relationshipTypeCanMoveAirUnitsOverOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveAirUnitsOverOwnedLand());
  }

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> territoryAllowsCanMoveAirUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().match(t)) {
        return true;
      }
      final PlayerID territoryOwner = t.getOwner();
      if (territoryOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(territoryOwner, ownerOfUnitsMoving);
    });
  }

  public static Match<RelationshipType> relationshipTypeCanLandAirUnitsOnOwnedLand() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canLandAirUnitsOnOwnedLand());
  }

  public static Match<RelationshipType> relationshipTypeCanTakeOverOwnedTerritory() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canTakeOverOwnedTerritory());
  }

  public static Match<RelationshipType> relationshipTypeGivesBackOriginalTerritories() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().givesBackOriginalTerritories());
  }

  public static Match<RelationshipType> relationshipTypeCanMoveIntoDuringCombatMove() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveIntoDuringCombatMove());
  }

  public static Match<RelationshipType> relationshipTypeCanMoveThroughCanals() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canMoveThroughCanals());
  }

  public static Match<RelationshipType> relationshipTypeRocketsCanFlyOver() {
    return Match.of(relationship -> relationship.getRelationshipTypeAttachment().canRocketsFlyOver());
  }

  public static Match<String> isValidRelationshipName(final GameData data) {
    return Match.of(relationshipName -> data.getRelationshipTypeList().getRelationshipType(relationshipName) != null);
  }

  public static Match<PlayerID> isAtWar(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAtWar()
        .match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Match<PlayerID> isAtWarWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(player2, players));
  }

  public static Match<PlayerID> isAllied(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAllied()
        .match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Match<PlayerID> isAlliedWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(player2, players));
  }

  public static Match<Unit> unitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(unit -> unitCanProduceUnits().match(unit) && unitIsOwnedBy(player).match(unit));
  }

  public static Match<Unit> unitCanReceiveAbilityWhenWith() {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getReceivesAbilityWhenWith().isEmpty());
  }

  public static Match<Unit> unitCanReceiveAbilityWhenWith(final String filterForAbility,
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

  private static Match<Unit> unitHasWhenCombatDamagedEffect() {
    return Match.of(u -> !UnitAttachment.get(u.getType()).getWhenCombatDamaged().isEmpty());
  }

  static Match<Unit> unitHasWhenCombatDamagedEffect(final String filterForEffect) {
    return Match.of(u -> {
      if (!unitHasWhenCombatDamagedEffect().match(u)) {
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

  static Match<Territory> territoryHasWhenCapturedByGoesTo() {
    return Match.of(t -> {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      return !ta.getWhenCapturedByGoesTo().isEmpty();
    });
  }

  static Match<Unit> unitWhenCapturedChangesIntoDifferentUnitType() {
    return Match.of(u -> !UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty());
  }

  public static <T extends AbstractUserActionAttachment> Match<T> abstractUserActionAttachmentCanBeAttempted(
      final HashMap<ICondition, Boolean> testedConditions) {
    return Match.of(uaa -> uaa.hasAttemptsLeft() && uaa.canPerform(testedConditions));
  }

  public static Match<PoliticalActionAttachment> politicalActionHasCostBetween(final int greaterThanEqualTo,
      final int lessThanEqualTo) {
    return Match.of(paa -> paa.getCostPU() >= greaterThanEqualTo && paa.getCostPU() <= lessThanEqualTo);
  }

  static Match<Unit> unitCanOnlyPlaceInOriginalTerritories() {
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
  public static Match<Territory> territoryIsOriginallyOwnedBy(final PlayerID player) {
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

  static Match<PlayerID> isAlliedAndAlliancesCanChainTogether(final PlayerID player, final GameData data) {
    return Match.of(player2 -> relationshipTypeIsAlliedAndAlliancesCanChainTogether()
        .match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Match<RelationshipType> relationshipTypeIsAlliedAndAlliancesCanChainTogether() {
    return Match.of(rt -> relationshipTypeIsAllied().match(rt)
        && rt.getRelationshipTypeAttachment().canAlliancesChainTogether());
  }

  public static Match<RelationshipType> relationshipTypeIsDefaultWarPosition() {
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
  public static Match<PoliticalActionAttachment> politicalActionIsRelationshipChangeOf(final PlayerID player,
      final Match<RelationshipType> currentRelation, final Match<RelationshipType> newRelation, final GameData data) {
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
        if (currentRelation.match(currentType) && newRelation.match(newType)) {
          return true;
        }
      }
      return false;
    });
  }

  public static Match<PoliticalActionAttachment> politicalActionAffectsAtLeastOneAlivePlayer(
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

  public static Match<Territory> airCanLandOnThisAlliedNonConqueredLandTerritory(final PlayerID player,
      final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().match(t)) {
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

  static Match<Territory> territoryAllowsRocketsCanFlyOver(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (!territoryIsLand().match(t)) {
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

  public static Match<Unit> unitCanScrambleOnRouteDistance(final Route route) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getMaxScrambleDistance() >= route.getMovementCost(unit));
  }

  static Match<Unit> unitCanIntercept() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanIntercept());
  }

  static Match<Unit> unitCanEscort() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanEscort());
  }

  static Match<Unit> unitCanAirBattle() {
    return Match.of(u -> UnitAttachment.get(u.getType()).getCanAirBattle());
  }

  static Match<Territory> territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(
      final PlayerID attacker) {
    return Match.of(t -> {
      if (t.getOwner().equals(attacker)) {
        return false;
      }
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(attacker, t.getData()).match(t)) {
        return false;
      }
      return relationshipTypeCanTakeOverOwnedTerritory()
          .match(t.getData().getRelationshipTracker().getRelationshipType(attacker, t.getOwner()));
    });
  }

  static Match<Territory> territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(final PlayerID movingPlayer) {
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

  public static Match<Unit> unitCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAa, final boolean doNotIncludeBombardingSeaUnits) {
    return Match.of(unit -> unitTypeCanBeInBattle(attack, isLandBattle, unit.getOwner(), battleRound,
        includeAttackersThatCanNotMove, doNotIncludeAa, doNotIncludeBombardingSeaUnits).match(unit.getType()));
  }

  public static Match<UnitType> unitTypeCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final PlayerID player, final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAa, final boolean doNotIncludeBombardingSeaUnits) {

    // Filter out anything like factories, or units that have no combat ability AND cannot be taken casualty
    final PredicateBuilder<UnitType> canBeInBattleBuilder = PredicateBuilder.of(unitTypeIsInfrastructure().invert())
        .or(unitTypeIsSupporterOrHasCombatAbility(attack, player))
        .orIf(!doNotIncludeAa, Match.allOf(unitTypeIsAaForCombatOnly(), unitTypeIsAaThatCanFireOnRound(battleRound)));

    if (attack) {
      if (!includeAttackersThatCanNotMove) {
        canBeInBattleBuilder
            .and(unitTypeCanNotMoveDuringCombatMove().invert())
            .and(unitTypeCanMove(player));
      }
      if (isLandBattle) {
        if (doNotIncludeBombardingSeaUnits) {
          canBeInBattleBuilder.and(unitTypeIsSea().invert());
        }
      } else { // is sea battle
        canBeInBattleBuilder.and(unitTypeIsLand().invert());
      }
    } else { // defense
      canBeInBattleBuilder.and((isLandBattle ? unitTypeIsSea() : unitTypeIsLand()).invert());
    }

    return Match.of(canBeInBattleBuilder.build());
  }

  static Match<Unit> unitIsAirborne() {
    return Match.of(obj -> ((TripleAUnit) obj).getAirborne());
  }

  public static <T> Match<T> isNotInList(final List<T> list) {
    return Match.of(not(list::contains));
  }
}
