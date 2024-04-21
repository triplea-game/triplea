package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.Constants.EDIT_MODE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitSeaTransport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelectMainBattleCasualtiesTest {

  @Mock IDelegateBridge delegateBridge;
  @Mock GamePlayer gamePlayer;
  BattleState battleState;
  Unit firingUnit;

  @BeforeEach
  public void givenBattleState() {
    battleState = givenBattleStateBuilder().build();
  }

  @BeforeEach
  public void givenFiringUnit() {
    firingUnit = givenAnyUnit();
  }

  private DiceRoll givenDiceRollWithHits(final int hits) {
    final DiceRoll diceRoll = mock(DiceRoll.class);
    when(diceRoll.getHits()).thenReturn(hits);
    return diceRoll;
  }

  @Nested
  class TransportCasualtiesNotRestricted {

    @Test
    @DisplayName(
        "The units have a total of 4 hit points and only take 3 damage so the user needs "
            + "to select some casualties")
    @SuppressWarnings("unchecked")
    void moreHitPointsThanHits() {
      final List<Unit> targetUnits = List.of(givenAnyUnit(), givenAnyUnit());

      targetUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(2);
          });

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(3));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);
      final CasualtyDetails expected = new CasualtyDetails();
      when(selectFunction.apply(any(), any(), anyCollection(), anyInt())).thenReturn(expected);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details, is(expected));

      verify(selectFunction)
          .apply(
              eq(delegateBridge),
              eq(selectCasualties),
              (Collection<Unit>) argThat(containsInAnyOrder(targetUnits.toArray())),
              eq(3));
    }

    @Test
    @DisplayName(
        "The units have a total of 4 hit points but have already sustained 2 hits. They "
            + "take 3 damage which kills them off.")
    void moreHitsThanHitPointsRemaining() {
      final List<Unit> targetUnits = List.of(givenAnyUnit(), givenAnyUnit());

      targetUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(2);
            when(unit.getHits()).thenReturn(1);
          });

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(3));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getKilled(), is(targetUnits));
      assertThat(details.getDamaged(), is(empty()));
      assertThat(details.getAutoCalculated(), is(true));

      verify(
              selectFunction,
              never()
                  .description(
                      "Enough hits were made to kill all of the units "
                          + "so the user doesn't need to select"))
          .apply(any(), any(), anyCollection(), anyInt());
    }
  }

  @Nested
  @DisplayName(
      "Restricted transport casualties means that transports are only allowed to be selected as "
          + "a casualty if all other units are killed.")
  class TransportCasualtiesRestricted {

    @Test
    @DisplayName(
        "Non transports have 4 hit points and there is only 3 damages so the player "
            + "needs to select from them, not the transport unit")
    @SuppressWarnings("unchecked")
    void moreNonTransportsHitPointsThanHits() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(2);
          });

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.add(givenUnitSeaTransport());

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(3));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);
      final CasualtyDetails expected = new CasualtyDetails();
      when(selectFunction.apply(any(), any(), anyCollection(), anyInt())).thenReturn(expected);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details, is(expected));

      verify(selectFunction)
          .apply(
              eq(delegateBridge),
              eq(selectCasualties),
              (Collection<Unit>) argThat(containsInAnyOrder(nonTransportUnits.toArray())),
              eq(3));
    }

    @Test
    @DisplayName(
        "Non transport units have 4 hit points and take 4 damage. The player doesn't "
            + "need to pick any casualties as they all die.  The transport units are left alone.")
    void equalNonTransportsHitPointsToHits() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(2);
          });

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.add(givenUnitSeaTransport());

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(4));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getKilled().toArray(), is(nonTransportUnits.toArray()));
      assertThat(details.getAutoCalculated(), is(true));
      verify(selectFunction, never()).apply(any(), any(), anyCollection(), anyInt());
    }

    @Test
    @DisplayName(
        "Non transports have 2 hit points but there is 4 damages. The player needs "
            + "to select some transport casualties.")
    @SuppressWarnings("unchecked")
    void lessNonTransportsHitPointsThanHits() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(1);
          });

      final List<Unit> transportUnits =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());
      transportUnits.forEach(unit -> when(unit.getOwner()).thenReturn(gamePlayer));

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.addAll(transportUnits);

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(4));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final List<Unit> expectedCasualtyList = new ArrayList<>(transportUnits.subList(0, 2));

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);
      final CasualtyDetails expected = new CasualtyDetails(expectedCasualtyList, List.of(), false);
      when(selectFunction.apply(any(), any(), anyCollection(), anyInt())).thenReturn(expected);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getAutoCalculated(), is(true));
      final List<Unit> killedUnits = new ArrayList<>(nonTransportUnits);
      killedUnits.addAll(expected.getKilled());
      assertThat(details.getKilled().toArray(), is(killedUnits.toArray()));

      verify(selectFunction)
          .apply(
              eq(delegateBridge),
              eq(selectCasualties),
              (Collection<Unit>) argThat(containsInAnyOrder(expectedCasualtyList.toArray())),
              eq(2));
    }

    @Test
    @DisplayName(
        "Non transports have 2 hit points but there is 4 damages. The player needs "
            + "to select some transport casualties. Some of the transports are allies so there "
            + "should be enough from all of the allies to cover the casualties.")
    @SuppressWarnings("unchecked")
    void lessNonTransportsHitPointsThanHitsWithAlliedTransports() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(1);
          });

      final List<Unit> transportUnits =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());
      transportUnits.forEach(unit -> when(unit.getOwner()).thenReturn(gamePlayer));

      final GamePlayer ally = mock(GamePlayer.class);

      final List<Unit> transportUnitsAlly =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());
      transportUnitsAlly.forEach(unit -> when(unit.getOwner()).thenReturn(ally));

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.addAll(transportUnits);
      targetUnits.addAll(transportUnitsAlly);

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(4));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final List<Unit> expectedCasualtyList = new ArrayList<>(transportUnits.subList(0, 2));
      expectedCasualtyList.addAll(transportUnitsAlly.subList(0, 2));

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);
      final CasualtyDetails expected = new CasualtyDetails(expectedCasualtyList, List.of(), false);
      when(selectFunction.apply(any(), any(), anyCollection(), anyInt())).thenReturn(expected);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getAutoCalculated(), is(true));
      final List<Unit> killedUnits = new ArrayList<>(nonTransportUnits);
      killedUnits.addAll(expected.getKilled());
      assertThat(details.getKilled().toArray(), is(killedUnits.toArray()));

      verify(selectFunction)
          .apply(
              eq(delegateBridge),
              eq(selectCasualties),
              (Collection<Unit>) argThat(containsInAnyOrder(expectedCasualtyList.toArray())),
              eq(2));
    }

    @Test
    @DisplayName(
        "Non transport units have 2 hit points and take 10 damage. There is only 5 "
            + "transports so everything dies and the player doesn't need to make a choice.")
    void moreHitsThanHitPoints() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(1);
          });

      final List<Unit> transportUnits =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.addAll(transportUnits);

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(10));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getAutoCalculated(), is(true));
      assertThat(details.getKilled().toArray(), is(targetUnits.toArray()));

      verify(selectFunction, never()).apply(any(), any(), anyCollection(), anyInt());
    }

    @Test
    @DisplayName(
        "Non transport units have 2 hit points and take 40 damage. There is 5 "
            + "transports from the player and an ally so everything dies and the player doesn't "
            + "need to make a choice.")
    void moreHitsThanHitPointsWithAlly() {
      when(battleState.getGameData().getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false))
          .thenReturn(true);
      when(battleState.getGameData().getProperties().get(EDIT_MODE)).thenReturn(false);

      final List<Unit> nonTransportUnits = List.of(givenAnyUnit(), givenAnyUnit());

      nonTransportUnits.forEach(
          unit -> {
            final UnitAttachment unitAttachment =
                (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
            when(unitAttachment.getHitPoints()).thenReturn(1);
          });

      final List<Unit> transportUnits =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());

      final List<Unit> transportUnitsAlly =
          List.of(
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport(),
              givenUnitSeaTransport());

      final List<Unit> targetUnits = new ArrayList<>(nonTransportUnits);
      targetUnits.addAll(transportUnits);
      targetUnits.addAll(transportUnitsAlly);

      final FiringGroup firingGroup =
          FiringGroup.groupBySuicideOnHit(UNITS, List.of(firingUnit), targetUnits).get(0);

      final FireRoundState fireRoundState = new FireRoundState();
      fireRoundState.setDice(givenDiceRollWithHits(40));

      final SelectCasualties selectCasualties =
          new SelectCasualties(
              battleState,
              BattleState.Side.OFFENSE,
              firingGroup,
              fireRoundState,
              (arg1, arg2) -> new CasualtyDetails());

      final SelectMainBattleCasualties.Select selectFunction =
          mock(SelectMainBattleCasualties.Select.class);

      final CasualtyDetails details =
          new SelectMainBattleCasualties(selectFunction).apply(delegateBridge, selectCasualties);

      assertThat(details.getAutoCalculated(), is(true));
      assertThat(details.getKilled().toArray(), is(targetUnits.toArray()));

      verify(selectFunction, never()).apply(any(), any(), anyCollection(), anyInt());
    }
  }
}
