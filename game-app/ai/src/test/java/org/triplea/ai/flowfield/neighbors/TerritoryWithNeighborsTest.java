package org.triplea.ai.flowfield.neighbors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Territory;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryWithNeighborsTest {

  @Test
  void verifyEqualsIsNotInfiniteRecursive() {
    final TerritoryWithNeighbors territory = new TerritoryWithNeighbors(mock(Territory.class));
    final TerritoryWithNeighbors territory2 = new TerritoryWithNeighbors(mock(Territory.class));
    final TerritoryWithNeighbors territory3 = new TerritoryWithNeighbors(mock(Territory.class));
    territory.addNeighbors(List.of(territory2, territory3));
    territory2.addNeighbors(List.of(territory, territory3));
    territory3.addNeighbors(List.of(territory, territory2));

    assertThat(territory.equals(territory2))
        .as(
            "TerritoryWithNeighbors has a member `neighbors` which can refer to itself. "
                + "So `@EqualsAndHashCode(exclude = \"neighbors\")` needs to be added to prevent "
                + "Lombok from creating a recursive equals method")
        .isFalse();
  }

  @Test
  void verifyToStringIsNotInfiniteRecursive() {
    final TerritoryWithNeighbors territory = new TerritoryWithNeighbors(mock(Territory.class));
    final TerritoryWithNeighbors territory2 = new TerritoryWithNeighbors(mock(Territory.class));
    final TerritoryWithNeighbors territory3 = new TerritoryWithNeighbors(mock(Territory.class));
    territory.addNeighbors(List.of(territory2, territory3));
    territory2.addNeighbors(List.of(territory, territory3));
    territory3.addNeighbors(List.of(territory, territory2));

    assertThat(territory.toString())
        .as(
            "TerritoryWithNeighbors has a member `neighbors` which can refer to itself. "
                + "So `@ToString.Exclude` needs to be added to prevent Lombok from "
                + "creating a recursive toString method")
        .isInstanceOf(String.class);
  }
}
