package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.io.IoUtils;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class ChangeTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.TEST.getGameData();
  }

  private Change serialize(final Change change) throws Exception {
    final byte[] bytes = IoUtils.writeToMemory(os -> {
      try (ObjectOutputStream output = new GameObjectOutputStream(os)) {
        output.writeObject(change);
      }
    });
    return IoUtils.readFromMemory(bytes, is -> {
      try (ObjectInputStream input = new GameObjectInputStream(new GameObjectStreamFactory(gameData), is)) {
        return (Change) input.readObject();
      } catch (final ClassNotFoundException e) {
        throw new IOException(e);
      }
    });
  }

  @Test
  public void testUnitsAddTerritory() {
    // make sure we know where we are starting
    final Territory can = gameData.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // add some units
    final Change change =
        ChangeFactory.addUnits(can, gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF).create(10, null));
    gameData.performChange(change);
    assertEquals(15, can.getUnits().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(5, can.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsRemoveTerritory() {
    // make sure we now where we are starting
    final Territory can = gameData.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        can.getUnits().getUnits(gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.removeUnits(can, units);
    gameData.performChange(change);

    assertEquals(2, can.getUnits().getUnitCount());
    gameData.performChange(change.invert());
    assertEquals(5, can.getUnits().getUnitCount(), "last change inverted, should have gained units.");
  }

  @Test
  public void testSerializeUnitsRemoteTerritory() throws Exception {
    // make sure we now where we are starting
    final Territory can = gameData.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        can.getUnits().getUnits(gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    Change change = ChangeFactory.removeUnits(can, units);
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(2, can.getUnits().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(5, can.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsAddPlayer() {
    // make sure we know where we are starting
    final PlayerID chretian = gameData.getPlayerList().getPlayerId("chretian");
    assertEquals(10, chretian.getUnits().getUnitCount());
    // add some units
    final Change change =
        ChangeFactory.addUnits(chretian,
            gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF).create(10, null));
    gameData.performChange(change);
    assertEquals(20, chretian.getUnits().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(10, chretian.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsRemovePlayer() {
    // make sure we know where we are starting
    final PlayerID chretian = gameData.getPlayerList().getPlayerId("chretian");
    assertEquals(10, chretian.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        chretian.getUnits().getUnits(gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.removeUnits(chretian, units);
    gameData.performChange(change);
    assertEquals(chretian.getUnits().getUnitCount(), 7);
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(chretian.getUnits().getUnitCount(), 10);
  }

  @Test
  public void testUnitsMove() {
    final Territory canada = gameData.getMap().getTerritory("canada");
    final Territory greenland = gameData.getMap().getTerritory("greenland");
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
    final Collection<Unit> units =
        canada.getUnits().getUnits(gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.moveUnits(canada, greenland, units);
    gameData.performChange(change);
    assertEquals(canada.getUnits().getUnitCount(), 2);
    assertEquals(greenland.getUnits().getUnitCount(), 3);
    gameData.performChange(change.invert());
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
  }

  @Test
  public void testUnitsMoveSerialization() throws Exception {
    final Territory canada = gameData.getMap().getTerritory("canada");
    final Territory greenland = gameData.getMap().getTerritory("greenland");
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
    final Collection<Unit> units =
        canada.getUnits().getUnits(gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    Change change = ChangeFactory.moveUnits(canada, greenland, units);
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(canada.getUnits().getUnitCount(), 2);
    assertEquals(greenland.getUnits().getUnitCount(), 3);
    gameData.performChange(change.invert());
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
  }

  @Test
  public void testProductionFrontierChange() {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final ProductionFrontier uspf = gameData.getProductionFrontierList().getProductionFrontier("usProd");
    final ProductionFrontier canpf = gameData.getProductionFrontierList().getProductionFrontier("canProd");
    assertEquals(can.getProductionFrontier(), canpf);
    final Change change = ChangeFactory.changeProductionFrontier(can, uspf);
    gameData.performChange(change);
    assertEquals(can.getProductionFrontier(), uspf);
    gameData.performChange(change.invert());
    assertEquals(can.getProductionFrontier(), canpf);
  }

  @Test
  public void testChangeResourcesChange() {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final Resource gold = gameData.getResourceList().getResource("gold");
    final Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
    assertEquals(can.getResources().getQuantity(gold), 100);
    gameData.performChange(change);
    assertEquals(can.getResources().getQuantity(gold), 150);
    gameData.performChange(change.invert());
    assertEquals(can.getResources().getQuantity(gold), 100);
  }

  @Test
  public void testSerializeResourceChange() throws Exception {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final Resource gold = gameData.getResourceList().getResource("gold");
    Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
    change = serialize(change);
    assertEquals(can.getResources().getQuantity(gold), 100);
    gameData.performChange(change);
    assertEquals(can.getResources().getQuantity(gold), 150);
  }

  @Test
  public void testChangeOwner() {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final PlayerID us = gameData.getPlayerList().getPlayerId("bush");
    final Territory greenland = gameData.getMap().getTerritory("greenland");
    final Change change = ChangeFactory.changeOwner(greenland, us);
    assertEquals(greenland.getOwner(), can);
    gameData.performChange(change);
    assertEquals(greenland.getOwner(), us);
    gameData.performChange(change.invert());
    assertEquals(greenland.getOwner(), can);
  }

  @Test
  public void testChangeOwnerSerialize() throws Exception {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final PlayerID us = gameData.getPlayerList().getPlayerId("bush");
    final Territory greenland = gameData.getMap().getTerritory("greenland");
    Change change = ChangeFactory.changeOwner(greenland, us);
    change = serialize(change);
    assertEquals(greenland.getOwner(), can);
    gameData.performChange(change);
    assertEquals(greenland.getOwner(), us);
    change = change.invert();
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(greenland.getOwner(), can);
  }

  @Test
  public void testPlayerOwnerChange() {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final PlayerID us = gameData.getPlayerList().getPlayerId("bush");
    final UnitType infantry = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    final Unit inf1 = infantry.create(1, can).iterator().next();
    final Unit inf2 = infantry.create(1, us).iterator().next();
    final Collection<Unit> units = new ArrayList<>();
    units.add(inf1);
    units.add(inf2);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
    Change change = ChangeFactory.changeOwner(units, can, gameData.getMap().getTerritory("greenland"));
    gameData.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(can, inf2.getOwner());
    change = change.invert();
    gameData.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
  }

  @Test
  public void testPlayerOwnerChangeSerialize() throws Exception {
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    final PlayerID us = gameData.getPlayerList().getPlayerId("bush");
    final UnitType infantry = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    final Unit inf1 = infantry.create(1, can).iterator().next();
    final Unit inf2 = infantry.create(1, us).iterator().next();
    final Collection<Unit> units = new ArrayList<>();
    units.add(inf1);
    units.add(inf2);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
    Change change = ChangeFactory.changeOwner(units, can, gameData.getMap().getTerritory("greenland"));
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(can, inf2.getOwner());
    change = change.invert();
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
  }

  @Test
  public void testChangeProductionFrontier() throws Exception {
    final ProductionFrontier usProd = gameData.getProductionFrontierList().getProductionFrontier("usProd");
    final ProductionFrontier canProd = gameData.getProductionFrontierList().getProductionFrontier("canProd");
    final PlayerID can = gameData.getPlayerList().getPlayerId("chretian");
    assertEquals(can.getProductionFrontier(), canProd);
    Change prodChange = ChangeFactory.changeProductionFrontier(can, usProd);
    gameData.performChange(prodChange);

    assertEquals(can.getProductionFrontier(), usProd);
    prodChange = prodChange.invert();
    gameData.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), canProd);
    prodChange = serialize(prodChange.invert());
    gameData.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), usProd);
    prodChange = serialize(prodChange.invert());
    gameData.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), canProd);
  }

  @Test
  public void testBlank() {
    final CompositeChange compositeChange = new CompositeChange();
    assertTrue(compositeChange.isEmpty());
    compositeChange.add(new CompositeChange());
    assertTrue(compositeChange.isEmpty());
    final Territory can = gameData.getMap().getTerritory("canada");
    final Collection<Unit> units = Collections.emptyList();
    compositeChange.add(ChangeFactory.removeUnits(can, units));
    assertFalse(compositeChange.isEmpty());
  }
}
