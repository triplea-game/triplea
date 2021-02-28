package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CasualtySortingUtilTest {

  @Mock GameData gameData;
  @Mock GamePlayer player;

  @Test
  void sortByUnitTypeName() {
    final UnitType unitType1 = mock(UnitType.class);
    when(unitType1.getName()).thenReturn("B");
    final Unit unitB = new Unit(unitType1, player, gameData);

    final UnitType unitType2 = mock(UnitType.class);
    when(unitType2.getName()).thenReturn("A");
    final Unit unitA = new Unit(unitType2, player, gameData);

    final List<Unit> units = Arrays.asList(unitB, unitA);

    CasualtySortingUtil.sortPreBattle(units);

    assertThat(units, is(List.of(unitA, unitB)));
  }

  @Test
  void sortByMovement() {
    final UnitType unitType1 = mock(UnitType.class);
    when(unitType1.getName()).thenReturn("A");
    final UnitAttachment unitAttachment1 = mock(UnitAttachment.class);
    when(unitType1.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment1);
    when(unitAttachment1.getMovement(player)).thenReturn(2);
    final Unit unitWith2Movement = new Unit(unitType1, player, gameData);

    final UnitType unitType2 = mock(UnitType.class);
    when(unitType2.getName()).thenReturn("A");
    final UnitAttachment unitAttachment2 = mock(UnitAttachment.class);
    when(unitAttachment2.getMovement(player)).thenReturn(1);
    when(unitType2.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment2);
    final Unit unitWith1Movement = new Unit(unitType2, player, gameData);

    final List<Unit> units = Arrays.asList(unitWith2Movement, unitWith1Movement);

    CasualtySortingUtil.sortPreBattle(units);

    assertThat(units, is(List.of(unitWith1Movement, unitWith2Movement)));
  }

  @Test
  void sortByMarine() {
    final UnitType unitType1 = mock(UnitType.class);
    when(unitType1.getName()).thenReturn("A");
    final UnitAttachment unitAttachment1 = mock(UnitAttachment.class);
    when(unitType1.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment1);
    lenient().when(unitAttachment1.getMovement(player)).thenReturn(1);
    when(unitAttachment1.getIsMarine()).thenReturn(0);
    final Unit unitWithNoMarine = spy(new Unit(unitType1, player, gameData));
    lenient().when(unitWithNoMarine.getWasAmphibious()).thenReturn(true);

    final UnitType unitType2 = mock(UnitType.class);
    when(unitType2.getName()).thenReturn("A");
    final UnitAttachment unitAttachment2 = mock(UnitAttachment.class);
    lenient().when(unitAttachment2.getMovement(player)).thenReturn(1);
    when(unitAttachment2.getIsMarine()).thenReturn(1);
    when(unitType2.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment2);
    final Unit unitWith1Marine = spy(new Unit(unitType2, player, gameData));
    when(unitWith1Marine.getWasAmphibious()).thenReturn(true);

    final UnitType unitType3 = mock(UnitType.class);
    when(unitType3.getName()).thenReturn("A");
    final UnitAttachment unitAttachment3 = mock(UnitAttachment.class);
    lenient().when(unitAttachment3.getMovement(player)).thenReturn(1);
    when(unitAttachment3.getIsMarine()).thenReturn(2);
    when(unitType3.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment3);
    final Unit unitWith2Marine = spy(new Unit(unitType3, player, gameData));
    when(unitWith2Marine.getWasAmphibious()).thenReturn(true);

    final List<Unit> units = Arrays.asList(unitWith2Marine, unitWith1Marine, unitWithNoMarine);

    CasualtySortingUtil.sortPreBattle(units);

    assertThat(units, is(List.of(unitWithNoMarine, unitWith1Marine, unitWith2Marine)));
  }

  @Test
  void sortByMarineWithNonAmphibiousMarine() {
    final UnitType unitType1 = mock(UnitType.class);
    when(unitType1.getName()).thenReturn("A");
    final UnitAttachment unitAttachment1 = mock(UnitAttachment.class);
    when(unitType1.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment1);
    lenient().when(unitAttachment1.getMovement(player)).thenReturn(1);
    when(unitAttachment1.getIsMarine()).thenReturn(0);
    final Unit unitWithNoMarine = spy(new Unit(unitType1, player, gameData));
    when(unitWithNoMarine.getWasAmphibious()).thenReturn(true);

    final UnitType unitType2 = mock(UnitType.class);
    when(unitType2.getName()).thenReturn("A");
    final UnitAttachment unitAttachment2 = mock(UnitAttachment.class);
    lenient().when(unitAttachment2.getMovement(player)).thenReturn(1);
    when(unitAttachment2.getIsMarine()).thenReturn(1);
    when(unitType2.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment2);
    final Unit unitWith1Marine = spy(new Unit(unitType2, player, gameData));
    when(unitWith1Marine.getWasAmphibious()).thenReturn(true);

    final UnitType unitType3 = mock(UnitType.class);
    when(unitType3.getName()).thenReturn("A");
    final UnitAttachment unitAttachment3 = mock(UnitAttachment.class);
    lenient().when(unitAttachment3.getMovement(player)).thenReturn(1);
    when(unitAttachment3.getIsMarine()).thenReturn(2);
    when(unitType3.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment3);
    final Unit unitWith2Marine = spy(new Unit(unitType3, player, gameData));
    when(unitWith2Marine.getWasAmphibious()).thenReturn(true);

    final UnitType unitType4 = mock(UnitType.class);
    when(unitType4.getName()).thenReturn("A");
    final UnitAttachment unitAttachment4 = mock(UnitAttachment.class);
    // add a little more movement than unitWithNoMarine so it sorts stably.
    lenient().when(unitAttachment4.getMovement(player)).thenReturn(2);
    lenient().when(unitAttachment4.getIsMarine()).thenReturn(1);
    lenient().when(unitType4.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment4);
    final Unit unitWith3MarineButNotAmphibious = spy(new Unit(unitType4, player, gameData));
    when(unitWith3MarineButNotAmphibious.getWasAmphibious()).thenReturn(false);

    final List<Unit> units =
        Arrays.asList(
            unitWith2Marine, unitWith1Marine, unitWithNoMarine, unitWith3MarineButNotAmphibious);

    CasualtySortingUtil.sortPreBattle(units);

    assertThat(
        units,
        is(
            List.of(
                unitWith3MarineButNotAmphibious,
                unitWithNoMarine,
                unitWith1Marine,
                unitWith2Marine)));
  }
}
