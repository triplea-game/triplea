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
import java.util.Map;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Utility class providing static operations to Unit objects.
 *
 * <p>Note: avoid adding methods to this class, instead favor placing utils into topic specific
 * utility classes.
 */
@UtilityClass
public class UnitUtils {

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
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
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
          productionCapacity =
              (Properties.getWW2V2(data.getProperties())
                      || Properties.getWW2V3(data.getProperties()))
                  ? 0
                  : 1;
        }
      }
    } else {
      if (ua.getCanProduceXUnits() < 0
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
              data.getProperties())) {
        productionCapacity = territoryProduction;
      } else if (ua.getCanProduceXUnits() < 0
          && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
        productionCapacity = territoryUnitProduction;
      } else {
        productionCapacity = ua.getCanProduceXUnits();
      }
      if (productionCapacity < 1
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
              data.getProperties())) {
        productionCapacity =
            (Properties.getWW2V2(data.getProperties()) || Properties.getWW2V3(data.getProperties()))
                ? 0
                : 1;
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
      final Territory territory) {
    return unitsThatWillGetAttributes.stream()
        .map(
            receivingUnit -> {
              final CompositeChange unitChange = new CompositeChange();
              final int transferHits =
                  Math.min(
                      unitGivingAttributes.getHits(),
                      // ensure the receiving unit has at least 1 hit point after hits are
                      // transferred
                      receivingUnit.getUnitAttachment().getHitPoints() - 1);
              if (transferHits > 0) {
                unitChange.add(
                    ChangeFactory.unitsHit(
                        IntegerMap.of(Map.of(receivingUnit, transferHits)), List.of(territory)));
              }

              final int transferDamage =
                  Math.min(
                      unitGivingAttributes.getUnitDamage(),
                      receivingUnit.getHowMuchDamageCanThisUnitTakeTotal(territory));
              if (transferDamage > 0) {
                unitChange.add(
                    ChangeFactory.bombingUnitDamage(
                        IntegerMap.of(Map.of(receivingUnit, transferDamage)), List.of(territory)));
              }
              return unitChange;
            })
        .reduce(new CompositeChange(), CompositeChange::new);
  }
}
