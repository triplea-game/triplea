/*
 * MoveValidatorTest.java
 *
 * Created on November 8, 2001, 5:00 PM
 */

package games.strategy.triplea.delegate;

import junit.framework.*;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.attatchments.*;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MoveValidatorTest extends DelegateTest
{
		
	/** Creates new PlaceDelegateTest */
    public MoveValidatorTest(String name) 
	{
		super(name);
    }

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(MoveValidatorTest.class);
		
		return suite;
	}

	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	public void testHasEnoughMovement()
	{
		IntegerMap alreadyMoved = new IntegerMap();
		Collection units = bomber.create(3, british);
		Object[] objs = units.toArray();
		alreadyMoved.put(objs[0], 2);
		alreadyMoved.put(objs[1], 1);
		
		assertTrue(MoveValidator.hasEnoughMovement(units, alreadyMoved, 2));
	}
	
	public void testHasWate()
	{
		Route route = new Route();
		route.setStart(eastMediteranean);
		assertTrue(MoveValidator.hasWater(route));
		
		route = new Route();
		route.setStart(eastAfrica);
		assertTrue(!MoveValidator.hasWater(route));
		
		route.add(kenya);
		assertTrue(!MoveValidator.hasWater(route));
		
		route.add(eastMediteranean);
		assertTrue(MoveValidator.hasWater(route));
	}
	
	public void testNotEnoughMovement()
	{
		IntegerMap alreadyMoved = new IntegerMap();
		Collection units = bomber.create(3, british);
		Object[] objs = units.toArray();
		assertTrue(MoveValidator.hasEnoughMovement(units, alreadyMoved, 6));
		assertTrue(!MoveValidator.hasEnoughMovement(units, alreadyMoved, 7));
		
		alreadyMoved.put(objs[1], 1);
		assertTrue(!MoveValidator.hasEnoughMovement(units, alreadyMoved, 6));
		
		alreadyMoved.put(objs[0], 2);
		assertTrue(!MoveValidator.hasEnoughMovement(units, alreadyMoved, 5));
	}	
	
	public void testEnemyUnitsInPath()
	{
		//japanese unit in congo
		Route bad = new Route();
		//the empty case
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(bad, british, m_data));
		
		bad.add(egypt);
		bad.add(congo);
		bad.add(kenya);
		
		assertTrue(!MoveValidator.onlyAlliedUnitsOnPath(bad, british, m_data));
		
		Route good = new Route();
		good.add(egypt);
		good.add(kenya);
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(good, british, m_data));

		//at end so should still be good
		good.add(congo);
		assertTrue(MoveValidator.onlyAlliedUnitsOnPath(good, british, m_data));	
	}
	
	public void testIsBlitz()
	{
		Route route = new Route();
		assertTrue(!MoveValidator.isBlitz(route, british, m_data));
		
		route.add(egypt);
		assertTrue(!MoveValidator.isBlitz(route, british, m_data));
		
		//doesnt count last territory
		route.add(libya);
		assertTrue(!MoveValidator.isBlitz(route, british, m_data));
		
		//now wed be blitzing through libya
		route.add(algeria);
		assertTrue(MoveValidator.isBlitz(route, british, m_data));
	}
	
	public void testIsBlitzNuetral()
	{
		Route route = new Route();
		route.add(egypt);
		//nuetral
		route.add(equatorialAfrica);
		route.add(libya);
		
		assertTrue(!MoveValidator.isBlitz(route, british, m_data));
	}
	
	public void testIsBlitzEnemyUnits()
	{
		Route route = new Route();
		route.add(egypt);
		//congo has 1 japanese unit. shouldnt be able to blitz
		route.add(congo);
		route.add(kenya);
		
		assertTrue(!MoveValidator.isBlitz(route, british, m_data));
	}
	
	public void testCasBlitz()
	{
		Collection units = new ArrayList();
		
		assertTrue(MoveValidator.canBlitz(units));
		
		units.addAll( armour.create(3,british));
		assertTrue(MoveValidator.canBlitz(units));
		
		units.addAll( infantry.create(3,british));
		assertTrue(!MoveValidator.canBlitz(units));
	}
	
	public void testHasNeutralBeforEnd()
	{
		Route route = new Route();
		route.add(egypt);
		assertTrue(!MoveValidator.hasNuetralBeforeEnd(route));
		
		//nuetral
		route.add(westAfrica);
		assertTrue(!MoveValidator.hasNuetralBeforeEnd(route));
		
		route.add(libya);
		assertTrue(MoveValidator.hasNuetralBeforeEnd(route));
	}
	
	public void testHasUnitsThatCantGoOnWater()
	{
		Collection units = new ArrayList();
		units.addAll( infantry.create(1,british));
		units.addAll( armour.create(1,british));
		units.addAll( transport.create(1,british));
		units.addAll( fighter.create(1,british));
		assertTrue(! MoveValidator.hasUnitsThatCantGoOnWater(units));
		
		assertTrue( MoveValidator.hasUnitsThatCantGoOnWater( factory.create(1,british)));
	}
	
	
	public void testCarrierCapacity()
	{
		Collection units = carrier.create(5,british);
		assertEquals(10, MoveValidator.carrierCapacity(units));
	}
	
	public void testCarrierCost()
	{
		Collection units = fighter.create(5,british);
		assertEquals(5, MoveValidator.carrierCost(units));
	}
	
	public void testGetLeastMovement()
	{
		IntegerMap already = new IntegerMap();
		Collection collection = bomber.create(1, british);
		
		assertEquals( MoveValidator.getLeastMovement(collection, already), 6);
		
		
		Object[] objs = collection.toArray();
		already.put(objs[0], 1);
		
		assertEquals( MoveValidator.getLeastMovement(collection, already), 5);
		
		collection.addAll(factory.create(2,british));
		assertEquals( MoveValidator.getLeastMovement(collection, already), 0);			
	}
		
	public void testCanLand()
	{
		Collection units = fighter.create(4, british);
		//2 carriers in red sea
		assertTrue(MoveValidator.canLand(units, redSea, british, m_data));
		//britian owns egypt
		assertTrue(MoveValidator.canLand(units, egypt, british, m_data));
		//only 2 carriers
		Collection tooMany = fighter.create(6, british);
		assertTrue(!MoveValidator.canLand(tooMany, redSea, british, m_data));
		
		//nowhere to land
		assertTrue(!MoveValidator.canLand(units, japanSeaZone, british, m_data));
		//nuetral
		assertTrue(!MoveValidator.canLand(units, westAfrica,  british, m_data));
	}
	
	public void testCanLandInfantry()
	{
		try
		{
			Collection units = infantry.create(1, british);
			MoveValidator.canLand(units, redSea, british, m_data);
		} catch(IllegalArgumentException e)
		{
			return;
		}
		fail("No exception thrown");

	}

	public void testCanLandBomber()
	{		
		Collection units = bomber.create(1, british);
		assertTrue(!MoveValidator.canLand(units, redSea, british, m_data));
	}

	public void testHasSomeLand()
	{
		Collection units = transport.create(3,british);
		assertTrue(! MoveValidator.hasSomeLand(units));
		
		units.addAll( infantry.create(2,british));
		assertTrue( MoveValidator.hasSomeLand(units));
	}
	
}