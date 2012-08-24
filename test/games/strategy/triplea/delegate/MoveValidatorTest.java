/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * MoveValidatorTest.java
 * 
 * Created on November 8, 2001, 5:00 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class MoveValidatorTest extends DelegateTest
{
	/** Creates new PlaceDelegateTest */
	public MoveValidatorTest(final String name)
	{
		super(name);
	}
	
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
	}
	
	/**
	 * @deprecated test Matches / Route class instead
	 */
	@Deprecated
	public void testHasEnoughMovement()
	{
		final List<Unit> units = bomber.create(3, british);
		TripleAUnit.get(units.get(0)).setAlreadyMoved(2);
		TripleAUnit.get(units.get(1)).setAlreadyMoved(1);
		assertTrue(MoveValidator.hasEnoughMovement(units, 2));
	}
	
	/**
	 * @deprecated test Matches / Route class instead
	 */
	@Deprecated
	public void testHasWater()
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
	
	/**
	 * @deprecated test Matches / Route class instead
	 */
	@Deprecated
	public void testNotEnoughMovement()
	{
		final Collection<Unit> units = bomber.create(3, british);
		final Object[] objs = units.toArray();
		assertTrue(MoveValidator.hasEnoughMovement(units, 6));
		assertTrue(!MoveValidator.hasEnoughMovement(units, 7));
		((TripleAUnit) objs[1]).setAlreadyMoved(1);
		assertTrue(!MoveValidator.hasEnoughMovement(units, 6));
		((TripleAUnit) objs[1]).setAlreadyMoved(2);
		assertTrue(!MoveValidator.hasEnoughMovement(units, 5));
	}
	
	public void testEnemyUnitsInPath()
	{
		// japanese unit in congo
		final Route bad = new Route();
		// the empty case
		assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(bad, british, m_data));
		bad.add(egypt);
		bad.add(congo);
		bad.add(kenya);
		assertTrue(!MoveValidator.noEnemyUnitsOnPathMiddleSteps(bad, british, m_data));
		final Route good = new Route();
		good.add(egypt);
		good.add(kenya);
		assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(good, british, m_data));
		// at end so should still be good
		good.add(congo);
		assertTrue(MoveValidator.noEnemyUnitsOnPathMiddleSteps(good, british, m_data));
	}
	
	/**
	 * @deprecated test Matches / Route class instead
	 */
	@Deprecated
	public void testHasNeutralBeforEnd()
	{
		final Route route = new Route();
		route.add(egypt);
		assertTrue(!MoveValidator.hasNeutralBeforeEnd(route));
		// nuetral
		route.add(westAfrica);
		assertTrue(!MoveValidator.hasNeutralBeforeEnd(route));
		route.add(libya);
		assertTrue(MoveValidator.hasNeutralBeforeEnd(route));
	}
	
	public void testHasUnitsThatCantGoOnWater()
	{
		final Collection<Unit> units = new ArrayList<Unit>();
		units.addAll(infantry.create(1, british));
		units.addAll(armour.create(1, british));
		units.addAll(transport.create(1, british));
		units.addAll(fighter.create(1, british));
		assertTrue(!MoveValidator.hasUnitsThatCantGoOnWater(units));
		assertTrue(MoveValidator.hasUnitsThatCantGoOnWater(factory.create(1, british)));
	}
	
	public void testCarrierCapacity()
	{
		final Collection<Unit> units = carrier.create(5, british);
		assertEquals(10, AirMovementValidator.carrierCapacity(units, new Territory("TestTerritory", true, m_data)));
	}
	
	public void testCarrierCost()
	{
		final Collection<Unit> units = fighter.create(5, british);
		assertEquals(5, AirMovementValidator.carrierCost(units));
	}
	
	public void testGetLeastMovement()
	{
		final Collection<Unit> collection = bomber.create(1, british);
		assertEquals(MoveValidator.getLeastMovement(collection), 6);
		final Object[] objs = collection.toArray();
		((TripleAUnit) objs[0]).setAlreadyMoved(1);
		assertEquals(MoveValidator.getLeastMovement(collection), 5);
		collection.addAll(factory.create(2, british));
		assertEquals(MoveValidator.getLeastMovement(collection), 0);
	}
	
	public void testCanLand()
	{
		final Collection<Unit> units = fighter.create(4, british);
		// 2 carriers in red sea
		assertTrue(AirMovementValidator.canLand(units, redSea, british, m_data));
		// britian owns egypt
		assertTrue(AirMovementValidator.canLand(units, egypt, british, m_data));
		// only 2 carriers
		final Collection<Unit> tooMany = fighter.create(6, british);
		assertTrue(!AirMovementValidator.canLand(tooMany, redSea, british, m_data));
		// nowhere to land
		assertTrue(!AirMovementValidator.canLand(units, japanSeaZone, british, m_data));
		// nuetral
		assertTrue(!AirMovementValidator.canLand(units, westAfrica, british, m_data));
	}
	
	public void testCanLandInfantry()
	{
		try
		{
			final Collection<Unit> units = infantry.create(1, british);
			AirMovementValidator.canLand(units, redSea, british, m_data);
		} catch (final IllegalArgumentException e)
		{
			return;
		}
		fail("No exception thrown");
	}
	
	public void testCanLandBomber()
	{
		final Collection<Unit> units = bomber.create(1, british);
		assertTrue(!AirMovementValidator.canLand(units, redSea, british, m_data));
	}
	
	public void testHasSomeLand()
	{
		final Collection<Unit> units = transport.create(3, british);
		assertTrue(!Match.someMatch(units, Matches.UnitIsLand));
		units.addAll(infantry.create(2, british));
		assertTrue(Match.someMatch(units, Matches.UnitIsLand));
	}
}
