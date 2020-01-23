package games.strategy.triplea.formatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MyFormatterTest {
  @Nested
  final class UnitsToTextTest {
    private final GameData gameData = new GameData();

    private GamePlayer newPlayerId(final String name) {
      return new GamePlayer(name, gameData);
    }

    private Unit newUnit(final UnitType unitType, final GamePlayer owner) {
      return new Unit(unitType, owner, gameData);
    }

    private UnitType newUnitType(final String name) {
      return new UnitType(name, gameData);
    }

    @Test
    void shouldReturnEmptyStringWhenUnitsIsEmpty() {
      assertThat(MyFormatter.unitsToText(List.of()), is(""));
    }

    @Test
    void shouldReturnOneEntryPerUnitTypeAndPlayerCombination() {
      final GamePlayer gamePlayer1 = newPlayerId("playerId1");
      final GamePlayer gamePlayer2 = newPlayerId("playerId2");
      final UnitType unitType1 = newUnitType("unitType1");
      final UnitType unitType2 = newUnitType("unitType2");
      final Collection<Unit> units =
          List.of(
              newUnit(unitType1, gamePlayer1),
              newUnit(unitType2, gamePlayer1),
              newUnit(unitType1, gamePlayer2),
              newUnit(unitType2, gamePlayer2));

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
      final GamePlayer gamePlayer = newPlayerId("playerId");
      final UnitType unitType = newUnitType("unitType");
      final Collection<Unit> units =
          List.of(newUnit(unitType, gamePlayer), newUnit(unitType, gamePlayer));

      assertThat(MyFormatter.unitsToText(units), is("2 unitTypes owned by the playerId"));
    }

    @Test
    void addHtmlBreaksAndIndentsWithEmptyString() {
      assertThat(MyFormatter.addHtmlBreaksAndIndents("", 80, 100), is(""));
    }

    @Test
    void addHtmlBreaksAndIndentsWithNoBreak() {
      final String target = "unitType, unitType";
      assertThat(MyFormatter.addHtmlBreaksAndIndents(target, 80, 100), is(target));
    }

    @Test
    void addHtmlBreaksAndIndentsWithBreak() {
      final String target =
          "unitType, unitType, unitType, unitType, unitType, unitType, unitType, unitType, unitType, unitType, unitType, unitType";
      final String result =
          "unitType, unitType, unitType, unitType, unitType, unitType, unitType, "
              + "<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;unitType, unitType, unitType, unitType, unitType";
      assertThat(MyFormatter.addHtmlBreaksAndIndents(target, 80, 100), is(result));
    }

    @Test
    void addHtmlBreaksAndIndentsWithVeryLongWord() {
      final String target =
          "aVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongUnitType";
      final String result =
          "<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;aVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongUnitType";
      assertThat(MyFormatter.addHtmlBreaksAndIndents(target, 80, 100), is(result));
    }
  }
}
