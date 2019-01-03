package games.strategy.triplea.formatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

final class MyFormatterTest {
  @Nested
  final class UnitsToTextTest {
    private final GameData gameData = new GameData();

    private PlayerId newPlayerId(final String name) {
      return new PlayerId(name, gameData);
    }

    private Unit newUnit(final UnitType unitType, final PlayerId owner) {
      return new Unit(unitType, owner, gameData);
    }

    private UnitType newUnitType(final String name) {
      return new UnitType(name, gameData);
    }

    @Test
    void shouldReturnEmptyStringWhenUnitsIsEmpty() {
      assertThat(MyFormatter.unitsToText(Collections.emptyList()), is(""));
    }

    @Test
    void shouldReturnOneEntryPerUnitTypeAndPlayerCombination() {
      final PlayerId playerId1 = newPlayerId("playerId1");
      final PlayerId playerId2 = newPlayerId("playerId2");
      final UnitType unitType1 = newUnitType("unitType1");
      final UnitType unitType2 = newUnitType("unitType2");
      final Collection<Unit> units =
          Arrays.asList(
              newUnit(unitType1, playerId1),
              newUnit(unitType2, playerId1),
              newUnit(unitType1, playerId2),
              newUnit(unitType2, playerId2));

      assertThat(
          MyFormatter.unitsToText(units),
          is(
              ""
                  + "1 unitType1 owned by the playerId2, "
                  + "1 unitType2 owned by the playerId2, "
                  + "1 unitType1 owned by the playerId1 "
                  + "and 1 unitType2 owned by the playerId1"));
    }

    @Test
    void shouldPluralizeTextWhenMultipleUnitsOwnedBySamePlayer() {
      final PlayerId playerId = newPlayerId("playerId");
      final UnitType unitType = newUnitType("unitType");
      final Collection<Unit> units =
          Arrays.asList(newUnit(unitType, playerId), newUnit(unitType, playerId));

      assertThat(MyFormatter.unitsToText(units), is("2 unitTypes owned by the playerId"));
    }
  }
}
