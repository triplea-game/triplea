/*
 * MapTest.java
 *
 * Created on October 12, 2001, 2:23 PM
 */

package games.strategy.engine.data;

import junit.framework.*;
import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MapTest extends TestCase 
{
	Territory aa;
	Territory ab;
	Territory ac;
	Territory ad; 

	Territory ba;
	Territory bb;
	Territory bc;
	Territory bd; 

	Territory ca;
	Territory cb;
	Territory cc;
	Territory cd; 
	
	Territory da;
	Territory db;
	Territory dc;
	Territory dd; 

	Territory nowhere;
	
	GameMap map;
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(MapTest.class);
		return suite;
	}
	
	public MapTest(String name)
	{
		super(name);
	}
	
	public void setUp()
	{
		
		//map, l is land, w is water
		// llll
		// llww
		// llwl
		// llww
		aa = new Territory("aa", false, null);
		ab = new Territory("ab", false, null);
		ac = new Territory("ac", false, null);
		ad = new Territory("ad", false, null);
		
		ba = new Territory("ba", false, null);
		bb = new Territory("bb", false, null);
		bc = new Territory("bc", true, null);
		bd = new Territory("bd", true, null);
		
		ca = new Territory("ca", false, null);
		cb = new Territory("cb", false, null);
		cc = new Territory("cc", true, null);
		cd = new Territory("cd", false, null);
		
		da = new Territory("da", false, null);
		db = new Territory("db", false, null);
		dc = new Territory("dc", true, null);
		dd = new Territory("dd", true, null);
		
		map = new GameMap(null);
		
		map.addTerritory(aa);
		map.addTerritory(ab);
		map.addTerritory(ac);
		map.addTerritory(ad);
		
		map.addTerritory(ba);
		map.addTerritory(bb);
		map.addTerritory(bc);
		map.addTerritory(bd);
		
		map.addTerritory(ca);
		map.addTerritory(cb);
		map.addTerritory(cc);
		map.addTerritory(cd);
		
		map.addTerritory(da);
		map.addTerritory(db);
		map.addTerritory(dc);
		map.addTerritory(dd);
		
		map.addConnection(aa,ab);
		map.addConnection(ab,ac);
		map.addConnection(ac,ad);
		
		map.addConnection(ba,bb);
		map.addConnection(bb,bc);
		map.addConnection(bc,bd);
		
		map.addConnection(ca,cb);
		map.addConnection(cb,cc);
		map.addConnection(cc,cd);
		
		map.addConnection(da,db);
		map.addConnection(db,dc);
		map.addConnection(dc,dd);
		
		map.addConnection(aa,ba);
		map.addConnection(ba,ca);
		map.addConnection(ca,da);
		
		map.addConnection(ab,bb);
		map.addConnection(bb,cb);
		map.addConnection(cb,db);
		
		map.addConnection(ac,bc);
		map.addConnection(bc,cc);
		map.addConnection(cc,dc);
		
		map.addConnection(ad,bd);
		map.addConnection(bd,cd);
		map.addConnection(cd,dd);
		
		nowhere = new Territory("nowhere", false, null);
	}
	
	public void testNowhere()
	{
		assertTrue(-1 == map.getDistance(aa, nowhere));
	}
	
	public void testCantFindByName()
	{
		assertNull( map.getTerritory("nowhere"));
	}
	
	public void testCanFindByName()
	{
		assertNotNull( map.getTerritory("aa"));
	}

	public void testSame()
	{
		assertTrue(0 == map.getDistance(aa,aa));
	}
	
	public void testImpossibleConditionRoute()
	{
		GameMap.TerritoryTest test = new GameMap.TerritoryTest()
		{
			public boolean test(Territory t)
			{
				return false;
			}		
		};
		assertNull(map.getRoute(aa,ba,test));
	}
	
	public void testOne()
	{
		int distance = map.getDistance(aa,ab);
		assertTrue("" + distance, 1==distance );
	}
	
	public void testTwo()
	{
		int distance = map.getDistance(aa,ac);
		assertTrue("" + distance, 2==distance );
	}
	
	public void testOverWater()
	{
		assertTrue(map.getDistance(ca, cd) == 3);
	}
	
	public void testOverWaterCantReach()
	{
		assertTrue(map.getLandDistance(ca, cd) == -1);
	}
	
	public void testLong()
	{
		assertTrue(map.getLandDistance(ad,da) == 6);
	}

	public void testNeighborLandNoSeaConnect()
	{
		assertTrue(-1 == map.getWaterDistance(aa,ab));
	}
	
	public void testNeighborSeaNoLandConnect()
	{
		assertTrue(-1 == map.getLandDistance(bc,bd));
	}
	
	public void testRouteToSelf()
	{
		Route rt = map.getRoute(aa,aa);
		assertTrue(rt.getLength() == 0);
	}
	
	public void testRouteSizeOne()
	{
		Route rt = map.getRoute(aa,ab);
		assertTrue(rt.getLength() == 1);
	}
	
	public void testImpossibleRoute()
	{
		Route rt = map.getRoute(aa,nowhere);
		assertNull(rt);
	}
	
	public void testImpossibleLandRoute()
	{
		Route rt = map.getLandRoute(aa,cd);
		assertTrue(rt == null);
	}

	public void testImpossibleLandDistance()
	{
		int distance =  map.getLandDistance(aa,cd);
		assertTrue("wrongDistance exp -1, got:" + distance, distance == -1);
	}

	public void testWaterRout()
	{
		Route rt = map.getWaterRoute(bd, dd);
		assertTrue("bc:" + rt , rt.at(0).equals(bc) );
		assertTrue("cc", rt.at(1).equals(cc) );
		assertTrue("dc", rt.at(2).equals(dc) );
		assertTrue("dd", rt.at(3).equals(dd) );
	}
	
	public void testMultiplePossible()
	{
		Route rt = map.getRoute(aa,dd);
		assertTrue(rt.getLength() == 6);
	}
	
	public void testNeighbors()
	{
		Set neighbors = map.getNeighbors(aa);
		assertTrue(neighbors.size() == 2);
		assertTrue(neighbors.contains(ab));
		assertTrue(neighbors.contains(ba));
		
	}
	
	public void testNeighborsWithDistance()
	{
		Set neighbors = map.getNeighbors(aa,0 );
		assertTrue(neighbors.size() == 0);
		
		neighbors = map.getNeighbors(aa,1 );
		assertTrue(neighbors.size() == 2);
		assertTrue(neighbors.contains(ab));
		assertTrue(neighbors.contains(ba));
		
		neighbors = map.getNeighbors(aa, 2);

		assertTrue(neighbors.size() == 5);
		assertTrue(neighbors.contains(ab));
		assertTrue(neighbors.contains(ac));
		assertTrue(neighbors.contains(ba));
		assertTrue(neighbors.contains(bb));
		assertTrue(neighbors.contains(ca));
		
	}
	
}
