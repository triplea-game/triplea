package games.strategy.triplea.formatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

public final class MyFormatterTest {
  private final GameData gameData = new GameData();

  private PlayerID newPlayerId(final String name) {
    return new PlayerID(name, gameData);
  }

  private Unit newUnit(final UnitType unitType, final PlayerID owner) {
    return new Unit(unitType, owner, gameData);
  }

  private UnitType newUnitType(final String name) {
    return new UnitType(name, gameData);
  }

  @Test
  public void unitsToText_ShouldReturnEmptyStringWhenUnitsIsEmpty() {
    assertThat(MyFormatter.unitsToText(Collections.emptyList()), is(""));
  }

  @Test
  public void unitsToText_ShouldReturnOneEntryPerUnitTypeAndPlayerCombination() {
    final PlayerID playerId1 = newPlayerId("playerId1");
    final PlayerID playerId2 = newPlayerId("playerId2");
    final UnitType unitType1 = newUnitType("unitType1");
    final UnitType unitType2 = newUnitType("unitType2");
    final Collection<Unit> units = Arrays.asList(
        newUnit(unitType1, playerId1),
        newUnit(unitType2, playerId1),
        newUnit(unitType1, playerId2),
        newUnit(unitType2, playerId2));

    assertThat(MyFormatter.unitsToText(units), is(""
        + "1 unitType1 owned by the playerId2, "
        + "1 unitType2 owned by the playerId2, "
        + "1 unitType1 owned by the playerId1 "
        + "and 1 unitType2 owned by the playerId1"));
  }

  @Test
  public void unitsToText_ShouldPluralizeTextWhenMultipleUnitsOwnedBySamePlayer() {
    final PlayerID playerId = newPlayerId("playerId");
    final UnitType unitType = newUnitType("unitType");
    final Collection<Unit> units = Arrays.asList(
        newUnit(unitType, playerId),
        newUnit(unitType, playerId));

    assertThat(MyFormatter.unitsToText(units), is("2 unitTypes owned by the playerId"));
  }
}
