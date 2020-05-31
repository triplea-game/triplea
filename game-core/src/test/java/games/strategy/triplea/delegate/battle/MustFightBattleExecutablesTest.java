package games.strategy.triplea.delegate.battle;

import static games.strategy.engine.data.Unit.ALREADY_MOVED;
import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SEA_BATTLE_ROUNDS;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.LAND;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.WATER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.player.Player;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IExecutable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
class MustFightBattleExecutablesTest {

  @Mock GameData mockGameData;
  @Mock GameProperties mockGameProperties;
  @Mock BattleTracker mockBattleTracker;

  @Mock Territory mockBattleSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Mock Unit unit1;
  @Mock UnitType mockUnit1Type;
  @Mock UnitAttachment mockUnit1Attachment;

  @Mock Unit unit2;
  @Mock UnitType mockUnit2Type;
  @Mock UnitAttachment mockUnit2Attachment;

  enum BattleTerrain {
    WATER,
    LAND
  }

  private MustFightBattle newBattle(final BattleTerrain terrain) {
    when(mockGameData.getProperties()).thenReturn(mockGameProperties);

    final UnitCollection mockUnitCollection = mock(UnitCollection.class);
    when(mockBattleSite.getUnitCollection()).thenReturn(mockUnitCollection);

    if (terrain == WATER) {
      when(mockBattleSite.isWater()).thenReturn(true);
      final IntegerMap<GamePlayer> players = new IntegerMap<>();
      players.add(defender, 1);
      players.add(attacker, 1);
      when(mockUnitCollection.getPlayerUnitCounts()).thenReturn(players);
      when(mockGameProperties.get(SEA_BATTLE_ROUNDS, -1)).thenReturn(100);
    } else {
      when(mockBattleSite.getOwner()).thenReturn(defender);
      when(mockGameProperties.get(LAND_BATTLE_ROUNDS, -1)).thenReturn(100);
    }

    final RelationshipTracker mockRelationshipTracker = mock(RelationshipTracker.class);
    when(mockGameData.getRelationshipTracker()).thenReturn(mockRelationshipTracker);
    when(mockRelationshipTracker.isAtWar(attacker, defender)).thenReturn(true);

    return new MustFightBattle(mockBattleSite, attacker, mockGameData, mockBattleTracker);
  }

  private void assertStepIsMissing(final List<IExecutable> execs, final Class<? extends IExecutable> stepClass) {
    final AssertionFailedError missingClassException = assertThrows(
        AssertionFailedError.class,
        () -> getIndex(execs, stepClass),
        stepClass.getName() + " should not be in the steps"
    );

    assertThat(missingClassException.toString(), containsString("No instance:"));
  }

  @Test
  @DisplayName("Verify Offensive/Defensive AA step is not added if no AA offensive/defensive units")
  void aaStepsNotAdded() {
    final MustFightBattle battle = newBattle(LAND);
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertStepIsMissing(execs, MustFightBattle.FireOffensiveAaGuns.class);
    assertStepIsMissing(execs, MustFightBattle.FireDefensiveAaGuns.class);
    assertStepIsMissing(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class);
  }

  @Test
  @DisplayName("Verify Offensive AA step is added if has AA offensive units")
  void aaOffensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(mockGameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);
    when(unit1.getType()).thenReturn(mockUnit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit1.getData()).thenReturn(mockGameData);
    when(mockUnit1Type.getAttachment(anyString())).thenReturn(mockUnit1Attachment);
    when(mockUnit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(mockUnit1Attachment.getOffensiveAttackAa(attacker)).thenReturn(1);
    when(mockUnit1Attachment.getMaxAaAttacks()).thenReturn(1);
    when(mockUnit1Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(mockUnit1Attachment.getTargetsAa(mockGameData)).thenReturn(Set.of(mockUnit2Type));
    when(mockUnit1Attachment.getIsAaForCombatOnly()).thenReturn(true);

    when(unit2.getType()).thenReturn(mockUnit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(mockUnit2Type.getAttachment(anyString())).thenReturn(mockUnit2Attachment);

    battle.setUnits(
        List.of(unit2),
        List.of(unit1),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireOffensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireOffensiveAaGuns.class), is(0));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireOffensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class), is(1));

    assertStepIsMissing(execs, MustFightBattle.FireDefensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify Defensive AA step is added if has AA defensive units")
  void aaDefensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(mockGameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);
    when(unit2.getType()).thenReturn(mockUnit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2.getData()).thenReturn(mockGameData);
    when(mockUnit2Type.getAttachment(anyString())).thenReturn(mockUnit2Attachment);
    when(mockUnit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(mockUnit2Attachment.getAttackAa(defender)).thenReturn(1);
    when(mockUnit2Attachment.getMaxAaAttacks()).thenReturn(1);
    when(mockUnit2Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(mockUnit2Attachment.getTargetsAa(mockGameData)).thenReturn(Set.of(mockUnit1Type));
    when(mockUnit2Attachment.getIsAaForCombatOnly()).thenReturn(true);

    when(unit1.getType()).thenReturn(mockUnit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(mockUnit1Type.getAttachment(anyString())).thenReturn(mockUnit1Attachment);

    battle.setUnits(
        List.of(unit2),
        List.of(unit1),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireDefensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireDefensiveAaGuns.class), is(0));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireDefensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class), is(1));

    assertStepIsMissing(execs, MustFightBattle.FireOffensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify Offensive/Defensive AA step is added if has both AA offensive and defensive units")
  void aaOffensiveAndDefensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(mockGameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);

    // Unit1 is an AA attacker that can target Unit2
    when(unit1.getType()).thenReturn(mockUnit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit1.getData()).thenReturn(mockGameData);
    when(mockUnit1Type.getAttachment(anyString())).thenReturn(mockUnit1Attachment);
    when(mockUnit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(mockUnit1Attachment.getOffensiveAttackAa(attacker)).thenReturn(1);
    when(mockUnit1Attachment.getMaxAaAttacks()).thenReturn(1);
    when(mockUnit1Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(mockUnit1Attachment.getTargetsAa(mockGameData)).thenReturn(Set.of(mockUnit2Type));
    when(mockUnit1Attachment.getIsAaForCombatOnly()).thenReturn(true);

    // Unit2 is an AA defender that can target Unit1
    when(unit2.getType()).thenReturn(mockUnit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2.getData()).thenReturn(mockGameData);
    when(mockUnit2Type.getAttachment(anyString())).thenReturn(mockUnit2Attachment);
    when(mockUnit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(mockUnit2Attachment.getAttackAa(defender)).thenReturn(1);
    when(mockUnit2Attachment.getMaxAaAttacks()).thenReturn(1);
    when(mockUnit2Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(mockUnit2Attachment.getTargetsAa(mockGameData)).thenReturn(Set.of(mockUnit1Type));
    when(mockUnit2Attachment.getIsAaForCombatOnly()).thenReturn(true);

    battle.setUnits(
        List.of(unit2),
        List.of(unit1),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireOffensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireOffensiveAaGuns.class), is(0));

    assertThat(
        "FireDefensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireDefensiveAaGuns.class), is(1));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireOffensiveAaGuns and FireDefensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class), is(2));
  }

  @Test
  @DisplayName("Verify Bombard step is added on first round")
  void bombardStepAddedOnFirstRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireNavalBombardment should be added for first round",
        getIndex(execs, MustFightBattle.FireNavalBombardment.class), is(0));
  }

  @Test
  @DisplayName("Verify Bombard step is not added on subsequent rounds")
  void bombardStepNotAddedOnSubsequentRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertStepIsMissing(execs, MustFightBattle.FireNavalBombardment.class);
  }

  @Test
  @DisplayName("Verify Bombard step is added but no bombardment happens if bombard units are empty")
  void bombardStepAddedButNoBombardUnits() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    doAnswer(
        invocation -> {
          final Change change = invocation.getArgument(0);
          mockGameData.performChange(change);
          return null;
        })
        .when(delegateBridge)
        .addChange(any());

    step.execute(null, delegateBridge);

    verify(mockGameData, times(1)).performChange(ChangeFactory.EMPTY_CHANGE);
    // TODO: Somehow get the stack and check that no new execs were added to it.
  }

  @Test
  @DisplayName("Verify Bombard step is added and bombardment happens if bombard units exist")
  void bombardStepAddedAndBombardHappens() {
    final MustFightBattle battle = newBattle(LAND);

    when(unit1.getType()).thenReturn(mockUnit1Type);
    when(unit1.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit1).getPropertyOrThrow(ALREADY_MOVED);
    when(mockUnit1Type.getAttachment(anyString())).thenReturn(mockUnit1Attachment);

    when(unit2.getType()).thenReturn(mockUnit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(mockUnit2Type.getAttachment(anyString())).thenReturn(mockUnit2Attachment);

    battle.setUnits(
        List.of(unit2),
        List.of(),
        List.of(unit1),
        List.of(),
        defender,
        List.of()
    );
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    doAnswer(
        invocation -> {
          final Change change = invocation.getArgument(0);
          mockGameData.performChange(change);
          return null;
        })
        .when(delegateBridge)
        .addChange(any());
    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));

    step.execute(null, delegateBridge);

    verify(mockGameData, times(1)).performChange(argThat((Change change) -> !change.isEmpty()));
    // TODO: Somehow get the stack and check if a new exec was added to it.
  }
}