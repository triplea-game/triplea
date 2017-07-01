package games.strategy.triplea.delegate;

import static games.strategy.util.PredicateUtils.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * boolean hasLand = Match.someMatch(someCollection, Matches.UnitIsAir);
 * </pre>
 *
 * <p>
 * The benefits should be obvious to any right minded person.
 * </p>
 */
public class Matches {
  public static final Match<Object> IsTerritory = Match.of(o -> o != null && o instanceof Territory);

  public static final Match<UnitType> UnitTypeHasMoreThanOneHitPointTotal =
      Match.of(ut -> UnitAttachment.get(ut).getHitPoints() > 1);

  public static final Match<Unit> UnitHasMoreThanOneHitPointTotal =
      Match.of(unit -> UnitTypeHasMoreThanOneHitPointTotal.match(unit.getType()));

  public static final Match<Unit> UnitHasTakenSomeDamage = Match.of(unit -> unit.getHits() > 0);

  public static final Match<Unit> UnitHasNotTakenAnyDamage = UnitHasTakenSomeDamage.invert();

  public static final Match<Unit> UnitHasOnlyOneHitPointLeft =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getHitPoints() - unit.getHits() <= 1);

  public static final Match<Unit> UnitIsSea = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSea());

  public static final Match<Unit> UnitIsSub = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSub());

  public static final Match<Unit> UnitIsNotSub = UnitIsSub.invert();

  public static final Match<Unit> UnitIsCombatTransport = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return (ua.getIsCombatTransport() && ua.getIsSea());
  });

  public static final Match<Unit> UnitIsNotCombatTransport = UnitIsCombatTransport.invert();

  public static final Match<Unit> UnitIsTransportButNotCombatTransport = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return (ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport());
  });

  public static final Match<Unit> UnitIsNotTransportButCouldBeCombatTransport = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua.getTransportCapacity() == -1) {
      return true;
    } else {
      return ua.getIsCombatTransport() && ua.getIsSea();
    }
  });

  public static final Match<Unit> UnitIsDestroyer =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getIsDestroyer());

  public static final Match<UnitType> UnitTypeIsDestroyer = Match.of(type -> UnitAttachment.get(type).getIsDestroyer());

  public static final Match<Unit> UnitIsTransport = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return (ua.getTransportCapacity() != -1 && ua.getIsSea());
  });

  public static final Match<Unit> UnitIsNotTransport = UnitIsTransport.invert();

  public static final Match<Unit> UnitIsTransportAndNotDestroyer = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return (!Matches.UnitIsDestroyer.match(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea());
  });

  public static final Match<UnitType> UnitTypeIsStrategicBomber = Match.of(obj -> {
    final UnitAttachment ua = UnitAttachment.get(obj);
    if (ua == null) {
      return false;
    }
    return ua.getIsStrategicBomber();
  });

  public static final Match<Unit> UnitIsStrategicBomber =
      Match.of(obj -> UnitTypeIsStrategicBomber.match(obj.getType()));

  public static final Match<Unit> UnitIsNotStrategicBomber = UnitIsStrategicBomber.invert();

  public static final Match<UnitType> UnitTypeCanLandOnCarrier = Match.of(obj -> {
    final UnitAttachment ua = UnitAttachment.get(obj);
    if (ua == null) {
      return false;
    }
    return ua.getCarrierCost() != -1;
  });

  public static final Match<UnitType> UnitTypeCannotLandOnCarrier = UnitTypeCanLandOnCarrier.invert();

  public static final Match<Unit> unitHasMoved = Match.of(unit -> TripleAUnit.get(unit).getAlreadyMoved() > 0);

  public static final Match<Unit> unitHasNotMoved = unitHasMoved.invert();

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

  public static final Match<Unit> UnitIsNotSea = Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsSea());

  public static final Match<UnitType> UnitTypeIsSea = Match.of(type -> UnitAttachment.get(type).getIsSea());

  public static final Match<UnitType> UnitTypeIsNotSea = Match.of(type -> !UnitAttachment.get(type).getIsSea());

  public static final Match<UnitType> UnitTypeIsSeaOrAir = Match.of(type -> {
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsSea() || ua.getIsAir();
  });

  public static final Match<UnitType> UnitTypeIsCarrier =
      Match.of(type -> UnitAttachment.get(type).getCarrierCapacity() != -1);

  public static final Match<Unit> UnitIsAir = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAir());

  public static final Match<Unit> UnitIsNotAir = Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsAir());

  public static Match<UnitType> unitTypeCanBombard(final PlayerID id) {
    return Match.of(type -> UnitAttachment.get(type).getCanBombard(id));
  }

  static Match<Unit> unitCanBeGivenByTerritoryTo(final PlayerID player) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBeGivenByTerritoryTo().contains(player));
  }

  static Match<Unit> unitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr,
      final GameData data) {
    return Match.of(unit -> {
      if (!games.strategy.triplea.Properties.getCaptureUnitsOnEnteringTerritory(data)) {
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
    return Match.any(unitDestroyedWhenCapturedBy(playerBy), unitDestroyedWhenCapturedFrom());
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

  public static final Match<Unit> UnitIsAirBase = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsAirBase());

  public static final Match<UnitType> UnitTypeCanBeDamaged = Match.of(ut -> UnitAttachment.get(ut).getCanBeDamaged());

  public static final Match<Unit> UnitCanBeDamaged = Match.of(unit -> UnitTypeCanBeDamaged.match(unit.getType()));

  static Match<Unit> unitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
    return Match.of(unit -> {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return true;
      }
      if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())) {
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
      final HashSet<UnitType> allowedTargets = ua.getBombingTargets(bomberOrRocket.getData());
      return allowedTargets == null || allowedTargets.contains(unit.getType());
    });
  }

  public static final Match<Unit> UnitHasTakenSomeBombingUnitDamage =
      Match.of(unit -> ((TripleAUnit) unit).getUnitDamage() > 0);

  public static final Match<Unit> UnitHasNotTakenAnyBombingUnitDamage = UnitHasTakenSomeBombingUnitDamage.invert();

  public static final Match<Unit> UnitIsDisabled = Match.of(unit -> {
    if (!UnitCanBeDamaged.match(unit)) {
      return false;
    }
    if (!games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(unit.getData())) {
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

  public static final Match<Unit> UnitIsNotDisabled = UnitIsDisabled.invert();

  public static final Match<Unit> UnitCanDieFromReachingMaxDamage = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (!ua.getCanBeDamaged()) {
      return false;
    }
    return ua.getCanDieFromReachingMaxDamage();
  });

  public static final Match<UnitType> UnitTypeIsInfrastructure =
      Match.of(ut -> UnitAttachment.get(ut).getIsInfrastructure());

  public static final Match<Unit> UnitIsInfrastructure =
      Match.of(unit -> UnitTypeIsInfrastructure.match(unit.getType()));

  public static final Match<Unit> UnitIsNotInfrastructure = UnitIsInfrastructure.invert();

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  public static Match<Unit> unitIsSupporterOrHasCombatAbility(final boolean attack) {
    return Match.of(
        unit -> Matches.unitTypeIsSupporterOrHasCombatAbility(attack, unit.getOwner()).match(unit.getType()));
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

  public static final Match<Unit> UnitCanScramble =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getCanScramble());

  public static final Match<Unit> UnitWasScrambled = Match.of(obj -> ((TripleAUnit) obj).getWasScrambled());

  public static final Match<Unit> UnitWasInAirBattle = Match.of(obj -> ((TripleAUnit) obj).getWasInAirBattle());

  public static Match<Unit> unitCanBombard(final PlayerID id) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBombard(id));
  }

  public static final Match<Unit> UnitCanBlitz =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getCanBlitz(unit.getOwner()));

  public static final Match<Unit> UnitIsLandTransport =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getIsLandTransport());

  static Match<Unit> unitIsNotInfrastructureAndNotCapturedOnEntering(final PlayerID player,
      final Territory terr, final GameData data) {
    return Match.of(unit -> !UnitAttachment.get(unit.getType()).getIsInfrastructure()
        && !unitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).match(unit));
  }

  public static final Match<Unit> UnitIsSuicide = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsSuicide());

  public static final Match<Unit> UnitIsKamikaze = Match.of(unit -> UnitAttachment.get(unit.getType()).getIsKamikaze());

  public static final Match<UnitType> UnitTypeIsAir = Match.of(type -> UnitAttachment.get(type).getIsAir());

  public static final Match<UnitType> UnitTypeIsNotAir = Match.of(type -> !UnitAttachment.get(type).getIsAir());

  public static final Match<Unit> UnitCanLandOnCarrier =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCost() != -1);

  public static final Match<Unit> UnitIsCarrier =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1);

  static Match<Territory> territoryHasOwnedCarrier(final PlayerID player) {
    return Match.of(t -> t.getUnits().someMatch(Match.all(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier)));
  }

  public static Match<Unit> unitIsAlliedCarrier(final PlayerID player, final GameData data) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
        && data.getRelationshipTracker().isAllied(player, unit.getOwner()));
  }

  public static final Match<Unit> UnitCanBeTransported =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCost() != -1);

  public static final Match<Unit> UnitCanNotBeTransported = UnitCanBeTransported.invert();

  public static final Match<Unit> UnitWasAmphibious = Match.of(obj -> ((TripleAUnit) obj).getWasAmphibious());

  public static final Match<Unit> UnitWasNotAmphibious = UnitWasAmphibious.invert();

  public static final Match<Unit> UnitWasInCombat = Match.of(obj -> ((TripleAUnit) obj).getWasInCombat());

  public static final Match<Unit> UnitWasUnloadedThisTurn =
      Match.of(obj -> ((TripleAUnit) obj).getUnloadedTo() != null);

  public static final Match<Unit> UnitWasLoadedThisTurn = Match.of(obj -> ((TripleAUnit) obj).getWasLoadedThisTurn());

  public static final Match<Unit> UnitWasNotLoadedThisTurn = UnitWasLoadedThisTurn.invert();

  public static final Match<Unit> UnitCanTransport =
      Match.of(unit -> UnitAttachment.get(unit.getType()).getTransportCapacity() != -1);

  public static final Match<UnitType> UnitTypeCanTransport =
      Match.of(type -> UnitAttachment.get(type).getTransportCapacity() != -1);

  public static final Match<UnitType> UnitTypeCanBeTransported =
      Match.of(type -> UnitAttachment.get(type).getTransportCost() != -1);

  public static final Match<UnitType> UnitTypeCanProduceUnits =
      Match.of(obj -> UnitAttachment.get(obj).getCanProduceUnits());

  public static final Match<Unit> UnitCanProduceUnits = Match.of(obj -> UnitTypeCanProduceUnits.match(obj.getType()));

  public static final Match<Unit> UnitCanNotProduceUnits = UnitCanProduceUnits.invert();

  public static final Match<UnitType> UnitTypeIsInfantry = Match.of(type -> UnitAttachment.get(type).getIsInfantry());

  public static final Match<UnitType> UnitTypeIsArtillery = Match.of(type -> UnitAttachment.get(type).getArtillery());

  public static final Match<UnitType> UnitTypeHasMaxBuildRestrictions =
      Match.of(type -> UnitAttachment.get(type).getMaxBuiltPerPlayer() >= 0);

  public static final Match<UnitType> UnitTypeIsRocket = Match.of(obj -> UnitAttachment.get(obj).getIsRocket());

  public static final Match<Unit> UnitIsRocket = Match.of(obj -> UnitTypeIsRocket.match(obj.getType()));

  public static final Match<Unit> UnitHasPlacementLimit = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getPlacementLimit() != null;
  });

  public static final Match<Unit> UnitHasMovementLimit = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getMovementLimit() != null;
  });

  public static final Match<Unit> UnitHasAttackingLimit = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getAttackingLimit() != null;
  });

  public static final Match<UnitType> UnitTypeCanNotMoveDuringCombatMove =
      Match.of(type -> UnitAttachment.get(type).getCanNotMoveDuringCombatMove());

  public static final Match<Unit> UnitCanNotMoveDuringCombatMove =
      Match.of(obj -> UnitTypeCanNotMoveDuringCombatMove.match(obj.getType()));

  public static Match<Unit> unitIsAaThatCanHitTheseUnits(final Collection<Unit> targets,
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
      return Match.someMatch(targets, Match.all(Matches.UnitIsAirborne,
          Matches.unitIsOfTypes(airborneTechTargetsAllowed.get(ua.getTypeAA()))));
    });
  }

  static Match<Unit> unitIsAaOfTypeAa(final String typeAa) {
    return Match.of(obj -> UnitAttachment.get(obj.getType()).getTypeAA().matches(typeAa));
  }

  public static final Match<Unit> UnitAAShotDamageableInsteadOfKillingInstantly =
      Match.of(obj -> UnitAttachment.get(obj.getType()).getDamageableAA());

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
    return Match.all(Matches.enemyUnit(playerMovingOrAttacking, data),
        Matches.unitIsBeingTransported().invert(),
        Matches.unitIsAaThatCanHitTheseUnits(unitsMovingOrAttacking, typeOfAa, airborneTechTargetsAllowed),
        Matches.unitIsAaThatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).invert(),
        Matches.unitIsAaThatCanFireOnRound(battleRoundNumber),
        (defending ? UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero
            : UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero));
  }

  public static final Match<UnitType> UnitTypeIsAAforCombatOnly =
      Match.of(obj -> UnitAttachment.get(obj).getIsAAforCombatOnly());

  public static final Match<Unit> UnitIsAAforCombatOnly =
      Match.of(obj -> UnitTypeIsAAforCombatOnly.match(obj.getType()));

  public static final Match<UnitType> UnitTypeIsAAforBombingThisUnitOnly =
      Match.of(obj -> UnitAttachment.get(obj).getIsAAforBombingThisUnitOnly());

  public static final Match<Unit> UnitIsAAforBombingThisUnitOnly =
      Match.of(obj -> UnitTypeIsAAforBombingThisUnitOnly.match(obj.getType()));

  public static final Match<UnitType> UnitTypeIsAAforFlyOverOnly =
      Match.of(obj -> UnitAttachment.get(obj).getIsAAforFlyOverOnly());

  public static final Match<Unit> UnitIsAAforFlyOverOnly =
      Match.of(obj -> UnitTypeIsAAforFlyOverOnly.match(obj.getType()));

  public static final Match<UnitType> UnitTypeIsAAforAnything = Match.of(obj -> {
    final UnitAttachment ua = UnitAttachment.get(obj);
    return ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly() || ua.getIsAAforFlyOverOnly();
  });

  public static final Match<Unit> UnitIsAAforAnything = Match.of(obj -> UnitTypeIsAAforAnything.match(obj.getType()));

  public static final Match<Unit> UnitIsNotAA = UnitIsAAforAnything.invert();

  public static final Match<UnitType> UnitTypeMaxAAattacksIsInfinite =
      Match.of(obj -> UnitAttachment.get(obj).getMaxAAattacks() == -1);

  public static final Match<Unit> UnitMaxAAattacksIsInfinite =
      Match.of(obj -> UnitTypeMaxAAattacksIsInfinite.match(obj.getType()));

  public static final Match<UnitType> UnitTypeMayOverStackAA =
      Match.of(obj -> UnitAttachment.get(obj).getMayOverStackAA());

  public static final Match<Unit> UnitMayOverStackAA = Match.of(obj -> UnitTypeMayOverStackAA.match(obj.getType()));

  public static final Match<Unit> UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero = Match.of(obj -> {
    final UnitAttachment ua = UnitAttachment.get(obj.getType());
    return ua.getAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
  });

  public static final Match<Unit> UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero = Match.of(obj -> {
    final UnitAttachment ua = UnitAttachment.get(obj.getType());
    return ua.getOffensiveAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
  });

  public static final Match<Unit> UnitIsInfantry = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsInfantry();
  });

  public static final Match<Unit> UnitIsNotInfantry = UnitIsInfantry.invert();

  public static final Match<Unit> UnitHasMarinePositiveBonus = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsMarine() > 0;
  });

  public static final Match<Unit> UnitHasMarineNegativeBonus = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsMarine() < 0;
  });

  public static final Match<Unit> UnitIsNotMarine = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsMarine() == 0;
  });

  public static final Match<Unit> UnitIsAirTransportable = Match.of(obj -> {
    final TechAttachment ta = TechAttachment.get(obj.getOwner());
    if (ta == null || !ta.getParatroopers()) {
      return false;
    }
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsAirTransportable();
  });

  public static final Match<Unit> UnitIsNotAirTransportable = UnitIsAirTransportable.invert();

  public static final Match<Unit> UnitIsAirTransport = Match.of(obj -> {
    final TechAttachment ta = TechAttachment.get(obj.getOwner());
    if (ta == null || !ta.getParatroopers()) {
      return false;
    }
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getIsAirTransport();
  });

  public static final Match<Unit> UnitIsNotAirTransport = UnitIsAirTransport.invert();

  public static final Match<Unit> UnitIsArtillery = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getArtillery();
  });

  public static final Match<Unit> UnitIsArtillerySupportable = Match.of(obj -> {
    final UnitType type = obj.getUnitType();
    final UnitAttachment ua = UnitAttachment.get(type);
    return ua.getArtillerySupportable();
  });

  // TODO: CHECK whether this makes any sense
  public static final Match<Territory> TerritoryIsLandOrWater = Match.of(Objects::nonNull);

  public static final Match<Territory> TerritoryIsWater = Match.of(Territory::isWater);

  public static final Match<Territory> TerritoryIsIsland = Match.of(t -> {
    final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
    return neighbors.size() == 1 && TerritoryIsWater.match(neighbors.iterator().next());
  });

  public static final Match<Territory> TerritoryIsVictoryCity = Match.of(t -> {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return false;
    }
    return ta.getVictoryCity() != 0;
  });

  public static final Match<Territory> TerritoryIsLand = TerritoryIsWater.invert();

  public static final Match<Territory> TerritoryIsEmpty = Match.of(t -> t.getUnits().size() == 0);

  /**
   * Tests for Land, Convoys Centers and Convoy Routes, and Contested Territories.
   * Assumes player is either the owner of the territory we are testing, or about to become the owner (ie: this doesn't
   * test ownership).
   * If the game option for contested territories not producing is on, then will also remove any contested territories.
   */
  public static Match<Territory> territoryCanCollectIncomeFrom(final PlayerID player, final GameData data) {
    final boolean contestedDoNotProduce =
        games.strategy.triplea.Properties.getContestedTerritoriesProduceNoIncome(data);
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
      return !(contestedDoNotProduce && !Matches.territoryHasNoEnemyUnits(player, data).match(t));
    });
  }

  public static Match<Territory> territoryHasNeighborMatching(final GameData data, final Match<Territory> match) {
    return Match.of(t -> data.getMap().getNeighbors(t, match).size() > 0);
  }

  public static Match<Territory> territoryHasEnemyLandNeighbor(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      // This method will still return true if territory t is an impassable or restricted territory With enemy
      // neighbors. Makes sure your
      // AI does not include any impassable or restricted territories by using this:
      // CompositeMatch<Territory> territoryHasEnemyLandNeighborAndIsNotImpassableOrRestricted = new
      // CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player),
      // Matches.territoryHasEnemyLandNeighbor(data,
      // player));
      final Match<Territory> condition = Match.all(Matches.TerritoryIsLand,
          Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player, data));
      return data.getMap().getNeighbors(t, condition).size() > 0;
    });
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
      for (final PlayerID ePlayer : data.getPlayerList().getPlayers()) {
        final List<Territory> capitalsListOwned =
            new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
        for (final Territory current : capitalsListOwned) {
          if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
            continue;
          }
          if (data.getMap().getDistance(t, current,
              Matches.territoryIsPassableAndNotRestricted(player, data)) != -1) {
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
      for (final PlayerID ePlayer : data.getPlayerList().getPlayers()) {
        final List<Territory> capitalsListOwned =
            new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
        for (final Territory current : capitalsListOwned) {
          if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
            continue;
          }
          if (data.getMap().getDistance(t, current,
              Matches.territoryIsNotImpassableToLandUnits(player, data)) != -1) {
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
        .getNeighbors(t, Matches.territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(data, player))
        .size() > 0);
  }

  public static Match<Territory> territoryHasWaterNeighbor(final GameData data) {
    return Match.of(t -> data.getMap().getNeighbors(t, TerritoryIsWater).size() > 0);
  }

  public static Match<Territory> territoryIsAlliedAndHasAlliedUnitMatching(final GameData data, final PlayerID player,
      final Match<Unit> unitMatch) {
    return Match.of(t -> {
      if (!data.getRelationshipTracker().isAllied(t.getOwner(), player)) {
        return false;
      }
      return t.getUnits().someMatch(Match.all(Matches.alliedUnit(player, data), unitMatch));
    });
  }

  public static Match<Territory> territoryIsOwnedAndHasOwnedUnitMatching(final PlayerID player,
      final Match<Unit> unitMatch) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().someMatch(Match.all(Matches.unitIsOwnedBy(player), unitMatch));
    });
  }

  public static Match<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      return t.getUnits().someMatch(Matches.UnitCanProduceUnits);
    });
  }

  private static Match<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(final GameData data,
      final PlayerID player) {
    return Match.of(t -> {
      if (!t.getOwner().equals(player)) {
        return false;
      }
      if (!t.getUnits().someMatch(Matches.UnitCanProduceUnits)) {
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
      return t.getUnits().someMatch(Matches.UnitCanProduceUnits);
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
      return t.getUnits().someMatch(Match.all(Matches.enemyUnit(player, data), unitMatch));
    });
  }

  static Match<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player) {
    return Match.of(t -> {
      final Match<Unit> nonCom = Match.any(UnitIsInfrastructure, enemyUnit(player, data).invert());
      return t.getUnits().allMatch(nonCom);
    });
  }

  public static final Match<Territory> TerritoryIsNeutralButNotWater = Match.of(t -> {
    if (t.isWater()) {
      return false;
    }
    return t.getOwner().equals(PlayerID.NULL_PLAYERID);
  });

  public static final Match<Territory> TerritoryIsNotNeutralButCouldBeWater = TerritoryIsNeutralButNotWater.invert();

  public static final Match<Territory> TerritoryIsImpassable = Match.of(t -> {
    if (t.isWater()) {
      return false;
    }
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    return ta != null && ta.getIsImpassable();
  });

  public static final Match<Territory> TerritoryIsNotImpassable = TerritoryIsImpassable.invert();

  static Match<Territory> seaCanMoveOver(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (!TerritoryIsWater.match(t)) {
        return false;
      }
      return territoryIsPassableAndNotRestricted(player, data).match(t);
    });
  }

  static Match<Territory> airCanFlyOver(final PlayerID player, final GameData data,
      final boolean areNeutralsPassableByAir) {
    return Match.of(t -> {
      if (!areNeutralsPassableByAir && TerritoryIsNeutralButNotWater.match(t)) {
        return false;
      }
      if (!territoryIsPassableAndNotRestricted(player, data).match(t)) {
        return false;
      }
      return !(TerritoryIsLand.match(t)
          && !data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
    });
  }

  public static Match<Territory> territoryIsPassableAndNotRestricted(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (Matches.TerritoryIsImpassable.match(t)) {
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
      } else if (Matches.territoryIsPassableAndNotRestricted(player, data).invert().match(t)) {
        return true;
      }
      return false;
    });
  }

  public static Match<Territory> territoryIsNotImpassableToLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> territoryIsImpassableToLandUnits(player, data).invert().match(t));
  }

  /**
   * Does NOT check for Canals, Blitzing, Loading units on transports, TerritoryEffects that disallow units, Stacking
   * Limits, Unit movement
   * left, Fuel available, etc.<br>
   * <br>
   * Does check for: Impassable, ImpassableNeutrals, ImpassableToAirNeutrals, RestrictedTerritories, Land units moving
   * on water, Sea units
   * moving on land,
   * and territories that are disallowed due to a relationship attachment (canMoveLandUnitsOverOwnedLand,
   * canMoveAirUnitsOverOwnedLand,
   * canLandAirUnitsOnOwnedLand, canMoveIntoDuringCombatMove, etc).
   */
  public static Match<Territory> territoryIsPassableAndNotRestrictedAndOkByRelationships(
      final PlayerID playerWhoOwnsAllTheUnitsMoving, final GameData data, final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded, final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported, final boolean isLandingZoneOnLandForAirUnits) {
    final boolean neutralsPassable = !games.strategy.triplea.Properties.getNeutralsImpassable(data);
    final boolean areNeutralsPassableByAir =
        neutralsPassable && games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data);
    return Match.of(t -> {
      if (Matches.TerritoryIsImpassable.match(t)) {
        return false;
      }
      if ((!neutralsPassable || (hasAirUnitsNotBeingTransported && !areNeutralsPassableByAir))
          && TerritoryIsNeutralButNotWater.match(t)) {
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
      final boolean isWater = Matches.TerritoryIsWater.match(t);
      final boolean isLand = Matches.TerritoryIsLand.match(t);
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

  public static final Match<IBattle> BattleIsEmpty = Match.of(IBattle::isEmpty);

  public static final Match<IBattle> BattleIsAmphibious = Match.of(IBattle::isAmphibious);

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
  public static final Match<Unit> unitHasMovementLeft = Match.of(o -> TripleAUnit.get(o).getMovementLeft() >= 1);

  public static final Match<Unit> UnitCanMove = Match.of(u -> unitTypeCanMove(u.getOwner()).match(u.getType()));

  private static Match<UnitType> unitTypeCanMove(final PlayerID player) {
    return Match.of(obj -> UnitAttachment.get(obj).getMovement(player) > 0);
  }

  public static final Match<Unit> UnitIsStatic = UnitCanMove.invert();

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
    return Match.all(UnitIsAAforAnything, enemyUnit(player, data));
  }

  private static Match<Unit> unitIsEnemyAaForCombat(final PlayerID player, final GameData data) {
    return Match.all(UnitIsAAforCombatOnly, enemyUnit(player, data));
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
      if (!Matches.territoryIsPassableAndNotRestricted(player, data).match(t)) {
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
          && !games.strategy.triplea.Properties.getNeutralsBlitzable(data)) {
        return false;
      }
      // was conquered but not blitzed
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
          && !AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(t)) {
        return false;
      }
      final Match.CompositeBuilder<Unit> blitzableUnitsBuilder = Match.newCompositeBuilder(
          // we ignore neutral units
          Matches.enemyUnit(player, data).invert());
      // WW2V2, cant blitz through factories and aa guns
      // WW2V1, you can
      if (!games.strategy.triplea.Properties.getWW2V2(data)
          && !games.strategy.triplea.Properties.getBlitzThroughFactoriesAndAARestricted(data)) {
        blitzableUnitsBuilder.add(Matches.UnitIsInfrastructure);
      }
      return t.getUnits().allMatch(blitzableUnitsBuilder.any());
    });
  }

  public static Match<Territory> isTerritoryFreeNeutral(final GameData data) {
    return Match.of(t -> t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) <= 0);
  }

  public static Match<Territory> territoryDoesNotCostMoneyToEnter(final GameData data) {
    return Match.of(t -> TerritoryIsLand.invert().match(t) || !t.getOwner().equals(PlayerID.NULL_PLAYERID)
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
      if (Matches.unitIsOwnedByOfAnyOfThesePlayers(players).match(unit)) {
        return true;
      }
      return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(unit.getOwner(), players);
    });
  }

  public static Match<Territory> territoryIs(final Territory test) {
    return Match.of(t -> t.equals(test));
  }

  public static Match<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player) {
    return Match.of(t -> t.getUnits().someMatch(Match.all(unitIsOwnedBy(player), UnitIsLand)));
  }

  public static Match<Territory> territoryHasUnitsOwnedBy(final PlayerID player) {
    final Match<Unit> unitOwnedBy = unitIsOwnedBy(player);
    return Match.of(t -> t.getUnits().someMatch(unitOwnedBy));
  }

  public static Match<Territory> territoryHasUnitsThatMatch(final Match<Unit> cond) {
    return Match.of(t -> t.getUnits().someMatch(cond));
  }

  public static Match<Territory> territoryHasEnemyAaForAnything(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(unitIsEnemyAaForAnything(player, data)));
  }

  public static Match<Territory> territoryHasEnemyAaForCombatOnly(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(unitIsEnemyAaForCombat(player, data)));
  }

  public static Match<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> !t.getUnits().someMatch(enemyUnit(player, data)));
  }

  public static Match<Territory> territoryHasAlliedUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(alliedUnit(player, data)));
  }

  static Match<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data) {
    final Match<Unit> match = Match.all(enemyUnit(player, data), UnitIsSubmerged.invert());
    return Match.of(t -> t.getUnits().someMatch(match));
  }

  public static Match<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(Match.all(enemyUnit(player, data), UnitIsLand)));
  }

  public static Match<Territory> territoryHasEnemySeaUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(Match.all(enemyUnit(player, data), UnitIsSea)));
  }

  public static Match<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> t.getUnits().someMatch(enemyUnit(player, data)));
  }

  /**
   * The territory is owned by the enemy of those enemy units (ie: probably owned by you or your ally, but not
   * necessarily so in an FFA type
   * game) and is not unowned water.
   */
  public static Match<Territory> territoryHasEnemyUnitsThatCanCaptureTerritoryAndTerritoryOwnedByTheirEnemyAndIsNotUnownedWater(
      final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (t.getOwner() == null) {
        return false;
      }
      if (t.isWater() && TerritoryAttachment.get(t) == null && t.getOwner().isNull()) {
        return false;
      }
      final Set<PlayerID> enemies = new HashSet<>();
      for (final Unit u : t.getUnits()
          .getMatches(Match.all(enemyUnit(player, data), UnitIsNotAir, UnitIsNotInfrastructure))) {
        enemies.add(u.getOwner());
      }
      return (Matches.isAtWarWithAnyOfThesePlayers(enemies, data)).match(t.getOwner());
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
        final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
        final Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
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

  public static final Match<Unit> UnitIsLand = Match.all(UnitIsNotSea, UnitIsNotAir);

  public static final Match<UnitType> UnitTypeIsLand = Match.all(UnitTypeIsNotSea, UnitTypeIsNotAir);

  public static final Match<Unit> UnitIsNotLand = UnitIsLand.invert();

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

  public static final Match<Unit> UnitIsSubmerged = Match.of(u -> TripleAUnit.get(u).getSubmerged());

  public static final Match<UnitType> UnitTypeIsSub = Match.of(type -> UnitAttachment.get(type).getIsSub());

  static Match<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return Match.of(u -> TechTracker.hasImprovedArtillerySupport(u.getOwner()));
  }

  public static Match<Territory> territoryHasNonAllowedCanal(final PlayerID player, final Collection<Unit> unitsMoving,
      final GameData data) {
    return Match.of(t -> MoveValidator.validateCanal(t, null, unitsMoving, player, data).isPresent());
  }

  public static Match<Territory> territoryIsBlockedSea(final PlayerID player, final GameData data) {
    final Match<Unit> sub = Match.all(Matches.UnitIsSub.invert());
    final Match<Unit> transport =
        Match.all(Matches.UnitIsTransportButNotCombatTransport.invert(), Matches.UnitIsLand.invert());
    final Match.CompositeBuilder<Unit> unitCondBuilder = Match.newCompositeBuilder(
        Matches.UnitIsInfrastructure.invert(),
        Matches.alliedUnit(player, data).invert());
    if (Properties.getIgnoreTransportInMovement(data)) {
      unitCondBuilder.add(transport);
    }
    if (Properties.getIgnoreSubInMovement(data)) {
      unitCondBuilder.add(sub);
    }
    return Match.all(
        Matches.territoryHasUnitsThatMatch(unitCondBuilder.all()).invert(),
        Matches.TerritoryIsWater);
  }

  public static final Match<Unit> UnitCanRepairOthers = Match.of(unit -> {
    if (UnitIsDisabled.match(unit)) {
      return false;
    }
    if (Matches.unitIsBeingTransported().match(unit)) {
      return false;
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua.getRepairsUnits() == null) {
      return false;
    }
    return !ua.getRepairsUnits().isEmpty();
  });

  static Match<Unit> unitCanRepairThisUnit(final Unit damagedUnit) {
    return Match.of(unitCanRepair -> {
      final UnitType type = unitCanRepair.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      // TODO: make sure the unit is operational
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
      final Match<Unit> damaged = Match.all(Matches.UnitHasMoreThanOneHitPointTotal, Matches.UnitHasTakenSomeDamage);
      if (!damaged.match(damagedUnit)) {
        return false;
      }
      final Match<Unit> repairUnit = Match.all(Matches.alliedUnit(player, data),
          Matches.UnitCanRepairOthers, Matches.unitCanRepairThisUnit(damagedUnit));
      if (Match.someMatch(territory.getUnits().getUnits(), repairUnit)) {
        return true;
      }
      if (Matches.UnitIsSea.match(damagedUnit)) {
        final Match<Unit> repairUnitLand = Match.all(repairUnit, Matches.UnitIsLand);
        final List<Territory> neighbors =
            new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
        for (final Territory current : neighbors) {
          if (Match.someMatch(current.getUnits().getUnits(), repairUnitLand)) {
            return true;
          }
        }
      } else if (Matches.UnitIsLand.match(damagedUnit)) {
        final Match<Unit> repairUnitSea = Match.all(repairUnit, Matches.UnitIsSea);
        final List<Territory> neighbors =
            new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsWater));
        for (final Territory current : neighbors) {
          if (Match.someMatch(current.getUnits().getUnits(), repairUnitSea)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  public static final Match<Unit> UnitCanGiveBonusMovement = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua == null) {
      return false;
    }
    return ua.getGivesMovement().size() > 0 && Matches.unitIsBeingTransported().invert().match(unit);
  });

  static Match<Unit> unitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return Match.of(unitCanGiveBonusMovement -> {
      if (UnitIsDisabled.match(unitCanGiveBonusMovement)) {
        return false;
      }
      final UnitType type = unitCanGiveBonusMovement.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      // TODO: make sure the unit is operational
      return UnitCanGiveBonusMovement.match(unitCanGiveBonusMovement)
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
      final Match<Unit> givesBonusUnit = Match.all(Matches.alliedUnit(player, data),
          unitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
      if (Match.someMatch(territory.getUnits().getUnits(), givesBonusUnit)) {
        return true;
      }
      if (Matches.UnitIsSea.match(unitWhichWillGetBonus)) {
        final Match<Unit> givesBonusUnitLand = Match.all(givesBonusUnit, Matches.UnitIsLand);
        final List<Territory> neighbors =
            new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
        for (final Territory current : neighbors) {
          if (Match.someMatch(current.getUnits().getUnits(), givesBonusUnitLand)) {
            return true;
          }
        }
      }
      return false;
    });
  }

  public static final Match<Unit> UnitCreatesUnits = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua == null) {
      return false;
    }
    return (ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0);
  });

  public static final Match<Unit> UnitCreatesResources = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua == null) {
      return false;
    }
    return (ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0);
  });

  public static final Match<UnitType> UnitTypeConsumesUnitsOnCreation = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit);
    if (ua == null) {
      return false;
    }
    return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
  });

  public static final Match<Unit> UnitConsumesUnitsOnCreation = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua == null) {
      return false;
    }
    return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
  });

  static Match<Unit> unitWhichConsumesUnitsHasRequiredUnits(final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!Matches.UnitConsumesUnitsOnCreation.match(unitWhichRequiresUnits)) {
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
      final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
      boolean canBuild = true;
      for (final UnitType ut : requiredUnits) {
        final Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = Match.all(
            Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.unitIsOfType(ut),
            Matches.UnitHasNotTakenAnyBombingUnitDamage, Matches.UnitHasNotTakenAnyDamage, Matches.UnitIsNotDisabled);
        final int requiredNumber = requiredUnitsMap.getInt(ut);
        final int numberInTerritory =
            Match.countMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndOfTypeAndNotDamaged);
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

  public static final Match<Unit> UnitRequiresUnitsOnCreation = Match.of(unit -> {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    if (ua == null) {
      return false;
    }
    return (ua.getRequiresUnits() != null && ua.getRequiresUnits().size() > 0);
  });

  public static Match<Unit> unitWhichRequiresUnitsHasRequiredUnitsInList(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits)) {
        return true;
      }
      final Match<Unit> unitIsOwnedByAndNotDisabled = Match.all(
          Matches.unitIsOwnedBy(unitWhichRequiresUnits.getOwner()), Matches.UnitIsNotDisabled);
      unitsInTerritoryAtStartOfTurn
          .retainAll(Match.getMatches(unitsInTerritoryAtStartOfTurn, unitIsOwnedByAndNotDisabled));
      boolean canBuild = false;
      final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
      final ArrayList<String[]> unitComboPossibilities = ua.getRequiresUnits();
      for (final String[] combo : unitComboPossibilities) {
        if (combo != null) {
          boolean haveAll = true;
          final Collection<UnitType> requiredUnits = ua.getListedUnits(combo);
          for (final UnitType ut : requiredUnits) {
            if (Match.countMatches(unitsInTerritoryAtStartOfTurn, Matches.unitIsOfType(ut)) < 1) {
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

  public static final Match<Territory> territoryIsBlockadeZone = Match.of(t -> {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta != null) {
      return ta.getBlockadeZone();
    }
    return false;
  });

  public static final Match<UnitType> UnitTypeIsConstruction = Match.of(type -> {
    final UnitAttachment ua = UnitAttachment.get(type);
    if (ua == null) {
      return false;
    }
    return ua.getIsConstruction();
  });

  public static final Match<Unit> UnitIsConstruction = Match.of(obj -> UnitTypeIsConstruction.match(obj.getType()));

  public static final Match<Unit> UnitIsNotConstruction = UnitIsConstruction.invert();

  public static final Match<Unit> UnitCanProduceUnitsAndIsConstruction =
      Match.all(UnitCanProduceUnits, UnitIsConstruction);
  public static final Match<UnitType> UnitTypeCanProduceUnitsAndIsConstruction =
      Match.all(UnitTypeCanProduceUnits, UnitTypeIsConstruction);
  public static final Match<Unit> UnitCanProduceUnitsAndIsInfrastructure =
      Match.all(UnitCanProduceUnits, UnitIsInfrastructure);
  public static final Match<Unit> UnitCanProduceUnitsAndCanBeDamaged =
      Match.all(UnitCanProduceUnits, UnitCanBeDamaged);

  /**
   * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from any other unit.
   * Otherwise, units must have
   * a specific unit in this list to be able to invade from that unit.
   */
  public static final Match<Unit> UnitCanInvade = Match.of(unit -> {
    // is the unit being transported?
    final Unit transport = TripleAUnit.get(unit).getTransportedBy();
    if (transport == null) {
      // Unit isn't transported so can Invade
      return true;
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    return ua.canInvadeFrom(transport.getUnitType().getName());
  });

  public static final Match<RelationshipType> RelationshipTypeIsAllied =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().isAllied());

  public static final Match<Relationship> RelationshipIsAllied =
      Match.of(relationship -> relationship.getRelationshipType().getRelationshipTypeAttachment().isAllied());

  public static final Match<RelationshipType> RelationshipTypeIsNeutral =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().isNeutral());

  public static final Match<Relationship> RelationshipIsNeutral =
      Match.of(relationship -> relationship.getRelationshipType().getRelationshipTypeAttachment().isNeutral());

  public static final Match<RelationshipType> RelationshipTypeIsAtWar =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().isWar());

  public static final Match<Relationship> RelationshipIsAtWar =
      Match.of(relationship -> relationship.getRelationshipType().getRelationshipTypeAttachment().isWar());

  public static final Match<RelationshipType> RelationshipTypeCanMoveLandUnitsOverOwnedLand =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanMoveLandUnitsOverOwnedLand());

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> territoryAllowsCanMoveLandUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!Matches.TerritoryIsLand.match(t)) {
        return true;
      }
      final PlayerID tOwner = t.getOwner();
      if (tOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(tOwner, ownerOfUnitsMoving);
    });
  }

  public static final Match<RelationshipType> RelationshipTypeCanMoveAirUnitsOverOwnedLand =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanMoveAirUnitsOverOwnedLand());

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> territoryAllowsCanMoveAirUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return Match.of(t -> {
      if (!Matches.TerritoryIsLand.match(t)) {
        return true;
      }
      final PlayerID tOwner = t.getOwner();
      if (tOwner == null) {
        return true;
      }
      return data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(tOwner, ownerOfUnitsMoving);
    });
  }

  public static final Match<RelationshipType> RelationshipTypeCanLandAirUnitsOnOwnedLand =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanLandAirUnitsOnOwnedLand());

  public static final Match<RelationshipType> RelationshipTypeCanTakeOverOwnedTerritory =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanTakeOverOwnedTerritory());

  public static final Match<RelationshipType> RelationshipTypeGivesBackOriginalTerritories =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getGivesBackOriginalTerritories());

  public static final Match<RelationshipType> RelationshipTypeCanMoveIntoDuringCombatMove =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanMoveIntoDuringCombatMove());

  public static final Match<RelationshipType> RelationshipTypeCanMoveThroughCanals =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getCanMoveThroughCanals());

  public static final Match<RelationshipType> RelationshipTypeRocketsCanFlyOver =
      Match.of(relationship -> relationship.getRelationshipTypeAttachment().getRocketsCanFlyOver());

  public static Match<String> isValidRelationshipName(final GameData data) {
    return Match.of(relationshipName -> data.getRelationshipTypeList().getRelationshipType(relationshipName) != null);
  }

  public static Match<PlayerID> isAtWar(final PlayerID player, final GameData data) {
    return Match.of(
        player2 -> RelationshipTypeIsAtWar.match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Match<PlayerID> isAtWarWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(player2, players));
  }

  public static Match<PlayerID> isAllied(final PlayerID player, final GameData data) {
    return Match.of(
        player2 -> RelationshipTypeIsAllied.match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static Match<PlayerID> isAlliedWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return Match.of(player2 -> data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(player2, players));
  }

  public static Match<Unit> unitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player) {
    return Match.of(unit -> UnitCanProduceUnits.match(unit) && unitIsOwnedBy(player).match(unit));
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
      final ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamagedList =
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

  public static final Match<Unit> UnitCanOnlyPlaceInOriginalTerritories = Match.of(u -> {
    final UnitAttachment ua = UnitAttachment.get(u.getType());
    final Set<String> specialOptions = ua.getSpecial();
    for (final String option : specialOptions) {
      if (option.equals("canOnlyPlaceInOriginalTerritories")) {
        return true;
      }
    }
    return false;
  });

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
    return Match.of(player2 -> RelationshipTypeIsAlliedAndAlliancesCanChainTogether
        .match(data.getRelationshipTracker().getRelationshipType(player, player2)));
  }

  public static final Match<RelationshipType> RelationshipTypeIsAlliedAndAlliancesCanChainTogether = Match.of(rt -> {
    return RelationshipTypeIsAllied.match(rt) && rt.getRelationshipTypeAttachment().getAlliancesCanChainTogether();
  });

  public static final Match<RelationshipType> RelationshipTypeIsDefaultWarPosition =
      Match.of(rt -> rt.getRelationshipTypeAttachment().getIsDefaultWarPosition());

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
        final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
        final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
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
        final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
        final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
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
      if (!Matches.TerritoryIsLand.match(t)) {
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
      if (!Matches.TerritoryIsLand.match(t)) {
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

  public static final Match<Unit> unitCanIntercept = Match.of(u -> UnitAttachment.get(u.getType()).getCanIntercept());

  public static final Match<Unit> unitCanEscort = Match.of(u -> UnitAttachment.get(u.getType()).getCanEscort());

  public static final Match<Unit> unitCanAirBattle = Match.of(u -> UnitAttachment.get(u.getType()).getCanAirBattle());

  static Match<Territory> territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(
      final PlayerID attacker) {
    return Match.of(t -> {
      if (t.getOwner().equals(attacker)) {
        return false;
      }
      if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
        return false;
      }
      if (!Matches.territoryIsPassableAndNotRestricted(attacker, t.getData()).match(t)) {
        return false;
      }
      return RelationshipTypeCanTakeOverOwnedTerritory
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
    return Match.of(ut -> {
      // Filter out anything like factories, or units that have no combat ability AND cannot be taken casualty
      final Match.CompositeBuilder<UnitType> canBeInBattle = Match.newCompositeBuilder();

      final Match.CompositeBuilder<UnitType> supportOrNotInfrastructureOrAa = Match.newCompositeBuilder();
      supportOrNotInfrastructureOrAa.add(Matches.UnitTypeIsInfrastructure.invert());
      supportOrNotInfrastructureOrAa.add(Matches.unitTypeIsSupporterOrHasCombatAbility(attack, player));
      if (!doNotIncludeAa) {
        supportOrNotInfrastructureOrAa.add(Match.all(Matches.UnitTypeIsAAforCombatOnly,
            Matches.unitTypeIsAaThatCanFireOnRound(battleRound)));
      }
      canBeInBattle.add(supportOrNotInfrastructureOrAa.any());

      if (attack) {
        if (!includeAttackersThatCanNotMove) {
          canBeInBattle.add(Matches.UnitTypeCanNotMoveDuringCombatMove.invert());
          canBeInBattle.add(Matches.unitTypeCanMove(player));
        }
        if (isLandBattle) {
          if (doNotIncludeBombardingSeaUnits) {
            canBeInBattle.add(Matches.UnitTypeIsSea.invert());
          }
        } else { // is sea battle
          canBeInBattle.add(Matches.UnitTypeIsLand.invert());
        }
      } else { // defense
        if (isLandBattle) {
          canBeInBattle.add(Matches.UnitTypeIsSea.invert());
        } else { // is sea battle
          canBeInBattle.add(Matches.UnitTypeIsLand.invert());
        }
      }

      return canBeInBattle.all().match(ut);
    });
  }

  public static final Match<Unit> UnitIsAirborne = Match.of(obj -> ((TripleAUnit) obj).getAirborne());

  public static <T> Match<T> isNotInList(final List<T> list) {
    return Match.of(not(list::contains));
  }

  /** Creates new Matches. */
  private Matches() {}
}
