package games.strategy.engine.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import games.strategy.triplea.Constants;

public class ChangeTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    // get the xml file
    final URL url = this.getClass().getResource("Test.xml");
    // System.out.println(url);
    final InputStream input = url.openStream();
    m_data = (new GameParser()).parse(input, new AtomicReference<>(), false);
  }

  private Change serialize(final Change aChange) throws Exception {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    final ObjectOutputStream output = new GameObjectOutputStream(sink);
    output.writeObject(aChange);
    output.flush();
    // System.out.println("bytes:" + sink.toByteArray().length);
    final InputStream source = new ByteArrayInputStream(sink.toByteArray());
    final ObjectInputStream input =
        new GameObjectInputStream(new games.strategy.engine.framework.GameObjectStreamFactory(m_data), source);
    final Change newChange = (Change) input.readObject();
    input.close();
    output.close();
    return newChange;
  }

  @Test
  public void testUnitsAddTerritory() {
    // make sure we know where we are starting
    final Territory can = m_data.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // add some units
    final Change change =
        ChangeFactory.addUnits(can, m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF).create(10, null));
    m_data.performChange(change);
    assertEquals(15, can.getUnits().getUnitCount());
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(5, can.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsRemoveTerritory() {
    // make sure we now where we are starting
    final Territory can = m_data.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.removeUnits(can, units);
    m_data.performChange(change);

    assertEquals(2, can.getUnits().getUnitCount());
    m_data.performChange(change.invert());
    assertEquals("last change inverted, should have gained units.", 5,
        can.getUnits().getUnitCount());
  }

  @Test
  public void testSerializeUnitsRemoteTerritory() throws Exception {
    // make sure we now where we are starting
    final Territory can = m_data.getMap().getTerritory("canada");
    assertEquals(5, can.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    Change change = ChangeFactory.removeUnits(can, units);
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(2, can.getUnits().getUnitCount());
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(5, can.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsAddPlayer() {
    // make sure we know where we are starting
    final PlayerID chretian = m_data.getPlayerList().getPlayerID("chretian");
    assertEquals(10, chretian.getUnits().getUnitCount());
    // add some units
    final Change change =
        ChangeFactory.addUnits(chretian,
            m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF).create(10, null));
    m_data.performChange(change);
    assertEquals(20, chretian.getUnits().getUnitCount());
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(10, chretian.getUnits().getUnitCount());
  }

  @Test
  public void testUnitsRemovePlayer() {
    // make sure we know where we are starting
    final PlayerID chretian = m_data.getPlayerList().getPlayerID("chretian");
    assertEquals(10, chretian.getUnits().getUnitCount());
    // remove some units
    final Collection<Unit> units =
        chretian.getUnits().getUnits(m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.removeUnits(chretian, units);
    m_data.performChange(change);
    assertEquals(chretian.getUnits().getUnitCount(), 7);
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(chretian.getUnits().getUnitCount(), 10);
  }

  @Test
  public void testUnitsMove() {
    final Territory canada = m_data.getMap().getTerritory("canada");
    final Territory greenland = m_data.getMap().getTerritory("greenland");
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
    final Collection<Unit> units =
        canada.getUnits().getUnits(m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    final Change change = ChangeFactory.moveUnits(canada, greenland, units);
    m_data.performChange(change);
    assertEquals(canada.getUnits().getUnitCount(), 2);
    assertEquals(greenland.getUnits().getUnitCount(), 3);
    m_data.performChange(change.invert());
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
  }

  @Test
  public void testUnitsMoveSerialization() throws Exception {
    final Territory canada = m_data.getMap().getTerritory("canada");
    final Territory greenland = m_data.getMap().getTerritory("greenland");
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
    final Collection<Unit> units =
        canada.getUnits().getUnits(m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF), 3);
    Change change = ChangeFactory.moveUnits(canada, greenland, units);
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(canada.getUnits().getUnitCount(), 2);
    assertEquals(greenland.getUnits().getUnitCount(), 3);
    m_data.performChange(change.invert());
    assertEquals(canada.getUnits().getUnitCount(), 5);
    assertEquals(greenland.getUnits().getUnitCount(), 0);
  }

  @Test
  public void testProductionFrontierChange() {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final ProductionFrontier uspf = m_data.getProductionFrontierList().getProductionFrontier("usProd");
    final ProductionFrontier canpf = m_data.getProductionFrontierList().getProductionFrontier("canProd");
    assertEquals(can.getProductionFrontier(), canpf);
    final Change change = ChangeFactory.changeProductionFrontier(can, uspf);
    m_data.performChange(change);
    assertEquals(can.getProductionFrontier(), uspf);
    m_data.performChange(change.invert());
    assertEquals(can.getProductionFrontier(), canpf);
  }

  @Test
  public void testChangeResourcesChange() {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final Resource gold = m_data.getResourceList().getResource("gold");
    final Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
    assertEquals(can.getResources().getQuantity(gold), 100);
    m_data.performChange(change);
    assertEquals(can.getResources().getQuantity(gold), 150);
    m_data.performChange(change.invert());
    assertEquals(can.getResources().getQuantity(gold), 100);
  }

  @Test
  public void testSerializeResourceChange() throws Exception {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final Resource gold = m_data.getResourceList().getResource("gold");
    Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
    change = serialize(change);
    assertEquals(can.getResources().getQuantity(gold), 100);
    m_data.performChange(change);
    assertEquals(can.getResources().getQuantity(gold), 150);
  }

  @Test
  public void testChangeOwner() {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final PlayerID us = m_data.getPlayerList().getPlayerID("bush");
    final Territory greenland = m_data.getMap().getTerritory("greenland");
    final Change change = ChangeFactory.changeOwner(greenland, us);
    assertEquals(greenland.getOwner(), can);
    m_data.performChange(change);
    assertEquals(greenland.getOwner(), us);
    m_data.performChange(change.invert());
    assertEquals(greenland.getOwner(), can);
  }

  @Test
  public void testChangeOwnerSerialize() throws Exception {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final PlayerID us = m_data.getPlayerList().getPlayerID("bush");
    final Territory greenland = m_data.getMap().getTerritory("greenland");
    Change change = ChangeFactory.changeOwner(greenland, us);
    change = serialize(change);
    assertEquals(greenland.getOwner(), can);
    m_data.performChange(change);
    assertEquals(greenland.getOwner(), us);
    change = change.invert();
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(greenland.getOwner(), can);
  }

  @Test
  public void testPlayerOwnerChange() throws Exception {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final PlayerID us = m_data.getPlayerList().getPlayerID("bush");
    final UnitType infantry = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    final Unit inf1 = infantry.create(1, can).iterator().next();
    final Unit inf2 = infantry.create(1, us).iterator().next();
    final Collection<Unit> units = new ArrayList<>();
    units.add(inf1);
    units.add(inf2);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
    Change change = ChangeFactory.changeOwner(units, can, m_data.getMap().getTerritory("greenland"));
    m_data.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(can, inf2.getOwner());
    change = change.invert();
    m_data.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
  }

  @Test
  public void testPlayerOwnerChangeSerialize() throws Exception {
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    final PlayerID us = m_data.getPlayerList().getPlayerID("bush");
    final UnitType infantry = m_data.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    final Unit inf1 = infantry.create(1, can).iterator().next();
    final Unit inf2 = infantry.create(1, us).iterator().next();
    final Collection<Unit> units = new ArrayList<>();
    units.add(inf1);
    units.add(inf2);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
    Change change = ChangeFactory.changeOwner(units, can, m_data.getMap().getTerritory("greenland"));
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(can, inf2.getOwner());
    change = change.invert();
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(can, inf1.getOwner());
    assertEquals(us, inf2.getOwner());
  }

  @Test
  public void testChangeProductionFrontier() throws Exception {
    final ProductionFrontier usProd = m_data.getProductionFrontierList().getProductionFrontier("usProd");
    final ProductionFrontier canProd = m_data.getProductionFrontierList().getProductionFrontier("canProd");
    final PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
    assertEquals(can.getProductionFrontier(), canProd);
    Change prodChange = ChangeFactory.changeProductionFrontier(can, usProd);
    m_data.performChange(prodChange);

    assertEquals(can.getProductionFrontier(), usProd);
    prodChange = prodChange.invert();
    m_data.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), canProd);
    prodChange = serialize(prodChange.invert());
    m_data.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), usProd);
    prodChange = serialize(prodChange.invert());
    m_data.performChange(prodChange);
    assertEquals(can.getProductionFrontier(), canProd);
  }

  @Test
  public void testBlank() {
    final CompositeChange compositeChange = new CompositeChange();
    assertTrue(compositeChange.isEmpty());
    compositeChange.add(new CompositeChange());
    assertTrue(compositeChange.isEmpty());
    final Territory can = m_data.getMap().getTerritory("canada");
    final Collection<Unit> units = Collections.emptyList();
    compositeChange.add(ChangeFactory.removeUnits(can, units));
    assertFalse(compositeChange.isEmpty());
  }
}
