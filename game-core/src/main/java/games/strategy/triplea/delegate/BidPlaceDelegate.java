package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

@MapSupport
public class BidPlaceDelegate extends AbstractPlaceDelegate {
  public BidPlaceDelegate() {}

  // Allow production of any number of units
  @Override
  protected String checkProduction(final Territory to, final Collection<Unit> units, final PlayerID player) {
    return null;
  }

  // Return whether we can place bid in a certain territory
  @Override
  protected String canProduce(final Territory to, final Collection<Unit> units, final PlayerID player) {
    return canProduce(to, to, units, player);
  }

  @Override
  protected String canProduce(final Territory producer, final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    // we can place if no enemy units and its water
    if (to.isWater()) {
      if (units.stream().anyMatch(Matches.unitIsLand())) {
        return "Cant place land units at sea";
      } else if (to.getUnits().anyMatch(Matches.enemyUnit(player, getData()))) {
        return "Cant place in sea zone containing enemy units";
      } else if (!to.getUnits().anyMatch(Matches.unitIsOwnedBy(player))) {
        return "Cant place in sea zone that does not contain a unit owned by you";
      } else {
        return null;
      }
    }

    // we can place on territories we own
    if (units.stream().anyMatch(Matches.unitIsSea())) {
      return "Cant place sea units on land";
    } else if (to.getOwner() == null) {
      return "You dont own " + to.getName();
    } else if (!to.getOwner().equals(player)) {
      final PlayerAttachment pa = PlayerAttachment.get(to.getOwner());
      if ((pa != null) && (pa.getGiveUnitControl() != null) && pa.getGiveUnitControl().contains(player)) {
        return null;
      } else if (to.getUnits().anyMatch(Matches.unitIsOwnedBy(player))) {
        return null;
      }
      return "You dont own " + to.getName();
    } else {
      return null;
    }
  }

  @Override
  protected List<Territory> getAllProducers(final Territory to, final PlayerID player,
      final Collection<Unit> unitsToPlace) {
    final List<Territory> producers = new ArrayList<>();
    producers.add(to);
    return producers;
  }

  @Override
  protected int getMaxUnitsToBePlaced(final Collection<Unit> units, final Territory to, final PlayerID player,
      final boolean countSwitchedProductionToNeighbors) {
    if (units == null) {
      return -1;
    }
    return units.size();
  }

  @Override
  protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to,
      final PlayerID player, final boolean countSwitchedProductionToNeighbors,
      final Collection<Territory> notUsableAsOtherProducers,
      final Map<Territory, Integer> currentAvailablePlacementForOtherProducers) {
    if (units == null) {
      return -1;
    }
    return units.size();
  }

  @Override
  protected int getMaxUnitsToBePlacedFrom(final Territory producer, final Collection<Unit> units, final Territory to,
      final PlayerID player) {
    if (units == null) {
      return -1;
    }
    return getMaxUnitsToBePlacedFrom(producer, units, to, player, false, null, null);
  }

  // Return collection of bid units which can placed in a land territory
  @Override
  protected Collection<Unit> getUnitsToBePlacedLand(final Territory to, final Collection<Unit> units,
      final PlayerID player) {
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> placeableUnits = new ArrayList<>();
    // we add factories and constructions later
    final Predicate<Unit> groundUnits = Matches.unitIsLand().and(Matches.unitIsNotConstruction());
    final Predicate<Unit> airUnits = Matches.unitIsAir().and(Matches.unitIsNotConstruction());
    placeableUnits.addAll(CollectionUtils.getMatches(units, groundUnits));
    placeableUnits.addAll(CollectionUtils.getMatches(units, airUnits));
    if (units.stream().anyMatch(Matches.unitIsConstruction())) {
      final IntegerMap<String> constructionsMap = howManyOfEachConstructionCanPlace(to, to, units, player);
      final Collection<Unit> skipUnit = new ArrayList<>();
      for (final Unit currentUnit : CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
        final int maxUnits = howManyOfConstructionUnit(currentUnit, constructionsMap);
        if (maxUnits > 0) {
          // we are doing this because we could have multiple unitTypes with the same constructionType, so we have to be
          // able to place the
          // max placement by constructionType of each unitType
          if (skipUnit.contains(currentUnit)) {
            continue;
          }
          placeableUnits
              .addAll(CollectionUtils.getNMatches(units, maxUnits, Matches.unitIsOfType(currentUnit.getType())));
          skipUnit.addAll(CollectionUtils.getMatches(units, Matches.unitIsOfType(currentUnit.getType())));
        }
      }
    }
    // remove any units that require other units to be consumed on creation (veqryn)
    if (placeableUnits.stream().anyMatch(Matches.unitConsumesUnitsOnCreation())) {
      final Collection<Unit> unitsWhichConsume =
          CollectionUtils.getMatches(placeableUnits, Matches.unitConsumesUnitsOnCreation());
      for (final Unit unit : unitsWhichConsume) {
        if (Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo).negate().test(unit)) {
          placeableUnits.remove(unit);
        }
      }
    }
    // now check stacking limits
    final Collection<Unit> placeableUnits2 = new ArrayList<>();
    final Collection<UnitType> typesAlreadyChecked = new ArrayList<>();
    for (final Unit currentUnit : placeableUnits) {
      final UnitType ut = currentUnit.getType();
      if (typesAlreadyChecked.contains(ut)) {
        continue;
      }
      typesAlreadyChecked.add(ut);
      placeableUnits2.addAll(
          CollectionUtils.getNMatches(placeableUnits, UnitAttachment.getMaximumNumberOfThisUnitTypeToReachStackingLimit(
              "placementLimit", ut, to, player, getData()), Matches.unitIsOfType(ut)));
    }
    return placeableUnits2;
  }
}
