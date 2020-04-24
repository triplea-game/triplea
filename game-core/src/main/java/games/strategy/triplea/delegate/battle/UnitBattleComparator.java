package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;

// TODO: make the Comparator be serializable. To get there all class members need to be serializable
// or removed.

/**
 * A comparator that sorts units in order from most likely to least likely to be chosen as a battle
 * loss.
 */
public class UnitBattleComparator implements Comparator<Unit> {
  private final boolean defending;
  private final IntegerMap<UnitType> costs;
  private final boolean bonus;
  private final boolean ignorePrimaryPower;
  private final Collection<TerritoryEffect> territoryEffects;
  private final Collection<UnitType> multiHitpointCanRepair = new HashSet<>();
  private final boolean amphibious;

  public UnitBattleComparator(
      final boolean defending,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data) {
    this(defending, costs, territoryEffects, data, false, false, false);
  }

  public UnitBattleComparator(
      final boolean defending,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean bonus) {
    this(defending, costs, territoryEffects, data, bonus, false, false);
  }

  public UnitBattleComparator(
      final boolean defending,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean bonus,
      final boolean ignorePrimaryPower,
      final boolean isAmphibious) {
    this.defending = defending;
    this.costs = costs;
    this.bonus = bonus;
    this.ignorePrimaryPower = ignorePrimaryPower;
    this.territoryEffects = territoryEffects;
    this.amphibious = isAmphibious;
    if (Properties.getBattleshipsRepairAtEndOfRound(data)
        || Properties.getBattleshipsRepairAtBeginningOfRound(data)) {
      for (final UnitType ut : data.getUnitTypeList()) {
        if (Matches.unitTypeHasMoreThanOneHitPointTotal().test(ut)) {
          multiHitpointCanRepair.add(ut);
        }
      }
      // TODO: check if there are units in the game that can repair this unit
    }
  }

  @Override
  public int compare(final Unit u1, final Unit u2) {
    if (u1.equals(u2)) {
      return 0;
    }
    final boolean transporting1 = TransportTracker.isTransporting(u1);
    final boolean transporting2 = TransportTracker.isTransporting(u2);
    final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
    final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
    if (ua1.equals(ua2) && u1.getOwner().equals(u2.getOwner())) {
      if (transporting1 && !transporting2) {
        return 1;
      }
      if (!transporting1 && transporting2) {
        return -1;
      }
      return 0;
    }
    final boolean airOrCarrierOrTransport1 =
        Matches.unitIsAir().test(u1)
            || Matches.unitIsCarrier().test(u1)
            || (!transporting1 && Matches.unitIsTransport().test(u1));
    final boolean airOrCarrierOrTransport2 =
        Matches.unitIsAir().test(u2)
            || Matches.unitIsCarrier().test(u2)
            || (!transporting2 && Matches.unitIsTransport().test(u2));
    final boolean subDestroyer1 =
        Matches.unitHasSubBattleAbilities().test(u1) || Matches.unitIsDestroyer().test(u1);
    final boolean subDestroyer2 =
        Matches.unitHasSubBattleAbilities().test(u2) || Matches.unitIsDestroyer().test(u2);
    final boolean multiHpCanRepair1 = multiHitpointCanRepair.contains(u1.getType());
    final boolean multiHpCanRepair2 = multiHitpointCanRepair.contains(u2.getType());
    if (!ignorePrimaryPower) {
      final var combatModifiers =
          CombatModifiers.builder()
              .defending(defending)
              .territoryEffects(territoryEffects)
              .amphibious(amphibious)
              .build();
      int power1 = 8 * getUnitPowerForSorting(u1, combatModifiers);
      int power2 = 8 * getUnitPowerForSorting(u2, combatModifiers);
      if (bonus) {
        if (subDestroyer1 && !subDestroyer2) {
          power1 += 4;
        } else if (!subDestroyer1 && subDestroyer2) {
          power2 += 4;
        }
        if (multiHpCanRepair1 && !multiHpCanRepair2) {
          power1++;
        } else if (!multiHpCanRepair1 && multiHpCanRepair2) {
          power2++;
        }
        if (transporting1 && !transporting2) {
          power1++;
        } else if (!transporting1 && transporting2) {
          power2++;
        }
        if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2) {
          power1++;
        } else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2) {
          power2++;
        }
      }
      if (power1 != power2) {
        return power1 - power2;
      }
    }
    {
      final int cost1 = costs.getInt(u1.getType());
      final int cost2 = costs.getInt(u2.getType());
      if (cost1 != cost2) {
        return cost1 - cost2;
      }
    }
    {
      final var combatModifiers =
          CombatModifiers.builder()
              .defending(!defending)
              .territoryEffects(territoryEffects)
              .amphibious(amphibious)
              .build();
      int power1reverse = 8 * getUnitPowerForSorting(u1, combatModifiers);
      int power2reverse = 8 * getUnitPowerForSorting(u2, combatModifiers);
      if (bonus) {
        if (subDestroyer1 && !subDestroyer2) {
          power1reverse += 4;
        } else if (!subDestroyer1 && subDestroyer2) {
          power2reverse += 4;
        }
        if (multiHpCanRepair1 && !multiHpCanRepair2) {
          power1reverse++;
        } else if (!multiHpCanRepair1 && multiHpCanRepair2) {
          power2reverse++;
        }
        if (transporting1 && !transporting2) {
          power1reverse++;
        } else if (!transporting1 && transporting2) {
          power2reverse++;
        }
        if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2) {
          power1reverse++;
        } else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2) {
          power2reverse++;
        }
      }
      if (power1reverse != power2reverse) {
        return power1reverse - power2reverse;
      }
    }
    if (subDestroyer1 && !subDestroyer2) {
      return 1;
    } else if (!subDestroyer1 && subDestroyer2) {
      return -1;
    }
    if (multiHpCanRepair1 && !multiHpCanRepair2) {
      return 1;
    } else if (!multiHpCanRepair1 && multiHpCanRepair2) {
      return -1;
    }
    if (transporting1 && !transporting2) {
      return 1;
    } else if (!transporting1 && transporting2) {
      return -1;
    }
    if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2) {
      return 1;
    } else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2) {
      return -1;
    }
    return ua1.getMovement(u1.getOwner()) - ua2.getMovement(u2.getOwner());
  }

  @Builder
  @Getter
  public static class CombatModifiers {
    @Builder.Default private Collection<TerritoryEffect> territoryEffects = List.of();
    private final boolean amphibious;
    private final boolean defending;
  }

  /**
   * This returns the exact Power that a unit has according to what DiceRoll.rollDiceLowLuck() would
   * give it. As such, it needs to exactly match DiceRoll, otherwise this method will become
   * useless. It does NOT take into account SUPPORT. It DOES take into account ROLLS.
   */
  private int getUnitPowerForSorting(final Unit unit, final CombatModifiers combatModifiers) {
    final GameData data = unit.getData();
    final boolean lhtrBombers = Properties.getLhtrHeavyBombers(data);
    final UnitAttachment ua = UnitAttachment.get(unit.getType());

    final GamePlayer owner = unit.getOwner();
    final int rolls =
        combatModifiers.defending ? ua.getDefenseRolls(owner) : ua.getAttackRolls(owner);
    int strengthWithoutSupport = 0;
    // Find the strength the unit has without support
    // lhtr heavy bombers take best of n dice for both attack and defense
    if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
      strengthWithoutSupport =
          combatModifiers.defending ? ua.getDefense(owner) : ua.getAttack(owner);
      strengthWithoutSupport +=
          TerritoryEffectHelper.getTerritoryCombatBonus(
              unit.getType(), combatModifiers.territoryEffects, combatModifiers.defending);
      // just add one like LL if we are LHTR bombers
      strengthWithoutSupport =
          Math.min(Math.max(strengthWithoutSupport + 1, 0), data.getDiceSides());
    } else {
      for (int i = 0; i < rolls; i++) {
        final int tempStrength =
            combatModifiers.defending ? ua.getDefense(owner) : ua.getAttack(owner);
        strengthWithoutSupport +=
            TerritoryEffectHelper.getTerritoryCombatBonus(
                unit.getType(), combatModifiers.territoryEffects, combatModifiers.defending);
        strengthWithoutSupport += Math.min(Math.max(tempStrength, 0), data.getDiceSides());
      }
    }

    if (combatModifiers.amphibious) {
      strengthWithoutSupport += ua.getIsMarine();
    }
    return strengthWithoutSupport;
  }
}
