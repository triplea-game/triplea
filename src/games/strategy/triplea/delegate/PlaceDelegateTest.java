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
 * PlaceDelegateTest.java
 *
 * Created on November 8, 2001, 5:00 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.util.IntegerMap;

import java.util.*;

import junit.framework.*;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PlaceDelegateTest extends DelegateTest
{
		
	protected PlaceDelegate m_delegate;
	protected TestDelegateBridge m_bridge;
	
	/** Creates new PlaceDelegateTest */
    public PlaceDelegateTest(String name) 
	{
		super(name);
    }

	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(PlaceDelegateTest.class);
		
		return suite;
	}
	
	private Collection getInfantry(int count, PlayerID player)
	{
	    return m_data.getUnitTypeList().getUnitType(Constants.INFANTRY_TYPE).create(count, player);
	}

	public void setUp() throws Exception
	{
		super.setUp();
		
		
		m_bridge = new TestDelegateBridge(m_data, british);
		m_delegate = new PlaceDelegate();
		m_delegate.initialize("place");
		m_delegate.start(m_bridge, m_data);
	}

	private Collection getUnits(IntegerMap units, PlayerID from)
	{
		Iterator iter = units.keySet().iterator();
		Collection rVal = new ArrayList(units.totalValues());
		while(iter.hasNext())
		{
			UnitType type = (UnitType) iter.next();
			rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
		}
		return rVal;
	}

	
	public void testValid()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry ,2);
		
		String response = m_delegate.placeUnits(getUnits(map, british), uk);
		assertValid(response);
	}
	
	public void testNotCorrectUnitsValid()
	{
		
		String response = m_delegate.placeUnits(infantry.create(3, british), uk);
		assertError(response);
	}

	public void testOnlySeaInSeaZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry ,2);
		
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
		assertError(response);
	}

	public void testSeaCanGoInSeaZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport ,2);
		
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
		assertValid(response );
	}	
	
	public void testLandCanGoInLandZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry ,2);
		
		String response = m_delegate.placeUnits(getUnits(map, british), uk);
		assertValid(response );
	}

	public void testSeaCantGoInSeaInLandZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport ,2);
		
		String response = m_delegate.canUnitsBePlaced(uk,getUnits(map, british),british);
		assertError(response);
	}	
	
	public void testNoGoIfOpposingTroopsSea()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport, 2);
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);		
		assertError(response);
		
	}
	
	public void testNoGoIfOpposingTroopsLand()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry, 2);
		String response = m_delegate.canUnitsBePlaced(japan, getUnits(map, british), british);
		
		assertError(response);
	}
	
	public void testOnlyOneFactoryPlaced()
	{
		IntegerMap map = new IntegerMap();
		map.add(factory, 1);
		String response = m_delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
		
		assertError(response );
	}

	public void testCantPlaceAAWhenOneAlreadyThere()
	{
		IntegerMap map = new IntegerMap();
		map.add(aaGun, 1);
		String response = m_delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
		assertError(response );
	}

	public void testCantPlaceTwoAA()
	{
		IntegerMap map = new IntegerMap();
		map.add(aaGun, 2);
		String response = m_delegate.canUnitsBePlaced(westCanada, getUnits(map, british), british);
		assertError(response );
	}
	
	public void testProduceFactory()
	{
		IntegerMap map = new IntegerMap();
		map.add(factory, 1);
		String response = m_delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
		assertValid(response );
	}

		public void testMustOwnToPlace()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry, 2);
		String response = m_delegate.canUnitsBePlaced(germany, getUnits(map, british), british);
		assertError(response );
	}

	
	public void testCanProduce()
	{
		IntegerMap map = new IntegerMap();
		
		map.add(infantry, 2);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertFalse(response.isError() );
	}
	
	public void testCanProduceInSea()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport, 2);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), northSea);
		assertFalse(response.isError() );
	}
	
	
	public void testCanNotProduceThatManyUnits()
	{
		IntegerMap map = new IntegerMap();
		
		map.add(infantry, 3);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertTrue(response.isError() );
	}
	
	public void testAlreadyProducedUnits()
	{
		IntegerMap map = new IntegerMap();
		Map alreadyProduced = new HashMap();
		alreadyProduced.put(westCanada, getInfantry(2, british));
		m_delegate.setProduced(alreadyProduced);
		map.add(infantry, 1);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertTrue(response.isError() );
	}
	
	public void testMultipleFactories()
	{
		IntegerMap map = new IntegerMap();
		Map alreadyProduced = new HashMap();
		alreadyProduced.put(westCanada, getInfantry(2, british));
		m_delegate.setProduced(alreadyProduced);
		map.add(infantry, 1);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		
		assertTrue(response.isError() );
	}
}
