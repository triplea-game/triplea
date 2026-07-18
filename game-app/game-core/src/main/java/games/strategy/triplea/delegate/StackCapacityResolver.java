package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Resolves terrain capacity and unit stacking cost for every unit-entry path. */
public final class StackCapacityResolver {
  private static final String SMALL_FRONT_MEUSE_GAME = "Small Front: Meuse Corridor";
  private static final Map<String, Integer> SMALL_FRONT_MEUSE_CAPACITIES =
      Map.of("Open", 7, "Town", 6, "Forest", 5);

  private StackCapacityResolver() {}

  public static OptionalInt resolveCapacity(final Collection<TerritoryEffect> effects) {
    return resolveCapacity(effects, "");
  }

  static OptionalInt resolveCapacity(
      final Collection<TerritoryEffect> effects, final String gameName) {
    boolean configured = false;
    int finiteCapacity = Integer.MAX_VALUE;
    for (final TerritoryEffect effect : effects) {
      final OptionalInt configuredCapacity =
          TerritoryEffectAttachment.get(effect).getStackCapacity();
      if (configuredCapacity.isEmpty()) {
        continue;
      }
      configured = true;
      final int capacity =
          SMALL_FRONT_MEUSE_GAME.equals(gameName)
              ? SMALL_FRONT_MEUSE_CAPACITIES.getOrDefault(
                  effect.getName(), configuredCapacity.getAsInt())
              : configuredCapacity.getAsInt();
      if (capacity >= 0) {
        finiteCapacity = Math.min(finiteCapacity, capacity);
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
        pendingUnits,
        owner.getData().getGameName());
  }

  static List<Unit> filterUnitsToFit(
      final Collection<Unit> candidates,
      final GamePlayer owner,
      final Collection<TerritoryEffect> effects,
      final Collection<Unit> existingUnits,
      final Collection<Unit> pendingUnits) {
    return filterUnitsToFit(candidates, owner, effects, existingUnits, pendingUnits, "");
  }

  private static List<Unit> filterUnitsToFit(
      final Collection<Unit> candidates,
      final GamePlayer owner,
      final Collection<TerritoryEffect> effects,
      final Collection<Unit> existingUnits,
      final Collection<Unit> pendingUnits,
      final String gameName) {
    final OptionalInt capacity = resolveCapacity(effects, gameName);
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
