package org.triplea.ai.flowfield.influence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.Territory;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;
import org.triplea.ai.flowfield.odds.BattleDetails;
import org.triplea.java.collections.CollectionUtils;

/** Diffuses an initial value from an initial set of territories to the rest of the territories */
@Value
public class InfluenceMap {

  String name;
  /** Percentage of the value to be copied to the */
  double diffuseRate;

  Map<Territory, InfluenceTerritory> territories = new HashMap<>();

  public InfluenceMap(
      final String suffix,
      final InfluenceMapSetup influenceMapSetup,
      final MapWithNeighbors mapWithNeighbors,
      final Function<Territory, BattleDetails> getBattleDetails) {
    this(
        influenceMapSetup.getName() + "(" + suffix + ")",
        influenceMapSetup.getDiffusion(),
        influenceMapSetup.getTerritoryValuations(),
        mapWithNeighbors);
    Preconditions.checkArgument(
        influenceMapSetup.getTerritoryValuations().size() == 1,
        "A custom getBattleDetails only works if one territory has an initial value");
    diffuseBattleDetails(
        territories.get(
            CollectionUtils.getAny(influenceMapSetup.getTerritoryValuations().keySet())),
        getBattleDetails,
        mapWithNeighbors);
  }

  public InfluenceMap(
      final String suffix,
      final InfluenceMapSetup influenceMapSetup,
      final MapWithNeighbors mapWithNeighbors) {
    this(
        influenceMapSetup.getName() + "(" + suffix + ")",
        influenceMapSetup.getDiffusion(),
        influenceMapSetup.getTerritoryValuations(),
        mapWithNeighbors);
  }

  @VisibleForTesting
  InfluenceMap(
      final String name,
      final double diffuseRate,
      final Map<Territory, Long> initialTerritories,
      final MapWithNeighbors mapWithNeighbors) {
    Preconditions.checkArgument(diffuseRate < 1.0, "Diffusion rates can't be 1.0 or greater.");
    Preconditions.checkArgument(diffuseRate >= 0.0, "Diffusion rates can't be negative.");
    this.name = name;
    this.diffuseRate = diffuseRate;
    final Collection<InfluenceTerritory> fieldTerritories =
        initialTerritories.keySet().stream()
            .map(InfluenceTerritory::new)
            .peek(t -> territories.put(t.getTerritory(), t))
            .collect(Collectors.toList());

    // from each of the initial territories, diffuse their values to all of their neighbors
    fieldTerritories.forEach(
        territory ->
            diffuseValue(
                territory,
                diffuseRate,
                initialTerritories.get(territory.getTerritory()),
                mapWithNeighbors));
  }

  /**
   * Diffuse the value from one territory to all of its neighbors
   *
   * <p>The value decreases by diffuseRate as the distance increases between the territories and the
   * initial territory
   */
  private void diffuseValue(
      final InfluenceTerritory initialTerritory,
      final double diffuseRate,
      final long value,
      final MapWithNeighbors mapWithNeighbors) {
    final Set<Territory> seenTerritories = new HashSet<>(Set.of(initialTerritory.getTerritory()));
    final ArrayDeque<InfluenceTerritory> workingTerritories =
        new ArrayDeque<>(List.of(initialTerritory));
    InfluenceTerritory lastTerritoryOfCurrentDistance = workingTerritories.peekLast();
    long diffusedValue = value;
    while (!workingTerritories.isEmpty()) {
      final InfluenceTerritory currentTerritory = workingTerritories.removeFirst();
      currentTerritory.addDiffusedInfluence(diffusedValue);

      mapWithNeighbors.getNeighbors(currentTerritory.getTerritory()).stream()
          .map(neighbor -> this.territories.computeIfAbsent(neighbor, InfluenceTerritory::new))
          .filter(Predicate.not(neighbor -> seenTerritories.contains(neighbor.getTerritory())))
          .peek(neighbor -> seenTerritories.add(neighbor.getTerritory()))
          .forEach(workingTerritories::add);

      if (lastTerritoryOfCurrentDistance.equals(currentTerritory)) {
        lastTerritoryOfCurrentDistance = workingTerritories.peekLast();
        diffusedValue = (long) (diffusedValue * diffuseRate);
        if (diffusedValue < 1) {
          break;
        }
      }
    }
  }

  /**
   * Diffuse the battle details away from the initial territory
   *
   * <p>This doesn't just follow distance from the initial territory. Take the case where there is a
   * map that has the following connections: A -> B -> C -> D -> E A -> F -> G -> H -> D
   *
   * <p>There are two paths from A to E. One through B, C, and D and one through F, G, H, and D.
   * Normally, the BCD path would be the best path, but if B has a massive army compared to F, then
   * it would be better to take the FGHD path. During the diffusion, when D is initially reached, it
   * will pick up the battle details of C, since that is the shortest path. During the next
   * diffusion round, H will try to diffuse to D. It will compare its diffused BattleDetails and if
   * it is smaller than what is already in D, it will replace what is in D and add D back to the
   * list to diffuse.
   *
   * <p>This only diffuses to territories that received an influence value in {@link #diffuseValue},
   * so it can assume that all the territories it sees have an InfluenceTerritory already created.
   */
  private void diffuseBattleDetails(
      final InfluenceTerritory initialTerritory,
      final Function<Territory, BattleDetails> getBattleDetails,
      final MapWithNeighbors mapWithNeighbors) {
    final Set<Territory> seenTerritories = new HashSet<>(Set.of(initialTerritory.getTerritory()));
    final ArrayDeque<InfluenceTerritory> workingTerritories =
        new ArrayDeque<>(List.of(initialTerritory));
    InfluenceTerritory lastTerritoryOfCurrentDistance = workingTerritories.peekLast();
    int distance = 0;
    while (!workingTerritories.isEmpty()) {
      final InfluenceTerritory currentTerritory = workingTerritories.removeFirst();
      currentTerritory.setBattleDetails(getBattleDetails.apply(currentTerritory.getTerritory()));
      currentTerritory.setDistanceFromInitialTerritory(distance);

      mapWithNeighbors.getNeighbors(currentTerritory.getTerritory()).stream()
          .map(this.territories::get)
          .filter(neighbor -> neighbor.getInfluence() > 0)
          .filter(
              neighbor ->
                  !seenTerritories.contains(neighbor.getTerritory())
                      || neighbor.shouldBattleDetailsByUpdated(currentTerritory))
          .peek(neighbor -> neighbor.updateDiffusedBattleDetails(currentTerritory))
          .peek(neighbor -> seenTerritories.add(neighbor.getTerritory()))
          .forEach(workingTerritories::add);

      if (lastTerritoryOfCurrentDistance.equals(currentTerritory)) {
        lastTerritoryOfCurrentDistance = workingTerritories.peekLast();
        distance++;
      }
    }
  }
}
