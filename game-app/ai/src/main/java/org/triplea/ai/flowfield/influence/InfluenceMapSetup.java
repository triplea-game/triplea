package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class InfluenceMapSetup {
  String name;
  double diffusion;
  Map<Territory, Long> territoryValuations;

  Collection<InfluenceMapSetup> splitIntoSingleTerritoryMaps() {
    return territoryValuations.entrySet().stream()
        .map(
            entry ->
                new InfluenceMapSetup(name, diffusion, Map.of(entry.getKey(), entry.getValue())))
        .collect(Collectors.toList());
  }
}
