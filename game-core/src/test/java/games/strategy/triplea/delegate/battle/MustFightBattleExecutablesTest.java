package games.strategy.triplea.delegate.battle;

import static games.strategy.engine.data.Unit.ALREADY_MOVED;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.IGNORE_TRANSPORT_IN_MOVEMENT;
import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.RETREATING_UNITS_REMAIN_IN_PLACE;
import static games.strategy.triplea.Constants.SEA_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.WW2V2;
import static games.strategy.triplea.Constants.WW2V3;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.LAND;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.WATER;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitAirTransport;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitTransport;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.newUnitAndAttachment;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IExecutable;
import java.math.BigDecimal;
import java.util.EnumMap;
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
import org.triplea.util.Tuple;

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
    lenient().when(mockRelationshipTracker.isAtWar(attacker, defender)).thenReturn(true);
    lenient().when(mockRelationshipTracker.isAllied(attacker, attacker)).thenReturn(true);
    lenient().when(mockRelationshipTracker.isAllied(defender, defender)).thenReturn(true);
    lenient().when(mockRelationshipTracker.isAllied(defender, attacker)).thenReturn(false);
    lenient().when(mockRelationshipTracker.isAllied(attacker, defender)).thenReturn(false);

    return new MustFightBattle(battleSite, attacker, gameData, battleTracker);
  }

  private void assertThatStepIsMissing(
      final List<IExecutable> execs, final Class<? extends IExecutable> stepClass) {
    final AssertionFailedError missingClassException =
        assertThrows(
            AssertionFailedError.class,
            () -> getIndex(execs, stepClass),
            stepClass.getName() + " should not be in the steps");

    assertThat(missingClassException.toString(), containsString("No instance:"));
  }

  private void assertThatStepExists(
      final List<IExecutable> execs, final Class<? extends IExecutable> stepClass) {
    assertThat(
        stepClass.getName() + " is missing from the steps",
        getIndex(execs, stepClass),
        greaterThanOrEqualTo(0));
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on first run")
  void bombardOnFirstRun() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepExists(execs, MustFightBattle.FireNavalBombardment.class);
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on subsequent run")
  void bombardOnSubsequentRun() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertThatStepIsMissing(execs, MustFightBattle.FireNavalBombardment.class);
  }

  @Test
  @DisplayName("Verify Bombard step is added but no bombardment happens if bombard units are empty")
  void bombardStepAddedButNoBombardUnits() {
    final MustFightBattle battle = spy(newBattle(LAND));

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);

    step.execute(null, delegateBridge);

    verify(delegateBridge).addChange(ChangeFactory.EMPTY_CHANGE);
    verify(battle, never())
        .fire(anyString(), any(), any(), any(), any(), anyBoolean(), any(), anyString());
  }

  @Test
  @DisplayName("Verify Bombard step is added and bombardment happens if bombard units exist")
  void bombardStepAddedAndBombardHappens() {
    final MustFightBattle battle = spy(newBattle(LAND));

    final Unit unit1 = mock(Unit.class);
    when(unit1.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit1).getPropertyOrThrow(ALREADY_MOVED);

    final Unit unit2 = givenAnyUnit();
    when(unit2.getOwner()).thenReturn(defender);

    battle.setUnits(List.of(unit2), List.of(), List.of(unit1), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.FireNavalBombardment.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));
    doNothing()
        .when(battle)
        .fire(anyString(), any(), any(), any(), any(), anyBoolean(), any(), anyString());

    step.execute(null, delegateBridge);

    verify(delegateBridge).addChange(argThat((Change change) -> !change.isEmpty()));
    verify(battle).fire(anyString(), any(), any(), any(), any(), anyBoolean(), any(), anyString());
  }

  @Test
  @DisplayName("Verify paratrooper battle steps on first run")
  void paratrooperStepAddedOnFirstRound() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepExists(execs, MustFightBattle.LandParatroopers.class);
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on first run")
  void paratroopersFirstRun() {
    final MustFightBattle battle = spy(newBattle(LAND));
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);

    final Unit unit1 = givenAnyUnit();
    when(unit1.getOwner()).thenReturn(attacker);
    final Unit unit3 = givenUnitAirTransport();
    when(unit3.getOwner()).thenReturn(attacker);

    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    doReturn(List.of(unit1)).when(battle).getDependentUnits(any());

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());

    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.LandParatroopers.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge).addChange(any());
  }

  @Test
  @DisplayName("Verify basic land battle with no AirTransport tech on first run")
  void noAirTransportTech() {
    final MustFightBattle battle = newBattle(LAND);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.LandParatroopers.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on subsequent run")
  void paratroopersSubsequentRun() {
    final MustFightBattle battle = newBattle(LAND);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(false);

    assertThatStepIsMissing(execs, MustFightBattle.LandParatroopers.class);
  }

  @Test
  @DisplayName("Verify basic land battle with empty paratroopers on first run")
  void emptyParatroopersFirstRun() {
    final MustFightBattle battle = spy(newBattle(LAND));
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);

    final Unit unit1 = givenAnyUnit();
    when(unit1.getOwner()).thenReturn(attacker);
    final Unit unit3 = givenUnitAirTransport();
    when(unit3.getOwner()).thenReturn(attacker);

    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    doReturn(List.of()).when(battle).getDependentUnits(any());

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());

    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.LandParatroopers.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
  }

  @Test
  @DisplayName("Verify basic land battle with offensive Aa")
  void offensiveAaFire() {
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

    assertThatStepIsMissing(execs, MustFightBattle.FireDefensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify basic land battle with defensive Aa")
  void defensiveAaFire() {
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

    assertThatStepIsMissing(execs, MustFightBattle.FireOffensiveAaGuns.class);
  }

  @Test
  @DisplayName("Verify basic land battle with offensive and defensive Aa")
  void offensiveAndDefensiveAaFire() {
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
        "ClearAaWaitingToDieAndDamagedChangesInto is after "
            + "FireOffensiveAaGuns and FireDefensiveAaGuns",
        getIndex(execs, MustFightBattle.ClearAaWaitingToDieAndDamagedChangesInto.class),
        is(2));
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE, no destroyers, and retreat territory")
  void attackingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());
    doReturn(List.of(battleSite)).when(battle).getAttackerRetreatTerritories();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    final Unit unit = givenUnitCanEvade();
    when(unit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE and destroyers")
  void attackingSubsNotRetreatIfDestroyersAndCanRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    // it doesn't even check if the unit can evade
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    final Unit destroyer = givenUnitDestroyer();

    battle.setUnits(List.of(destroyer), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can not retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE is true, SUBMERSIBLE_SUBS is false, and no retreat")
  void attackingSubsCanNotRetreatIfRetreatBeforeBattleAndSubmersibleAndNoRetreatTerritories() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(false);

    // it doesn't even check if the unit can evade
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can not retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE is true, SUBMERSIBLE_SUBS is false, retreat exists, "
          + "but has defenseless transports")
  void attackingSubsCanNotRetreatIfBeforeBattleAndSubmersibleAndTerritoriesAndDefenselessTransp() {
    final MustFightBattle battle = spy(newBattle(WATER));

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(false);

    // it doesn't even check if the unit can evade
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    final Unit transport = givenUnitTransport();

    battle.setUnits(List.of(transport), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can not retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE is true, SUBMERSIBLE_SUBS is false, retreat exists, "
          + "has defenseless transports that are not restricted")
  void attackingSubsCanNotRetreatIfBeforeBattleAndSubmersibleAndTerritoriesAndUnRestrTransp() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of(battleSite)).when(battle).getAttackerRetreatTerritories();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);

    final Unit unit = givenUnitCanEvade();
    when(unit.getOwner()).thenReturn(attacker);

    // it won't even check if the unit is a transport
    final Unit transport = givenAnyUnit();

    battle.setUnits(List.of(transport), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE is true, SUBMERSIBLE_SUBS is false, retreat exists, "
          + "has no defenseless transports")
  void attackingSubsCanRetreatIfBeforeBattleAndSubmersibleAndRetreatAndNoDefenselessTransports() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of(battleSite)).when(battle).getAttackerRetreatTerritories();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);

    final Unit canEvadeUnit = givenUnitCanEvade();
    when(canEvadeUnit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(canEvadeUnit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName("Verify attacking canEvade units can retreat if SUB_RETREAT_BEFORE_BATTLE")
  void attackerSubsRetreatBeforeBattleIsAdded() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepExists(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE is false")
  void attackingSubsRetreatIfCanNotRetreatBeforeBattle() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepIsMissing(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
  }

  @Test
  // firstStrike is actually not checked, unlike in BattleSteps
  @DisplayName(
      "Verify attacking firstStrike submerge before battle if "
          + "SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true and no destroyers")
  void attackingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(RETREATING_UNITS_REMAIN_IN_PLACE, false)).thenReturn(false);
    when(gameProperties.get(IGNORE_TRANSPORT_IN_MOVEMENT, false)).thenReturn(false);
    when(gameProperties.get(WW2V3, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);

    final Unit canEvadeUnit = givenUnitCanEvade();
    when(canEvadeUnit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(canEvadeUnit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.AttackerRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE, no destroyers, and retreat territory")
  void defendingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());
    doReturn(List.of(battleSite)).when(battle).getEmptyOrFriendlySeaNeighbors(any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    final Unit canEvadeUnit = givenUnitCanEvade();
    when(canEvadeUnit.getOwner()).thenReturn(defender);

    battle.setUnits(List.of(canEvadeUnit), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE and destroyers")
  void defendingSubsNotRetreatIfDestroyersAndCanRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    // it doesn't even check if the unit can evade
    final Unit canEvadeUnit = givenAnyUnit();
    when(canEvadeUnit.getOwner()).thenReturn(defender);

    final Unit destroyer = givenUnitDestroyer();

    battle.setUnits(
        List.of(canEvadeUnit), List.of(destroyer), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if "
          + "SUB_RETREAT_BEFORE_BATTLE is true, SUBMERSIBLE_SUBS is false, and no retreat")
  void defendingSubsCanNotRetreatIfRetreatBeforeBattleAndSubmersibleAndNoRetreatTerritories() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getEmptyOrFriendlySeaNeighbors(any(), any());

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(false);

    // it doesn't even check if the unit can evade
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName("Verify defending canEvade units can retreat if SUB_RETREAT_BEFORE_BATTLE")
  void defenderSubsRetreatBeforeBattleIsAdded() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepExists(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE is false")
  void defendingSubsRetreatIfCanNotRetreatBeforeBattle() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepIsMissing(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
  }

  @Test
  // firstStrike is actually not checked, unlike in BattleSteps
  @DisplayName(
      "Verify defending firstStrike submerge before battle if "
          + "SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true and no destroyers")
  void defendingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doNothing().when(battle).queryRetreat(anyBoolean(), any(), any(), any());
    doReturn(List.of()).when(battle).getEmptyOrFriendlySeaNeighbors(any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);

    final Unit canEvadeUnit = givenUnitCanEvade();
    when(canEvadeUnit.getOwner()).thenReturn(defender);

    battle.setUnits(List.of(canEvadeUnit), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.DefenderRetreatSubsBeforeBattle.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(battle).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  @DisplayName("Verify transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  void transportsAreRemovedIfTransportCasualtiesRestricted() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepExists(execs, MustFightBattle.RemoveUndefendedTransports.class);
  }

  @Test
  @DisplayName("Verify transports are not removed if TRANSPORT_CASUALTIES_RESTRICTED is false")
  void transportsAreNotRemovedIfTransportCasualtiesUnRestricted() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    assertThatStepIsMissing(execs, MustFightBattle.RemoveUndefendedTransports.class);
  }

  @Test
  @DisplayName("Verify unescorted attacking transports are removed if casualities are restricted")
  void unescortedAttackingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    doNothing().when(battle).remove(any(), any(), any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getFirst();
    when(unit.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment1 = unitAndAttachment.getSecond();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(attacker)).thenReturn(1);
    when(attachment2.getAttack(attacker)).thenReturn(1);
    when(attachment2.getIsSea()).thenReturn(true);
    when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit2).getPropertyOrThrow(ALREADY_MOVED);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge).addChange(any());
    verify(battle).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName(
      "Verify attacking transports are not removed if "
          + "TRANSPORT_CASUALTIES_RESTRICTED is true but has retreat territories")
  void attackingTransportsAreNotRemovedIfTransportCasualtiesRestrictedButHasRetreat() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of(retreatSite)).when(battle).getAttackerRetreatTerritories();
    // doNothing().when(battle).remove(any(), any(), any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
    verify(battle, never()).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName(
      "Verify attacking transports are not removed if "
          + "TRANSPORT_CASUALTIES_RESTRICTED is true but has no transports")
  void attackingTransportsAreNotRemovedIfTransportCasualtiesRestrictedButNoTransports() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenUnitDestroyer();
    when(unit.getOwner()).thenReturn(attacker);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
    verify(battle, never()).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName(
      "Verify attacking transports are not removed if "
          + "TRANSPORT_CASUALTIES_RESTRICTED is true but no defenders")
  void attackingTransportsAreNotRemovedIfTransportCasualtiesRestrictedButNoDefenders() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getFirst();
    when(unit.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment1 = unitAndAttachment.getSecond();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(attacker)).thenReturn(0);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
    verify(battle, never()).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName("Verify unescorted defending transports are removed if casualities are restricted")
  void unescortedDefendingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    doNothing().when(battle).remove(any(), any(), any(), any());
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getFirst();
    when(unit.getOwner()).thenReturn(defender);
    final UnitAttachment attachment1 = unitAndAttachment.getSecond();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(defender)).thenReturn(1);
    when(attachment2.getAttack(defender)).thenReturn(1);
    when(attachment2.getIsSea()).thenReturn(true);
    when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit2).getPropertyOrThrow(ALREADY_MOVED);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit), List.of(unit2), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge).addChange(any());
    verify(battle).remove(any(), any(), any(), eq(true));
  }

  @Test
  @DisplayName(
      "Verify defending transports are not removed if "
          + "TRANSPORT_CASUALTIES_RESTRICTED is true but has no transports")
  void defendingTransportsAreNotRemovedIfTransportCasualtiesRestrictedButNoTransports() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenUnitDestroyer();
    when(unit.getOwner()).thenReturn(defender);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
    verify(battle, never()).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName(
      "Verify defending transports are not removed if "
          + "TRANSPORT_CASUALTIES_RESTRICTED is true but no defenders")
  void defendingTransportsAreNotRemovedIfTransportCasualtiesRestrictedButNoDefenders() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getFirst();
    when(unit.getOwner()).thenReturn(defender);
    final UnitAttachment attachment1 = unitAndAttachment.getSecond();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final Tuple<Unit, UnitAttachment> unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getFirst();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getSecond();
    when(attachment2.getMovement(defender)).thenReturn(0);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    final int index = getIndex(execs, MustFightBattle.RemoveUndefendedTransports.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    verify(delegateBridge, never()).addChange(any());
    verify(battle, never()).remove(any(), any(), any(), eq(false));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  private Tuple<MustFightBattle, List<IExecutable>> givenFirstStrikeBattleSetup(
      final boolean attackerDestroyer,
      final boolean defenderDestroyer,
      final boolean ww2v2,
      final boolean defendingSubsSneakAttack,
      final boolean ignoreDefendingSubsSneakAttack) {
    final MustFightBattle battle = spy(newBattle(WATER));
    lenient().doNothing().when(battle).firstStrikeAttackersFire(any());
    lenient().doNothing().when(battle).firstStrikeDefendersFire(any());

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(ww2v2);
    if (!ignoreDefendingSubsSneakAttack) {
      when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false))
          .thenReturn(defendingSubsSneakAttack);
    }

    final Unit attackerUnit = attackerDestroyer ? givenUnitDestroyer() : givenAnyUnit();
    final Unit defenderUnit = defenderDestroyer ? givenUnitDestroyer() : givenAnyUnit();

    battle.setUnits(
        List.of(defenderUnit), List.of(attackerUnit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables(true);

    return Tuple.of(battle, execs);
  }

  private enum FirstStrikeBattleStep {
    ATTACKER,
    DEFENDER,
    STANDARD,
  }

  private void assertThatFirstStrikeStepOrder(
      final Tuple<MustFightBattle, List<IExecutable>> battleTuple,
      final List<FirstStrikeBattleStep> stepOrder) {
    final List<IExecutable> execs = battleTuple.getSecond();

    final EnumMap<FirstStrikeBattleStep, Integer> indices =
        new EnumMap<>(FirstStrikeBattleStep.class);

    indices.put(
        FirstStrikeBattleStep.ATTACKER,
        getIndex(execs, MustFightBattle.FirstStrikeAttackersFire.class));
    indices.put(
        FirstStrikeBattleStep.DEFENDER,
        getIndex(execs, MustFightBattle.FirstStrikeDefendersFire.class));
    indices.put(
        FirstStrikeBattleStep.STANDARD,
        getIndex(execs, MustFightBattle.StandardAttackersFire.class));

    assertThat(indices.get(stepOrder.get(0)), lessThan(indices.get(stepOrder.get(1))));
    assertThat(indices.get(stepOrder.get(1)), lessThan(indices.get(stepOrder.get(2))));
  }

  private void assertThatFirstStrikeReturnFireIs(
      final Tuple<MustFightBattle, List<IExecutable>> battleTuple,
      final MustFightBattle.ReturnFire returnFire,
      final boolean attacker) {
    final MustFightBattle battle = battleTuple.getFirst();
    final List<IExecutable> execs = battleTuple.getSecond();
    final int index =
        getIndex(
            execs,
            attacker
                ? MustFightBattle.FirstStrikeAttackersFire.class
                : MustFightBattle.FirstStrikeDefendersFire.class);
    final IExecutable step = execs.get(index);

    final IDelegateBridge delegateBridge = mock(IDelegateBridge.class);
    step.execute(null, delegateBridge);

    if (attacker) {
      verify(battle).firstStrikeAttackersFire(returnFire);
    } else {
      verify(battle).firstStrikeDefendersFire(returnFire);
    }
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, true, true, true),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, true, true, true),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, false, true, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, true, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, true, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, true, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, true, true, true),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, true, true, true),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, false, true, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, true, false),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, true, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(true, false, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, false, false),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, true, true, true),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, true, true, true),
        MustFightBattle.ReturnFire.SUBS,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender>attacker>standard")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, false, true, false),
        List.of(
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, true, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire none")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, true, false),
        MustFightBattle.ReturnFire.NONE,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, true, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, true, true, true),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, true, true, true),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, true, true, true),
        MustFightBattle.ReturnFire.SUBS,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker>defender>standard")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, false, true, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.DEFENDER,
            FirstStrikeBattleStep.STANDARD));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, true, false),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, true, false),
        MustFightBattle.ReturnFire.SUBS,
        false);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker>standard>defender")
  void firstStrikeOrderAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeStepOrder(
        givenFirstStrikeBattleSetup(false, false, false, false, false),
        List.of(
            FirstStrikeBattleStep.ATTACKER,
            FirstStrikeBattleStep.STANDARD,
            FirstStrikeBattleStep.DEFENDER));
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, false, false),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }
}
