/*
 * PlaceDelegateTest.java
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
		
		PlaceMessage message = new PlaceMessage(getUnits(map, british), uk);
		StringMessage response = (StringMessage) m_delegate.isValidPlacement(message, british);
		assertValid(response);
	}
	
	public void testNotCorrectUnitsValid()
	{
		
		PlaceMessage message = new PlaceMessage(infantry.create(3, british), uk);
		StringMessage response = (StringMessage) m_delegate.playerHasEnoughUnits(message, british);
		assertError(response);
	}

	public void testOnlySeaInSeaZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry ,2);
		
		PlaceMessage message = new PlaceMessage(getUnits(map, british), northSea);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response);
	}

	public void testSeaCanGoInSeaZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport ,2);
		
		PlaceMessage message = new PlaceMessage(getUnits(map, british), northSea);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);

		assertValid(response );
	}	
	
	public void testLandCanGoInLandZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry ,2);
		
		PlaceMessage message = new PlaceMessage(getUnits(map, british), uk);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertValid(response );
	}

	public void testSeaCantGoInSeaInLandZone()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport ,2);
		
		PlaceMessage message = new PlaceMessage(getUnits(map, british), uk);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);

		assertError(response);
	}	
	
	public void testNoGoIfOpposingTroopsSea()
	{
		IntegerMap map = new IntegerMap();
		map.add(transport, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), northSea);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, japanese);
		assertError(response);
		
	}
	
	public void testNoGoIfOpposingTroopsLand()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), japan);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response);
	}
	
	public void testOnlyOneFactoryPlaced()
	{
		IntegerMap map = new IntegerMap();
		map.add(factory, 1);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), uk);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response );
	}

	public void testCantPlaceAAWhenOneAlreadyThere()
	{
		IntegerMap map = new IntegerMap();
		map.add(aaGun, 1);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), uk);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response );
	}

	public void testCantPlaceTwoAA()
	{
		IntegerMap map = new IntegerMap();
		map.add(aaGun, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), westCanada);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response );
	}
	
	public void testProduceFactory()
	{
		IntegerMap map = new IntegerMap();
		map.add(factory, 1);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), egypt);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertValid(response );
	}

		public void testMustOwnToPlace()
	{
		IntegerMap map = new IntegerMap();
		map.add(infantry, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), germany);
		StringMessage response = (StringMessage) m_delegate.canUnitsBePlaced(message, british);
		assertError(response );
	}

	
	public void testCanProduce()
	{
		IntegerMap map = new IntegerMap();
		IntegerMap alreadyProduced = new IntegerMap();
		map.add(infantry, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), westCanada);
		StringMessage response = (StringMessage) m_delegate.canProduce(message, british, alreadyProduced);
		assertValid(response );
	}
	
	public void testCanProduceInSea()
	{
		IntegerMap map = new IntegerMap();
		IntegerMap alreadyProduced = new IntegerMap();
		map.add(transport, 2);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), northSea);
		StringMessage response = (StringMessage) m_delegate.canProduce(message, british, alreadyProduced);
		assertValid(response );
	}
	
	
	public void testCanNotProduceThatManyUnits()
	{
		IntegerMap map = new IntegerMap();
		IntegerMap alreadyProduced = new IntegerMap();
		map.add(infantry, 3);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), westCanada);
		StringMessage response = (StringMessage) m_delegate.canProduce(message, british, alreadyProduced);
		assertError(response);
	}
	
	public void testAlreadyProducedUnits()
	{
		IntegerMap map = new IntegerMap();
		IntegerMap alreadyProduced = new IntegerMap();
		alreadyProduced.put(westCanada, 2);
		map.add(infantry, 1);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), westCanada);
		StringMessage response = (StringMessage) m_delegate.canProduce(message, british, alreadyProduced);
		assertError(response);
	}
	
	public void testMultipleFactories()
	{
		IntegerMap map = new IntegerMap();
		IntegerMap alreadyProduced = new IntegerMap();
		alreadyProduced.put(westCanada, 2);
		map.add(infantry, 1);
		PlaceMessage message = new PlaceMessage(getUnits(map, british), westCanada);
		StringMessage response = (StringMessage) m_delegate.canProduce(message, british, alreadyProduced);
		assertError(response );
	}
}
