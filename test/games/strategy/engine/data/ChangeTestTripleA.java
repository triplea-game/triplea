package games.strategy.engine.data;

import games.strategy.triplea.xml.LoadGameUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ChangeTestTripleA extends TestCase
{
	private GameData m_data;
	
	public ChangeTestTripleA(final String name)
	{
		super(name);
	}
	
	public static Test suite()
	{
		final TestSuite suite = new TestSuite();
		suite.addTestSuite(ChangeTestTripleA.class);
		return suite;
	}
	
	@Override
	public void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("Big World : 1942", "big_world" + File.separator + "games" + File.separator + "big_world_1942.xml");
	}
	
	private Change serialize(final Change aChange) throws Exception
	{
		final ByteArrayOutputStream sink = new ByteArrayOutputStream();
		final ObjectOutputStream output = new GameObjectOutputStream(sink);
		output.writeObject(aChange);
		output.flush();
		// System.out.println("bytes:" + sink.toByteArray().length);
		final InputStream source = new ByteArrayInputStream(sink.toByteArray());
		final ObjectInputStream input = new GameObjectInputStream(new games.strategy.engine.framework.GameObjectStreamFactory(m_data), source);
		final Change newChange = (Change) input.readObject();
		return newChange;
	}
	
	public void testUnitsAddTerritory()
	{
		// make sure we know where we are starting
		final Territory can = m_data.getMap().getTerritory("Western Canada");
		assertEquals(can.getUnits().getUnitCount(), 2);
		// add some units
		final Change change = ChangeFactory.addUnits(can, m_data.getUnitTypeList().getUnitType("infantry").create(10, null));
		final ChangePerformer changePerformer = new ChangePerformer(m_data);
		changePerformer.perform(change);
		assertEquals(can.getUnits().getUnitCount(), 12);
		// invert the change
		changePerformer.perform(change.invert());
		assertEquals(can.getUnits().getUnitCount(), 2);
	}
	
	public void testUnitsRemoveTerritory()
	{
		// make sure we now where we are starting
		final Territory can = m_data.getMap().getTerritory("Western Canada");
		assertEquals(can.getUnits().getUnitCount(), 2);
		// remove some units
		final Collection<Unit> units = can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("infantry"), 1);
		final Change change = ChangeFactory.removeUnits(can, units);
		final ChangePerformer changePerformer = new ChangePerformer(m_data);
		changePerformer.perform(change);
		assertEquals(can.getUnits().getUnitCount(), 1);
		// invert the change
		changePerformer.perform(change.invert());
		assertEquals(can.getUnits().getUnitCount(), 2);
	}
	
	public void testSerializeUnitsRemoteTerritory() throws Exception
	{
		// make sure we now where we are starting
		final Territory can = m_data.getMap().getTerritory("Western Canada");
		assertEquals(can.getUnits().getUnitCount(), 2);
		// remove some units
		final Collection<Unit> units = can.getUnits().getUnits(m_data.getUnitTypeList().getUnitType("infantry"), 1);
		Change change = ChangeFactory.removeUnits(can, units);
		change = serialize(change);
		final ChangePerformer changePerformer = new ChangePerformer(m_data);
		changePerformer.perform(change);
		assertEquals(can.getUnits().getUnitCount(), 1);
		// invert the change
		changePerformer.perform(change.invert());
		assertEquals(can.getUnits().getUnitCount(), 2);
	}
}
