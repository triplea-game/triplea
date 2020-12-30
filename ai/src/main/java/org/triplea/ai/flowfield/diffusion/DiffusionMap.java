package org.triplea.ai.flowfield.diffusion;

import games.strategy.engine.data.Territory;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;

/** Diffuses an initial value from an initial set of territories to the rest of the territories */
@Value
public class DiffusionMap {

  String name;
  /** Percentage of the value to be copied to the */
  double diffuseRate;

  Map<Territory, FieldTerritory> territories = new HashMap<>();

  public DiffusionMap(
      final String suffix,
      final DiffusionType diffusionType,
      final MapWithNeighbors mapWithNeighbors) {
    this(
        diffusionType.getName() + "(" + suffix + ")",
        diffusionType.getDiffusion(),
        diffusionType.getTerritoryValuations(),
        mapWithNeighbors);
  }

  DiffusionMap(
      final String name,
      final double diffuseRate,
      final Map<Territory, Long> initialTerritories,
      final MapWithNeighbors mapWithNeighbors) {
    this.name = name;
    this.diffuseRate = diffuseRate;
    final Collection<FieldTerritory> fieldTerritories =
        initialTerritories.keySet().stream()
            .map(FieldTerritory::new)
            .peek(t -> territories.put(t.getTerritory(), t))
            .collect(Collectors.toList());

    // from each of the initial territories, diffuse their values to all of their neighbors
    fieldTerritories.forEach(
        territory -> {
          diffuseValue(
              territory,
              diffuseRate,
              initialTerritories.get(territory.getTerritory()),
              mapWithNeighbors);
        });
  }

  /**
   * Diffuse the value from one territory to all of its neighbors
   *
   * <p>The value decreases by diffuseRate as the distance increases between the territories and the
   * initial territory
   */
  private void diffuseValue(
      final FieldTerritory territory,
      final double diffuseRate,
      final long value,
      final MapWithNeighbors mapWithNeighbors) {
    final Set<Territory> seenTerritories = new HashSet<>(Set.of(territory.getTerritory()));
    final ArrayDeque<FieldTerritory> fieldTerritories = new ArrayDeque<>(List.of(territory));
    FieldTerritory lastTerritoryOfCurrentDistance = fieldTerritories.peekLast();
    long diffusedValue = value;
    while (!fieldTerritories.isEmpty()) {
      final FieldTerritory currentTerritory = fieldTerritories.removeFirst();
      currentTerritory.addValue(diffusedValue);

      mapWithNeighbors.getNeighbors(currentTerritory.getTerritory()).stream()
          .map(neighbor -> this.territories.computeIfAbsent(neighbor, FieldTerritory::new))
          .filter(Predicate.not(t -> seenTerritories.contains(t.getTerritory())))
          .peek(t -> seenTerritories.add(t.getTerritory()))
          .forEach(fieldTerritories::add);

      if (lastTerritoryOfCurrentDistance.equals(currentTerritory)) {
        lastTerritoryOfCurrentDistance = fieldTerritories.peekLast();
        diffusedValue = (long) (diffusedValue * diffuseRate);
        if (diffusedValue < 1) {
          break;
        }
      }
    }
  }
}
