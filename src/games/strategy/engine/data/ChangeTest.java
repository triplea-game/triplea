/*
 * ChangeTest.java
 *
 * Created on October 26, 2001, 7:09 PM
 */

package games.strategy.engine.data;


import java.io.*;
import java.net.URL;
import java.util.*;

import junit.framework.*;

import games.strategy.engine.xml.*;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ChangeTest extends TestCase
{

	private GameData m_data;
	
	/** Creates new ChangeTest */
    public ChangeTest(String name) 
	{
		super(name);
    }
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(ChangeTest.class);
		return suite;
	}
	
	public void setUp() throws Exception
	{
		//get the xml file
		URL url = this.getClass().getResource("Test.xml");
		//System.out.println(url);
		InputStream input= url.openStream();
		m_data = (new GameParser()).parse(input);
	}
	
	private Change serialize(Change aChange) throws Exception
	{
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ObjectOutputStream output = new GameObjectOutputStream(m_data, sink);
		output.writeObject(aChange);
		output.flush();
		//System.out.println("bytes:" + sink.toByteArray().length);
		
		InputStream source = new ByteArrayInputStream(sink.toByteArray());
		ObjectInputStream input = new GameObjectInputStream(m_data, source);
		Change newChange = (Change) input.readObject();
		return newChange;
	}
	
	
	public void testUnitsAddTerritory()
	{
		//make sure we know where we are starting
		Territory can = m_data.getMap().getTerritory("canada");
		assertEquals(can.getUnits().getUnitCount(), 5);
		
		//add some units
		Change change = ChangeFactory.addUnits(can, m_data.getUnitTypeList().getUnitType("inf").create(10, null));
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 15);	
		
		//invert the change
		change = change.invert();
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 5);
	}
	
	public void testUnitsRemoveTerritory()
	{
		//make sure we now where we are starting
		Territory can = m_data.getMap().getTerritory("canada");
		assertEquals(can.getUnits().getUnitCount(), 5);
		
		//remove some units
		Collection units = can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("inf"), 3);
		Change change = ChangeFactory.removeUnits(can, units);
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 2);		
		
		//invert the change
		change = change.invert();
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 5);
	}
	
	public void testSerializeUnitsRemoteTerritory() throws Exception
	{
		//make sure we now where we are starting
		Territory can = m_data.getMap().getTerritory("canada");
		assertEquals(can.getUnits().getUnitCount(), 5);
		
		//remove some units
		Collection units = can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("inf"), 3);
		Change change = ChangeFactory.removeUnits(can, units);
		change = serialize(change);
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 2);		
		
		//invert the change
		change = change.invert();
		change.perform(m_data);
		assertEquals(can.getUnits().getUnitCount(), 5);
	}
	
	public void testUnitsAddPlayer()
	{
		//make sure we know where we are starting
		PlayerID chretian = m_data.getPlayerList().getPlayerID("chretian");
		assertEquals(chretian.getUnits().getUnitCount(), 10);
		
		//add some units
		Change change = ChangeFactory.addUnits(chretian, m_data.getUnitTypeList().getUnitType("inf").create(10, null));
		change.perform(m_data);
		assertEquals(chretian.getUnits().getUnitCount(), 20);	
		
		//invert the change
		change = change.invert();
		change.perform(m_data);
		assertEquals(chretian.getUnits().getUnitCount(), 10);
	}
	
	public void testUnitsRemovePlayer()
	{
		//make sure we know where we are starting
		PlayerID chretian = m_data.getPlayerList().getPlayerID("chretian");
		assertEquals(chretian.getUnits().getUnitCount(), 10);
		
		//remove some units
		Collection units = chretian.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("inf"), 3);
		Change change = ChangeFactory.removeUnits(chretian, units);
		change.perform(m_data);
		assertEquals(chretian.getUnits().getUnitCount(), 7);		
		
		//invert the change
		change = change.invert();
		change.perform(m_data);
		assertEquals(chretian.getUnits().getUnitCount(), 10);
	}
	

	public void testUnitsMove()
	{
		Territory canada = m_data.getMap().getTerritory("canada");
		Territory greenland = m_data.getMap().getTerritory("greenland");
		
		assertEquals(canada.getUnits().getUnitCount(), 5);
		assertEquals(greenland.getUnits().getUnitCount(), 0);
		
		Collection units = canada.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("inf"), 3);
		Change change = ChangeFactory.moveUnits(canada, greenland, units);
		change.perform(m_data);
		assertEquals(canada.getUnits().getUnitCount(), 2);
		assertEquals(greenland.getUnits().getUnitCount(), 3);
		
		change.invert().perform(m_data);
		
		assertEquals(canada.getUnits().getUnitCount(), 5);
		assertEquals(greenland.getUnits().getUnitCount(), 0);
	}
	
	

	public void testUnitsMoveSerialization() throws Exception
	{
		Territory canada = m_data.getMap().getTerritory("canada");
		Territory greenland = m_data.getMap().getTerritory("greenland");
		
		assertEquals(canada.getUnits().getUnitCount(), 5);
		assertEquals(greenland.getUnits().getUnitCount(), 0);
		
		Collection units = canada.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("inf"), 3);
		Change change = ChangeFactory.moveUnits(canada, greenland, units);
		change = serialize(change);
		change.perform(m_data);
		assertEquals(canada.getUnits().getUnitCount(), 2);
		assertEquals(greenland.getUnits().getUnitCount(), 3);
		
		serialize(change.invert()).perform(m_data);
		
		assertEquals(canada.getUnits().getUnitCount(), 5);
		assertEquals(greenland.getUnits().getUnitCount(), 0);
		
	}
	
	public void testProductionFrontierChange()
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		ProductionFrontier uspf = m_data.getProductionFrontierList().getProductionFrontier("usProd");
		ProductionFrontier canpf = m_data.getProductionFrontierList().getProductionFrontier("canProd");
		
		assertEquals(can.getProductionFrontier(), canpf);
		
		Change change = ChangeFactory.changeProductionFrontier(can, uspf);
		
		change.perform(m_data);
		assertEquals(can.getProductionFrontier(), uspf);
		
		change.invert().perform(m_data);
		assertEquals(can.getProductionFrontier(), canpf);
	}
	
	public void testChangeResourcesChange()
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		Resource gold = m_data.getResourceList().getResource("gold");
		
		Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
		
		assertEquals(can.getResources().getQuantity(gold), 100);
		change.perform(m_data);
		assertEquals(can.getResources().getQuantity(gold), 150);
		
		change = change.invert();
		change.perform(m_data);
		assertEquals(can.getResources().getQuantity(gold), 100);		
	}
	
	public void testSerializeResourceChange() throws Exception
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		Resource gold = m_data.getResourceList().getResource("gold");
		
		Change change = ChangeFactory.changeResourcesChange(can, gold, 50);
		change = serialize(change);
		
		assertEquals(can.getResources().getQuantity(gold), 100);
		change.perform(m_data);
		assertEquals(can.getResources().getQuantity(gold), 150);
	
	}
	
	public void testChangeOwner()
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		PlayerID us = m_data.getPlayerList().getPlayerID("bush");
		Territory greenland = m_data.getMap().getTerritory("greenland");
		
		Change change = ChangeFactory.changeOwner(greenland, us);
		
		assertEquals(greenland.getOwner(), can);
		change.perform(m_data);
		assertEquals(greenland.getOwner(), us);
		
		change.invert().perform(m_data);
		assertEquals(greenland.getOwner(), can);
	}
	
	public void testChangeOwnerSerialize() throws Exception
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		PlayerID us = m_data.getPlayerList().getPlayerID("bush");
		Territory greenland = m_data.getMap().getTerritory("greenland");
		
		Change change = ChangeFactory.changeOwner(greenland, us);
		change = serialize(change);
		
		assertEquals(greenland.getOwner(), can);
		change.perform(m_data);
		assertEquals(greenland.getOwner(), us);
		
		change = change.invert();
		change = serialize(change);
		change.perform(m_data);
		assertEquals(greenland.getOwner(), can);
	}
	
	public void testPlayerOwnerChange() throws Exception
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		PlayerID us = m_data.getPlayerList().getPlayerID("bush");
		UnitType infantry = m_data.getUnitTypeList().getUnitType("inf");
		Unit inf1 = (Unit) infantry.create(1, can).iterator().next();
		Unit inf2 = (Unit) infantry.create(1, us).iterator().next();
		
		Collection units = new ArrayList();
		units.add(inf1);
		units.add(inf2);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(us, inf2.getOwner());
		
		Change change = ChangeFactory.changeOwner(units, can);
		change.perform(m_data);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(can, inf2.getOwner());
		
		change = change.invert();
		change.perform(m_data);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(us, inf2.getOwner());
	
	}
	
	public void testPlayerOwnerChangeSerialize() throws Exception
	{
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		PlayerID us = m_data.getPlayerList().getPlayerID("bush");
		UnitType infantry = m_data.getUnitTypeList().getUnitType("inf");
		Unit inf1 = (Unit) infantry.create(1, can).iterator().next();
		Unit inf2 = (Unit) infantry.create(1, us).iterator().next();
		
		Collection units = new ArrayList();
		units.add(inf1);
		units.add(inf2);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(us, inf2.getOwner());
		
		Change change = ChangeFactory.changeOwner(units, can);
		change = serialize(change);
		change.perform(m_data);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(can, inf2.getOwner());
		
		change = change.invert();
		change = serialize(change);
		change.perform(m_data);
		
		assertEquals(can, inf1.getOwner());
		assertEquals(us, inf2.getOwner());

		
	}
	
	public void testChangeProductionFrontier() throws Exception
	{
		ProductionFrontier usProd = m_data.getProductionFrontierList().getProductionFrontier("usProd");
		ProductionFrontier canProd = m_data.getProductionFrontierList().getProductionFrontier("canProd");
		PlayerID can = m_data.getPlayerList().getPlayerID("chretian");
		
		assertEquals( can.getProductionFrontier(), canProd);
		
		Change prodChange = ChangeFactory.changeProductionFrontier(can, usProd);
		prodChange.perform(m_data);
		assertEquals( can.getProductionFrontier(), usProd);
		
		
		prodChange = prodChange.invert();
		prodChange.perform(m_data);
		assertEquals( can.getProductionFrontier(), canProd);
		
		
		prodChange = serialize(prodChange.invert());
		prodChange.perform(m_data);
		assertEquals( can.getProductionFrontier(), usProd);
		
		
		prodChange = serialize(prodChange.invert());
		prodChange.perform(m_data);
		assertEquals( can.getProductionFrontier(), canProd);
		
	}
}
