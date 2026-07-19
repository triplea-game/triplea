package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

/** Resolves terrain capacity and unit stacking cost for every unit-entry path. */
public final class StackCapacityResolver {
  private StackCapacityResolver() {}

  public static OptionalInt resolveCapacity(final Collection<TerritoryEffect> effects) {
    boolean configured = false;
    int finiteCapacity = Integer.MAX_VALUE;
    for (final TerritoryEffect effect : effects) {
      final OptionalInt capacity = TerritoryEffectAttachment.get(effect).getStackCapacity();
      if (capacity.isEmpty()) {
        continue;
      }
      configured = true;
      if (capacity.getAsInt() >= 0) {
        finiteCapacity = Math.min(finiteCapacity, capacity.getAsInt());
      }
    }
    if (!configured) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(finiteCapacity == Integer.MAX_VALUE ? -1 : finiteCapacity);
  }

  public static int getStackCost(final Unit unit) {
    return unit.getUnitAttachment().getStackCost();
  }

  public static int getOccupiedCapacity(final Collection<Unit> units) {
    return units.stream().mapToInt(StackCapacityResolver::getStackCost).sum();
  }

  public static boolean canFit(
      final Collection<Unit> enteringUnits,
      final GamePlayer owner,
      final Territory territory,
      final Collection<Unit> pendingUnits) {
    return filterUnitsToFit(enteringUnits, owner, territory, pendingUnits).size()
        == enteringUnits.size();
  }

  public static List<Unit> filterUnitsToFit(
      final Collection<Unit> candidates,
      final GamePlayer owner,
      final Territory territory,
      final Collection<Unit> pendingUnits) {
    return filterUnitsToFit(
        candidates,
        owner,
        TerritoryEffectHelper.getEffects(territory),
        alliedUnits(owner, territory.getUnits()),
        pendingUnits);
  }

  static List<Unit> filterUnitsToFit(
      final Collection<Unit> candidates,
      final GamePlayer owner,
      final Collection<TerritoryEffect> effects,
      final Collection<Unit> existingUnits,
      final Collection<Unit> pendingUnits) {
    final OptionalInt capacity = resolveCapacity(effects);
    if (capacity.isEmpty() || capacity.getAsInt() < 0) {
      return new ArrayList<>(candidates);
    }
    int occupied = getOccupiedCapacity(existingUnits) + getOccupiedCapacity(pendingUnits);
    final List<Unit> accepted = new ArrayList<>();
    for (final Unit candidate : candidates) {
      final int cost = getStackCost(candidate);
      if (cost == 0 || occupied + cost <= capacity.getAsInt()) {
        accepted.add(candidate);
        occupied += cost;
      }
    }
    return accepted;
  }

  private static List<Unit> alliedUnits(
      final GamePlayer owner, final Collection<Unit> territoryUnits) {
    return territoryUnits.stream()
        .filter(
            unit ->
                owner.equals(unit.getOwner())
                    || owner.getData().getRelationshipTracker().isAllied(owner, unit.getOwner()))
        .toList();
  }
}
