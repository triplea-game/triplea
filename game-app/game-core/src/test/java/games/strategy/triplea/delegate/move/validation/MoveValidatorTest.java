package games.strategy.triplea.delegate.move.validation;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MoveValidatorTest extends AbstractDelegateTestCase {

  @Test
  void testHasUnitsThatCantGoOnWater() {
    final Collection<Unit> units = new ArrayList<>();
    units.addAll(infantry.create(1, british));
    units.addAll(armour.create(1, british));
    units.addAll(transport.create(1, british));
    units.addAll(fighter.create(1, british));
    assertFalse(MoveValidator.hasUnitsThatCantGoOnWater(units));
    assertTrue(MoveValidator.hasUnitsThatCantGoOnWater(factory.create(1, british)));
  }

  @Test
  void testCarrierCapacity() {
    final Collection<Unit> units = carrier.create(5, british);
    Assertions.assertEquals(
        10,
        AirMovementValidator.carrierCapacity(
            units, new Territory("TestTerritory", true, gameData)));
  }

  @Test
  void testCarrierCost() {
    final Collection<Unit> units = fighter.create(5, british);
    assertEquals(5, AirMovementValidator.carrierCost(units));
  }

  @Test
  void testGetLeastMovement() {
    final Collection<Unit> collection = bomber.create(1, british);
    assertEquals(new BigDecimal(6), MoveValidator.getLeastMovement(collection));
    collection.iterator().next().setAlreadyMoved(BigDecimal.ONE);
    assertEquals(new BigDecimal(5), MoveValidator.getLeastMovement(collection));
    collection.addAll(factory.create(2, british));
    assertEquals(BigDecimal.ZERO, MoveValidator.getLeastMovement(collection));
  }

  @Test
  void testCanLand() {
    final Collection<Unit> units = fighter.create(4, british);
    // 2 carriers in red sea
    assertTrue(AirMovementValidator.canLand(units, redSea, british, gameData));
    // britain owns egypt
    assertTrue(AirMovementValidator.canLand(units, egypt, british, gameData));
    // only 2 carriers
    final Collection<Unit> tooMany = fighter.create(6, british);
    assertFalse(AirMovementValidator.canLand(tooMany, redSea, british, gameData));
    // nowhere to land
    assertFalse(AirMovementValidator.canLand(units, japanSeaZone, british, gameData));
    // neutral
    assertFalse(AirMovementValidator.canLand(units, westAfrica, british, gameData));
  }

  @Test
  void testCanLandInfantry() {
    try {
      final Collection<Unit> units = infantry.create(1, british);
      AirMovementValidator.canLand(units, redSea, british, gameData);
    } catch (final IllegalArgumentException e) {
      return;
    }
    fail("No exception thrown");
  }

  @Test
  void testCanLandBomber() {
    final Collection<Unit> units = bomber.create(1, british);
    assertFalse(AirMovementValidator.canLand(units, redSea, british, gameData));
  }

  @Test
  void testHasSomeLand() {
    final Collection<Unit> units = transport.create(3, british);
    assertTrue(units.stream().noneMatch(Matches.unitIsLand()));
    units.addAll(infantry.create(2, british));
    assertTrue(units.stream().anyMatch(Matches.unitIsLand()));
  }

  @Test
  void testValidateMoveForRequiresUnitsToMove() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Move regular units
    final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
    final Territory berlin = territory("Berlin", twwGameData);
    final Territory easternGermany = territory("Eastern Germany", twwGameData);
    final Route r = new Route(berlin, easternGermany);
    List<Unit> toMove = berlin.getUnitCollection().getMatches(Matches.unitCanMove());

    MoveValidator moveValidator = new MoveValidator(twwGameData, false);
    MoveValidationResult results =
        moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertTrue(results.isMoveValid());

    // Add germanTrain to units which fails since it requires germainRail
    addTo(berlin, GameDataTestUtil.germanTrain(twwGameData).create(1, germans));
    toMove = berlin.getUnitCollection().getMatches(Matches.unitCanMove());
    results = moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertFalse(results.isMoveValid());

    // Add germanRail to only destination so it fails
    final Collection<Unit> germanRail = GameDataTestUtil.germanRail(twwGameData).create(1, germans);
    addTo(easternGermany, germanRail);
    results = moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertFalse(results.isMoveValid());

    // Add germanRail to start so move succeeds
    addTo(berlin, GameDataTestUtil.germanRail(twwGameData).create(1, germans));
    results = moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertTrue(results.isMoveValid());

    // Remove germanRail from destination so move fails
    GameDataTestUtil.removeFrom(easternGermany, germanRail);
    results = moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertFalse(results.isMoveValid());

    // Add allied owned germanRail to destination so move succeeds
    final GamePlayer japan = GameDataTestUtil.japan(twwGameData);
    addTo(easternGermany, GameDataTestUtil.germanRail(twwGameData).create(1, japan));
    results = moveValidator.validateMove(new MoveDescription(toMove, r), germans);
    assertTrue(results.isMoveValid());
  }

  @Test
  void testValidateMoveForLandTransports() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Move truck 2 territories
    final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
    final Territory berlin = territory("Berlin", twwGameData);
    final Territory easternGermany = territory("Eastern Germany", twwGameData);
    final Territory poland = territory("Poland", twwGameData);
    final Route r = new Route(berlin, easternGermany, poland);
    berlin.getUnitCollection().clear();
    GameDataTestUtil.truck(twwGameData).create(1, germans);
    addTo(berlin, GameDataTestUtil.truck(twwGameData).create(1, germans));

    MoveValidator moveValidator = new MoveValidator(twwGameData, true);
    MoveValidationResult results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertTrue(results.isMoveValid());

    // Add an infantry for truck to transport
    addTo(berlin, GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertTrue(results.isMoveValid());

    // Add an infantry and the truck can't transport both
    addTo(berlin, GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertFalse(results.isMoveValid());

    // Add a large truck (has capacity for 2 infantry) to transport second infantry
    addTo(berlin, GameDataTestUtil.largeTruck(twwGameData).create(1, germans));
    results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertTrue(results.isMoveValid());

    // Add an infantry that the large truck can also transport
    addTo(berlin, GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertTrue(results.isMoveValid());

    // Add an infantry that can't be transported
    addTo(berlin, GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    results =
        moveValidator.validateMove(new MoveDescription(berlin.getUnitCollection(), r), germans);
    assertFalse(results.isMoveValid());
  }

  @Test
  void testValidateUnitsCanLoadInHostileSeaZones() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Load german unit in sea zone with no enemy ships
    final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
    final Territory northernGermany = territory("Northern Germany", twwGameData);
    final Territory sz27 = territory("27 Sea Zone", twwGameData);
    final Route r = new Route(northernGermany, sz27);
    northernGermany.getUnitCollection().clear();
    addTo(northernGermany, GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    final List<Unit> transport = sz27.getUnitCollection().getMatches(Matches.unitIsSeaTransport());
    Map<Unit, Unit> unitsToTransports =
        TransportUtils.mapTransports(r, northernGermany.getUnitCollection(), transport);

    MoveValidator moveValidator = new MoveValidator(twwGameData, false);
    MoveValidationResult results =
        moveValidator.validateMove(
            new MoveDescription(northernGermany.getUnitCollection(), r, unitsToTransports),
            germans);
    assertTrue(results.isMoveValid());

    // Add USA ship to transport sea zone
    final GamePlayer usa = GameDataTestUtil.usa(twwGameData);
    addTo(sz27, GameDataTestUtil.americanCruiser(twwGameData).create(1, usa));
    unitsToTransports =
        TransportUtils.mapTransports(r, northernGermany.getUnitCollection(), transport);
    results =
        moveValidator.validateMove(
            new MoveDescription(northernGermany.getUnitCollection(), r, unitsToTransports),
            germans);
    assertFalse(results.isMoveValid());

    // Set 'Units Can Load In Hostile Sea Zones' to true
    twwGameData.getProperties().set(Constants.UNITS_CAN_LOAD_IN_HOSTILE_SEA_ZONES, true);
    unitsToTransports =
        TransportUtils.mapTransports(r, northernGermany.getUnitCollection(), transport);
    results =
        moveValidator.validateMove(
            new MoveDescription(northernGermany.getUnitCollection(), r, unitsToTransports),
            germans);
    assertTrue(results.isMoveValid());
  }

  @Test
  void testStackingLimitOnMove() {
    MoveValidator moveValidator = new MoveValidator(gameData, true);
    Route r = new Route(westCanada, eastCanada);

    // 7 infantry can move, no problem.
    Collection<Unit> units = infantry.create(7, british);
    westCanada.getUnitCollection().addAll(units);
    assertThat(units, everyItem(in(westCanada.getUnitCollection())));
    var result = moveValidator.validateMove(new MoveDescription(units, r), british);
    assertTrue(result.isMoveValid());

    // But 8 infantry is not allowed due to a movementLimit on the PlayerAttachment.
    Collection<Unit> unit = infantry.create(1, british);
    units.addAll(unit);
    westCanada.getUnitCollection().addAll(unit);
    result = moveValidator.validateMove(new MoveDescription(units, r), british);
    assertFalse(result.isMoveValid());
  }
}
