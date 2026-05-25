package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getCombinedTurns;
import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getRepairPlayers;
import static games.strategy.triplea.delegate.GameStepPropertiesHelper.getTurnSummaryPlayers;
import static games.strategy.triplea.delegate.GameStepPropertiesHelper.isResetUnitStateAtEnd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameStepPropertiesHelperTest {
  private final GameData gameData = new GameData();
  private final GamePlayer player1 = new GamePlayer("player1", gameData);
  private final GamePlayer player2 = new GamePlayer("player2", gameData);
  private final GamePlayer player3 = new GamePlayer("player3", gameData);
  private final GamePlayer player4 = new GamePlayer("player4", gameData);
  private final Properties gameStepProperties = new Properties();
  private final GameStep gameStep =
      new GameStep("name", "displayName", null, newDelegate(), gameData, gameStepProperties);

  private static IDelegate newDelegate() {
    final IDelegate delegate = new TestDelegate();
    delegate.initialize("delegateName", "delegateDisplayName");
    return delegate;
  }

  private static String joinPlayerNames(final GamePlayer... gamePlayers) {
    return Arrays.stream(gamePlayers).map(GamePlayer::getName).collect(Collectors.joining(":"));
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
      givenGameStepProperty(
          GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(player2, player3, player4));

      assertThat(getCombinedTurns(gameData, player1), contains(player1, player2, player3, player4));
    }

    @Test
    void shouldReturnCombinedTurnsPlayersWhenArgumentPlayerIsNull() {
      givenGameStepProperty(
          GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(player2, player3, player4));

      assertThat(getCombinedTurns(gameData, null), contains(player2, player3, player4));
    }

    @Test
    void shouldIgnoreCombinedTurnsPlayersThatDoNotExist() {
      final GamePlayer unknownPlayer = new GamePlayer("unknownPlayer", gameData);
      givenGameStepProperty(
          GameStep.PropertyKeys.COMBINED_TURNS, joinPlayerNames(unknownPlayer, player2));

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
      givenGameStepProperty(
          GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(player2, player3, player4));

      assertThat(getRepairPlayers(gameData, player1), contains(player1, player2, player3, player4));
    }

    @Test
    void shouldReturnRepairPlayersWhenArgumentPlayerIsNull() {
      givenGameStepProperty(
          GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(player2, player3, player4));

      assertThat(getRepairPlayers(gameData, null), contains(player2, player3, player4));
    }

    @Test
    void shouldIgnoreRepairPlayersThatDoNotExist() {
      final GamePlayer unknownPlayer = new GamePlayer("unknownPlayer", gameData);
      givenGameStepProperty(
          GameStep.PropertyKeys.REPAIR_PLAYERS, joinPlayerNames(unknownPlayer, player2));

      assertThat(getRepairPlayers(gameData, player1), contains(player1, player2));
    }
  }

  @Nested
  final class IsResetUnitStateAtEndTest {
    private final GameData ownGameData = new GameData();
    private final GamePlayer ownPlayer = new GamePlayer("ownPlayer", ownGameData);
    private final GamePlayer otherOwnPlayer = new GamePlayer("otherOwnPlayer", ownGameData);

    @BeforeEach
    void setUp() {
      ownGameData.getPlayerList().addPlayerId(ownPlayer);
      ownGameData.getPlayerList().addPlayerId(otherOwnPlayer);
    }

    private GameStep newStep(final String name, final GamePlayer stepPlayer) {
      return newStep(name, stepPlayer, new Properties());
    }

    private GameStep newStep(
        final String name, final GamePlayer stepPlayer, final Properties stepProperties) {
      return new GameStep(name, name, stepPlayer, newDelegate(), ownGameData, stepProperties);
    }

    @Test
    void resetsAtEndOfNonCombatMoveByDefault() {
      ownGameData.getSequence().addStep(newStep("playerNonCombatMove", ownPlayer));
      ownGameData.getSequence().setRoundAndStep(1, "playerNonCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(true));
    }

    @Test
    void doesNotResetAtEndOfCombatMoveWhenNonCombatMoveFollowsForSamePlayer() {
      ownGameData.getSequence().addStep(newStep("playerCombatMove", ownPlayer));
      ownGameData.getSequence().addStep(newStep("playerNonCombatMove", ownPlayer));
      ownGameData.getSequence().setRoundAndStep(1, "playerCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(false));
    }

    @Test
    void resetsAtEndOfCombatMoveWhenNoFollowingNonCombatMove() {
      ownGameData.getSequence().addStep(newStep("playerCombatMove", ownPlayer));
      ownGameData.getSequence().addStep(newStep("playerPurchase", ownPlayer));
      ownGameData.getSequence().setRoundAndStep(1, "playerCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(true));
    }

    @Test
    void resetsAtEndOfCombatMoveWhenFollowingNonCombatMoveIsForOtherPlayer() {
      ownGameData.getSequence().addStep(newStep("playerCombatMove", ownPlayer));
      ownGameData.getSequence().addStep(newStep("otherNonCombatMove", otherOwnPlayer));
      ownGameData.getSequence().setRoundAndStep(1, "playerCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(true));
    }

    @Test
    void explicitFalsePropertyOverridesDefault() {
      final Properties props = new Properties();
      props.setProperty(GameStep.PropertyKeys.RESET_UNIT_STATE_AT_END, "false");
      ownGameData.getSequence().addStep(newStep("playerCombatMove", ownPlayer, props));
      ownGameData.getSequence().setRoundAndStep(1, "playerCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(false));
    }

    @Test
    void explicitTruePropertyOverridesDefault() {
      final Properties props = new Properties();
      props.setProperty(GameStep.PropertyKeys.RESET_UNIT_STATE_AT_END, "true");
      ownGameData.getSequence().addStep(newStep("playerCombatMove", ownPlayer, props));
      ownGameData.getSequence().addStep(newStep("playerNonCombatMove", ownPlayer));
      ownGameData.getSequence().setRoundAndStep(1, "playerCombatMove", ownPlayer);

      assertThat(isResetUnitStateAtEnd(ownGameData), is(true));
    }
  }

  @Nested
  final class GetTurnSummaryPlayersTest {
    @Test
    void shouldReturnEmptyWhenPropertyNotSet() {
      givenGameStepPropertyNotSet(GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS);

      assertThat(getTurnSummaryPlayers(gameData), is(empty()));
    }

    @Test
    void shouldReturnTurnSummaryPlayers() {
      givenGameStepProperty(
          GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS, joinPlayerNames(player1, player2, player3));

      assertThat(getTurnSummaryPlayers(gameData), contains(player1, player2, player3));
    }

    @Test
    void shouldIgnoreTurnSummaryPlayersThatDoNotExist() {
      final GamePlayer unknownPlayer = new GamePlayer("unknownPlayer", gameData);
      givenGameStepProperty(
          GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS, joinPlayerNames(unknownPlayer, player1));

      assertThat(getTurnSummaryPlayers(gameData), contains(player1));
    }
  }
}
