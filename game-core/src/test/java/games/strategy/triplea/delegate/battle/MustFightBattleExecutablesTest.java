package games.strategy.triplea.delegate.battle;

import static games.strategy.engine.data.Unit.ALREADY_MOVED;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE;
import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SEA_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.WW2V2;
import static games.strategy.triplea.delegate.GameDataTestUtil.getIndex;
import static games.strategy.triplea.delegate.battle.MustFightBattleExecutablesTest.BattleTerrain.WATER;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.UnitAndAttachment;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.newUnitAndAttachment;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IExecutable;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import junit.framework.AssertionFailedError;
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
  @DisplayName("Verify transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  void transportsAreRemovedIfTransportCasualtiesRestricted() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

    assertThatStepExists(execs, MustFightBattle.RemoveUndefendedTransports.class);
  }

  @Test
  @DisplayName("Verify transports are not removed if TRANSPORT_CASUALTIES_RESTRICTED is false")
  void transportsAreNotRemovedIfTransportCasualtiesUnRestricted() {
    final MustFightBattle battle = newBattle(WATER);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);

    battle.setUnits(List.of(), List.of(), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

    assertThatStepIsMissing(execs, MustFightBattle.RemoveUndefendedTransports.class);
  }

  @Test
  @DisplayName("Verify unescorted attacking transports are removed if casualities are restricted")
  void unescortedAttackingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    final MustFightBattle battle = spy(newBattle(WATER));
    doReturn(List.of()).when(battle).getAttackerRetreatTerritories();
    doNothing().when(battle).remove(any(), any(), any(), any());
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getUnit();
    when(unit.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment1 = unitAndAttachment.getUnitAttachment();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(attacker)).thenReturn(1);
    when(attachment2.getAttack(attacker)).thenReturn(1);
    when(attachment2.getIsSea()).thenReturn(true);
    when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit2).getPropertyOrThrow(ALREADY_MOVED);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    battle.setUnits(List.of(), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(attacker);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getUnit();
    when(unit.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment1 = unitAndAttachment.getUnitAttachment();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(defender);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(attacker)).thenReturn(0);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getUnit();
    when(unit.getOwner()).thenReturn(defender);
    final UnitAttachment attachment1 = unitAndAttachment.getUnitAttachment();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getTransportCapacity()).thenReturn(-1);
    when(attachment2.getMovement(defender)).thenReturn(1);
    when(attachment2.getAttack(defender)).thenReturn(1);
    when(attachment2.getIsSea()).thenReturn(true);
    when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO);
    final MutableProperty<Boolean> alreadyMovedProperty = MutableProperty.ofReadOnly(() -> true);
    doReturn(alreadyMovedProperty).when(unit2).getPropertyOrThrow(ALREADY_MOVED);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit), List.of(unit2), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(defender);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);

    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit = unitAndAttachment.getUnit();
    when(unit.getOwner()).thenReturn(defender);
    final UnitAttachment attachment1 = unitAndAttachment.getUnitAttachment();
    when(attachment1.getIsCombatTransport()).thenReturn(false);
    when(attachment1.getTransportCapacity()).thenReturn(2);
    when(attachment1.getIsSea()).thenReturn(true);

    final UnitAndAttachment unitAndAttachment2 = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment2.getUnit();
    when(unit2.getOwner()).thenReturn(attacker);
    final UnitAttachment attachment2 = unitAndAttachment2.getUnitAttachment();
    when(attachment2.getMovement(defender)).thenReturn(0);
    when(attachment2.getIsSea()).thenReturn(true);

    when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

    battle.setUnits(List.of(unit2), List.of(unit), List.of(), List.of(), defender, List.of());
    final List<IExecutable> execs = battle.getBattleExecutables();

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
    lenient().doNothing().when(battle).firstStrikeAttackersFire(any());
    lenient().doNothing().when(battle).firstStrikeDefendersFire(any());

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(ww2v2);
    lenient()
        .when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false))
        .thenReturn(defendingSubsSneakAttack);

    final Unit attackerUnit = attackerDestroyer ? givenUnitDestroyer() : givenAnyUnit();
    final Unit defenderUnit = defenderDestroyer ? givenUnitDestroyer() : givenAnyUnit();

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
      final MustFightBattle battle,
      final MustFightBattle.ReturnFire returnFire,
      final boolean attacker) {
    final List<IExecutable> execs = battle.getBattleExecutables();
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
        givenFirstStrikeBattleSetup(true, true, true, true), MustFightBattle.ReturnFire.ALL, true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, true, true), MustFightBattle.ReturnFire.ALL, false);
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
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, true), MustFightBattle.ReturnFire.ALL, true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, true),
        MustFightBattle.ReturnFire.ALL,
        false);
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
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, true, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
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
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, true, true),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, true, true),
        MustFightBattle.ReturnFire.ALL,
        false);
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
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, true),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, true),
        MustFightBattle.ReturnFire.ALL,
        false);
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
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, false),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has a destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttHasDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(true, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
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
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, true, true), MustFightBattle.ReturnFire.ALL, true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, true, true),
        MustFightBattle.ReturnFire.SUBS,
        false);
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
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, true),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire none")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, true),
        MustFightBattle.ReturnFire.NONE,
        false);
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
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire all")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, false),
        MustFightBattle.ReturnFire.ALL,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has a destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefHasDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, true, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
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
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, true, true),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is true, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is either, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2TrueSneakAttackTrueFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, true, true),
        MustFightBattle.ReturnFire.SUBS,
        false);
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
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then attacker has return fire subs")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, true),
        MustFightBattle.ReturnFire.SUBS,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is true, then defender has return fire subs")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackTrue() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, true),
        MustFightBattle.ReturnFire.SUBS,
        false);
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

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then attacker has return fire none")
  void firstStrikeAttackerReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, false),
        MustFightBattle.ReturnFire.NONE,
        true);
  }

  @Test
  @DisplayName(
      "When attacker has no destroyer, defender has no destroyer, WW2v2 is false, "
          + "and DEFENDING_SUBS_SNEAK_ATTACK is false, then defender has return fire all")
  void firstStrikeDefenderReturnFireAttNoDestroyerDefNoDestroyerWW2v2FalseSneakAttackFalse() {
    assertThatFirstStrikeReturnFireIs(
        givenFirstStrikeBattleSetup(false, false, false, false),
        MustFightBattle.ReturnFire.ALL,
        false);
  }
}
