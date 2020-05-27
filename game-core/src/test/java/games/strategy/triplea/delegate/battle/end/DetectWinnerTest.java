package games.strategy.triplea.delegate.battle.end;

import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DetectWinnerTest {

  private final static GameData GLOBAL_1940_GAME_DATA = TestMapGameData.GLOBAL1940.getGameData();
  private final static GamePlayer BRITISH = GameDataTestUtil.british(GLOBAL_1940_GAME_DATA);
  private final static GamePlayer GERMANS = GameDataTestUtil.germans(GLOBAL_1940_GAME_DATA);

  @Test
  @DisplayName("Game isn't over yet")
  void testUnitsOnBothSides() {
    final List<Unit> attackers = GameDataTestUtil.armour(GLOBAL_1940_GAME_DATA).create(1, GERMANS);
    final List<Unit> defenders = GameDataTestUtil.armour(GLOBAL_1940_GAME_DATA).create(1, BRITISH);

    final DetectWinner.Winner result = DetectWinner.builder()
        .attackingUnits(attackers)
        .defendingUnits(defenders)
        .gameData(GLOBAL_1940_GAME_DATA)
        .removeUndefendedTransports(() -> {})
        .removeUnitsWithNoRollsLeft(() -> {})
        .build().detect();

    assertThat(result, is(DetectWinner.Winner.NOT_YET));
  }

  @Test
  @DisplayName("Verify defender wins if there are no attackers left")
  void noAttackersLeft() {
    final List<Unit> attackers = List.of();
    final List<Unit> defenders = GameDataTestUtil.armour(GLOBAL_1940_GAME_DATA).create(1, BRITISH);

    final DetectWinner.Winner result = DetectWinner.builder()
        .attackingUnits(attackers)
        .defendingUnits(defenders)
        .gameData(GLOBAL_1940_GAME_DATA)
        .removeUndefendedTransports(() -> {})
        .removeUnitsWithNoRollsLeft(() -> {})
        .build().detect();

    assertThat(result, is(DetectWinner.Winner.DEFENDER));
  }

  @Test
  @DisplayName("Verify attacker wins if there are no defenders left")
  void noDefendersLeft() {
    final List<Unit> attackers = GameDataTestUtil.armour(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> defenders = List.of();

    final Runnable removeUndefendedTransports = mock(Runnable.class);
    final Runnable removeUnitsWithNoRollsLeft = mock(Runnable.class);

    final DetectWinner.Winner result = DetectWinner.builder()
        .attackingUnits(attackers)
        .defendingUnits(defenders)
        .gameData(GLOBAL_1940_GAME_DATA)
        .removeUndefendedTransports(removeUndefendedTransports)
        .removeUnitsWithNoRollsLeft(removeUnitsWithNoRollsLeft)
        .build().detect();

    assertThat(result, is(DetectWinner.Winner.ATTACKER));
    verify(removeUndefendedTransports, times(1)).run();
    verify(removeUnitsWithNoRollsLeft, times(1)).run();
  }

  @Test
  @DisplayName("Verify attacker wins if there are no defenders left with TRANSPORT_CASUALTIES_RESTRICTED == false")
  void noDefendersLeftAndTransportCasualtiesUnrestricted() {
    final List<Unit> attackers = GameDataTestUtil.armour(GLOBAL_1940_GAME_DATA).create(1, BRITISH);
    final List<Unit> defenders = List.of();

    final Runnable removeUndefendedTransports = mock(Runnable.class);
    final Runnable removeUnitsWithNoRollsLeft = mock(Runnable.class);

    GLOBAL_1940_GAME_DATA.getProperties().set(TRANSPORT_CASUALTIES_RESTRICTED, false);

    final DetectWinner.Winner result = DetectWinner.builder()
        .attackingUnits(attackers)
        .defendingUnits(defenders)
        .gameData(GLOBAL_1940_GAME_DATA)
        .removeUndefendedTransports(removeUndefendedTransports)
        .removeUnitsWithNoRollsLeft(removeUnitsWithNoRollsLeft)
        .build().detect();

    GLOBAL_1940_GAME_DATA.getProperties().set(TRANSPORT_CASUALTIES_RESTRICTED, true);

    assertThat(result, is(DetectWinner.Winner.ATTACKER));
    verify(removeUndefendedTransports, times(0)).run();
    verify(removeUnitsWithNoRollsLeft, times(1)).run();
  }
}