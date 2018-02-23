package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.util.IntegerMap;

public class UnitBattleComparator implements Comparator<Unit> {
  private final boolean defending;
  private final IntegerMap<UnitType> costs;
  private final GameData gameData;
  private final boolean bonus;
  private final boolean ignorePrimaryPower;
  private final Collection<TerritoryEffect> territoryEffects;
  private final Collection<UnitType> multiHitpointCanRepair = new HashSet<>();

  public UnitBattleComparator(final boolean defending, final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean bonus,
      final boolean ignorePrimaryPower) {
    this.defending = defending;
    this.costs = costs;
    gameData = data;
    this.bonus = bonus;
    this.ignorePrimaryPower = ignorePrimaryPower;
    this.territoryEffects = territoryEffects;
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
    final boolean transporting1 = Matches.transportIsTransporting().test(u1);
    final boolean transporting2 = Matches.transportIsTransporting().test(u2);
    final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
    final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
    if ((ua1 == ua2) && u1.getOwner().equals(u2.getOwner())) {
      if (transporting1 && !transporting2) {
        return 1;
      }
      if (!transporting1 && transporting2) {
        return -1;
      }
      return 0;
    }
    final boolean airOrCarrierOrTransport1 = Matches.unitIsAir().test(u1) || Matches.unitIsCarrier().test(u1)
        || (!transporting1 && Matches.unitIsTransport().test(u1));
    final boolean airOrCarrierOrTransport2 = Matches.unitIsAir().test(u2) || Matches.unitIsCarrier().test(u2)
        || (!transporting2 && Matches.unitIsTransport().test(u2));
    final boolean subDestroyer1 = Matches.unitIsSub().test(u1) || Matches.unitIsDestroyer().test(u1);
    final boolean subDestroyer2 = Matches.unitIsSub().test(u2) || Matches.unitIsDestroyer().test(u2);
    final boolean multiHpCanRepair1 = multiHitpointCanRepair.contains(u1.getType());
    final boolean multiHpCanRepair2 = multiHitpointCanRepair.contains(u2.getType());
    if (!ignorePrimaryPower) {
      int power1 = 8 * BattleCalculator.getUnitPowerForSorting(u1, defending, gameData, territoryEffects);
      int power2 = 8 * BattleCalculator.getUnitPowerForSorting(u2, defending, gameData, territoryEffects);
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
      int power1reverse = 8 * BattleCalculator.getUnitPowerForSorting(u1, !defending, gameData, territoryEffects);
      int power2reverse = 8 * BattleCalculator.getUnitPowerForSorting(u2, !defending, gameData, territoryEffects);
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
