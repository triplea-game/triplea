package games.strategy.triplea.delegate.battle;

import static games.strategy.engine.data.Unit.ALREADY_MOVED;
import static games.strategy.engine.data.Unit.SUBMERGED;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.IGNORE_TRANSPORT_IN_MOVEMENT;
import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.RETREATING_UNITS_REMAIN_IN_PLACE;
import static games.strategy.triplea.Constants.SEA_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.WW2V2;
import static games.strategy.triplea.Constants.WW2V3;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.LAND;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.WATER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
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

  @Mock GameData gameData;
  @Mock GameProperties gameProperties;
  @Mock BattleTracker battleTracker;

  @Mock Territory battleSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Mock Unit unit1;
  @Mock UnitType unit1Type;
  @Mock UnitAttachment unit1Attachment;

  @Mock Unit unit2;
  @Mock UnitType unit2Type;
  @Mock UnitAttachment unit2Attachment;

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

    return new MustFightBattle(battleSite, attacker, gameData, battleTracker);
  }

  private IDelegateBridge newDelegateBridge() {
    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    doAnswer(
            invocation -> {
              final Change change = invocation.getArgument(0);
              gameData.performChange(change);
              return null;
            })
        .when(delegateBridge)
        .addChange(any());
    return delegateBridge;
  }

  private Unit newCanEvadeUnit(final GamePlayer owner) {
    final Unit unit = mock(Unit.class);
    final UnitType unitType = mock(UnitType.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);

    when(unit.getOwner()).thenReturn(owner);
    when(unit.getType()).thenReturn(unitType);
    when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
    when(unitAttachment.getCanEvade()).thenReturn(true);

    return unit;
  }

  private void assertStepIsMissing(
      final List<IExecutable> execs, final Class<? extends IExecutable> stepClass) {
    final AssertionFailedError missingClassException =
        assertThrows(
            AssertionFailedError.class,
            () -> getIndex(execs, stepClass),
            stepClass.getName() + " should not be in the steps");

    assertThat(missingClassException.toString(), containsString("No instance:"));
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on first run")
  void bombardStepAddedOnFirstRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireNavalBombardment should be added for first round",
        getIndex(execs, MustFightBattle.FireNavalBombardment.class),
        greaterThanOrEqualTo(0));
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on subsequent run")
  void bombardStepNotAddedOnSubsequentRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertStepIsMissing(execs, MustFightBattle.FireNavalBombardment.class);
  }

  @Test
  @DisplayName("Verify Bombard step is added but no bombardment happens if bombard units are empty")
  void bombardStepAddedButNoBombardUnits() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = newDelegateBridge();

    step.execute(null, delegateBridge);

    verify(gameData, times(1)).performChange(ChangeFactory.EMPTY_CHANGE);
  }

  @Test
  @DisplayName("Verify Bombard step is added and bombardment happens if bombard units exist")
  void bombardStepAddedAndBombardHappens() {
    final MustFightBattle battle = newBattle(LAND);

    when(unit1.getType()).thenReturn(unit1Type);
    when(unit1.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit1).getPropertyOrThrow(ALREADY_MOVED);
    when(unit1Type.getAttachment(anyString())).thenReturn(unit1Attachment);

    when(unit2.getType()).thenReturn(unit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2Type.getAttachment(anyString())).thenReturn(unit2Attachment);

    battle.setUnits(List.of(unit2), List.of(), List.of(unit1), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = newDelegateBridge();
    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));

    step.execute(null, delegateBridge);

    verify(gameData, times(1)).performChange(argThat((Change change) -> !change.isEmpty()));
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on first run")
  void paratrooperStepAddedOnFirstRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "LandParatroopers should be added for first round",
        getIndex(execs, MustFightBattle.LandParatroopers.class),
        greaterThanOrEqualTo(0));
  }
  //@DisplayName("Verify basic land battle with no AirTransport tech on first run")

  @Test
  @DisplayName("Verify basic land battle with paratroopers on subsequent run")
  void paratrooperStepNotAddedOnSubsequentRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertStepIsMissing(execs, MustFightBattle.LandParatroopers.class);
  }
  //@DisplayName("Verify basic land battle with empty paratroopers on first run")

  @Test
  @DisplayName("Verify basic land battle with offensive Aa")
  void aaOffensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);
    when(unit1.getType()).thenReturn(unit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit1.getData()).thenReturn(gameData);
    when(unit1Type.getAttachment(anyString())).thenReturn(unit1Attachment);
    when(unit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unit1Attachment.getOffensiveAttackAa(attacker)).thenReturn(1);
    when(unit1Attachment.getMaxAaAttacks()).thenReturn(1);
    when(unit1Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(unit1Attachment.getTargetsAa(gameData)).thenReturn(Set.of(unit2Type));
    when(unit1Attachment.getIsAaForCombatOnly()).thenReturn(true);

    when(unit2.getType()).thenReturn(unit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2Type.getAttachment(anyString())).thenReturn(unit2Attachment);

    battle.setUnits(List.of(unit2), List.of(unit1), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireOffensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireOffensiveAaGuns.class),
        is(0));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireOffensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class),
        is(1));

    assertStepIsMissing(execs, MustFightBattle.FireDefensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify basic land battle with defensive Aa")
  void aaDefensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);
    when(unit2.getType()).thenReturn(unit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2.getData()).thenReturn(gameData);
    when(unit2Type.getAttachment(anyString())).thenReturn(unit2Attachment);
    when(unit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unit2Attachment.getAttackAa(defender)).thenReturn(1);
    when(unit2Attachment.getMaxAaAttacks()).thenReturn(1);
    when(unit2Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(unit2Attachment.getTargetsAa(gameData)).thenReturn(Set.of(unit1Type));
    when(unit2Attachment.getIsAaForCombatOnly()).thenReturn(true);

    when(unit1.getType()).thenReturn(unit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit1Type.getAttachment(anyString())).thenReturn(unit1Attachment);

    battle.setUnits(List.of(unit2), List.of(unit1), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireDefensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireDefensiveAaGuns.class),
        is(0));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireDefensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class),
        is(1));

    assertStepIsMissing(execs, MustFightBattle.FireOffensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify basic land battle with offensive and defensive Aa")
  void aaOffensiveAndDefensiveStepAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameData.getRelationshipTracker().isAtWar(defender, attacker)).thenReturn(true);

    // Unit1 is an AA attacker that can target Unit2
    when(unit1.getType()).thenReturn(unit1Type);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit1.getData()).thenReturn(gameData);
    when(unit1Type.getAttachment(anyString())).thenReturn(unit1Attachment);
    when(unit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unit1Attachment.getOffensiveAttackAa(attacker)).thenReturn(1);
    when(unit1Attachment.getMaxAaAttacks()).thenReturn(1);
    when(unit1Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(unit1Attachment.getTargetsAa(gameData)).thenReturn(Set.of(unit2Type));
    when(unit1Attachment.getIsAaForCombatOnly()).thenReturn(true);

    // Unit2 is an AA defender that can target Unit1
    when(unit2.getType()).thenReturn(unit2Type);
    when(unit2.getOwner()).thenReturn(defender);
    when(unit2.getData()).thenReturn(gameData);
    when(unit2Type.getAttachment(anyString())).thenReturn(unit2Attachment);
    when(unit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unit2Attachment.getAttackAa(defender)).thenReturn(1);
    when(unit2Attachment.getMaxAaAttacks()).thenReturn(1);
    when(unit2Attachment.getMaxRoundsAa()).thenReturn(-1);
    when(unit2Attachment.getTargetsAa(gameData)).thenReturn(Set.of(unit1Type));
    when(unit2Attachment.getIsAaForCombatOnly()).thenReturn(true);

    battle.setUnits(List.of(unit2), List.of(unit1), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThat(
        "FireOffensiveAaGuns should be the first step",
        getIndex(execs, MustFightBattle.FireOffensiveAaGuns.class),
        is(0));

    assertThat(
        "FireDefensiveAaGuns should be the second step",
        getIndex(execs, MustFightBattle.FireDefensiveAaGuns.class),
        is(1));

    assertThat(
        "ClearAaWaitingToDieAndDamagedChangesInto is after FireOffensiveAaGuns and FireDefensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class),
        is(2));
  }
  //@DisplayName("Verify impossible sea battle with bombarding and paratroopers will not add a bombarding or paratrooper step")

  //@DisplayName("Verify attacking canEvade units retreating if SUB_RETREAT_BEFORE_BATTLE and no destroyers")
  //@DisplayName("Verify defending canEvade units retreating if SUB_RETREAT_BEFORE_BATTLE and no destroyers")

  @Test
  @DisplayName(
      "Verify AttackerRetreatSubsBeforeBattle step is added if SUB_RETREAT_BEFORE_BATTLE is true")
  void attackerSubsRetreatBeforeBattleIsAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertThat(
        getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class),
        greaterThanOrEqualTo(0));
  }

  @Test
  @DisplayName(
      "Verify AttackerRetreatSubsBeforeBattle step is NOT added if SUB_RETREAT_BEFORE_BATTLE is false")
  void attackerSubsRetreatBeforeBattleIsNotAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertStepIsMissing(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
  }

  @Test
  @DisplayName(
      "Verify DefenderRetreatSubsBeforeBattle step is added if SUB_RETREAT_BEFORE_BATTLE is true")
  void defenderSubsRetreatBeforeBattleIsAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertThat(
        getIndex(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class),
        greaterThanOrEqualTo(0));
  }

  @Test
  @DisplayName(
      "Verify DefenderRetreatSubsBeforeBattle step is NOT added if SUB_RETREAT_BEFORE_BATTLE is false")
  void defenderSubsRetreatBeforeBattleIsNotAdded() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertStepIsMissing(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
  }

  //@DisplayName("Verify attacking transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  //@DisplayName("Verify defending transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  //@DisplayName("Verify basic attacker firstStrike (no other attackers, no special defenders, all options false)")
  //@DisplayName("Verify attacker firstStrike with destroyers")
  //@DisplayName("Verify basic defender firstStrike (no other attackers, no special defenders, all options false)")
  //@DisplayName("Verify defender firstStrike with DEFENDING_SUBS_SNEAK_ATTACK true")
  //@DisplayName("Verify defender firstStrike with DEFENDING_SUBS_SNEAK_ATTACK true and attacker destroyers")
  //@DisplayName("Verify defender firstStrike with WW2v2 true")
  //@DisplayName("Verify defender firstStrike with WW2v2 true and attacker destroyers")
  //@DisplayName("Verify basic attacker and defender firstStrikes (no other attackers, no special defenders, all options false)")
  //@DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true")
  //@DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true and attacker/defender destroyers")
  //@DisplayName("Verify attacking/defender firstStrikes with WW2v2 true")
  //@DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and attacker/defender destroyers")
  //@DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true and defender destroyers")
  //@DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true and attacking destroyers")
  //@DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and defender destroyers")
  //@DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and attacking destroyers")
  //@DisplayName("Verify attacking firstStrikes against air")
  //@DisplayName("Verify attacking firstStrikes against air with destroyer")
  //@DisplayName("Verify defending firstStrikes against air")
  //@DisplayName("Verify defending firstStrikes against air with destroyer")
  //@DisplayName("Verify unescorted attacking transports are removed if casualities are restricted")
  //@DisplayName("Verify unescorted defending transports are removed if casualities are restricted")
  //@DisplayName("Verify unescorted attacking transports are not removed if casualties are not restricted")
  //@DisplayName("Verify unescorted defending transports are removed if casualities are not restricted")
  //@DisplayName("Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  //@DisplayName("Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true even with destroyers")

  // destroyers always prevent submerging
  @Test
  @DisplayName(
      "Verify attacking firstStrike submerge before battle "
          + "if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true and no destroyers")
  void attackerSubsRetreatBeforeBattle() {
    final MustFightBattle battle = newBattle(LAND);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(RETREATING_UNITS_REMAIN_IN_PLACE, false)).thenReturn(false);
    when(gameProperties.get(IGNORE_TRANSPORT_IN_MOVEMENT, false)).thenReturn(false);
    when(gameProperties.get(WW2V3, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);

    when(attacker.getName()).thenReturn("mockAttacker");

    final Unit unit = newCanEvadeUnit(attacker);

    final MutableProperty<Boolean> submergedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(submergedProperty).when(unit).getPropertyOrThrow(SUBMERGED);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    doAnswer(
        invocation -> {
          final Change change = invocation.getArgument(0);
          gameData.performChange(change);
          return null;
        })
        .when(delegateBridge)
        .addChange(any());
    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(mock(IDisplay.class));
    when(delegateBridge.getHistoryWriter()).thenReturn(DelegateHistoryWriter.NO_OP_INSTANCE);
    final Player remotePlayer = mock(Player.class);
    when(delegateBridge.getRemotePlayer(attacker)).thenReturn(remotePlayer);
    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));
    when(remotePlayer.retreatQuery(
        any(), eq(true), eq(battleSite), eq(List.of(battleSite)), anyString()))
        .thenReturn(battleSite);
    step.execute(null, delegateBridge);

    verify(gameData, times(1)).performChange(argThat((Change change) -> !change.isEmpty()));
  }
  
  //@DisplayName("Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  //@DisplayName("Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true even with destroyers")
  //@DisplayName("Verify defending firstStrike submerge before battle if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true")
  //@DisplayName("Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  //@DisplayName("Verify attacking firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and no retreat territories")
  //@DisplayName("Verify attacking firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and destroyers present")
  //@DisplayName("Verify attacking firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and destroyers waiting to die")
  //@DisplayName("Verify attacking firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and defenseless transports with restricted casualties")
  //@DisplayName("Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false and defenseless transports with non restricted casualties")
  //@DisplayName("Verify defending firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  //@DisplayName("Verify defending firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and no retreat territories")
  //@DisplayName("Verify defending firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and destroyers present")
  //@DisplayName("Verify defending firstStrike can't withdraw when SUBMERSIBLE_SUBS is false and destroyers waiting to die")
  //@DisplayName("Verify attacking air units at sea can withdraw")
  //@DisplayName("Verify partial amphibious attack can withdraw if a unit was not amphibious")
  //@DisplayName("Verify partial amphibious attack can not withdraw if all units were amphibious")
  //@DisplayName("Verify partial amphibious attack can not withdraw if partial amphibious withdrawal not allowed")
  //@DisplayName("Verify partial amphibious attack can not withdraw if not amphibious")
  //@DisplayName("Verify attacker planes can withdraw if ww2v2 and amphibious")
  //@DisplayName("Verify attacker planes can withdraw if attacker can partial amphibious retreat and amphibious")
  //@DisplayName("Verify attacker planes can withdraw if attacker can retreat planes and amphibious")
  //@DisplayName("Verify attacker planes can not withdraw if ww2v2 and not amphibious")
  //@DisplayName("Verify attacker planes can not withdraw if attacker can partial amphibious retreat and not amphibious")
  //@DisplayName("Verify attacker planes can not withdraw if attacker can retreat planes and not amphibious")
}
