package games.strategy.triplea;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

  public static Set<UnitType> getUnitTypesFromUnitList(final Collection<Unit> units) {
    return units.stream().map(Unit::getType).collect(Collectors.toSet());
  }

  public static int getProductionPotentialOfTerritory(
      final Collection<Unit> unitsAtStartOfStepInTerritory,
      final Territory producer,
      final GamePlayer player,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    return getHowMuchCanUnitProduce(
        getBiggestProducer(unitsAtStartOfStepInTerritory, producer, player, accountForDamage)
            .orElse(null),
        producer,
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
  public static Optional<Unit> getBiggestProducer(
      final Collection<Unit> units,
      final Territory producer,
      final GamePlayer player,
      final boolean accountForDamage) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and(producer.isWater() ? Matches.unitIsLand().negate() : Matches.unitIsSea().negate());
    final Collection<Unit> factories = CollectionUtils.getMatches(units, factoryMatch);
    if (factories.isEmpty()) {
      return Optional.empty();
    }
    final IntegerMap<Unit> productionPotential = new IntegerMap<>();
    Unit highestUnit = CollectionUtils.getAny(factories);
    int highestCapacity = Integer.MIN_VALUE;
    for (final Unit u : factories) {
      final int capacity = getHowMuchCanUnitProduce(u, producer, accountForDamage, false);
      productionPotential.put(u, capacity);
      if (capacity > highestCapacity) {
        highestCapacity = capacity;
        highestUnit = u;
      }
    }
    return Optional.of(highestUnit);
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
      final @Nullable Unit unit,
      final Territory producer,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    if (unit == null) {
      return 0;
    }
    if (!Matches.unitCanProduceUnits().test(unit)) {
      return 0;
    }
    final UnitAttachment ua = unit.getUnitAttachment();
    int territoryProduction = 0;
    int territoryUnitProduction = 0;
    final Optional<TerritoryAttachment> optionalTerritoryAttachment =
        TerritoryAttachment.get(producer);
    if (optionalTerritoryAttachment.isPresent()) {
      territoryProduction = optionalTerritoryAttachment.get().getProduction();
      territoryUnitProduction = optionalTerritoryAttachment.get().getUnitProduction();
    }
    int productionCapacity;
    final GameProperties properties = producer.getData().getProperties();
    if (accountForDamage) {
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(properties)) {
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
              (Properties.getWW2V2(properties) || Properties.getWW2V3(properties)) ? 0 : 1;
        }
      }
    } else {
      if (ua.getCanProduceXUnits() < 0
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(properties)) {
        productionCapacity = territoryProduction;
      } else if (ua.getCanProduceXUnits() < 0
          && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(properties)) {
        productionCapacity = territoryUnitProduction;
      } else {
        productionCapacity = ua.getCanProduceXUnits();
      }
      if (productionCapacity < 1
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(properties)) {
        productionCapacity =
            (Properties.getWW2V2(properties) || Properties.getWW2V3(properties)) ? 0 : 1;
      }
    }
    final TechTracker techTracker = producer.getData().getTechTracker();
    // Increase production if we have industrial technology
    if (territoryProduction
        >= techTracker.getMinimumTerritoryValueForProductionBonus(unit.getOwner())) {
      productionCapacity += techTracker.getProductionBonus(unit.getOwner(), unit.getType());
    }
    return mathMaxZero ? Math.max(0, productionCapacity) : productionCapacity;
  }

  /**
   * Translates attributes and properties from one unit to a collection of units.
   *
   * <p>Used when a unit is being transformed, so the old unit is going away and the new units are
   * taking its place
   *
   * <p>Currently, it translates: Hits, Damage, Unloaded units, and Transported units
   *
   * <p>Hits and Damage are modified as needed to fit within the limits of the new units. Units will
   * always have at least 1 hp.
   *
   * <p>Unloaded units and transported units are given to the unit that matches stream().findFirst()
   *
   * @return change for unit's properties
   */
  public static Change translateAttributesToOtherUnits(
      final Unit unitGivingAttributes,
      final Collection<Unit> unitsThatWillGetAttributes,
      final Territory territory) {

    // first, translate attributes that can only go to one receiving unit
    final CompositeChange changes =
        unitsThatWillGetAttributes.stream()
            .findFirst()
            .map(
                receivingUnit ->
                    translateDependentUnitsToOtherUnit(unitGivingAttributes, receivingUnit))
            .orElse(new CompositeChange());

    // next, translate attributes that can go to all of the receiving units
    return unitsThatWillGetAttributes.stream()
        .map(
            receivingUnit ->
                translateHitPointsAndDamageToOtherUnit(
                    unitGivingAttributes, territory, receivingUnit))
        .reduce(changes, CompositeChange::new);
  }

  /** Translates dependent units from one unit to another */
  private static CompositeChange translateDependentUnitsToOtherUnit(
      final Unit unitGivingAttributes, final Unit receivingUnit) {
    final CompositeChange unitChange = new CompositeChange();
    final List<Unit> unloaded = unitGivingAttributes.getUnloaded();
    if (!unloaded.isEmpty()) {
      unitChange.add(
          ChangeFactory.unitPropertyChange(receivingUnit, unloaded, Unit.PropertyName.UNLOADED));
    }

    final List<Unit> transporting = unitGivingAttributes.getTransporting();
    return transporting.stream()
        .map(
            transported ->
                new CompositeChange(
                    ChangeFactory.unitPropertyChange(
                        transported, receivingUnit, Unit.PropertyName.TRANSPORTED_BY)))
        .reduce(unitChange, CompositeChange::new);
  }

  private static CompositeChange translateHitPointsAndDamageToOtherUnit(
      final Unit unitGivingAttributes, final Territory territory, final Unit receivingUnit) {
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
  }

  public static @Nullable GamePlayer findPlayerWithMostUnits(final Iterable<Unit> units) {
    final IntegerMap<GamePlayer> playerUnitCount = new IntegerMap<>();
    for (final Unit unit : units) {
      playerUnitCount.add(unit.getOwner(), 1);
    }
    int max = -1;
    GamePlayer player = null;
    for (final GamePlayer current : playerUnitCount.keySet()) {
      final int count = playerUnitCount.getInt(current);
      if (count > max) {
        max = count;
        player = current;
      }
    }
    return player;
  }
}
