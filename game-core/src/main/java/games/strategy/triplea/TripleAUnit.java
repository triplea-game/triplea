package games.strategy.triplea;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Extended unit for triplea games.
 *
 * <p>As with all game data components, changes made to this unit must be made through a Change
 * instance. Calling setters on this directly will not serialize the changes across the network.
 */
public class TripleAUnit {

  public static int getProductionPotentialOfTerritory(
      final Collection<Unit> unitsAtStartOfStepInTerritory,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    return getHowMuchCanUnitProduce(
        getBiggestProducer(unitsAtStartOfStepInTerritory, producer, player, data, accountForDamage),
        producer,
        player,
        data,
        accountForDamage,
        mathMaxZero);
  }

  /**
   * Returns the unit from the specified collection that has the largest production capacity within
   * the specified territory.
   *
   * @param accountForDamage {@code true} if the production capacity should account for unit damage;
   *     otherwise {@code false}.
   */
  public static Unit getBiggestProducer(
      final Collection<Unit> units,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and(producer.isWater() ? Matches.unitIsLand().negate() : Matches.unitIsSea().negate());
    final Collection<Unit> factories = CollectionUtils.getMatches(units, factoryMatch);
    if (factories.isEmpty()) {
      return null;
    }
    final IntegerMap<Unit> productionPotential = new IntegerMap<>();
    Unit highestUnit = factories.iterator().next();
    int highestCapacity = Integer.MIN_VALUE;
    for (final Unit u : factories) {
      final int capacity =
          getHowMuchCanUnitProduce(u, producer, player, data, accountForDamage, false);
      productionPotential.put(u, capacity);
      if (capacity > highestCapacity) {
        highestCapacity = capacity;
        highestUnit = u;
      }
    }
    return highestUnit;
  }

  /**
   * Returns the production capacity for the specified unit within the specified territory.
   *
   * @param accountForDamage {@code true} if the production capacity should account for unit damage;
   *     otherwise {@code false}.
   * @param mathMaxZero {@code true} if a negative production capacity should be rounded to zero;
   *     {@code false} to allow a negative production capacity.
   */
  public static int getHowMuchCanUnitProduce(
      final Unit unit,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    if (unit == null) {
      return 0;
    }
    if (!Matches.unitCanProduceUnits().test(unit)) {
      return 0;
    }
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    final TerritoryAttachment ta = TerritoryAttachment.get(producer);
    int territoryProduction = 0;
    int territoryUnitProduction = 0;
    if (ta != null) {
      territoryProduction = ta.getProduction();
      territoryUnitProduction = ta.getUnitProduction();
    }
    int productionCapacity;
    if (accountForDamage) {
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        if (ua.getCanProduceXUnits() < 0) {
          // we could use territoryUnitProduction OR
          // territoryProduction if we wanted to, however we should
          // change damage to be based on whichever we choose.
          productionCapacity = territoryUnitProduction - unit.getUnitDamage();
        } else {
          productionCapacity = ua.getCanProduceXUnits() - unit.getUnitDamage();
        }
      } else {
        productionCapacity = territoryProduction;
        if (productionCapacity < 1) {
          productionCapacity = (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0 : 1;
        }
      }
    } else {
      if (ua.getCanProduceXUnits() < 0
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = territoryProduction;
      } else if (ua.getCanProduceXUnits() < 0
          && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = territoryUnitProduction;
      } else {
        productionCapacity = ua.getCanProduceXUnits();
      }
      if (productionCapacity < 1
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0 : 1;
      }
    }
    // Increase production if have industrial technology
    if (territoryProduction
        >= TechAbilityAttachment.getMinimumTerritoryValueForProductionBonus(player, data)) {
      productionCapacity += TechAbilityAttachment.getProductionBonus(unit.getType(), player, data);
    }
    return mathMaxZero ? Math.max(0, productionCapacity) : productionCapacity;
  }

  /**
   * Currently made for translating unit damage from one unit to another unit. Will adjust damage to
   * be within max damage for the new units.
   *
   * @return change for unit's properties
   */
  public static Change translateAttributesToOtherUnits(
      final Unit unitGivingAttributes,
      final Collection<Unit> unitsThatWillGetAttributes,
      final Territory t) {
    final CompositeChange changes = new CompositeChange();
    // must look for hits, unitDamage,
    final int combatDamage = unitGivingAttributes.getHits();
    final IntegerMap<Unit> hits = new IntegerMap<>();
    if (combatDamage > 0) {
      for (final Unit u : unitsThatWillGetAttributes) {
        final int maxHitPoints = UnitAttachment.get(u.getType()).getHitPoints();
        final int transferDamage = Math.min(combatDamage, maxHitPoints - 1);
        if (transferDamage <= 0) {
          continue;
        }
        hits.put(u, transferDamage);
      }
    }
    if (!hits.isEmpty()) {
      changes.add(ChangeFactory.unitsHit(hits, List.of(t)));
    }
    final int unitDamage = unitGivingAttributes.getUnitDamage();
    final IntegerMap<Unit> damageMap = new IntegerMap<>();
    if (unitDamage > 0) {
      for (final Unit u : unitsThatWillGetAttributes) {
        final int maxDamage = u.getHowMuchDamageCanThisUnitTakeTotal(t);
        final int transferDamage = Math.max(0, Math.min(unitDamage, maxDamage));
        if (transferDamage <= 0) {
          continue;
        }
        damageMap.put(u, transferDamage);
      }
    }
    if (!damageMap.isEmpty()) {
      changes.add(ChangeFactory.bombingUnitDamage(damageMap));
    }
    return changes;
  }
}
