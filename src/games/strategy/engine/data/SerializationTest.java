/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * SerializationTest.java
 *
 * Created on January 3, 2002, 1:37 PM
 */

package games.strategy.engine.data;

import java.io.*;
import java.net.URL;

import junit.framework.*;

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
		ObjectOutputStream output = new GameObjectOutputStream(sink);
		output.writeObject(anObject);
		output.flush();
		InputStream source = new ByteArrayInputStream(sink.toByteArray());
		ObjectInputStream input = new GameObjectInputStream(new games.strategy.engine.framework.GameObjectStreamFactory(m_dataSource), source);
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
		assertTrue(localID != readID);
	}

	public void testWriteUnitType() throws Exception
	{
		Object orig = m_dataSource.getUnitTypeList().getUnitType("inf");

		Object read = serialize(orig);
		Object local = m_dataSink.getUnitTypeList().getUnitType("inf");
		assertTrue(local != read);
	}

	public void testWriteTerritory() throws Exception
	{
		Object orig = m_dataSource.getMap().getTerritory("canada");

		Object read = serialize(orig);
		Object local = m_dataSink.getMap().getTerritory("canada");
		assertTrue(local != read);
	}

	public void testWriteProductionRulte() throws Exception
	{

		Object orig = m_dataSource.getProductionRuleList().getProductionRule("infForSilver");

		Object read = serialize(orig);
		Object local = m_dataSink.getProductionRuleList().getProductionRule("infForSilver");
		assertTrue(local != read);
	}


}
