package games.strategy.triplea.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

/**
 * Handy utility methods for the writers of an AI.
 */
public class AIUtils {
  /**
   * How many PU's does it cost the given player to produce the given unit type.
   * <p>
   * If the player cannot produce the given unit, return Integer.MAX_VALUE
   * <p>
   */
  public static int getCost(final UnitType unitType, final PlayerID player, final GameData data) {
    if (unitType == null) {
      throw new IllegalArgumentException("null unit type");
    }
    if (player == null) {
      throw new IllegalArgumentException("null player id");
    }
    final Resource PUs = data.getResourceList().getResource(Constants.PUS);
    final ProductionRule rule = getProductionRule(unitType, player, data);
    if (rule == null) {
      return Integer.MAX_VALUE;
    } else {
      return rule.getCosts().getInt(PUs);
    }
  }

  /**
   * @return a comparator that sorts cheaper units before expensive ones
   */
  public static Comparator<Unit> getCostComparator() {
    return (o1, o2) -> getCost(o1.getType(), o1.getOwner(), o1.getData())
        - getCost(o2.getType(), o2.getOwner(), o2.getData());
  }

  /**
   * Get the production rule for the given player, for the given unit type.
   * <p>
   * If no such rule can be found, then return null.
   */
  public static ProductionRule getProductionRule(final UnitType unitType, final PlayerID player, final GameData data) {
    if (unitType == null) {
      throw new IllegalArgumentException("null unit type");
    }
    if (player == null) {
      throw new IllegalArgumentException("null player id");
    }
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
   * <p>
   *
   * @param units
   *        - the units to measure
   * @param attacking
   *        - are the units on attack or defense
   * @param sea
   *        - calculate the strength of the units in a sea or land battle?
   */
  public static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea) {
    int strength = 0;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      if (unitAttachment.getIsInfrastructure()) {
        // nothing
      } else if (unitAttachment.getIsSea() == sea) {
        // 2 points since we can absorb a hit
        strength += 2;
        // two hit
        strength += 1.5 * unitAttachment.getHitPoints();
        // the number of pips on the dice
        if (attacking) {
          strength += unitAttachment.getAttack(u.getOwner());
        } else {
          strength += unitAttachment.getDefense(u.getOwner());
        }
        if (attacking) {
          // a unit with attack of 0 isnt worth much
          // we dont want transports to try and gang up on subs
          if (unitAttachment.getAttack(u.getOwner()) == 0) {
            strength -= 1.2;
          }
        }
      }
    }
    if (attacking) {
      final int art = Match.countMatches(units, Matches.UnitIsArtillery);
      final int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

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
