/*
 * SerializationTest.java
 *
 * Created on January 3, 2002, 1:37 PM
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
 */
public class SerializationTest extends TestCase
{
	
	private GameData m_dataSource;
	private GameData m_dataSink;

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(SerializationTest.class);
		return suite;
	}
	
	public void setUp() throws Exception
	{
		//get the xml file
		URL url = this.getClass().getResource("Test.xml");
		
		//get the source  data
		InputStream input= url.openStream();
		m_dataSource = (new GameParser()).parse(input);
		
		//get the sink data
		input= url.openStream();
		m_dataSink = (new GameParser()).parse(input);
	}

	private Object serialize(Object anObject) throws Exception
	{
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		ObjectOutputStream output = new GameObjectOutputStream(m_dataSource, sink);
		output.writeObject(anObject);
		output.flush();
		InputStream source = new ByteArrayInputStream(sink.toByteArray());
		ObjectInputStream input = new GameObjectInputStream(m_dataSink, source);
		return input.readObject();
	}


	/** Creates a new instance of SerializationTest */
    public SerializationTest(String s) 
	{
		super(s);
    }

	public void testWritePlayerID() throws Exception
	{
		PlayerID id = m_dataSource.getPlayerList().getPlayerID("chretian");
		
		PlayerID readID = (PlayerID) serialize(id);
		PlayerID localID = m_dataSink.getPlayerList().getPlayerID("chretian");
		assertTrue(localID == readID);
	}
	
	public void testWriteUnitType() throws Exception
	{
		Object orig = m_dataSource.getUnitTypeList().getUnitType("inf");
		
		Object read = serialize(orig);
		Object local = m_dataSink.getUnitTypeList().getUnitType("inf");
		assertTrue(local == read);
	}
	
	public void testWriteTerritory() throws Exception
	{
		Object orig = m_dataSource.getMap().getTerritory("canada");
		
		Object read = serialize(orig);
		Object local = m_dataSink.getMap().getTerritory("canada");
		assertTrue(local == read);
	}
	
	public void testWriteProductionRulte() throws Exception
	{
		
		Object orig = m_dataSource.getProductionRuleList().getProductionRule("infForSilver");
		
		Object read = serialize(orig);
		Object local = m_dataSink.getProductionRuleList().getProductionRule("infForSilver");
		assertTrue(local == read);
	}
	
	public void testWriteUnit() throws Exception
	{
		UnitType type = m_dataSource.getUnitTypeList().getUnitType("inf");
		PlayerID chretian = m_dataSource.getPlayerList().getPlayerID("chretian");
		
		Unit orig = type.create(chretian);
		
		assertTrue(Unit.get(orig.getID()).equals(orig));
		
		
		Unit read = (Unit) serialize(orig);
		
		assertTrue(read == orig);
	}
}
