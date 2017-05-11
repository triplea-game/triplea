package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
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
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
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
  public static final Match<Object> IsTerritory = new Match<Object>() {
    @Override
    public boolean match(final Object o) {
      return o != null && o instanceof Territory;
    }
  };
  public static final Match<Unit> UnitHasMoreThanOneHitPointTotal = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      return UnitTypeHasMoreThanOneHitPointTotal.match(unit.getType());
    }
  };
  public static final Match<UnitType> UnitTypeHasMoreThanOneHitPointTotal = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType ut) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      return ua.getHitPoints() > 1;
    }
  };
  public static final Match<Unit> UnitHasTakenSomeDamage = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      return unit.getHits() > 0;
    }
  };
  public static final Match<Unit> UnitHasNotTakenAnyDamage = new InverseMatch<>(UnitHasTakenSomeDamage);
  public static final Match<Unit> UnitHasOnlyOneHitPointLeft = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getHitPoints() - unit.getHits() <= 1;
    }
  };
  public static final Match<Unit> UnitIsSea = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsSea();
    }
  };
  public static final Match<Unit> UnitIsSub = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsSub();
    }
  };
  public static final Match<Unit> UnitIsNotSub = new InverseMatch<>(UnitIsSub);
  public static final Match<Unit> UnitIsCombatTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return (ua.getIsCombatTransport() && ua.getIsSea());
    }
  };
  public static final Match<Unit> UnitIsNotCombatTransport = new InverseMatch<>(UnitIsCombatTransport);
  public static final Match<Unit> UnitIsTransportButNotCombatTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return (ua.getTransportCapacity() != -1 && ua.getIsSea() && !ua.getIsCombatTransport());
    }
  };
  public static final Match<Unit> UnitIsNotTransportButCouldBeCombatTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getTransportCapacity() == -1) {
        return true;
      } else {
        return ua.getIsCombatTransport() && ua.getIsSea();
      }
    }
  };
  public static final Match<Unit> UnitIsDestroyer = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsDestroyer();
    }
  };
  public static final Match<UnitType> UnitTypeIsDestroyer = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsDestroyer();
    }
  };
  public static final Match<Unit> UnitIsTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return (ua.getTransportCapacity() != -1 && ua.getIsSea());
    }
  };
  public static final Match<Unit> UnitIsNotTransport = UnitIsTransport.invert();
  public static final Match<Unit> UnitIsTransportAndNotDestroyer = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return (!Matches.UnitIsDestroyer.match(unit) && ua.getTransportCapacity() != -1 && ua.getIsSea());
    }
  };
  public static final Match<UnitType> UnitTypeIsStrategicBomber = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getIsStrategicBomber();
    }
  };
  public static final Match<Unit> UnitIsStrategicBomber = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsStrategicBomber.match(obj.getType());
    }
  };
  public static final Match<Unit> UnitIsNotStrategicBomber = new InverseMatch<>(UnitIsStrategicBomber);
  public static final Match<UnitType> UnitTypeCanLandOnCarrier = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      if (ua == null) {
        return false;
      }
      return ua.getCarrierCost() != -1;
    }
  };
  public static final Match<UnitType> UnitTypeCannotLandOnCarrier =
      new InverseMatch<>(UnitTypeCanLandOnCarrier);
  public static final Match<Unit> unitHasMoved = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      return TripleAUnit.get(unit).getAlreadyMoved() > 0;
    }
  };
  public static final Match<Unit> unitHasNotMoved = new InverseMatch<>(unitHasMoved);

  public static Match<Unit> unitCanAttack(final PlayerID id) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        if (ua.getMovement(id) <= 0) {
          return false;
        }
        return ua.getAttack(id) > 0;
      }
    };
  }

  public static Match<UnitType> unitTypeCanAttack(final PlayerID id) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType uT) {
        final UnitAttachment ua = UnitAttachment.get(uT);
        if (ua.getMovement(id) <= 0) {
          return false;
        }
        return ua.getAttack(id) > 0;
      }
    };
  }

  public static Match<Unit> unitHasAttackValueOfAtLeast(final int attackValue) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getAttack(unit.getOwner()) >= attackValue;
      }
    };
  }

  public static Match<Unit> unitHasDefendValueOfAtLeast(final int defendValue) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getDefense(unit.getOwner()) >= defendValue;
      }
    };
  }

  public static Match<Unit> unitIsEnemyOf(final GameData data, final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return data.getRelationshipTracker().isAtWar(u.getOwner(), player);
      }
    };
  }

  public static final Match<Unit> UnitIsNotSea = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !ua.getIsSea();
    }
  };
  public static final Match<UnitType> UnitTypeIsSea = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsSea();
    }
  };
  public static final Match<UnitType> UnitTypeIsNotSea = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      return !ua.getIsSea();
    }
  };
  public static final Match<UnitType> UnitTypeIsSeaOrAir = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsSea() || ua.getIsAir();
    }
  };
  public static final Match<UnitType> UnitTypeIsCarrier = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      return (ua.getCarrierCapacity() != -1);
    }
  };
  public static final Match<Unit> UnitIsAir = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsAir();
    }
  };
  public static final Match<Unit> UnitIsNotAir = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return !ua.getIsAir();
    }
  };

  public static Match<UnitType> unitTypeCanBombard(final PlayerID id) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType type) {
        final UnitAttachment ua = UnitAttachment.get(type);
        return ua.getCanBombard(id);
      }
    };
  }

  public static Match<Unit> UnitCanBeGivenByTerritoryTo(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        final Unit unit = o;
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getCanBeGivenByTerritoryTo().contains(player);
      }
    };
  }

  public static Match<Unit> UnitCanBeCapturedOnEnteringToInThisTerritory(final PlayerID player, final Territory terr,
      final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        if (!games.strategy.triplea.Properties.getCaptureUnitsOnEnteringTerritory(data)) {
          return false;
        }
        final Unit unit = o;
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
      }
    };
  }

  public static Match<Unit> UnitDestroyedWhenCapturedByOrFrom(final PlayerID playerBY) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        final Match<Unit> byOrFrom =
            new CompositeMatchOr<>(UnitDestroyedWhenCapturedBy(playerBY), UnitDestroyedWhenCapturedFrom());
        return byOrFrom.match(o);
      }
    };
  }

  private static Match<Unit> UnitDestroyedWhenCapturedBy(final PlayerID playerBY) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final UnitAttachment ua = UnitAttachment.get(u.getType());
        if (ua.getDestroyedWhenCapturedBy().isEmpty()) {
          return false;
        }
        for (final Tuple<String, PlayerID> tuple : ua.getDestroyedWhenCapturedBy()) {
          if (tuple.getFirst().equals("BY") && tuple.getSecond().equals(playerBY)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private static Match<Unit> UnitDestroyedWhenCapturedFrom() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
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
      }
    };
  }

  public static final Match<Unit> UnitIsAirBase = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsAirBase();
    }
  };
  public static final Match<Unit> UnitCanBeDamaged = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      return UnitTypeCanBeDamaged.match(unit.getType());
    }
  };
  public static final Match<UnitType> UnitTypeCanBeDamaged = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType ut) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      return ua.getCanBeDamaged();
    }
  };

  public static Match<Unit> UnitIsAtMaxDamageOrNotCanBeDamaged(final Territory t) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
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
      }
    };
  }

  public static Match<Unit> UnitIsLegalBombingTargetBy(final Unit bomberOrRocket) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(bomberOrRocket.getType());
        final HashSet<UnitType> allowedTargets = ua.getBombingTargets(bomberOrRocket.getData());
        return allowedTargets == null || allowedTargets.contains(unit.getType());
      }
    };
  }

  public static Match<Unit> UnitHasTakenSomeBombingUnitDamage = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final TripleAUnit taUnit = (TripleAUnit) unit;
      return taUnit.getUnitDamage() > 0;
    }
  };
  public static Match<Unit> UnitHasNotTakenAnyBombingUnitDamage =
      new InverseMatch<>(UnitHasTakenSomeBombingUnitDamage);
  public static Match<Unit> UnitIsDisabled = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
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
    }
  };
  public static Match<Unit> UnitIsNotDisabled = new InverseMatch<>(UnitIsDisabled);
  public static final Match<Unit> UnitCanDieFromReachingMaxDamage = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (!ua.getCanBeDamaged()) {
        return false;
      }
      return ua.getCanDieFromReachingMaxDamage();
    }
  };
  public static final Match<Unit> UnitIsInfrastructure = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      return UnitTypeIsInfrastructure.match(unit.getType());
    }
  };
  public static final Match<Unit> UnitIsNotInfrastructure = new InverseMatch<>(UnitIsInfrastructure);
  public static final Match<UnitType> UnitTypeIsInfrastructure = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType ut) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      return ua.getIsInfrastructure();
    }
  };

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  public static Match<Unit> UnitIsSupporterOrHasCombatAbility(final boolean attack) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return Matches.UnitTypeIsSupporterOrHasCombatAbility(attack, unit.getOwner()).match(unit.getType());
      }
    };
  }

  /**
   * Checks for having attack/defense and for providing support. Does not check for having AA ability.
   */
  private static Match<UnitType> UnitTypeIsSupporterOrHasCombatAbility(final boolean attack, final PlayerID player) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType ut) {
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
      }
    };
  }

  public static Match<UnitSupportAttachment> UnitSupportAttachmentCanBeUsedByPlayer(final PlayerID player) {
    return new Match<UnitSupportAttachment>() {
      @Override
      public boolean match(final UnitSupportAttachment usa) {
        return usa.getPlayers().contains(player);
      }
    };
  }

  public static final Match<Unit> UnitCanScramble = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getCanScramble();
    }
  };
  public static final Match<Unit> UnitWasScrambled = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getWasScrambled();
    }
  };
  public static final Match<Unit> UnitWasInAirBattle = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getWasInAirBattle();
    }
  };
  public static final Match<Territory> TerritoryIsIsland = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      final Collection<Territory> neighbors = t.getData().getMap().getNeighbors(t);
      return neighbors.size() == 1 && TerritoryIsWater.match(neighbors.iterator().next());
    }
  };

  public static Match<Unit> unitCanBombard(final PlayerID id) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getCanBombard(id);
      }
    };
  }

  public static final Match<Unit> UnitCanBlitz = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getCanBlitz(obj.getOwner());
    }
  };
  public static final Match<Unit> UnitIsLandTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsLandTransport();
    }
  };

  public static Match<Unit> UnitIsNotInfrastructureAndNotCapturedOnEntering(final PlayerID player,
      final Territory terr, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        final Unit unit = obj;
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return !ua.getIsInfrastructure()
            && !UnitCanBeCapturedOnEnteringToInThisTerritory(player, terr, data).match(unit);
      }
    };
  }

  public static final Match<Unit> UnitIsSuicide = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsSuicide();
    }
  };
  public static final Match<Unit> UnitIsKamikaze = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getIsKamikaze();
    }
  };
  public static final Match<UnitType> UnitTypeIsAir = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAir();
    }
  };
  public static final Match<UnitType> UnitTypeIsNotAir = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return !ua.getIsAir();
    }
  };
  public static final Match<Unit> UnitCanLandOnCarrier = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getCarrierCost() != -1;
    }
  };
  public static final Match<Unit> UnitIsCarrier = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getCarrierCapacity() != -1;
    }
  };

  public static Match<Territory> TerritoryHasOwnedCarrier(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits()
            .someMatch(new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier));
      }
    };
  }

  public static Match<Unit> UnitIsAlliedCarrier(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        final Unit unit = obj;
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getCarrierCapacity() != -1 && data.getRelationshipTracker().isAllied(player, obj.getOwner());
      }
    };
  }

  public static final Match<Unit> UnitCanBeTransported = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCost() != -1;
    }
  };
  public static final Match<Unit> UnitCanNotBeTransported = new InverseMatch<>(UnitCanBeTransported);
  public static final Match<Unit> UnitWasAmphibious = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getWasAmphibious();
    }
  };
  public static final Match<Unit> UnitWasNotAmphibious = new InverseMatch<>(UnitWasAmphibious);
  public static final Match<Unit> UnitWasInCombat = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getWasInCombat();
    }
  };
  public static final Match<Unit> UnitWasUnloadedThisTurn = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getUnloadedTo() != null;
    }
  };
  public static final Match<Unit> UnitWasLoadedThisTurn = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TripleAUnit taUnit = (TripleAUnit) obj;
      return taUnit.getWasLoadedThisTurn();
    }
  };
  public static final Match<Unit> UnitWasNotLoadedThisTurn = new InverseMatch<>(UnitWasLoadedThisTurn);
  public static final Match<Unit> UnitCanTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.getTransportCapacity() != -1;
    }
  };
  public static final Match<UnitType> UnitTypeCanTransport = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getTransportCapacity() != -1;
    }
  };
  public static final Match<UnitType> UnitTypeCanBeTransported = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getTransportCost() != -1;
    }
  };
  public static final Match<Unit> UnitCanProduceUnits = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeCanProduceUnits.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeCanProduceUnits = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getCanProduceUnits();
    }
  };
  public static final Match<Unit> UnitCanNotProduceUnits = new InverseMatch<>(UnitCanProduceUnits);
  public static final Match<UnitType> UnitTypeIsInfrastructureButNotAAofAnyKind = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      return UnitTypeIsInfrastructure.match(type) && !UnitTypeIsAAforAnything.match(type);
    }
  };
  public static final Match<UnitType> UnitTypeIsInfantry = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsInfantry();
    }
  };
  public static final Match<UnitType> UnitTypeIsArtillery = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getArtillery();
    }
  };
  public static final Match<Unit> UnitHasMaxBuildRestrictions = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getType();
      return UnitTypeHasMaxBuildRestrictions.match(type);
    }
  };
  public static final Match<UnitType> UnitTypeHasMaxBuildRestrictions = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getMaxBuiltPerPlayer() >= 0;
    }
  };
  public static final Match<Unit> UnitIsRocket = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsRocket.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsRocket = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsRocket();
    }
  };
  public static final Match<Unit> UnitHasPlacementLimit = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getPlacementLimit() != null;
    }
  };
  public static final Match<Unit> UnitHasMovementLimit = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getMovementLimit() != null;
    }
  };
  public static final Match<Unit> UnitHasAttackingLimit = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getAttackingLimit() != null;
    }
  };
  public static final Match<Unit> UnitCanNotMoveDuringCombatMove = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeCanNotMoveDuringCombatMove.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeCanNotMoveDuringCombatMove = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getCanNotMoveDuringCombatMove();
    }
  };

  public static Match<Unit> UnitIsAAthatCanHitTheseUnits(final Collection<Unit> targets,
      final Match<Unit> typeOfAA, final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        if (!typeOfAA.match(obj)) {
          return false;
        }
        final UnitAttachment ua = UnitAttachment.get(obj.getType());
        final Set<UnitType> targetsAA = ua.getTargetsAA(obj.getData());
        for (final Unit u : targets) {
          if (targetsAA.contains(u.getType())) {
            return true;
          }
        }
        return Match.someMatch(targets, new CompositeMatchAnd<>(Matches.UnitIsAirborne,
            Matches.unitIsOfTypes(airborneTechTargetsAllowed.get(ua.getTypeAA()))));
      }
    };
  }

  public static Match<Unit> UnitIsAAofTypeAA(final String typeAA) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        return UnitAttachment.get(obj.getType()).getTypeAA().matches(typeAA);
      }
    };
  }

  public static final Match<Unit> UnitAAShotDamageableInsteadOfKillingInstantly = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitAttachment.get(obj.getType()).getDamageableAA();
    }
  };

  private static Match<Unit> UnitIsAAthatWillNotFireIfPresentEnemyUnits(final Collection<Unit> enemyUnitsPresent) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        final UnitAttachment ua = UnitAttachment.get(obj.getType());
        for (final Unit u : enemyUnitsPresent) {
          if (ua.getWillNotFireIfPresent().contains(u.getType())) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private static Match<UnitType> UnitTypeIsAAthatCanFireOnRound(final int battleRoundNumber) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType obj) {
        final int maxRoundsAA = UnitAttachment.get(obj).getMaxRoundsAA();
        return maxRoundsAA < 0 || maxRoundsAA >= battleRoundNumber;
      }
    };
  }

  private static Match<Unit> UnitIsAAthatCanFireOnRound(final int battleRoundNumber) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit obj) {
        return UnitTypeIsAAthatCanFireOnRound(battleRoundNumber).match(obj.getType());
      }
    };
  }

  public static Match<Unit> UnitIsAAthatCanFire(final Collection<Unit> unitsMovingOrAttacking,
      final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed, final PlayerID playerMovingOrAttacking,
      final Match<Unit> typeOfAA, final int battleRoundNumber, final boolean defending, final GameData data) {
    return new CompositeMatchAnd<>(Matches.enemyUnit(playerMovingOrAttacking, data),
        Matches.unitIsBeingTransported().invert(),
        Matches.UnitIsAAthatCanHitTheseUnits(unitsMovingOrAttacking, typeOfAA, airborneTechTargetsAllowed),
        Matches.UnitIsAAthatWillNotFireIfPresentEnemyUnits(unitsMovingOrAttacking).invert(),
        Matches.UnitIsAAthatCanFireOnRound(battleRoundNumber),
        (defending ? UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero
            : UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero));
  }

  public static final Match<Unit> UnitIsAAforCombatOnly = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsAAforCombatOnly.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsAAforCombatOnly = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforCombatOnly();
    }
  };
  public static final Match<Unit> UnitIsAAforBombingThisUnitOnly = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsAAforBombingThisUnitOnly.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsAAforBombingThisUnitOnly = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforBombingThisUnitOnly();
    }
  };
  public static final Match<Unit> UnitIsAAforFlyOverOnly = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsAAforFlyOverOnly.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsAAforFlyOverOnly = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforFlyOverOnly();
    }
  };
  public static final Match<Unit> UnitIsAAforAnything = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsAAforAnything.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsAAforAnything = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly() || ua.getIsAAforFlyOverOnly();
    }
  };
  public static final Match<Unit> UnitIsNotAA = new InverseMatch<>(UnitIsAAforAnything);
  public static final Match<Unit> UnitMaxAAattacksIsInfinite = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeMaxAAattacksIsInfinite.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeMaxAAattacksIsInfinite = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getMaxAAattacks() == -1;
    }
  };
  public static final Match<Unit> UnitMayOverStackAA = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeMayOverStackAA.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeMayOverStackAA = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitAttachment ua = UnitAttachment.get(obj);
      return ua.getMayOverStackAA();
    }
  };
  public static final Match<Unit> UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    }
  };
  public static final Match<Unit> UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitAttachment ua = UnitAttachment.get(obj.getType());
      return ua.getOffensiveAttackAA(obj.getOwner()) > 0 && ua.getMaxAAattacks() != 0;
    }
  };
  public static final Match<Unit> UnitIsInfantry = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsInfantry();
    }
  };
  public static final Match<Unit> UnitIsNotInfantry = new InverseMatch<>(UnitIsInfantry);
  public static final Match<Unit> UnitHasMarinePositiveBonus = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsMarine() > 0;
    }
  };
  public static final Match<Unit> UnitHasMarineNegativeBonus = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsMarine() < 0;
    }
  };
  public static final Match<Unit> UnitIsNotMarine = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsMarine() == 0;
    }
  };
  public static final Match<Unit> UnitIsAirTransportable = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (ta == null || !ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransportable();
    }
  };
  public static final Match<Unit> UnitIsNotAirTransportable = new InverseMatch<>(UnitIsAirTransportable);
  public static final Match<Unit> UnitIsAirTransport = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final TechAttachment ta = TechAttachment.get(obj.getOwner());
      if (ta == null || !ta.getParatroopers()) {
        return false;
      }
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsAirTransport();
    }
  };
  public static final Match<Unit> UnitIsNotAirTransport = new InverseMatch<>(UnitIsAirTransport);
  public static final Match<Unit> UnitIsArtillery = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getArtillery();
    }
  };
  public static final Match<Unit> UnitIsArtillerySupportable = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final UnitType type = obj.getUnitType();
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getArtillerySupportable();
    }
  };
  // TODO: CHECK whether this makes any sense
  public static final Match<Territory> TerritoryIsLandOrWater = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      return t != null;
    }
  };
  public static final Match<Territory> TerritoryIsWater = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      return t.isWater();
    }
  };
  public static final Match<Territory> TerritoryIsVictoryCity = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta == null) {
        return false;
      }
      return ta.getVictoryCity() != 0;
    }
  };
  public static final Match<Territory> TerritoryIsLand = new InverseMatch<>(TerritoryIsWater);
  public static final Match<Territory> TerritoryIsEmpty = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      return t.getUnits().size() == 0;
    }
  };

  /**
   * Tests for Land, Convoys Centers and Convoy Routes, and Contested Territories.
   * Assumes player is either the owner of the territory we are testing, or about to become the owner (ie: this doesn't
   * test ownership).
   * If the game option for contested territories not producing is on, then will also remove any contested territories.
   */
  public static Match<Territory> territoryCanCollectIncomeFrom(final PlayerID player, final GameData data) {
    final boolean contestedDoNotProduce =
        games.strategy.triplea.Properties.getContestedTerritoriesProduceNoIncome(data);
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
      }
    };
  }

  public static Match<Territory> territoryHasNeighborMatching(final GameData data, final Match<Territory> match) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap().getNeighbors(t, match).size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasEnemyLandNeighbor(final GameData data, final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        // This method will still return true if territory t is an impassable or restricted territory With enemy
        // neighbors. Makes sure your
        // AI does not include any impassable or restricted territories by using this:
        // CompositeMatch<Territory> territoryHasEnemyLandNeighborAndIsNotImpassableOrRestricted = new
        // CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player),
        // Matches.territoryHasEnemyLandNeighbor(data,
        // player));
        final CompositeMatch<Territory> condition = new CompositeMatchAnd<>(Matches.TerritoryIsLand,
            Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player, data));
        return data.getMap().getNeighbors(t, condition).size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasAlliedNeighborWithAlliedUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap().getNeighbors(t, Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, unitMatch))
            .size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasValidLandRouteTo(final GameData data, final Territory goTerr) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final CompositeMatch<Territory> validLandRoute =
            new CompositeMatchAnd<>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
        return data.getMap().getRoute(t, goTerr, validLandRoute) != null;
      }
    };
  }

  public static Match<Territory> territoryIsInList(final Collection<Territory> list) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory ter) {
        return list.contains(ter);
      }
    };
  }

  public static Match<Territory> territoryIsNotInList(final Collection<Territory> list) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory ter) {
        return !list.contains(ter);
      }
    };
  }

  /**
   * @param data
   *        game data
   * @return Match&lt;Territory> that tests if there is a route to an enemy capital from the given territory.
   */
  public static Match<Territory> territoryHasRouteToEnemyCapital(final GameData data, final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        for (final PlayerID ePlayer : data.getPlayerList().getPlayers()) {
          final List<Territory> capitalsListOwned =
              new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
          for (final Territory current : capitalsListOwned) {
            if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
              continue;
            }
            if (data.getMap().getDistance(t, current,
                Matches.TerritoryIsPassableAndNotRestricted(player, data)) != -1) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  /**
   * @param data
   *        game data.
   * @return true only if the route is land
   */
  public static Match<Territory> territoryHasLandRouteToEnemyCapital(final GameData data, final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        for (final PlayerID ePlayer : data.getPlayerList().getPlayers()) {
          final List<Territory> capitalsListOwned =
              new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(ePlayer, data));
          for (final Territory current : capitalsListOwned) {
            if (!data.getRelationshipTracker().isAtWar(player, current.getOwner())) {
              continue;
            }
            if (data.getMap().getDistance(t, current,
                Matches.TerritoryIsNotImpassableToLandUnits(player, data)) != -1) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  public static Match<Territory> territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap()
            .getNeighbors(t, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, unitMatch))
            .size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasOwnedNeighborWithOwnedUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap().getNeighbors(t, Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, unitMatch))
            .size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnitsNeighbor(
      final GameData data, final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap()
            .getNeighbors(t, Matches.territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(data, player))
            .size() > 0;
      }
    };
  }

  public static Match<Territory> territoryHasWaterNeighbor(final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getMap().getNeighbors(t, Matches.TerritoryIsWater).size() > 0;
      }
    };
  }

  public static Match<Territory> territoryIsAlliedAndHasAlliedUnitMatching(final GameData data, final PlayerID player,
      final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!data.getRelationshipTracker().isAllied(t.getOwner(), player)) {
          return false;
        }
        return t.getUnits().someMatch(new CompositeMatchAnd<>(Matches.alliedUnit(player, data), unitMatch));
      }
    };
  }

  public static Match<Territory> territoryIsOwnedAndHasOwnedUnitMatching(final PlayerID player,
      final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!t.getOwner().equals(player)) {
          return false;
        }
        return t.getUnits().someMatch(new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), unitMatch));
      }
    };
  }

  public static Match<Territory> territoryHasOwnedIsFactoryOrCanProduceUnits(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!t.getOwner().equals(player)) {
          return false;
        }
        return t.getUnits().someMatch(Matches.UnitCanProduceUnits);
      }
    };
  }

  private static Match<Territory> territoryHasOwnedAtBeginningOfTurnIsFactoryOrCanProduceUnits(final GameData data,
      final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!t.getOwner().equals(player)) {
          return false;
        }
        if (!t.getUnits().someMatch(Matches.UnitCanProduceUnits)) {
          return false;
        }
        final BattleTracker bt = AbstractMoveDelegate.getBattleTracker(data);
        return !(bt == null || bt.wasConquered(t));
      }
    };
  }

  public static Match<Territory> territoryHasAlliedIsFactoryOrCanProduceUnits(final GameData data,
      final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!isTerritoryAllied(player, data).match(t)) {
          return false;
        }
        return t.getUnits().someMatch(Matches.UnitCanProduceUnits);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(final GameData data,
      final PlayerID player, final Match<Unit> unitMatch) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!data.getRelationshipTracker().isAtWar(player, t.getOwner())) {
          return false;
        }
        if (t.getOwner().isNull()) {
          return false;
        }
        return t.getUnits().someMatch(new CompositeMatchAnd<>(Matches.enemyUnit(player, data), unitMatch));
      }
    };
  }

  public static Match<Territory> territoryIsEmptyOfCombatUnits(final GameData data, final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final CompositeMatch<Unit> nonCom = new CompositeMatchOr<>();
        nonCom.add(UnitIsInfrastructure);
        nonCom.add(enemyUnit(player, data).invert());
        // this is causing issues where the newly captured
        // nonCom.add(UnitCanBeCapturedOnEnteringToInThisTerritory(player, t, data));
        // units fight against themselves
        return t.getUnits().allMatch(nonCom);
      }
    };
  }

  public static final Match<Territory> TerritoryIsNeutralButNotWater = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      if (t.isWater()) {
        return false;
      }
      return t.getOwner().equals(PlayerID.NULL_PLAYERID);
    }
  };
  public static final Match<Territory> TerritoryIsNotNeutralButCouldBeWater =
      new InverseMatch<>(TerritoryIsNeutralButNotWater);
  public static final Match<Territory> TerritoryIsImpassable = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      if (t.isWater()) {
        return false;
      }
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      return ta != null && ta.getIsImpassable();
    }
  };
  public static final Match<Territory> TerritoryIsNotImpassable = new InverseMatch<>(TerritoryIsImpassable);

  public static Match<Territory> seaCanMoveOver(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!TerritoryIsWater.match(t)) {
          return false;
        }
        return TerritoryIsPassableAndNotRestricted(player, data).match(t);
      }
    };
  }

  public static Match<Territory> airCanFlyOver(final PlayerID player, final GameData data,
      final boolean areNeutralsPassableByAir) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!areNeutralsPassableByAir && TerritoryIsNeutralButNotWater.match(t)) {
          return false;
        }
        if (!TerritoryIsPassableAndNotRestricted(player, data).match(t)) {
          return false;
        }
        return !(TerritoryIsLand.match(t)
            && !data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner()));
      }
    };
  }

  public static Match<Territory> TerritoryIsPassableAndNotRestricted(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
      }
    };
  }

  public static Match<Territory> TerritoryIsImpassableToLandUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.isWater()) {
          return true;
        } else if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t)) {
          return true;
        }
        return false;
      }
    };
  }

  public static Match<Territory> TerritoryIsNotImpassableToLandUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return TerritoryIsImpassableToLandUnits(player, data).invert().match(t);
      }
    };
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
  public static Match<Territory> TerritoryIsPassableAndNotRestrictedAndOkByRelationships(
      final PlayerID playerWhoOwnsAllTheUnitsMoving, final GameData data, final boolean isCombatMovePhase,
      final boolean hasLandUnitsNotBeingTransportedOrBeingLoaded, final boolean hasSeaUnitsNotBeingTransported,
      final boolean hasAirUnitsNotBeingTransported, final boolean isLandingZoneOnLandForAirUnits) {
    final boolean neutralsPassable = !games.strategy.triplea.Properties.getNeutralsImpassable(data);
    final boolean areNeutralsPassableByAir =
        neutralsPassable && games.strategy.triplea.Properties.getNeutralFlyoverAllowed(data);
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
      }
    };
  }

  public static final Match<IBattle> BattleIsEmpty = new Match<IBattle>() {
    @Override
    public boolean match(final IBattle battle) {
      return battle.isEmpty();
    }
  };
  public static final Match<IBattle> BattleIsAmphibious = new Match<IBattle>() {
    @Override
    public boolean match(final IBattle battle) {
      return battle.isAmphibious();
    }
  };

  public static Match<Unit> UnitHasEnoughMovementForRoutes(final List<Route> route) {
    return UnitHasEnoughMovementForRoute(Route.create(route));
  }

  public static Match<Unit> UnitHasEnoughMovementForRoute(final List<Territory> territories) {
    return UnitHasEnoughMovementForRoute(new Route(territories));
  }

  public static Match<Unit> UnitHasEnoughMovementForRoute(final Route route) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
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
      }
    };
  }

  /**
   * Match units that have at least 1 movement left.
   */
  public static final Match<Unit> unitHasMovementLeft = new Match<Unit>() {
    @Override
    public boolean match(final Unit o) {
      return TripleAUnit.get(o).getMovementLeft() >= 1;
    }
  };
  public static final Match<Unit> UnitCanMove = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      return UnitTypeCanMove(u.getOwner()).match(u.getType());
    }
  };

  private static Match<UnitType> UnitTypeCanMove(final PlayerID player) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType obj) {
        return UnitAttachment.get(obj).getMovement(player) > 0;
      }
    };
  }

  public static final Match<Unit> UnitIsStatic = new InverseMatch<>(UnitCanMove);

  public static Match<UnitType> UnitTypeIsStatic(final PlayerID id) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType uT) {
        return !UnitTypeCanMove(id).match(uT);
      }
    };
  }

  public static Match<Unit> unitIsLandAndOwnedBy(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return !ua.getIsSea() && !ua.getIsAir() && unit.getOwner().equals(player);
      }
    };
  }

  public static Match<Unit> unitIsOwnedBy(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return unit.getOwner().equals(player);
      }
    };
  }

  public static Match<Unit> unitIsOwnedByOfAnyOfThesePlayers(final Collection<PlayerID> players) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return players.contains(unit.getOwner());
      }
    };
  }

  public static Match<Unit> unitHasDefenseThatIsMoreThanOrEqualTo(final int minDefense) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        return ua.getDefense(unit.getOwner()) >= minDefense;
      }
    };
  }

  public static Match<Unit> unitIsTransporting() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
        return !(transporting == null || transporting.isEmpty());
      }
    };
  }

  public static Match<Unit> unitIsTransportingSomeCategories(final Collection<Unit> units) {
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units);
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        final Collection<Unit> transporting = TripleAUnit.get(unit).getTransporting();
        if (transporting == null) {
          return false;
        }
        return Util.someIntersect(UnitSeperator.categorize(transporting), unitCategories);
      }
    };
  }

  public static Match<Territory> isTerritoryAllied(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return data.getRelationshipTracker().isAllied(player, t.getOwner());
      }
    };
  }

  public static Match<Territory> isTerritoryOwnedBy(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getOwner().equals(player);
      }
    };
  }

  public static Match<Territory> isTerritoryOwnedBy(final Collection<PlayerID> players) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        for (final PlayerID player : players) {
          if (t.getOwner().equals(player)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static Match<Unit> isUnitAllied(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit t) {
        return data.getRelationshipTracker().isAllied(player, t.getOwner());
      }
    };
  }

  public static Match<Territory> isTerritoryFriendly(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.isWater()) {
          return true;
        }
        if (t.getOwner().equals(player)) {
          return true;
        }
        return data.getRelationshipTracker().isAllied(player, t.getOwner());
      }
    };
  }

  private static Match<Unit> unitIsEnemyAAforAnything(final PlayerID player, final GameData data) {
    final CompositeMatch<Unit> comp = new CompositeMatchAnd<>();
    comp.add(UnitIsAAforAnything);
    comp.add(enemyUnit(player, data));
    return comp;
  }

  private static Match<Unit> unitIsEnemyAAforCombat(final PlayerID player, final GameData data) {
    final CompositeMatch<Unit> comp = new CompositeMatchAnd<>();
    comp.add(UnitIsAAforCombatOnly);
    comp.add(enemyUnit(player, data));
    return comp;
  }

  public static Match<Unit> unitIsInTerritory(final Territory territory) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        return territory.getUnits().getUnits().contains(o);
      }
    };
  }

  public static Match<Territory> isTerritoryEnemy(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.getOwner().equals(player)) {
          return false;
        }
        return data.getRelationshipTracker().isAtWar(player, t.getOwner());
      }
    };
  }

  public static Match<Territory> isTerritoryEnemyAndNotUnownedWater(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
      }
    };
  }

  public static Match<Territory> isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
        if (!Matches.TerritoryIsPassableAndNotRestricted(player, data).match(t)) {
          return false;
        }
        return data.getRelationshipTracker().isAtWar(player, t.getOwner());
      }
    };
  }

  public static Match<Territory> TerritoryIsBlitzable(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
        final CompositeMatch<Unit> blitzableUnits = new CompositeMatchOr<>();
        // we ignore neutral units
        blitzableUnits.add(Matches.enemyUnit(player, data).invert());
        // WW2V2, cant blitz through factories and aa guns
        // WW2V1, you can
        if (!games.strategy.triplea.Properties.getWW2V2(data)
            && !games.strategy.triplea.Properties.getBlitzThroughFactoriesAndAARestricted(data)) {
          blitzableUnits.add(Matches.UnitIsInfrastructure);
        }
        return t.getUnits().allMatch(blitzableUnits);
      }
    };
  }

  public static Match<Territory> isTerritoryFreeNeutral(final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return (t.getOwner().equals(PlayerID.NULL_PLAYERID) && Properties.getNeutralCharge(data) <= 0);
      }
    };
  }

  public static Match<Territory> territoryDoesNotCostMoneyToEnter(final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return Matches.TerritoryIsLand.invert().match(t) || !t.getOwner().equals(PlayerID.NULL_PLAYERID)
            || Properties.getNeutralCharge(data) <= 0;
      }
    };
  }

  public static Match<Unit> enemyUnit(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return data.getRelationshipTracker().isAtWar(player, unit.getOwner());
      }
    };
  }

  public static Match<Unit> enemyUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(unit.getOwner(), players);
      }
    };
  }

  public static Match<Unit> unitOwnedBy(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        final Unit unit = o;
        return unit.getOwner().equals(player);
      }
    };
  }

  public static Match<Unit> unitOwnedBy(final List<PlayerID> players) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit o) {
        for (final PlayerID p : players) {
          if (o.getOwner().equals(p)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static Match<Unit> alliedUnit(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        if (unit.getOwner().equals(player)) {
          return true;
        }
        return data.getRelationshipTracker().isAllied(player, unit.getOwner());
      }
    };
  }

  public static Match<Unit> alliedUnitOfAnyOfThesePlayers(final Collection<PlayerID> players, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        if (Matches.unitIsOwnedByOfAnyOfThesePlayers(players).match(unit)) {
          return true;
        }
        return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(unit.getOwner(), players);
      }
    };
  }

  public static Match<Territory> territoryIs(final Territory test) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.equals(test);
      }
    };
  }

  public static Match<Territory> territoryHasLandUnitsOwnedBy(final PlayerID player) {
    final CompositeMatch<Unit> unitOwnedBy = new CompositeMatchAnd<>(unitIsOwnedBy(player), Matches.UnitIsLand);
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(unitOwnedBy);
      }
    };
  }

  public static Match<Territory> territoryHasUnitsOwnedBy(final PlayerID player) {
    final Match<Unit> unitOwnedBy = unitIsOwnedBy(player);
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(unitOwnedBy);
      }
    };
  }

  public static Match<Territory> territoryHasUnitsThatMatch(final Match<Unit> cond) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(cond);
      }
    };
  }

  public static Match<Territory> territoryHasEnemyAAforAnything(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(unitIsEnemyAAforAnything(player, data));
      }
    };
  }

  public static Match<Territory> territoryHasEnemyAAforCombatOnly(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(Matches.unitIsEnemyAAforCombat(player, data));
      }
    };
  }

  public static Match<Territory> territoryHasNoEnemyUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return !t.getUnits().someMatch(enemyUnit(player, data));
      }
    };
  }

  public static Match<Territory> territoryHasNoAlliedUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return !t.getUnits().someMatch(alliedUnit(player, data));
      }
    };
  }

  public static Match<Territory> territoryHasAlliedUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(alliedUnit(player, data));
      }
    };
  }

  public static Match<Territory> territoryHasNonSubmergedEnemyUnits(final PlayerID player, final GameData data) {
    final CompositeMatch<Unit> match = new CompositeMatchAnd<>();
    match.add(enemyUnit(player, data));
    match.add(UnitIsSubmerged.invert());
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(match);
      }
    };
  }

  public static Match<Territory> territoryHasEnemyLandUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(new CompositeMatchAnd<>(enemyUnit(player, data), UnitIsLand));
      }
    };
  }

  public static Match<Territory> territoryHasEnemySeaUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(new CompositeMatchAnd<>(enemyUnit(player, data), UnitIsSea));
      }
    };
  }

  public static Match<Territory> territoryHasEnemyBlitzUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(enemyUnit(player, data)) && t.getUnits().someMatch(Matches.UnitCanBlitz);
      }
    };
  }

  public static Match<Territory> territoryHasEnemyUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return t.getUnits().someMatch(enemyUnit(player, data));
      }
    };
  }

  /**
   * The territory is owned by the enemy of those enemy units (ie: probably owned by you or your ally, but not
   * necessarily so in an FFA type
   * game) and is not unowned water.
   */
  public static Match<Territory> territoryHasEnemyUnitsThatCanCaptureTerritoryAndTerritoryOwnedByTheirEnemyAndIsNotUnownedWater(
      final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.getOwner() == null) {
          return false;
        }
        if (t.isWater() && TerritoryAttachment.get(t) == null && t.getOwner().isNull()) {
          return false;
        }
        final Set<PlayerID> enemies = new HashSet<>();
        for (final Unit u : t.getUnits()
            .getMatches(new CompositeMatchAnd<>(enemyUnit(player, data), UnitIsNotAir, UnitIsNotInfrastructure))) {
          enemies.add(u.getOwner());
        }
        return (Matches.isAtWarWithAnyOfThesePlayers(enemies, data)).match(t.getOwner());
      }
    };
  }

  public static Match<Territory> territoryHasOwnedTransportingUnits(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final CompositeMatch<Unit> match = new CompositeMatchAnd<>();
        match.add(unitIsOwnedBy(player));
        match.add(transportIsTransporting());
        return t.getUnits().someMatch(match);
      }
    };
  }

  public static Match<Unit> transportCannotUnload(final Territory territory) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit transport) {
        if (TransportTracker.hasTransportUnloadedInPreviousPhase(transport)) {
          return true;
        }
        if (TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(transport, territory)) {
          return true;
        }
        return TransportTracker.isTransportUnloadRestrictedInNonCombat(transport);
      }
    };
  }

  public static Match<Unit> transportIsNotTransporting() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit transport) {
        return !TransportTracker.isTransporting(transport);
      }
    };
  }

  public static Match<Unit> transportIsTransporting() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit transport) {
        return TransportTracker.isTransporting(transport);
      }
    };
  }

  /**
   * @return Match that tests the TripleAUnit getTransportedBy value
   *         which is normally set for sea transport movement of land units,
   *         and sometimes set for other things like para-troopers and dependent allied fighters sitting as cargo on a
   *         ship. (not sure if
   *         set for mech inf or not)
   */
  public static Match<Unit> unitIsBeingTransported() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit dependent) {
        return ((TripleAUnit) dependent).getTransportedBy() != null;
      }
    };
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
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit dependent) {
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
      }
    };
  }

  public static final Match<Unit> UnitIsLand = new CompositeMatchAnd<>(UnitIsNotSea, UnitIsNotAir);
  public static final Match<UnitType> UnitTypeIsLand =
      new CompositeMatchAnd<>(UnitTypeIsNotSea, UnitTypeIsNotAir);
  public static final Match<Unit> UnitIsNotLand = new InverseMatch<>(UnitIsLand);

  public static Match<Unit> unitIsOfType(final UnitType type) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        return unit.getType().equals(type);
      }
    };
  }

  public static Match<Unit> unitIsOfTypes(final Set<UnitType> types) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unit) {
        if (types == null || types.isEmpty()) {
          return false;
        }
        return types.contains(unit.getType());
      }
    };
  }

  public static Match<Territory> territoryWasFoughOver(final BattleTracker tracker) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return tracker.wasBattleFought(t) || tracker.wasBlitzed(t);
      }
    };
  }

  public static final Match<Unit> UnitIsSubmerged = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      return TripleAUnit.get(u).getSubmerged();
    }
  };

  public static final Match<UnitType> UnitTypeIsSub = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType type = obj;
      final UnitAttachment ua = UnitAttachment.get(type);
      return ua.getIsSub();
    }
  };

  public static Match<Unit> unitOwnerHasImprovedArtillerySupportTech() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return TechTracker.hasImprovedArtillerySupport(u.getOwner());
      }
    };
  }

  public static Match<Territory> territoryHasNonAllowedCanal(final PlayerID player, final Collection<Unit> unitsMoving,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return MoveValidator.validateCanal(t, null, unitsMoving, player, data).isPresent();
      }
    };
  }

  public static Match<Territory> territoryIsBlockedSea(final PlayerID player, final GameData data) {
    final CompositeMatch<Unit> ignore =
        new CompositeMatchAnd<>(Matches.UnitIsInfrastructure.invert(), Matches.alliedUnit(player, data).invert());
    final CompositeMatch<Unit> sub = new CompositeMatchAnd<>(Matches.UnitIsSub.invert());
    final CompositeMatch<Unit> transport =
        new CompositeMatchAnd<>(Matches.UnitIsTransportButNotCombatTransport.invert(), Matches.UnitIsLand.invert());
    final CompositeMatch<Unit> unitCond = ignore;
    if (Properties.getIgnoreTransportInMovement(data)) {
      unitCond.add(transport);
    }
    if (Properties.getIgnoreSubInMovement(data)) {
      unitCond.add(sub);
    }
    final CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<>(
        Matches.territoryHasUnitsThatMatch(unitCond).invert(), Matches.TerritoryIsWater);
    return routeCondition;
  }

  public static final Match<Unit> UnitCanRepairOthers = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
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
    }
  };

  public static Match<Unit> UnitCanRepairThisUnit(final Unit damagedUnit) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitCanRepair) {
        final UnitType type = unitCanRepair.getUnitType();
        final UnitAttachment ua = UnitAttachment.get(type);
        // TODO: make sure the unit is operational
        if (ua.getRepairsUnits() != null && ua.getRepairsUnits().keySet().contains(damagedUnit.getType())) {
          return true;
        }
        return false;
      }
    };
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
  public static Match<Unit> UnitCanBeRepairedByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit damagedUnit) {
        final Match<Unit> damaged =
            new CompositeMatchAnd<>(Matches.UnitHasMoreThanOneHitPointTotal, Matches.UnitHasTakenSomeDamage);
        if (!damaged.match(damagedUnit)) {
          return false;
        }
        final Match<Unit> repairUnit = new CompositeMatchAnd<>(Matches.alliedUnit(player, data),
            Matches.UnitCanRepairOthers, Matches.UnitCanRepairThisUnit(damagedUnit));
        if (Match.someMatch(territory.getUnits().getUnits(), repairUnit)) {
          return true;
        }
        if (Matches.UnitIsSea.match(damagedUnit)) {
          final Match<Unit> repairUnitLand = new CompositeMatchAnd<>(repairUnit, Matches.UnitIsLand);
          final List<Territory> neighbors =
              new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
          for (final Territory current : neighbors) {
            if (Match.someMatch(current.getUnits().getUnits(), repairUnitLand)) {
              return true;
            }
          }
        } else if (Matches.UnitIsLand.match(damagedUnit)) {
          final Match<Unit> repairUnitSea = new CompositeMatchAnd<>(repairUnit, Matches.UnitIsSea);
          final List<Territory> neighbors =
              new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsWater));
          for (final Territory current : neighbors) {
            if (Match.someMatch(current.getUnits().getUnits(), repairUnitSea)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  public static final Match<Unit> UnitCanGiveBonusMovement = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null) {
        return false;
      }
      return ua.getGivesMovement().size() > 0 && Matches.unitIsBeingTransported().invert().match(unit);
    }
  };

  public static Match<Unit> UnitCanGiveBonusMovementToThisUnit(final Unit unitWhichWillGetBonus) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitCanGiveBonusMovement) {
        if (UnitIsDisabled.match(unitCanGiveBonusMovement)) {
          return false;
        }
        final UnitType type = unitCanGiveBonusMovement.getUnitType();
        final UnitAttachment ua = UnitAttachment.get(type);
        // TODO: make sure the unit is operational
        return UnitCanGiveBonusMovement.match(unitCanGiveBonusMovement)
            && ua.getGivesMovement().getInt(unitWhichWillGetBonus.getType()) != 0;
      }
    };
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
  public static Match<Unit> UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(final Territory territory,
      final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitWhichWillGetBonus) {
        final Match<Unit> givesBonusUnit = new CompositeMatchAnd<>(Matches.alliedUnit(player, data),
            UnitCanGiveBonusMovementToThisUnit(unitWhichWillGetBonus));
        if (Match.someMatch(territory.getUnits().getUnits(), givesBonusUnit)) {
          return true;
        }
        if (Matches.UnitIsSea.match(unitWhichWillGetBonus)) {
          final Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<>(givesBonusUnit, Matches.UnitIsLand);
          final List<Territory> neighbors =
              new ArrayList<>(data.getMap().getNeighbors(territory, Matches.TerritoryIsLand));
          for (final Territory current : neighbors) {
            if (Match.someMatch(current.getUnits().getUnits(), givesBonusUnitLand)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  public static final Match<Unit> UnitCreatesUnits = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null) {
        return false;
      }
      return (ua.getCreatesUnitsList() != null && ua.getCreatesUnitsList().size() > 0);
    }
  };
  public static final Match<Unit> UnitCreatesResources = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null) {
        return false;
      }
      return (ua.getCreatesResourcesList() != null && ua.getCreatesResourcesList().size() > 0);
    }
  };
  /** Any unit that creates at least a single positive resource. */
  public static final Match<Unit> UnitCreatesResourcesPositive = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      if (!UnitCreatesResources.match(unit)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null || ua.getCreatesResourcesList() == null) {
        return false;
      }
      final IntegerMap<Resource> resources = ua.getCreatesResourcesList();
      for (final Entry<Resource, Integer> entry : resources.entrySet()) {
        if (entry.getValue() > 0) {
          return true;
        }
      }
      return false;
    }
  };
  /** Any unit that does not create a single positive resource, but does create at least a single negative resource. */
  public static final Match<Unit> UnitCreatesResourcesNegative = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      if (!UnitCreatesResources.match(unit) || UnitCreatesResourcesPositive.match(unit)) {
        return false;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null || ua.getCreatesResourcesList() == null) {
        return false;
      }
      final IntegerMap<Resource> resources = ua.getCreatesResourcesList();
      for (final Entry<Resource, Integer> entry : resources.entrySet()) {
        if (entry.getValue() < 0) {
          return true;
        }
      }
      return false;
    }
  };
  public static final Match<UnitType> UnitTypeConsumesUnitsOnCreation = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType obj) {
      final UnitType unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit);
      if (ua == null) {
        return false;
      }
      return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
    }
  };
  public static final Match<Unit> UnitConsumesUnitsOnCreation = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null) {
        return false;
      }
      return (ua.getConsumesUnits() != null && ua.getConsumesUnits().size() > 0);
    }
  };

  public static Match<Unit> UnitWhichConsumesUnitsHasRequiredUnits(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitWhichRequiresUnits) {
        if (!Matches.UnitConsumesUnitsOnCreation.match(unitWhichRequiresUnits)) {
          return true;
        }
        final UnitAttachment ua = UnitAttachment.get(unitWhichRequiresUnits.getType());
        final IntegerMap<UnitType> requiredUnitsMap = ua.getConsumesUnits();
        final Collection<UnitType> requiredUnits = requiredUnitsMap.keySet();
        boolean canBuild = true;
        for (final UnitType ut : requiredUnits) {
          final Match<Unit> unitIsOwnedByAndOfTypeAndNotDamaged = new CompositeMatchAnd<>(
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
      }
    };
  }

  public static final Match<Unit> UnitRequiresUnitsOnCreation = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      final Unit unit = obj;
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua == null) {
        return false;
      }
      return (ua.getRequiresUnits() != null && ua.getRequiresUnits().size() > 0);
    }
  };

  public static Match<Unit> UnitWhichRequiresUnitsHasRequiredUnitsInList(
      final Collection<Unit> unitsInTerritoryAtStartOfTurn) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitWhichRequiresUnits) {
        if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits)) {
          return true;
        }
        final Match<Unit> unitIsOwnedByAndNotDisabled = new CompositeMatchAnd<>(
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
      }
    };
  }

  public static final Match<Territory> territoryIsBlockadeZone = new Match<Territory>() {
    @Override
    public boolean match(final Territory t) {
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta != null) {
        return ta.getBlockadeZone();
      }
      return false;
    }
  };
  public static final Match<Unit> UnitIsConstruction = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return UnitTypeIsConstruction.match(obj.getType());
    }
  };
  public static final Match<UnitType> UnitTypeIsConstruction = new Match<UnitType>() {
    @Override
    public boolean match(final UnitType type) {
      final UnitAttachment ua = UnitAttachment.get(type);
      if (ua == null) {
        return false;
      }
      return ua.getIsConstruction();
    }
  };
  public static final Match<Unit> UnitIsNotConstruction = new InverseMatch<>(UnitIsConstruction);
  public static final Match<Unit> UnitCanProduceUnitsAndIsConstruction =
      new CompositeMatchAnd<>(UnitCanProduceUnits, UnitIsConstruction);
  public static final Match<UnitType> UnitTypeCanProduceUnitsAndIsConstruction =
      new CompositeMatchAnd<>(UnitTypeCanProduceUnits, UnitTypeIsConstruction);
  public static final Match<Unit> UnitCanProduceUnitsAndIsInfrastructure =
      new CompositeMatchAnd<>(UnitCanProduceUnits, UnitIsInfrastructure);
  public static final Match<Unit> UnitCanProduceUnitsAndCanBeDamaged =
      new CompositeMatchAnd<>(UnitCanProduceUnits, UnitCanBeDamaged);
  /**
   * See if a unit can invade. Units with canInvadeFrom not set, or set to "all", can invade from any other unit.
   * Otherwise, units must have
   * a specific unit in this list to be able to invade from that unit.
   */
  public static final Match<Unit> UnitCanInvade = new Match<Unit>() {
    @Override
    public boolean match(final Unit unit) {
      // is the unit being transported?
      final Unit transport = TripleAUnit.get(unit).getTransportedBy();
      if (transport == null) {
        // Unit isn't transported so can Invade
        return true;
      }
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      return ua.canInvadeFrom(transport.getUnitType().getName());
    }
  };
  public static final Match<RelationshipType> RelationshipTypeIsAllied = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType relationship) {
      return relationship.getRelationshipTypeAttachment().isAllied();
    }
  };
  public static final Match<Relationship> RelationshipIsAllied = new Match<Relationship>() {
    @Override
    public boolean match(final Relationship relationship) {
      return relationship.getRelationshipType().getRelationshipTypeAttachment().isAllied();
    }
  };
  public static final Match<RelationshipType> RelationshipTypeIsNeutral = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType relationship) {
      return relationship.getRelationshipTypeAttachment().isNeutral();
    }
  };
  public static final Match<Relationship> RelationshipIsNeutral = new Match<Relationship>() {
    @Override
    public boolean match(final Relationship relationship) {
      return relationship.getRelationshipType().getRelationshipTypeAttachment().isNeutral();
    }
  };
  public static final Match<RelationshipType> RelationshipTypeIsAtWar = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType relationship) {
      return relationship.getRelationshipTypeAttachment().isWar();
    }
  };
  public static final Match<Relationship> RelationshipIsAtWar = new Match<Relationship>() {
    @Override
    public boolean match(final Relationship relationship) {
      return relationship.getRelationshipType().getRelationshipTypeAttachment().isWar();
    }
  };
  public static final Match<RelationshipType> RelationshipTypeCanMoveLandUnitsOverOwnedLand =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getCanMoveLandUnitsOverOwnedLand();
        }
      };

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> TerritoryAllowsCanMoveLandUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!Matches.TerritoryIsLand.match(t)) {
          return true;
        }
        final PlayerID tOwner = t.getOwner();
        if (tOwner == null) {
          return true;
        }
        return data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(tOwner, ownerOfUnitsMoving);
      }
    };
  }

  public static final Match<RelationshipType> RelationshipTypeCanMoveAirUnitsOverOwnedLand =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getCanMoveAirUnitsOverOwnedLand();
        }
      };

  /**
   * If the territory is not land, returns true. Else, tests relationship of the owners.
   */
  public static Match<Territory> TerritoryAllowsCanMoveAirUnitsOverOwnedLand(final PlayerID ownerOfUnitsMoving,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!Matches.TerritoryIsLand.match(t)) {
          return true;
        }
        final PlayerID tOwner = t.getOwner();
        if (tOwner == null) {
          return true;
        }
        return data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(tOwner, ownerOfUnitsMoving);
      }
    };
  }

  public static final Match<RelationshipType> RelationshipTypeCanLandAirUnitsOnOwnedLand =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getCanLandAirUnitsOnOwnedLand();
        }
      };
  public static final Match<RelationshipType> RelationshipTypeCanTakeOverOwnedTerritory =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getCanTakeOverOwnedTerritory();
        }
      };
  public static final Match<RelationshipType> RelationshipTypeGivesBackOriginalTerritories =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getGivesBackOriginalTerritories();
        }
      };
  public static final Match<RelationshipType> RelationshipTypeCanMoveIntoDuringCombatMove =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType relationship) {
          return relationship.getRelationshipTypeAttachment().getCanMoveIntoDuringCombatMove();
        }
      };
  public static final Match<RelationshipType> RelationshipTypeCanMoveThroughCanals = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType relationship) {
      return relationship.getRelationshipTypeAttachment().getCanMoveThroughCanals();
    }
  };
  public static final Match<RelationshipType> RelationshipTypeRocketsCanFlyOver = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType relationship) {
      return relationship.getRelationshipTypeAttachment().getRocketsCanFlyOver();
    }
  };

  public static Match<String> isValidRelationshipName(final GameData data) {
    return new Match<String>() {
      @Override
      public boolean match(final String relationshipName) {
        return data.getRelationshipTypeList().getRelationshipType(relationshipName) != null;
      }
    };
  }

  public static Match<PlayerID> isAtWar(final PlayerID player, final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return Matches.RelationshipTypeIsAtWar
            .match(data.getRelationshipTracker().getRelationshipType(player, player2));
      }
    };
  }

  public static Match<PlayerID> isAtWarWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return data.getRelationshipTracker().isAtWarWithAnyOfThesePlayers(player2, players);
      }
    };
  }

  public static Match<PlayerID> isAllied(final PlayerID player, final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return Matches.RelationshipTypeIsAllied
            .match(data.getRelationshipTracker().getRelationshipType(player, player2));
      }
    };
  }

  public static Match<PlayerID> isAlliedWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return data.getRelationshipTracker().isAlliedWithAnyOfThesePlayers(player2, players);
      }
    };
  }

  public static Match<PlayerID> isNeutral(final PlayerID player, final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return Matches.RelationshipTypeIsNeutral
            .match(data.getRelationshipTracker().getRelationshipType(player, player2));
      }
    };
  }

  public static Match<PlayerID> isNeutralWithAnyOfThesePlayers(final Collection<PlayerID> players,
      final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return data.getRelationshipTracker().isNeutralWithAnyOfThesePlayers(player2, players);
      }
    };
  }

  public static Match<Unit> UnitIsOwnedAndIsFactoryOrCanProduceUnits(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return (UnitCanProduceUnits.match(u) && unitIsOwnedBy(player).match(u));
      }
    };
  }

  public static Match<Unit> UnitCanReceivesAbilityWhenWith() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return !UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith().isEmpty();
      }
    };
  }

  public static Match<Unit> UnitCanReceivesAbilityWhenWith(final String filterForAbility,
      final String filterForUnitType) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        for (final String receives : UnitAttachment.get(u.getType()).getReceivesAbilityWhenWith()) {
          final String[] s = receives.split(":");
          if (s[0].equals(filterForAbility) && s[1].equals(filterForUnitType)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static Match<Unit> UnitHasWhenCombatDamagedEffect() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return !UnitAttachment.get(u.getType()).getWhenCombatDamaged().isEmpty();
      }
    };
  }

  public static Match<Unit> UnitHasWhenCombatDamagedEffect(final String filterForEffect) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (!UnitHasWhenCombatDamagedEffect().match(u)) {
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
      }
    };
  }

  public static Match<Territory> TerritoryHasWhenCapturedByGoesTo() {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta == null) {
          return false;
        }
        return !ta.getWhenCapturedByGoesTo().isEmpty();
      }
    };
  }

  public static Match<Unit> UnitWhenCapturedChangesIntoDifferentUnitType() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return !UnitAttachment.get(u.getType()).getWhenCapturedChangesInto().isEmpty();
      }
    };
  }

  public static Match<AbstractUserActionAttachment> AbstractUserActionAttachmentCanBeAttempted(
      final HashMap<ICondition, Boolean> testedConditions) {
    return new Match<AbstractUserActionAttachment>() {
      @Override
      public boolean match(final AbstractUserActionAttachment paa) {
        return paa.hasAttemptsLeft() && paa.canPerform(testedConditions);
      }
    };
  }

  public static Match<PoliticalActionAttachment> PoliticalActionHasCostBetween(final int greaterThanEqualTo,
      final int lessThanEqualTo) {
    return new Match<PoliticalActionAttachment>() {
      @Override
      public boolean match(final PoliticalActionAttachment paa) {
        return (paa.getCostPU() >= greaterThanEqualTo && paa.getCostPU() <= lessThanEqualTo);
      }
    };
  }

  public static final Match<Unit> UnitCanOnlyPlaceInOriginalTerritories = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      final Set<String> specialOptions = ua.getSpecial();
      for (final String option : specialOptions) {
        if (option.equals("canOnlyPlaceInOriginalTerritories")) {
          return true;
        }
      }
      return false;
    }
  };

  /**
   * Accounts for OccupiedTerrOf. Returns false if there is no territory attachment (like if it is water).
   */
  public static Match<Territory> TerritoryIsOriginallyOwnedBy(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta == null) {
          return false;
        }
        final PlayerID originalOwner = ta.getOriginalOwner();
        if (originalOwner == null) {
          return player == null;
        }
        return originalOwner.equals(player);
      }
    };
  }

  public static Match<PlayerID> isAlliedAndAlliancesCanChainTogether(final PlayerID player, final GameData data) {
    return new Match<PlayerID>() {
      @Override
      public boolean match(final PlayerID player2) {
        return RelationshipTypeIsAlliedAndAlliancesCanChainTogether
            .match(data.getRelationshipTracker().getRelationshipType(player, player2));
      }
    };
  }

  public static final Match<RelationshipType> RelationshipTypeIsAlliedAndAlliancesCanChainTogether =
      new Match<RelationshipType>() {
        @Override
        public boolean match(final RelationshipType rt) {
          return RelationshipTypeIsAllied.match(rt)
              && rt.getRelationshipTypeAttachment().getAlliancesCanChainTogether();
        }
      };
  public static final Match<RelationshipType> RelationshipTypeIsDefaultWarPosition = new Match<RelationshipType>() {
    @Override
    public boolean match(final RelationshipType rt) {
      return rt.getRelationshipTypeAttachment().getIsDefaultWarPosition();
    }
  };

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
    return new Match<PoliticalActionAttachment>() {
      @Override
      public boolean match(final PoliticalActionAttachment paa) {
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
      }
    };
  }

  public static Match<PoliticalActionAttachment> politicalActionAffectsAtLeastOneAlivePlayer(
      final PlayerID currentPlayer, final GameData data) {
    return new Match<PoliticalActionAttachment>() {
      @Override
      public boolean match(final PoliticalActionAttachment paa) {
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
      }
    };
  }

  public static Match<Territory> airCanLandOnThisAlliedNonConqueredLandTerritory(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
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
      }
    };
  }

  public static Match<Territory> territoryAllowsRocketsCanFlyOver(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (!Matches.TerritoryIsLand.match(t)) {
          return true;
        }
        final PlayerID owner = t.getOwner();
        if (owner == null || owner.isNull()) {
          return true;
        }
        final RelationshipTracker rt = data.getRelationshipTracker();
        return rt.rocketsCanFlyOver(player, owner);
      }
    };
  }

  public static Match<Unit> unitCanScrambleOnRouteDistance(final Route route) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return UnitAttachment.get(u.getType()).getMaxScrambleDistance() >= route.getMovementCost(u);
      }
    };
  }

  public static final Match<Unit> unitCanIntercept = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      return UnitAttachment.get(u.getType()).getCanIntercept();
    }
  };
  public static final Match<Unit> unitCanEscort = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      return UnitAttachment.get(u.getType()).getCanEscort();
    }
  };
  public static final Match<Unit> unitCanAirBattle = new Match<Unit>() {
    @Override
    public boolean match(final Unit u) {
      return UnitAttachment.get(u.getType()).getCanAirBattle();
    }
  };

  public static Match<Territory> territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(
      final PlayerID attacker) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.getOwner().equals(attacker)) {
          return false;
        }
        if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
          return false;
        }
        if (!Matches.TerritoryIsPassableAndNotRestricted(attacker, t.getData()).match(t)) {
          return false;
        }
        return RelationshipTypeCanTakeOverOwnedTerritory
            .match(t.getData().getRelationshipTracker().getRelationshipType(attacker, t.getOwner()));
      }
    };
  }

  public static Match<Territory> territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(
      final PlayerID movingPlayer) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (t.getOwner().equals(movingPlayer)) {
          return true;
        }
        if (t.getOwner().equals(PlayerID.NULL_PLAYERID) && t.isWater()) {
          return true;
        }
        return t.getData().getRelationshipTracker().canMoveIntoDuringCombatMove(movingPlayer, t.getOwner());
      }
    };
  }

  public static Match<Unit> UnitCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAA, final boolean doNotIncludeBombardingSeaUnits) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        return Matches.UnitTypeCanBeInBattle(attack, isLandBattle, u.getOwner(), battleRound,
            includeAttackersThatCanNotMove, doNotIncludeAA, doNotIncludeBombardingSeaUnits).match(u.getType());
      }
    };
  }

  public static Match<UnitType> UnitTypeCanBeInBattle(final boolean attack, final boolean isLandBattle,
      final PlayerID player, final int battleRound, final boolean includeAttackersThatCanNotMove,
      final boolean doNotIncludeAA, final boolean doNotIncludeBombardingSeaUnits) {
    return new Match<UnitType>() {
      @Override
      public boolean match(final UnitType ut) {
        // we want to filter out anything like factories, or units that have no combat ability AND cannot be taken
        // casualty.
        // in addition, as of right now AA guns cannot fire on the offensive side, so we want to take them out too,
        // unless they have other
        // combat abilities.
        final Match<UnitType> supporterOrNotInfrastructure =
            new CompositeMatchOr<>(Matches.UnitTypeIsInfrastructure.invert(),
                Matches.UnitTypeIsSupporterOrHasCombatAbility(attack, player));
        final Match<UnitType> combat;
        if (attack) {
          // AND match
          final CompositeMatch<UnitType> attackMatchAND = new CompositeMatchAnd<>();
          attackMatchAND.add(supporterOrNotInfrastructure);
          if (!includeAttackersThatCanNotMove) {
            attackMatchAND.add(Matches.UnitTypeCanNotMoveDuringCombatMove.invert());
            attackMatchAND.add(Matches.UnitTypeCanMove(player));
          }
          if (isLandBattle) {
            if (doNotIncludeBombardingSeaUnits) {
              attackMatchAND.add(Matches.UnitTypeIsSea.invert());
            }
          } else { // is sea battle
            attackMatchAND.add(Matches.UnitTypeIsLand.invert());
          }
          // assign it
          combat = attackMatchAND;
        } else { // defense
          // AND match
          final CompositeMatch<UnitType> defenseMatchAND = new CompositeMatchAnd<>();
          {
            // OR match
            final CompositeMatch<UnitType> defenseMatchOR = new CompositeMatchOr<>();
            if (!doNotIncludeAA) {
              defenseMatchOR.add(new CompositeMatchAnd<>(Matches.UnitTypeIsAAforCombatOnly,
                  Matches.UnitTypeIsAAthatCanFireOnRound(battleRound)));
            }
            defenseMatchOR.add(supporterOrNotInfrastructure);
            defenseMatchAND.add(defenseMatchOR);
          }
          if (isLandBattle) {
            defenseMatchAND.add(Matches.UnitTypeIsSea.invert());
          } else { // is sea battle
            defenseMatchAND.add(Matches.UnitTypeIsLand.invert());
          }
          // assign it
          combat = defenseMatchAND;
        }
        return combat.match(ut);
      }
    };
  }

  public static final Match<Unit> UnitIsAirborne = new Match<Unit>() {
    @Override
    public boolean match(final Unit obj) {
      return ((TripleAUnit) obj).getAirborne();
    }
  };

  public static <T> Match<T> isNotInList(final List<T> list) {
    return new Match<T>() {
      @Override
      public boolean match(final T ter) {
        return !list.contains(ter);
      }
    };
  }

  /** Creates new Matches. */
  private Matches() {}
}
