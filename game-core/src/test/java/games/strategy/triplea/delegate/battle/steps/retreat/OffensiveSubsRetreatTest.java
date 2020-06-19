package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitTransport;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveSubsRetreatTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource
  void retreatHappens(final String displayName, final BattleState battleState) {
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(false), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());
  }

  static List<Arguments> retreatHappens() {
    return List.of(
        Arguments.of(
            "Can not submerge but has retreat territories",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData().withTransportCasualtiesRestricted(false).build())
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .build()),
        Arguments.of(
            "Has no retreat territories but can submerge",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData()
                        .withTransportCasualtiesRestricted(false)
                        .withSubmersibleSubs(true)
                        .build())
                .attackerRetreatTerritories(List.of())
                .build()),
        Arguments.of(
            "Transports with other units on the defense",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData()
                        .withTransportCasualtiesRestricted(true)
                        .withSubmersibleSubs(true)
                        .build())
                .defendingUnits(List.of(givenUnitTransport(), givenAnyUnit()))
                .build()));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource
  void retreatDoesNotHappen(final String displayName, final BattleState battleState) {
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  static List<Arguments> retreatDoesNotHappen() {
    return List.of(
        Arguments.of(
            "Battle is over",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .over(true)
                .build()),
        Arguments.of(
            "Defending Destroyer exists",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .defendingUnits(List.of(givenUnitDestroyer()))
                .build()),
        Arguments.of(
            "Waiting to Die Defending Destroyer exists",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .defendingWaitingToDie(List.of(givenUnitDestroyer()))
                .build()),
        Arguments.of(
            "Amphibious assault",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData().withTransportCasualtiesRestricted(false).build())
                .amphibious(true)
                .build()),
        Arguments.of(
            "Can withdraw but only defenseless transports on the defense",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData()
                        .withTransportCasualtiesRestricted(true)
                        .withSubmersibleSubs(false)
                        .build())
                .defendingUnits(List.of(givenUnitTransport()))
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .build()),
        Arguments.of(
            "Can submerge but only defenseless transports on the defense",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvade()))
                .gameData(
                    MockGameData.givenGameData()
                        .withTransportCasualtiesRestricted(true)
                        .withSubmersibleSubs(true)
                        .build())
                .defendingUnits(List.of(givenUnitTransport()))
                .build()),
        Arguments.of(
            "No retreat territories and can not submerge",
            givenBattleStateBuilder()
                .gameData(
                    MockGameData.givenGameData().withTransportCasualtiesRestricted(false).build())
                .attackerRetreatTerritories(List.of())
                .build()));
  }

  static class MockGameData {
    private final GameData gameData;
    private final GameProperties gameProperties;
    private boolean propertiesSetup = false;

    private MockGameData() {
      gameData = mock(GameData.class);
      gameProperties = mock(GameProperties.class);
    }

    static MockGameData givenGameData() {
      return new MockGameData();
    }

    GameData build() {
      return gameData;
    }

    MockGameData withTransportCasualtiesRestricted(final boolean value) {
      setupProperties();
      when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(value);
      return this;
    }

    private void setupProperties() {
      if (!propertiesSetup) {
        propertiesSetup = true;
        when(gameData.getProperties()).thenReturn(gameProperties);
      }
    }

    MockGameData withSubmersibleSubs(final boolean value) {
      setupProperties();
      when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(value);
      return this;
    }
  }
}
