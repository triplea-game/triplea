package org.triplea.ai.flowfield.map;

import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(exclude = "neighbors")
public class FieldTerritory {
  private Territory territory;
  private long value;
  @ToString.Exclude private final Map<Territory, FieldTerritory> neighbors = new HashMap<>();

  FieldTerritory(final Territory territory) {
    this.territory = territory;
    this.value = 0;
  }

  void addValue(final long value) {
    this.value += value;
  }

  void addNeighbor(final FieldTerritory territory) {
    this.neighbors.put(territory.getTerritory(), territory);
  }

  Collection<FieldTerritory> getNeighbors() {
    return this.neighbors.values();
  }
}
