package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaBattleSite;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitAirTransport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LandParatroopersTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void secondRoundDoesNothing() {
    final BattleState battleState = givenBattleStateBuilder().battleRound(2).build();
    final LandParatroopers landParatroopers = new LandParatroopers(battleState, battleActions);

    assertThat(landParatroopers.getAllStepDetails(), is(empty()));

    landParatroopers.execute(executionStack, delegateBridge);
    verify(delegateBridge, never()).addChange(any(Change.class));
  }

  @Test
  void waterBattleDoesNothing() {
    final BattleState battleState =
        givenBattleStateBuilder().battleRound(1).battleSite(givenSeaBattleSite()).build();
    final LandParatroopers landParatroopers = new LandParatroopers(battleState, battleActions);

    assertThat(landParatroopers.getAllStepDetails(), is(empty()));

    landParatroopers.execute(executionStack, delegateBridge);
    verify(delegateBridge, never()).addChange(any(Change.class));
  }

  @Test
  void withoutAirTransportTechDoesNothing() {
    final GamePlayer attacker = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(false);
    final BattleState battleState =
        givenBattleStateBuilder().battleRound(1).attacker(attacker).build();
    final LandParatroopers landParatroopers = new LandParatroopers(battleState, battleActions);

    assertThat(landParatroopers.getAllStepDetails(), is(empty()));

    landParatroopers.execute(executionStack, delegateBridge);
    verify(delegateBridge, never()).addChange(any(Change.class));
  }

  @Test
  void withoutDependentsDoesNothing() {
    final GamePlayer attacker = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);

    final Unit airTransport = givenUnitAirTransport();
    when(airTransport.getOwner()).thenReturn(attacker);
    final Collection<Unit> airTransports = List.of(airTransport);
    final Territory battleSite = mock(Territory.class);
    when(battleSite.getUnits()).thenReturn(airTransports);

    final BattleState battleState =
        spy(
            givenBattleStateBuilder()
                .battleRound(1)
                .battleSite(battleSite)
                .attacker(attacker)
                .build());

    when(battleState.getDependentUnits(airTransports)).thenReturn(List.of());

    final LandParatroopers landParatroopers = new LandParatroopers(battleState, battleActions);

    assertThat(landParatroopers.getAllStepDetails(), is(empty()));

    landParatroopers.execute(executionStack, delegateBridge);
    verify(delegateBridge, never()).addChange(any(Change.class));
  }

  @Test
  void landParatroopers() {
    final GamePlayer attacker = mock(GamePlayer.class);
    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);

    final Unit airTransport = givenUnitAirTransport();
    when(airTransport.getOwner()).thenReturn(attacker);
    final Collection<Unit> airTransports = List.of(airTransport);
    final Territory battleSite = mock(Territory.class);
    when(battleSite.getUnits()).thenReturn(airTransports);

    final BattleState battleState =
        spy(
            givenBattleStateBuilder()
                .battleRound(1)
                .battleSite(battleSite)
                .attacker(attacker)
                .build());

    final Collection<Unit> dependents = List.of(mock(Unit.class));
    when(battleState.getDependentUnits(airTransports)).thenReturn(dependents);

    final LandParatroopers landParatroopers = new LandParatroopers(battleState, battleActions);

    assertThat(landParatroopers.getAllStepDetails(), hasSize(1));

    landParatroopers.execute(executionStack, delegateBridge);
    verify(delegateBridge).addChange(any(Change.class));
  }
}
