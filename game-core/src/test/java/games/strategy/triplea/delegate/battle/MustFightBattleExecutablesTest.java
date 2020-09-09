package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE;
import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SEA_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.WW2V2;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.WATER;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.general.OffensiveGeneral;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnmatchedTest")
class MustFightBattleExecutablesTest {

  @Mock GameData gameData;
  @Mock GameProperties gameProperties;
  @Mock BattleTracker battleTracker;

  @Mock Territory battleSite;
  @Mock Territory retreatSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  enum BattleTerrain {
    WATER,
    LAND
  }

  private MustFightBattle newBattle(final BattleTerrain terrain) {
    when(gameData.getProperties()).thenReturn(gameProperties);

    final UnitCollection mockUnitCollection = mock(UnitCollection.class);
    when(battleSite.getUnitCollection()).thenReturn(mockUnitCollection);

    if (terrain == WATER) {
      when(battleSite.isWater()).thenReturn(true);
      final IntegerMap<GamePlayer> players = new IntegerMap<>();
      players.add(defender, 1);
      players.add(attacker, 1);
      when(mockUnitCollection.getPlayerUnitCounts()).thenReturn(players);
      when(gameProperties.get(SEA_BATTLE_ROUNDS, -1)).thenReturn(100);
    } else {
      when(battleSite.getOwner()).thenReturn(defender);
      when(gameProperties.get(LAND_BATTLE_ROUNDS, -1)).thenReturn(100);
    }

    final RelationshipTracker mockRelationshipTracker = mock(RelationshipTracker.class);
    when(gameData.getRelationshipTracker()).thenReturn(mockRelationshipTracker);
    when(mockRelationshipTracker.isAtWar(attacker, defender)).thenReturn(true);
    when(mockRelationshipTracker.isAtWar(defender, attacker)).thenReturn(true);

    return new MustFightBattle(battleSite, attacker, gameData, battleTracker);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  private MustFightBattle givenFirstStrikeBattleSetup(
      final boolean attackerDestroyer,
      final boolean defenderDestroyer,
      final boolean ww2v2,
      final boolean defendingSubsSneakAttack) {
    final MustFightBattle battle = spy(newBattle(WATER));

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(ww2v2);
    lenient()
        .when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false))
        .thenReturn(defendingSubsSneakAttack);

    final Unit attackerUnit = attackerDestroyer ? givenUnitDestroyer() : givenAnyUnit();
    final Unit defenderUnit = defenderDestroyer ? givenUnitDestroyer() : givenAnyUnit();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    when(defenderUnit.getOwner()).thenReturn(defender);

    battle.setUnits(
        List.of(defenderUnit, givenUnitFirstStrike()),
        List.of(attackerUnit, givenUnitFirstStrike()),
        List.of(),
        List.of(),
        defender,
        List.of());

    return battle;
  }

  private enum FirstStrikeBattleStep {
    ATTACKER,
    DEFENDER,
    STANDARD,
  }

  private void assertThatFirstStrikeStepOrder(
      final MustFightBattle battle, final List<FirstStrikeBattleStep> stepOrder) {
    final List<IExecutable> execs = battle.getBattleExecutables();

    final EnumMap<FirstStrikeBattleStep, Integer> indices =
        new EnumMap<>(FirstStrikeBattleStep.class);

    indices.put(FirstStrikeBattleStep.ATTACKER, getIndex(execs, OffensiveFirstStrike.class));
    indices.put(FirstStrikeBattleStep.DEFENDER, getIndex(execs, DefensiveFirstStrike.class));
    indices.put(FirstStrikeBattleStep.STANDARD, getIndex(execs, OffensiveGeneral.class));

    assertThat(indices.get(stepOrder.get(0)), lessThan(indices.get(stepOrder.get(1))));
    assertThat(indices.get(stepOrder.get(1)), lessThan(indices.get(stepOrder.get(2))));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, false, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, false, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender>attacker>standard")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, false, true),
        List.of(
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, false, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }
}
