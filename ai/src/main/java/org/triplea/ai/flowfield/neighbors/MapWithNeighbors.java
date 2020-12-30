package org.triplea.ai.flowfield.neighbors;

import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class MapWithNeighbors {
  Map<Territory, TerritoryWithNeighbors> territories;

  public MapWithNeighbors(
      final Collection<Territory> territories,
      final Function<Territory, Collection<Territory>> getNeighbors) {
    this.territories =
        territories.stream()
            .collect(Collectors.toMap(Function.identity(), TerritoryWithNeighbors::new));

    this.territories
        .values()
        .forEach(
            territoryWithNeighbors ->
                territoryWithNeighbors.addNeighbors(
                    getNeighbors.apply(territoryWithNeighbors.getTerritory()).stream()
                        .map(this.territories::get)
                        .collect(Collectors.toSet())));
  }

  public Collection<Territory> getNeighbors(final Territory territory) {
    return territories.get(territory).getNeighbors().stream()
        .map(TerritoryWithNeighbors::getTerritory)
        .collect(Collectors.toSet());
  }
}
