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

import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class PlaceDelegateTest extends DelegateTest
{
		
	protected PlaceDelegate m_delegate;
	protected ITestDelegateBridge m_bridge;
	
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
		
		
		m_bridge = super.getDelegateBridge(british);
		m_delegate = new PlaceDelegate();
		m_delegate.initialize("place");
		m_delegate.start(m_bridge, m_data);
	}

	private Collection getUnits(IntegerMap<UnitType> units, PlayerID from)
	{
		Iterator<UnitType> iter = units.keySet().iterator();
		Collection rVal = new ArrayList(units.totalValues());
		while(iter.hasNext())
		{
			UnitType type = iter.next();
			rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
		}
		return rVal;
	}

	
	public void testValid()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
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
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(infantry ,2);
		
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
		assertError(response);
	}

	public void testSeaCanGoInSeaZone()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(transport ,2);
		
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
		assertValid(response );
	}	
	
	public void testLandCanGoInLandZone()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(infantry ,2);
		
		String response = m_delegate.placeUnits(getUnits(map, british), uk);
		assertValid(response );
	}

	public void testSeaCantGoInSeaInLandZone()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(transport ,2);
		
		String response = m_delegate.canUnitsBePlaced(uk,getUnits(map, british),british);
		assertError(response);
	}	
	
	public void testNoGoIfOpposingTroopsSea()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(transport, 2);
		String response = m_delegate.canUnitsBePlaced(northSea, getUnits(map, japanese), japanese);		
		assertError(response);
		
	}
	
	public void testNoGoIfOpposingTroopsLand()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(infantry, 2);
		String response = m_delegate.canUnitsBePlaced(japan, getUnits(map, british), british);
		
		assertError(response);
	}
	
	public void testOnlyOneFactoryPlaced()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(factory, 1);
		String response = m_delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
		
		assertError(response );
	}

	public void testCantPlaceAAWhenOneAlreadyThere()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(aaGun, 1);
		String response = m_delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
		assertError(response );
	}

	public void testCantPlaceTwoAA()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(aaGun, 2);
		String response = m_delegate.canUnitsBePlaced(westCanada, getUnits(map, british), british);
		assertError(response );
	}
	
	public void testProduceFactory()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(factory, 1);
		String response = m_delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
		assertValid(response );
	}

		public void testMustOwnToPlace()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(infantry, 2);
		String response = m_delegate.canUnitsBePlaced(germany, getUnits(map, british), british);
		assertError(response );
	}

	
	public void testCanProduce()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		
		map.add(infantry, 2);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertFalse(response.isError() );
	}
	
	public void testCanProduceInSea()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(transport, 2);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), northSea);
		assertFalse(response.isError() );
	}
	
	
	public void testCanNotProduceThatManyUnits()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		
		map.add(infantry, 3);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertTrue(response.getMaxUnits() == 2 );
	}
	
	public void testAlreadyProducedUnits()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		Map<Territory, Collection<Unit>> alreadyProduced = new HashMap<Territory, Collection<Unit>>();
		alreadyProduced.put(westCanada, getInfantry(2, british));
		m_delegate.setProduced(alreadyProduced);
		map.add(infantry, 1);
		PlaceableUnits response = m_delegate.getPlaceableUnits(getUnits(map, british), westCanada);
		assertTrue(response.getMaxUnits() == 0);
	}
	
	public void testMultipleFactories()
	{
		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
		map.add(factory, 1);
		String response = m_delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
		
		//we can place 1 factory
		assertValid(response);
		
		//we cant place 2
		map = new IntegerMap<UnitType>();
		map.add(factory, 2);
		response = m_delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
		assertError(response);
	}
}
