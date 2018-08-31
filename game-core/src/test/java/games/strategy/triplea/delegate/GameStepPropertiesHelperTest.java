package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getCombinedTurns;
import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getRepairPlayers;
import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getTurnSummaryPlayers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.xml.TestDelegate;

final class GameStepPropertiesHelperTest {
  private final GameData gameData = new GameData();
  private final PlayerID player1 = new PlayerID("player1", gameData);
  private final PlayerID player2 = new PlayerID("player2", gameData);
  private final PlayerID player3 = new PlayerID("player3", gameData);
  private final PlayerID player4 = new PlayerID("player4", gameData);
  private final Properties gameStepProperties = new Properties();
  private final GameStep gameStep =
      new GameStep("name", "displayName", null, newDelegate(), gameData, gameStepProperties);

  private static IDelegate newDelegate() {
    final IDelegate delegate = new TestDelegate();
    delegate.initialize("delegateName", "delegateDisplayName");
    return delegate;
  }

  private static String joinPlayerNames(final PlayerID... playerIds) {
    return Arrays.stream(playerIds)
        .map(PlayerID::getName)
        .collect(Collectors.joining(":"));
  }

  private void givenGameStepProperty(final String name, final String value) {
    gameStepProperties.setProperty(name, value);
  }

  private void givenGameStepPropertyNotSet(final String name) {
    gameStepProperties.remove(name);
  }

  @BeforeEach
  void setUpGameData() {
    gameData.getPlayerList().addPlayerId(player1);
    gameData.getPlayerList().addPlayerId(player2);
    gameData.getPlayerList().addPlayerId(player3);
    gameData.getPlayerList().addPlayerId(player4);

    gameData.getSequence().addStep(gameStep);
  }

  @Nested
  final class GetCombinedTurnsTest {
    @Test
    void shouldReturnArgumentPlayerWhenPropertyNotSet() {
      givenGameStepPropertyNotSet(GameStep.PropertyKeys.COMBINED_TURNS);

      assertThat(getCombinedTurns(gameData, player1), contains(player1));
    }

    @Test
    void shouldReturnUnionOfArgumentPlayerAndCombinedTurnsPlayers() {
      givenGameStepProperty(GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(player2, player3, player4));

      assertThat(getCombinedTurns(gameData, player1), contains(player1, player2, player3, player4));
    }

    @Test
    void shouldReturnCombinedTurnsPlayersWhenArgumentPlayerIsNull() {
      givenGameStepProperty(GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(player2, player3, player4));

      assertThat(getCombinedTurns(gameData, null), contains(player2, player3, player4));
    }

    @Test
    void shouldIgnoreCombinedTurnsPlayersThatDoNotExist() {
      final PlayerID unknownPlayer = new PlayerID("unknownPlayer", gameData);
      givenGameStepProperty(GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(unknownPlayer, player2));

      assertThat(getCombinedTurns(gameData, player1), contains(player1, player2));
    }
  }

  @Nested
  final class GetRepairPlayersTest {
    @Test
    void shouldReturnArgumentPlayerWhenPropertyNotSet() {
      givenGameStepPropertyNotSet(GameStep.PropertyKeys.REPAIR_PLAYERS);

      assertThat(getRepairPlayers(gameData, player1), contains(player1));
    }

    @Test
    void shouldReturnUnionOfArgumentPlayerAndRepairPlayers() {
      givenGameStepProperty(GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(player2, player3, player4));

      assertThat(getRepairPlayers(gameData, player1), contains(player1, player2, player3, player4));
    }

    @Test
    void shouldReturnRepairPlayersWhenArgumentPlayerIsNull() {
      givenGameStepProperty(GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(player2, player3, player4));

      assertThat(getRepairPlayers(gameData, null), contains(player2, player3, player4));
    }

    @Test
    void shouldIgnoreRepairPlayersThatDoNotExist() {
      final PlayerID unknownPlayer = new PlayerID("unknownPlayer", gameData);
      givenGameStepProperty(GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(unknownPlayer, player2));

      assertThat(getRepairPlayers(gameData, player1), contains(player1, player2));
    }
  }

  @Nested
  final class GetTurnSummaryPlayersTest {
    @Test
    void shouldReturnNullWhenPropertyNotSet() {
      givenGameStepPropertyNotSet(GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS);

      assertThat(getTurnSummaryPlayers(gameData), is(nullValue()));
    }

    @Test
    void shouldReturnTurnSummaryPlayers() {
      givenGameStepProperty(GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS, joinPlayerNames(player1, player2, player3));

      assertThat(getTurnSummaryPlayers(gameData), contains(player1, player2, player3));
    }

    @Test
    void shouldIgnoreTurnSummaryPlayersThatDoNotExist() {
      final PlayerID unknownPlayer = new PlayerID("unknownPlayer", gameData);
      givenGameStepProperty(GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS, joinPlayerNames(unknownPlayer, player1));

      assertThat(getTurnSummaryPlayers(gameData), contains(player1));
    }
  }
}
