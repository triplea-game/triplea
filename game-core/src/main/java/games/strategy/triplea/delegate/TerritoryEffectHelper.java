package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;

/**
 * Placeholder for all calculations to do with TerritoryEffects.
 */
public class TerritoryEffectHelper {
  public static Collection<TerritoryEffect> getEffects(final Territory location) {
    final TerritoryAttachment ta = TerritoryAttachment.get(location);
    return (ta != null) ? ta.getTerritoryEffect() : new ArrayList<>();
  }

  static int getTerritoryCombatBonus(final UnitType type, final Collection<TerritoryEffect> effects,
      final boolean defending) {
    if ((type == null) || (effects == null) || effects.isEmpty()) {
      return 0;
    }
    int combatBonus = 0;
    for (final TerritoryEffect effect : effects) {
      combatBonus += TerritoryEffectAttachment.get(effect).getCombatEffect(type, defending);
    }
    return combatBonus;
  }

  private static boolean unitTypeLoosesBlitz(final UnitType type, final Territory location) {
    if ((location == null) || (type == null)) {
      throw new IllegalStateException("Location and UnitType cannot be null");
    }
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

  public static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(final Territory location) {
    final Set<UnitType> unitTypes = new HashSet<>();
    for (final TerritoryEffect effect : getEffects(location)) {
      unitTypes.addAll(TerritoryEffectAttachment.get(effect).getUnitsNotAllowed());
    }
    return unitTypes;
  }

  static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(final Collection<Territory> steps) {
    final Set<UnitType> unitTypes = new HashSet<>();
    for (final Territory location : steps) {
      unitTypes.addAll(getUnitTypesForUnitsNotAllowedIntoTerritory(location));
    }
    return unitTypes;
  }
}
