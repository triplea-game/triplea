package org.triplea.ai.flowfield.neighbors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Territory;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapWithNeighborsTest {

  @Test
  void territories3InLineConnections() {
    final List<Territory> territories =
        List.of(mock(Territory.class), mock(Territory.class), mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else {
                return List.of(territories.get(1));
              }
            });
    assertThat(
        "First territory is only connected to the middle territory",
        mapWithNeighbors.getTerritories().get(territories.get(0)).getNeighbors(),
        hasSize(1));
    assertThat(
        "Middle territory is connected to both edge territories",
        mapWithNeighbors.getTerritories().get(territories.get(1)).getNeighbors(),
        hasSize(2));
    assertThat(
        "Last territory is only connected to the middle territory",
        mapWithNeighbors.getTerritories().get(territories.get(2)).getNeighbors(),
        hasSize(1));
  }
}
