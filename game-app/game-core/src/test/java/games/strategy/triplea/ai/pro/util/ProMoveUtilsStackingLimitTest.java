package games.strategy.triplea.ai.pro.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProMoveUtilsStackingLimitTest extends AbstractDelegateTestCase {

  @Test
  void unitsOverTheMovementLimitAreLeftBehindInsteadOfRejectingTheWholeMove() {
    // The PlayerAttachment movementLimit allows 7 infantry into eastCanada (see
    // MoveValidatorTest.testStackingLimitOnMove).
    final List<Unit> eightInfantry = infantry.create(8, british);
    westCanada.getUnitCollection().addAll(eightInfantry);
    final var proData = new ProData();
    proData.initializeSimulation(null, gameData, british);
    final var moveDelegate = mock(IMoveDelegate.class);
    when(moveDelegate.performMove(any())).thenReturn(Optional.empty());

    ProMoveUtils.doMove(
        proData,
        List.of(new MoveDescription(eightInfantry, new Route(westCanada, eastCanada))),
        moveDelegate);

    final var moved = ArgumentCaptor.forClass(MoveDescription.class);
    verify(moveDelegate).performMove(moved.capture());
    assertThat(moved.getValue().getUnits()).hasSize(7);
  }

  @Test
  void movesCarryingAirTransportCargoAreNotTrimmed() {
    final List<Unit> eightInfantry = infantry.create(8, british);
    westCanada.getUnitCollection().addAll(eightInfantry);
    final var proData = new ProData();
    proData.initializeSimulation(null, gameData, british);
    final var moveDelegate = mock(IMoveDelegate.class);
    when(moveDelegate.performMove(any())).thenReturn(Optional.empty());
    final var move =
        new MoveDescription(
            eightInfantry,
            new Route(westCanada, eastCanada),
            Map.of(),
            Map.of(eightInfantry.get(0), List.of(eightInfantry.get(1))));

    ProMoveUtils.doMove(proData, List.of(move), moveDelegate);

    final var moved = ArgumentCaptor.forClass(MoveDescription.class);
    verify(moveDelegate).performMove(moved.capture());
    assertThat(moved.getValue()).isEqualTo(move);
  }
}
