package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MoveBatcherTest {
  private final GameState gameData = TestMapGameData.REVISED.getGameData();
  private final Territory brazil = territory("Brazil", gameData);
  private final Territory sz19 = territory("19 Sea Zone", gameData);
  private final Territory sz18 = territory("18 Sea Zone", gameData);
  private final Territory sz12 = territory("12 Sea Zone", gameData);
  private final Territory algeria = territory("Algeria", gameData);
  private final Route brazilToSz18 = new Route(brazil, sz18);
  private final Route sz19ToSz18 = new Route(sz19, sz18);
  private final Route sz18ToSz12 = new Route(sz18, sz12);
  private final Route sz12ToAlgeria = new Route(sz12, algeria);
  private final Unit inf1 = infantry(gameData).create(americans(gameData));
  private final Unit inf2 = infantry(gameData).create(americans(gameData));
  private final Unit tank1 = armour(gameData).create(americans(gameData));
  private final Unit tank2 = armour(gameData).create(americans(gameData));
  private final Unit transport1 = transport(gameData).create(americans(gameData));
  private final Unit transport2 = transport(gameData).create(americans(gameData));

  private static List<Unit> unitList(final Unit... units) {
    return Arrays.asList(units);
  }

  @Test
  @DisplayName("batching units moved with two transports")
  public void testMoveUnitsWithMultipleTransports() {
    final MoveBatcher batcher = new MoveBatcher();

    // Load two units onto transport 1 then move and unload them.
    batcher.newSequence();
    batcher.addTransportLoad(inf1, brazilToSz18, transport1);
    batcher.addTransportLoad(tank1, brazilToSz18, transport1);
    batcher.addMove(unitList(transport1, tank1, inf1), sz18ToSz12);
    batcher.addMove(unitList(tank1, inf1), sz12ToAlgeria);

    // Load two units onto transport 2 then move and unload them, in
    // the same territories as the previous sequence.
    batcher.newSequence();
    batcher.addTransportLoad(tank2, brazilToSz18, transport2);
    batcher.addTransportLoad(inf2, brazilToSz18, transport2);
    batcher.addMove(unitList(transport2, inf2, tank2), sz18ToSz12);
    batcher.addMove(unitList(inf2, tank2), sz12ToAlgeria);

    final List<MoveDescription> moves = batcher.batchMoves();

    // After batching, there should be 3 moves:
    //   Move to load all the land units onto transports.
    //   Move transporting all the units.
    //   Move unloading the land units from the transports.
    assertThat(moves, hasSize(3));

    // Check move to load all the land units onto transports.
    var expected =
        new MoveDescription(
            List.of(inf1, tank1, tank2, inf2),
            brazilToSz18,
            Map.of(inf1, transport1, tank1, transport1, inf2, transport2, tank2, transport2));
    assertThat(moves.get(0), is(expected));

    // Check move transporting all the units.
    expected =
        new MoveDescription(List.of(transport1, tank1, inf1, transport2, inf2, tank2), sz18ToSz12);
    assertThat(moves.get(1), is(expected));

    // Check move unloading the land units from the transports.
    expected = new MoveDescription(List.of(tank1, inf1, inf2, tank2), sz12ToAlgeria);
    assertThat(moves.get(2), is(expected));
  }

  @Test
  @DisplayName("batching transports that pick up units and move")
  public void testTransportsPickingUpUnitsOnTheWay() {
    final MoveBatcher batcher = new MoveBatcher();

    // Move transport1, load a tank, move transport + tank, unload tank.
    batcher.newSequence();
    batcher.addMove(unitList(transport1), sz19ToSz18);
    batcher.addTransportLoad(tank1, brazilToSz18, transport1);
    batcher.addMove(unitList(transport1, tank1), sz18ToSz12);
    batcher.addMove(unitList(tank1), sz12ToAlgeria);

    // Move transport2, load two infantry, move transport + 2 infantry, unload infantry.
    batcher.newSequence();
    batcher.addMove(unitList(transport2), sz19ToSz18);
    batcher.addTransportLoad(inf1, brazilToSz18, transport2);
    batcher.addTransportLoad(inf2, brazilToSz18, transport2);
    batcher.addMove(unitList(transport2, inf1, inf2), sz18ToSz12);
    batcher.addMove(unitList(inf1, inf2), sz12ToAlgeria);

    final List<MoveDescription> moves = batcher.batchMoves();

    // After batching, there should be 4 moves:
    //   Move transports 1 and 2 into position.
    //   Load tank and two infantry onto the transports.
    //   Move transporting all the units together.
    //   Move to unload the loaded units from the transports.
    assertThat(moves, hasSize(4));

    // Check move of transports 1 and 2 into position.
    var expected = new MoveDescription(List.of(transport1, transport2), sz19ToSz18);
    assertThat(moves.get(0), is(expected));

    // Check load of tank and two infantry onto the transports.
    expected =
        new MoveDescription(
            List.of(tank1, inf1, inf2),
            brazilToSz18,
            Map.of(tank1, transport1, inf1, transport2, inf2, transport2));
    assertThat(moves.get(1), is(expected));

    // Check move transporting all the units together.
    expected = new MoveDescription(List.of(transport1, tank1, transport2, inf1, inf2), sz18ToSz12);
    assertThat(moves.get(2), is(expected));

    // Check move to unload the loaded units from the transports.
    expected = new MoveDescription(List.of(tank1, inf1, inf2), sz12ToAlgeria);
    assertThat(moves.get(3), is(expected));
  }
}
