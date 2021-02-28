package org.triplea.ai.flowfield.neighbors;

import games.strategy.engine.data.Territory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(exclude = "neighbors")
@RequiredArgsConstructor
public class TerritoryWithNeighbors {
  Territory territory;
  @ToString.Exclude Set<TerritoryWithNeighbors> neighbors = new HashSet<>();

  void addNeighbors(final Collection<TerritoryWithNeighbors> territories) {
    this.neighbors.addAll(territories);
  }
}
