package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

// TODO: make the Comparator be serializable. To get there all class members need to be serializable
// or removed.

/**
 * A comparator that sorts units in order from most likely to least likely to be chosen as a battle
 * loss.
 */
@Value
public class UnitBattleComparator implements Comparator<Unit> {
  IntegerMap<UnitType> costs;
  boolean bonus;
  boolean ignorePrimaryPower;
  Collection<UnitType> multiHitpointCanRepair = new HashSet<>();
  TotalPowerAndTotalRolls totalPowerAndTotalRolls;
  TotalPowerAndTotalRolls reversedTotalPowerAndTotalRolls;

  public UnitBattleComparator(
      final IntegerMap<UnitType> costs,
      final GameData data,
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls) {
    this(costs, data, totalPowerAndTotalRolls, false, false);
  }

  public UnitBattleComparator(
      final IntegerMap<UnitType> costs,
      final GameData data,
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final boolean bonus) {
    this(costs, data, totalPowerAndTotalRolls, bonus, false);
  }

  public UnitBattleComparator(
      final IntegerMap<UnitType> costs,
      final GameData data,
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final boolean bonus,
      final boolean ignorePrimaryPower) {
    this.costs = costs;
    this.totalPowerAndTotalRolls = totalPowerAndTotalRolls;
    this.reversedTotalPowerAndTotalRolls = totalPowerAndTotalRolls.buildOpposite();
    this.bonus = bonus;
    this.ignorePrimaryPower = ignorePrimaryPower;
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
    if (ua1.equals(ua2)
        && u1.getOwner().equals(u2.getOwner())
        && u1.getWasAmphibious() == u2.getWasAmphibious()) {
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
      int power1 = 8 * totalPowerAndTotalRolls.calculatePower(u1);
      int power2 = 8 * totalPowerAndTotalRolls.calculatePower(u2);
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
      int power1reverse = 8 * reversedTotalPowerAndTotalRolls.calculatePower(u1);
      int power2reverse = 8 * reversedTotalPowerAndTotalRolls.calculatePower(u2);
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
}
