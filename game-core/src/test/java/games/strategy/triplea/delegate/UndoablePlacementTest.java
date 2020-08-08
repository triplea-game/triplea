package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UndoablePlacementTest {
  private static final String UNIT_TYPE_NAME = "infantry";

  private final GameData gameData = new GameData();
  private final Territory placeTerritory = new Territory("placeTerritory", gameData);

  private UndoablePlacement newUndoablePlacement(
      final Territory producerTerritory, final Territory placeTerritory) {
    return new UndoablePlacement(
        new CompositeChange(),
        producerTerritory,
        placeTerritory,
        List.of(new Unit(new UnitType(UNIT_TYPE_NAME, gameData), null, gameData)));
  }

  @Nested
  final class GetMoveLabelTest {
    @Test
    void shouldUsePlaceTerritoryWhenPlaceTerritoryEqualsProducerTerritory() {
      final Territory producerTerritory = new Territory(placeTerritory.getName(), gameData);
      final UndoablePlacement undoablePlacement =
          newUndoablePlacement(producerTerritory, placeTerritory);

      assertThat(undoablePlacement.getMoveLabel(), is(placeTerritory.getName()));
    }

    @Test
    void shouldUsePlaceTerritoryAndProducerTerritoryWhenPlaceTerritoryNotEqualsProducerTerritory() {
      final Territory producerTerritory = new Territory("producerTerritory", gameData);
      final UndoablePlacement undoablePlacement =
          newUndoablePlacement(producerTerritory, placeTerritory);

      assertThat(
          undoablePlacement.getMoveLabel(),
          is(producerTerritory.getName() + " -> " + placeTerritory.getName()));
    }
  }

  @Nested
  final class ToStringTest {
    @Test
    void shouldUsePlaceTerritoryWhenPlaceTerritoryEqualsProducerTerritory() {
      final Territory producerTerritory = new Territory(placeTerritory.getName(), gameData);
      final UndoablePlacement undoablePlacement =
          newUndoablePlacement(producerTerritory, placeTerritory);

      assertThat(
          undoablePlacement.toString(), is(placeTerritory.getName() + ": 1 " + UNIT_TYPE_NAME));
    }

    @Test
    void shouldUsePlaceTerritoryAndProducerTerritoryWhenPlaceTerritoryNotEqualsProducerTerritory() {
      final Territory producerTerritory = new Territory("producerTerritory", gameData);
      final UndoablePlacement undoablePlacement =
          newUndoablePlacement(producerTerritory, placeTerritory);

      assertThat(
          undoablePlacement.toString(),
          is(
              producerTerritory.getName()
                  + " produces in "
                  + placeTerritory.getName()
                  + ": 1 "
                  + UNIT_TYPE_NAME));
    }
  }
}
