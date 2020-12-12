package org.triplea.ai.flowfield.map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Territory;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiffusionMapTest {

  @ParameterizedTest
  @MethodSource("territories3InLine")
  void territories3InLineConnections(
      final DiffusionMap diffusionMap, final List<Territory> territories) {
    assertThat(
        "First territory is only connected to the middle territory",
        diffusionMap.getTerritories().get(territories.get(0)).getNeighbors(),
        hasSize(1));
    assertThat(
        "Middle territory is connected to both edge territories",
        diffusionMap.getTerritories().get(territories.get(1)).getNeighbors(),
        hasSize(2));
    assertThat(
        "Last territory is only connected to the middle territory",
        diffusionMap.getTerritories().get(territories.get(2)).getNeighbors(),
        hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("territories3InLine")
  void territories3InLineValues(
      final DiffusionMap diffusionMap, final List<Territory> territories) {
    assertThat(
        "First territory has the initial value of 100.0",
        diffusionMap.getTerritories().get(territories.get(0)).getValue(),
        is(100L));
    assertThat(
        "Middle territory has 50% of the initial value",
        diffusionMap.getTerritories().get(territories.get(1)).getValue(), is(50L));
    assertThat(
        "Last territory has 50% * 50% (or 25%) of the initial value",
        diffusionMap.getTerritories().get(territories.get(2)).getValue(), is(25L));
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> territories3InLine() {
    final List<Territory> territories =
        List.of(mock(Territory.class), mock(Territory.class), mock(Territory.class));
    final DiffusionMap diffusionMap =
        new DiffusionMap(
            "Test",
            0.5,
            Map.of(territories.get(0), 100L),
            (t) -> {
              if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else {
                return List.of(territories.get(1));
              }
            });
    return Stream.of(Arguments.of(diffusionMap, territories));
  }

  @ParameterizedTest
  @MethodSource("territories4InLineWithInitialOnBothEnds")
  void territories4InLineWithInitialOnBothEndsConnections(
      final DiffusionMap diffusionMap, final List<Territory> territories) {
    assertThat(
        "First territory is only connected to the middle territory",
        diffusionMap.getTerritories().get(territories.get(0)).getNeighbors(),
        hasSize(1));
    assertThat(
        "Middle territory is connected to both edge territories",
        diffusionMap.getTerritories().get(territories.get(1)).getNeighbors(),
        hasSize(2));
    assertThat(
        "Last territory is only connected to the middle territory",
        diffusionMap.getTerritories().get(territories.get(2)).getNeighbors(),
        hasSize(2));
    assertThat(
        "Last territory is only connected to the middle territory",
        diffusionMap.getTerritories().get(territories.get(3)).getNeighbors(),
        hasSize(1));
  }

  @ParameterizedTest
  @MethodSource("territories4InLineWithInitialOnBothEnds")
  void territories4InLineWithInitialOnBothEndsValues(
      final DiffusionMap diffusionMap, final List<Territory> territories) {
    assertThat(
        "First territory has the initial value of 100 + 12 (diffused from the last)",
        diffusionMap.getTerritories().get(territories.get(0)).getValue(),
        is(112L));
    assertThat(
        "2nd territory has 50 (diffused from the first) + 25 (diffused from the last)",
        diffusionMap.getTerritories().get(territories.get(1)).getValue(),
        is(75L));
    assertThat(
        "3nd territory has 50 (diffused from the last) + 25 (diffused from the first)",
        diffusionMap.getTerritories().get(territories.get(2)).getValue(),
        is(75L));
    assertThat(
        "Last territory has the initial value of 100 + 12 (diffused from the first)",
        diffusionMap.getTerritories().get(territories.get(3)).getValue(),
        is(112L));
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> territories4InLineWithInitialOnBothEnds() {
    final List<Territory> territories =
        List.of(
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class));
    final DiffusionMap diffusionMap =
        new DiffusionMap(
            "Test",
            0.5,
            Map.of(territories.get(0), 100L, territories.get(3), 100L),
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else if (t.equals(territories.get(2))) {
                return List.of(territories.get(1), territories.get(3));
              } else {
                return List.of(territories.get(2));
              }
            });
    return Stream.of(Arguments.of(diffusionMap, territories));
  }

  @Test
  void initialTerritoriesWithDifferentValues() {
    final List<Territory> territories =
        List.of(
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class));
    final DiffusionMap diffusionMap =
        new DiffusionMap(
            "Test",
            0.5,
            Map.of(
                territories.get(0),
                25L,
                territories.get(1),
                50L,
                territories.get(2),
                100L,
                territories.get(3),
                200L),
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else if (t.equals(territories.get(2))) {
                return List.of(territories.get(1), territories.get(3));
              } else {
                return List.of(territories.get(2));
              }
            });

    assertThat(
        "1st territory has initial value of 25 + 25 from second + 25 from third + 25 from fourth",
        diffusionMap.getTerritories().get(territories.get(0)).getValue(),
        is(100L));
    assertThat(
        "2nd territory has initial value of 50 + 12 from first + 50 from third + 50 from fourth",
        diffusionMap.getTerritories().get(territories.get(1)).getValue(),
        is(162L));
    assertThat(
        "3nd territory has initial value of 100 + 6 from first + 25 from second + 100 from fourth",
        diffusionMap.getTerritories().get(territories.get(2)).getValue(),
        is(231L));
    assertThat(
        "4th territory has initial value of 200 + 3 from first + 12 from second + 50 from third",
        diffusionMap.getTerritories().get(territories.get(3)).getValue(),
        is(265L));
  }
}
