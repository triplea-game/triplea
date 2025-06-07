package games.strategy.triplea.ai;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;

/** Handy utility methods for the writers of an AI. */
public final class AiUtils {
  private AiUtils() {}

  /** Returns a comparator that sorts cheaper units before expensive ones. */
  public static Comparator<Unit> getCostComparator() {
    return Comparator.comparingInt(o -> getCost(o.getType(), o.getOwner(), o.getData()));
  }

  /**
   * How many PU's does it cost the given player to produce the given unit type.
   *
   * <p>If the player cannot produce the given unit, return Integer.MAX_VALUE
   */
  static int getCost(final UnitType unitType, final GamePlayer player, final GameState data) {
    final Resource pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    final ProductionRule rule = getProductionRule(unitType, player);
    return (rule == null) ? Integer.MAX_VALUE : rule.getCosts().getInt(pus);
  }

  /**
   * Get the production rule for the given player, for the given unit type.
   *
   * <p>If no such rule can be found, then return null.
   */
  private static @Nullable ProductionRule getProductionRule(
      final UnitType unitType, final GamePlayer player) {
    final ProductionFrontier frontier = player.getProductionFrontier();
    if (frontier == null) {
      return null;
    }
    for (final ProductionRule rule : frontier) {
      if (rule.getResults().getInt(unitType) == 1) {
        return rule;
      }
    }
    return null;
  }

  /**
   * Get a quick and dirty estimate of the strength of some units in a battle.
   *
   * @param units - the units to measure
   * @param attacking - are the units on attack or defense
   * @param sea - calculate the strength of the units in a sea or land battle?
   */
  public static float strength(
      final Collection<Unit> units, final boolean attacking, final boolean sea) {
    float strength = 0;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = u.getUnitAttachment();
      if (!unitAttachment.isInfrastructure() && unitAttachment.isSea() == sea) {
        // 2 points since we can absorb a hit
        strength += 2;
        // two hit
        strength += 1.5f * unitAttachment.getHitPoints();
        // the number of pips on the dice
        if (attacking) {
          strength += unitAttachment.getAttack(u.getOwner());
        } else {
          strength += unitAttachment.getDefense(u.getOwner());
        }
        // a unit with attack of 0 isn't worth much
        // we dont want transports to try and gang up on subs
        if (attacking && unitAttachment.getAttack(u.getOwner()) == 0) {
          strength -= 1.2f;
        }
      }
    }
    if (attacking) {
      final int art = CollectionUtils.countMatches(units, Matches.unitIsArtillery());
      final int artSupport =
          CollectionUtils.countMatches(units, Matches.unitIsArtillerySupportable());
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

  public static @Nullable Unit getLastUnitMatching(
      final List<Unit> units, final Predicate<Unit> match, final int endIndex) {
    final int index = getIndexOfLastUnitMatching(units, match, endIndex);
    if (index == -1) {
      return null;
    }
    return units.get(index);
  }

  public static int getIndexOfLastUnitMatching(
      final List<Unit> units, final Predicate<Unit> match, final int endIndex) {
    for (int i = endIndex; i >= 0; i--) {
      final Unit unit = units.get(i);
      if (match.test(unit)) {
        return i;
      }
    }
    return -1;
  }

  static List<Unit> interleaveCarriersAndPlanes(final List<Unit> units) {
    if (units.stream().noneMatch(Matches.unitIsCarrier())
        || units.stream().noneMatch(Matches.unitCanLandOnCarrier())) {
      return units;
    }
    // Clone the current list
    final List<Unit> result = new ArrayList<>(units);
    Unit seekedCarrier = null;
    int indexToPlaceCarrierAt = -1;
    int spaceLeftOnSeekedCarrier = -1;
    final List<Unit> filledCarriers = new ArrayList<>();
    // Loop through all units, starting from the right, and rearrange units
    for (int i = result.size() - 1; i >= 0; i--) {
      final Unit unit = result.get(i);
      final UnitAttachment ua = unit.getUnitAttachment();
      // If this is a plane
      if (ua.getCarrierCost() > 0) {
        // If this is the first carrier seek
        if (seekedCarrier == null) {
          final int seekedCarrierIndex =
              getIndexOfLastUnitMatching(
                  result,
                  Matches.unitIsCarrier().and(not(filledCarriers::contains)),
                  result.size() - 1);
          if (seekedCarrierIndex == -1) {
            // No carriers left
            break;
          }
          seekedCarrier = result.get(seekedCarrierIndex);
          // Tell the code to insert carrier to the right of this plane
          indexToPlaceCarrierAt = i + 1;
          spaceLeftOnSeekedCarrier = seekedCarrier.getUnitAttachment().getCarrierCapacity();
        }
        spaceLeftOnSeekedCarrier -= ua.getCarrierCost();
        // If the carrier has been filled or overflowed
        if (spaceLeftOnSeekedCarrier <= 0) {
          if (spaceLeftOnSeekedCarrier < 0) {
            // Move current unit index up one, so we re-process this unit (since it can't fit on the
            // current seeked
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
            seekedCarrier =
                getLastUnitMatching(
                    result,
                    Matches.unitIsCarrier().and(not(filledCarriers::contains)),
                    result.size() - 1);
            if (seekedCarrier == null) {
              // No carriers left
              break;
            }
            // Place next carrier right before this plane (which just filled the old carrier that
            // was just moved)
            indexToPlaceCarrierAt = i;
            spaceLeftOnSeekedCarrier = seekedCarrier.getUnitAttachment().getCarrierCapacity();
          } else { // If it's later in the list
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
              final UnitAttachment ua2 = unit2.getUnitAttachment();
              if (ua2.getCarrierCost() > 0) {
                planesBetweenHereAndCarrier.add(unit2);
              }
            }
            // Invert list, so they are inserted in the same order
            Collections.reverse(planesBetweenHereAndCarrier);
            int planeMoveCount = 0;
            for (final Unit plane : planesBetweenHereAndCarrier) {
              result.remove(plane);
              // Insert each plane right before carrier (index decreased cause removal of carrier
              // reduced indexes)
              result.add(carrierPlaceLocation - 1, plane);
              planeMoveCount++;
            }
            // Find the next carrier
            seekedCarrier =
                getLastUnitMatching(
                    result,
                    Matches.unitIsCarrier().and(not(filledCarriers::contains)),
                    result.size() - 1);
            if (seekedCarrier == null) {
              // No carriers left
              break;
            }
            // Since we only moved planes up, just reduce next carrier place index by plane move
            // count
            indexToPlaceCarrierAt = carrierPlaceLocation - planeMoveCount;
            spaceLeftOnSeekedCarrier = seekedCarrier.getUnitAttachment().getCarrierCapacity();
          }
        }
      }
    }
    return result;
  }
}
