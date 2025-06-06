package games.strategy.triplea.delegate;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Placeholder for all calculations to do with TerritoryEffects. */
public final class TerritoryEffectHelper {
  private TerritoryEffectHelper() {}

  public static Collection<TerritoryEffect> getEffects(final Territory location) {
    return TerritoryAttachment.get(location)
        .map(TerritoryAttachment::getTerritoryEffect)
        .orElse(new ArrayList<>());
  }

  public static int getTerritoryCombatBonus(
      final UnitType type, final Collection<TerritoryEffect> effects, final boolean defending) {
    int combatBonus = 0;
    for (final TerritoryEffect effect : effects) {
      combatBonus += TerritoryEffectAttachment.get(effect).getCombatEffect(type, defending);
    }
    return combatBonus;
  }

  private static boolean unitTypeLoosesBlitz(final UnitType type, final Territory location) {
    checkNotNull(type);
    checkNotNull(location);

    for (final TerritoryEffect effect : getEffects(location)) {
      if (TerritoryEffectAttachment.get(effect).getNoBlitz().contains(type)) {
        return true;
      }
    }
    return false;
  }

  public static boolean unitKeepsBlitz(final Unit unit, final Territory location) {
    return unitTypeKeepsBlitz(unit.getType(), location);
  }

  private static boolean unitTypeKeepsBlitz(final UnitType type, final Territory location) {
    return !unitTypeLoosesBlitz(type, location);
  }

  public static Set<UnitType> getUnitTypesThatLostBlitz(final Collection<Territory> steps) {
    final Set<UnitType> unitTypes = new HashSet<>();
    for (final Territory location : steps) {
      for (final TerritoryEffect effect : getEffects(location)) {
        unitTypes.addAll(TerritoryEffectAttachment.get(effect).getNoBlitz());
      }
    }
    return unitTypes;
  }

  public static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(
      final Territory location) {
    final Set<UnitType> unitTypes = new HashSet<>();
    for (final TerritoryEffect effect : getEffects(location)) {
      unitTypes.addAll(TerritoryEffectAttachment.get(effect).getUnitsNotAllowed());
    }
    return unitTypes;
  }

  public static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(
      final Collection<Territory> steps) {
    final Set<UnitType> unitTypes = new HashSet<>();
    for (final Territory location : steps) {
      unitTypes.addAll(getUnitTypesForUnitsNotAllowedIntoTerritory(location));
    }
    return unitTypes;
  }

  public static BigDecimal getMovementCost(final Territory t, final Unit unit) {
    return getMaxMovementCost(t, Set.of(unit));
  }

  /**
   * Finds movement cost for each unit by adding 1 plus any territory effects and then returns the
   * max movement across all units. If no territory effects then just returns the base cost of 1.
   */
  public static BigDecimal getMaxMovementCost(final Territory t, final Collection<Unit> units) {
    if (getEffects(t).isEmpty() || units.isEmpty()) {
      return BigDecimal.ONE;
    }
    BigDecimal max = new BigDecimal(Integer.MIN_VALUE);
    for (final Unit unit : units) {
      BigDecimal movementCost = BigDecimal.ONE;
      for (final TerritoryEffect effect : getEffects(t)) {
        movementCost = movementCost.add(getMovementCostModiferForUnitType(effect, unit.getType()));
      }
      if (movementCost.compareTo(max) > 0) {
        max = movementCost;
      }
    }
    return max;
  }

  private static BigDecimal getMovementCostModiferForUnitType(
      final TerritoryEffect effect, final UnitType unitType) {
    final Map<UnitType, BigDecimal> map =
        TerritoryEffectAttachment.get(effect).getMovementCostModifier();
    return map.getOrDefault(unitType, BigDecimal.ZERO);
  }
}
