package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MoveBatcherTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory brazil = territory("Brazil", gameData);
  private final Territory sz19 = territory("19 Sea Zone", gameData);
  private final Territory sz18 = territory("18 Sea Zone", gameData);
  private final Territory sz12 = territory("12 Sea Zone", gameData);
  private final Territory algeria = territory("Algeria", gameData);
  private final Route brazilToSz18 = new Route(brazil, sz18);
  private final Route sz19ToSz18 = new Route(sz19, sz18);
  private final Route sz18ToSz12 = new Route(sz18, sz12);
  private final Route sz12ToAlgeria = new Route(sz12, algeria);
  private final Route algeriaToSz18 = new Route(sz12, algeria);
  private final Unit inf1 = infantry(gameData).create(americans(gameData));
  private final Unit inf2 = infantry(gameData).create(americans(gameData));
  private final Unit tank1 = armour(gameData).create(americans(gameData));
  private final Unit tank2 = armour(gameData).create(americans(gameData));
  private final Unit transport1 = transport(gameData).create(americans(gameData));
  private final Unit transport2 = transport(gameData).create(americans(gameData));

  public MoveBatcherTest() throws Exception {}

  private static ArrayList<Unit> unitList(final Unit... units) {
    return new ArrayList<Unit>(List.of(units));
  }

  private static HashSet<Unit> unitSet(final Unit... units) {
    return new HashSet<Unit>(List.of(units));
  }

  @Test
  public void testMoveUnitsWithMultipleTransports() {
    final MoveBatcher moves = new MoveBatcher();

    moves.newSequence();
    moves.addTransportLoad(inf1, brazilToSz18, transport1);
    moves.addTransportLoad(tank1, brazilToSz18, transport1);
    moves.addMove(unitList(transport1, tank1, inf1), sz18ToSz12);
    moves.addMove(unitList(tank1, inf1), sz12ToAlgeria);

    moves.newSequence();
    moves.addTransportLoad(tank2, brazilToSz18, transport2);
    moves.addTransportLoad(inf2, brazilToSz18, transport2);
    moves.addMove(unitList(transport2, inf2, tank2), sz18ToSz12);
    moves.addMove(unitList(inf2, tank2), sz12ToAlgeria);

    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Collection<Unit>> transportsToLoad = new ArrayList<>();
    moves.batchAndEmit(moveUnits, moveRoutes, transportsToLoad);

    assertEquals(3, moveUnits.size());
    assertEquals(3, moveRoutes.size());
    assertEquals(3, transportsToLoad.size());

    assertEquals(unitList(inf1, tank1, tank2, inf2), moveUnits.get(0));
    assertEquals(brazilToSz18, moveRoutes.get(0));
    assertEquals(unitSet(transport1, transport2), transportsToLoad.get(0));

    assertEquals(unitList(transport1, tank1, inf1, transport2, inf2, tank2), moveUnits.get(1));
    assertEquals(sz18ToSz12, moveRoutes.get(1));
    assertEquals(null, transportsToLoad.get(1));

    assertEquals(unitList(tank1, inf1, inf2, tank2), moveUnits.get(2));
    assertEquals(sz12ToAlgeria, moveRoutes.get(2));
    assertEquals(null, transportsToLoad.get(2));
  }

  @Test
  public void testTransportsPickingUpUnitsOnTheWay() {
    final MoveBatcher moves = new MoveBatcher();

    moves.newSequence();
    moves.addMove(unitList(transport1), sz19ToSz18);
    moves.addTransportLoad(tank1, brazilToSz18, transport1);
    moves.addMove(unitList(transport1, tank1), sz18ToSz12);
    moves.addMove(unitList(tank1), sz12ToAlgeria);
    moves.newSequence();
    moves.addMove(unitList(transport2), sz19ToSz18);
    moves.addTransportLoad(inf1, brazilToSz18, transport2);
    moves.addTransportLoad(inf2, brazilToSz18, transport2);
    moves.addMove(unitList(transport2, inf1, inf2), sz18ToSz12);
    moves.addMove(unitList(inf1, inf2), sz12ToAlgeria);

    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Collection<Unit>> transportsToLoad = new ArrayList<>();
    moves.batchAndEmit(moveUnits, moveRoutes, transportsToLoad);

    assertEquals(4, moveUnits.size());
    assertEquals(4, moveRoutes.size());
    assertEquals(4, transportsToLoad.size());

    assertEquals(unitList(transport1, transport2), moveUnits.get(0));
    assertEquals(sz19ToSz18, moveRoutes.get(0));
    assertEquals(null, transportsToLoad.get(0));

    assertEquals(unitList(tank1, inf1, inf2), moveUnits.get(1));
    assertEquals(brazilToSz18, moveRoutes.get(1));
    assertEquals(unitSet(transport1, transport2), transportsToLoad.get(1));

    assertEquals(unitList(transport1, tank1, transport2, inf1, inf2), moveUnits.get(2));
    assertEquals(sz18ToSz12, moveRoutes.get(2));
    assertEquals(null, transportsToLoad.get(2));

    assertEquals(unitList(tank1, inf1, inf2), moveUnits.get(3));
    assertEquals(sz12ToAlgeria, moveRoutes.get(3));
    assertEquals(null, transportsToLoad.get(3));
  }
}
