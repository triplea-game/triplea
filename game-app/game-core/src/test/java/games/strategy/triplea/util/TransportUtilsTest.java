package games.strategy.triplea.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TransportUtilsTest {
  private final GameData gameData = createGameData();

  private final Territory sz5 = territory("5 Sea Zone", gameData);
  private final Territory sz6 = territory("6 Sea Zone", gameData);
  private final Territory norway = territory("Norway", gameData);
  private final Territory karelia = territory("Karelia S.S.R.", gameData);
  private final Route toNorway = new Route(sz5, norway);
  private final Route toSz6 = new Route(sz5, sz6);

  private final Unit transport1 = transport(gameData).create(germans(gameData));
  private final Unit transport2 = transport(gameData).create(germans(gameData));
  private final Unit transport3 = transport(gameData).create(germans(gameData));
  private final Unit transport4 = transport(gameData).create(germans(gameData));
  private final Unit infantry1 = infantry(gameData).create(germans(gameData));
  private final Unit infantry2 = infantry(gameData).create(germans(gameData));
  private final Unit infantry3 = infantry(gameData).create(germans(gameData));
  private final Unit infantry4 = infantry(gameData).create(germans(gameData));
  private final Unit tank1 = armour(gameData).create(germans(gameData));
  private final Unit tank2 = armour(gameData).create(germans(gameData));
  private final Unit tank3 = armour(gameData).create(germans(gameData));

  private static GameData createGameData() {
    GameData data = TestMapGameData.REVISED.getGameData();
    data.getSequence().setRoundAndStep(1, GameStep.PropertyKeys.NON_COMBAT_MOVE, germans(data));
    return data;
  }

  private static void addTransportedUnits(Territory t, Unit transport, List<Unit> units) {
    addTo(t, List.of(transport));
    addTo(t, units);
    units.forEach(u -> u.setTransportedBy(transport));
  }

  private static void setUnloadedTo(Unit transport, Territory t)
      throws MutableProperty.InvalidValueException {
    // Create a dummy unit that was "unloaded".
    final Unit dummyUnit = infantry(transport.getData()).create(transport.getOwner());
    transport.setUnloaded(List.of(dummyUnit));
    dummyUnit.getProperty(Unit.PropertyName.UNLOADED_TO).orElseThrow().setValue(t);
    assertThat(dummyUnit.getUnloadedTo(), equalTo(t));
  }

  @Nested
  class ChooseEquivalentUnitsToUnload {
    @Test
    void testNoSubstitutions() {
      addTransportedUnits(sz5, transport1, List.of(infantry1, infantry2));
      addTransportedUnits(sz5, transport2, List.of(infantry3, tank1));

      final var units = List.of(infantry1, infantry2, infantry3, tank1);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toNorway, units);
      assertThat(result, containsInAnyOrder(units.toArray()));
    }

    @Test
    void testSubstituteOneUnit() throws MutableProperty.InvalidValueException {
      addTransportedUnits(sz5, transport1, List.of(infantry1, infantry2));
      addTransportedUnits(sz5, transport2, List.of(infantry3));
      setUnloadedTo(transport2, karelia);

      final var units = List.of(infantry2, infantry3);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toNorway, units);
      assertThat(result, containsInAnyOrder(infantry1, infantry2));
    }

    @Test
    void testSubstituteTwoForTwo() throws MutableProperty.InvalidValueException {
      addTransportedUnits(sz5, transport1, List.of(infantry1, infantry2));
      addTransportedUnits(sz5, transport2, List.of(infantry3));
      addTransportedUnits(sz5, transport3, List.of(infantry4));
      setUnloadedTo(transport2, karelia);
      setUnloadedTo(transport3, karelia);

      final var units = List.of(infantry3, infantry4);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toNorway, units);
      assertThat(result, containsInAnyOrder(infantry1, infantry2));
    }

    @Test
    void testGroupOfDifferentUnits() throws MutableProperty.InvalidValueException {
      addTransportedUnits(sz5, transport1, List.of(infantry1, tank1));
      addTransportedUnits(sz5, transport2, List.of(infantry2, tank2));
      addTransportedUnits(sz5, transport3, List.of(infantry3));
      setUnloadedTo(transport3, karelia);
      addTransportedUnits(sz5, transport4, List.of(tank3));
      setUnloadedTo(transport4, karelia);

      final var units = List.of(infantry1, tank1, infantry3, tank3);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toNorway, units);
      assertThat(result, containsInAnyOrder(infantry1, tank1, infantry2, tank2));
    }

    @Test
    void testNonUnload() {
      addTransportedUnits(sz5, transport1, List.of(infantry1, tank1));
      addTransportedUnits(sz5, transport2, List.of(infantry2, tank2));
      final var units = List.of(transport1, transport2, infantry1, infantry2, tank1, tank2);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toSz6, units);
      assertThat(result, containsInAnyOrder(units.toArray()));
    }

    @Test
    void testNoTransports() {
      final Unit fighter = fighter(gameData).create(germans(gameData));
      final Unit bomber = bomber(gameData).create(germans(gameData));
      final var units = List.of(fighter, bomber);
      addTo(sz5, units);
      final var result = TransportUtils.chooseEquivalentUnitsToUnload(toNorway, units);
      assertThat(result, containsInAnyOrder(units.toArray()));
    }
  }
}
