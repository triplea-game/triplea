package games.strategy.triplea.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

public class AdvancedUtils {
  public static Unit getLastUnitMatching(final List<Unit> units, final Match<Unit> match, final int endIndex) {
    final int index = getIndexOfLastUnitMatching(units, match, endIndex);
    if (index == -1) {
      return null;
    }
    return units.get(index);
  }

  public static int getIndexOfLastUnitMatching(final List<Unit> units, final Match<Unit> match, final int endIndex) {
    for (int i = endIndex; i >= 0; i--) {
      final Unit unit = units.get(i);
      if (match.match(unit)) {
        return i;
      }
    }
    return -1;
  }

  public static List<Unit> interleaveCarriersAndPlanes(final List<Unit> units, final int planesThatDontNeedToLand) {
    if (!(Match.someMatch(units, Matches.UnitIsCarrier) && Match.someMatch(units, Matches.UnitCanLandOnCarrier))) {
      return units;
    }
    // Clone the current list
    final ArrayList<Unit> result = new ArrayList<>(units);
    Unit seekedCarrier = null;
    int indexToPlaceCarrierAt = -1;
    int spaceLeftOnSeekedCarrier = -1;
    int processedPlaneCount = 0;
    final List<Unit> filledCarriers = new ArrayList<>();
    // Loop through all units, starting from the right, and rearrange units
    for (int i = result.size() - 1; i >= 0; i--) {
      final Unit unit = result.get(i);
      final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
      // If this is a plane
      if (ua.getCarrierCost() > 0) {
        // If we haven't ignored enough trailing planes
        if (processedPlaneCount < planesThatDontNeedToLand) {
          // Increase number of trailing planes ignored
          processedPlaneCount++;
          // And skip any processing
          continue;
        }
        // If this is the first carrier seek
        if (seekedCarrier == null) {
          final int seekedCarrierIndex = getIndexOfLastUnitMatching(result,
              new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.isNotInList(filledCarriers)), result.size() - 1);
          if (seekedCarrierIndex == -1) {
            // No carriers left
            break;
          }
          seekedCarrier = result.get(seekedCarrierIndex);
          // Tell the code to insert carrier to the right of this plane
          indexToPlaceCarrierAt = i + 1;
          spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
        }
        spaceLeftOnSeekedCarrier -= ua.getCarrierCost();
        // If the carrier has been filled or overflowed
        if (spaceLeftOnSeekedCarrier <= 0) {
          if (spaceLeftOnSeekedCarrier < 0) {
            // Move current unit index up one, so we re-process this unit (since it can't fit on the current seeked
            // carrier)
            i++;
          }
          // If the seeked carrier is earlier in the list
          if (result.indexOf(seekedCarrier) < i) {
            // Move the carrier up to the planes by: removing carrier, then reinserting it
            // (index decreased cause removal of carrier reduced indexes)
            result.remove(seekedCarrier);
            result.add(indexToPlaceCarrierAt - 1, seekedCarrier);
            // We removed carrier in earlier part of list, so decrease index
            i--;
            filledCarriers.add(seekedCarrier);
            // Find the next carrier
            seekedCarrier = getLastUnitMatching(result,
                new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.isNotInList(filledCarriers)), result.size() - 1);
            if (seekedCarrier == null) {
              // No carriers left
              break;
            }
            // Place next carrier right before this plane (which just filled the old carrier that was just moved)
            indexToPlaceCarrierAt = i;
            spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
          } else
          // If it's later in the list
          {
            final int oldIndex = result.indexOf(seekedCarrier);
            int carrierPlaceLocation = indexToPlaceCarrierAt;
            // Place carrier where it's supposed to go
            result.remove(seekedCarrier);
            if (oldIndex < indexToPlaceCarrierAt) {
              carrierPlaceLocation--;
            }
            result.add(carrierPlaceLocation, seekedCarrier);
            filledCarriers.add(seekedCarrier);
            // Move the planes down to the carrier
            final List<Unit> planesBetweenHereAndCarrier = new ArrayList<>();
            for (int i2 = i; i2 < carrierPlaceLocation; i2++) {
              final Unit unit2 = result.get(i2);
              final UnitAttachment ua2 = UnitAttachment.get(unit2.getUnitType());
              if (ua2.getCarrierCost() > 0) {
                planesBetweenHereAndCarrier.add(unit2);
              }
            }
            // Invert list, so they are inserted in the same order
            Collections.reverse(planesBetweenHereAndCarrier);
            int planeMoveCount = 0;
            for (final Unit plane : planesBetweenHereAndCarrier) {
              result.remove(plane);
              // Insert each plane right before carrier (index decreased cause removal of carrier reduced indexes)
              result.add(carrierPlaceLocation - 1, plane);
              planeMoveCount++;
            }
            // Find the next carrier
            seekedCarrier = getLastUnitMatching(result,
                new CompositeMatchAnd<>(Matches.UnitIsCarrier, Matches.isNotInList(filledCarriers)), result.size() - 1);
            if (seekedCarrier == null) {
              // No carriers left
              break;
            }
            // Since we only moved planes up, just reduce next carrier place index by plane move count
            indexToPlaceCarrierAt = carrierPlaceLocation - planeMoveCount;
            spaceLeftOnSeekedCarrier = UnitAttachment.get(seekedCarrier.getUnitType()).getCarrierCapacity();
          }
        }
      }
    }
    return result;
  }
}
