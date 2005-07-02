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
 * MoveDelegateTest.java
 *
 * Created on November 8, 2001, 5:00 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.util.IntegerMap;

import java.util.*;

import junit.framework.*;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MoveDelegateTest extends DelegateTest
{

  MoveDelegate m_delegate;
  TestDelegateBridge m_bridge;

  /** Creates new PlaceDelegateTest */
    public MoveDelegateTest(String name)
  {
    super(name);


    }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(MoveDelegateTest.class);

    return suite;
  }

  public void setUp() throws Exception
  {
    super.setUp();
    m_bridge = new TestDelegateBridge(m_data, british);
    m_bridge.setStepName("BritishCombatMove");
    m_delegate = new MoveDelegate();
    m_delegate.initialize("MoveDelegate", "MoveDelegate");
    m_delegate.start(m_bridge, m_data);
  }

  private Collection<Unit> getUnits(IntegerMap<UnitType> units, Territory from)
  {
    Iterator<UnitType> iter = units.keySet().iterator();
    Collection<Unit> rVal = new ArrayList<Unit>(units.totalValues());
    while(iter.hasNext())
    {
      UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }


  public void testNotEnoughUnits()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    String results = m_delegate.move( armour.create(10, british), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    assertError( results);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
  }

  public void testCantMoveEnemy()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 1);
    Route route = new Route();
    route.setStart(algeria);
    route.add(libya);
    

    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());
  }

  public void testSimpleMove()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

    assertEquals(16, egypt.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }


  public void testSimpleMoveLength2()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    route.add(kenya);

    

    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, kenya.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

    assertEquals(16, egypt.getUnits().size());
    assertEquals(2, kenya.getUnits().size());
  }

  public void testCanReturnToCarrier()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 3);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

  }

  public void testLandOnCarrier()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 2);
    Route route = new Route();
    route.setStart(egypt);
    //extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);



    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, redSea.getUnits().size());
  }

  public void testCantLandWithNoCarrier()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 2);
    Route route = new Route();
    route.setStart(egypt);
    //extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(redSea);
    //no carriers
    route.add(mozambiqueSeaZone);




    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }


  public void testNotEnoughCarrierCapacity()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 5);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);



    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }

  public void testLandMoveToWaterWithNoTransports()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(eastMediteranean);



    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());
  }


  public void testSeaMove()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(carrier, 2);
    Route route = new Route();
    route.setStart(redSea);
    //exast movement to force landing
    route.add(mozambiqueSeaZone);



    assertEquals(4, redSea.getUnits().size());
    assertEquals(0, mozambiqueSeaZone.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

    assertEquals(2, redSea.getUnits().size());
    assertEquals(2, mozambiqueSeaZone.getUnits().size());

  }

  public void testSeaCantMoveToLand()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(carrier, 2);
    Route route = new Route();
    route.setStart(redSea);
    //exast movement to force landing
    route.add(egypt);



    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());


  }
  public void testLandMoveToWaterWithTransportsFull()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 1);
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    //exast movement to force landing
    route.add(congoSeaZone);



    assertEquals(4,equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(4,equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());

  }


  public void testAirCanFlyOverWater()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(bomber, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(redSea);
    route.add(syria);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);

    assertValid( results);
  }

  public void testLandMoveToWaterWithTransportsEmpty()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(redSea);



    assertEquals(18,egypt.getUnits().size());
    assertEquals(4,redSea.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid( results);

    assertEquals(16,egypt.getUnits().size());
    assertEquals(6,redSea.getUnits().size());

  }

  public void testBlitzWithArmour()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);



    assertEquals(18, egypt.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
    assertEquals(libya.getOwner(), japanese);
    
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    
    assertValid( results);

    assertEquals(16, egypt.getUnits().size());
    assertEquals(3, algeria.getUnits().size());
    assertEquals(libya.getOwner(), british);

  }

  public void testCantBlitzNuetral()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);



    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
  }


  public void testOverrunNeutral()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);



    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(0, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), PlayerID.NULL_PLAYERID);
    assertEquals(35, british.getResources().getQuantity(ipcs));

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid( results);

    assertEquals(2, equatorialAfrica.getUnits().size());
    assertEquals(2, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), british);
    assertEquals(32, british.getResources().getQuantity(ipcs));
  }

  public void testAirCanOverFlyEnemy()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(bomber, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);
    route.add(equatorialAfrica);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);

    assertValid( results);
  }

  public void testOverrunNeutralMustStop()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);

    assertValid( results);

    map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    route = new Route();
    route.setStart(westAfrica);
    route.add(equatorialAfrica);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError( results);

  }


  public void testmultipleMovesExceedingMovementLimit()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(eastAfrica);
    route.add(kenya);



    assertEquals(2, eastAfrica.getUnits().size());
    assertEquals(0, kenya.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);    
    assertValid( results);

    assertEquals(0, eastAfrica.getUnits().size());
    assertEquals(2, kenya.getUnits().size());

    route = new Route();
    route.setStart(kenya);
    route.add(egypt);


    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());

    results = m_delegate.move( getUnits(map, route.getStart()), route);    
    assertError( results);

    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
  }

  public void testMovingUnitsWithMostMovement()
  {
    //move 2 tanks to equatorial africa
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(equatorialAfrica);



    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, equatorialAfrica.getUnits().size());

    String results = m_delegate.move( getUnits(map, route.getStart()), route);    
    assertValid( results);

    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, equatorialAfrica.getUnits().size());

    //now move 2 tanks out of equatorial africa to east africa
    //only the tanks with movement 2 can make it,
    //this makes sure that the correct units are moving
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);
    route.add(eastAfrica);


    assertEquals(6, equatorialAfrica.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    results = m_delegate.move( getUnits(map, route.getStart()), route);    
    assertValid( results);

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }





  public void testTransportsMustStayWithUnits()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    
    String results = m_delegate.move( getUnits(map, route.getStart()), route,route.getEnd().getUnits().getUnits());
    assertValid(results);

    map = new IntegerMap<UnitType>();
    map.put(transport, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testUnitsStayWithTransports()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    
    String results = m_delegate.move( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);

    map = new IntegerMap<UnitType>();
    map.put(armour, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testUnload()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
    
  }

  public void testUnloadedCantMove()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);


    map = new IntegerMap<UnitType>();
    //only 2 originially, would have to move the 2 we just unloaded
    //as well
    map.put(infantry, 4);
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);

    //units were unloaded, shouldnt be able to move any more
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testUnloadingTransportsCantMove()
  {
    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 4);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    map = new IntegerMap<UnitType>();
    map.put(transport, 2);
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);

    //the transports unloaded so they cant move
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);


  }

  public void testTransportsCanSplit()
  {
    //move 1 armour to red sea
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);

    //move two infantry to red sea
    route = new Route();
    route.setStart(eastAfrica);
    route.add(redSea);

    map = new IntegerMap<UnitType>();
    map.put(infantry, 2);

    results = m_delegate.move( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);

    //try to move 1 transport to indian ocean with 1 tank
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);

    map = new IntegerMap<UnitType>();
    map.put(armour, 1);
    map.put(transport, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //move the other transport to west compass
    route = new Route();
    route.setStart(redSea);
    route.add(westCompass);

    map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    map.put(transport, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testUseTransportsWithLowestMovement()
  {
    //move transport south
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(transport, 1);
    map.put(infantry, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //move transport back
    route = new Route();
    route.setStart(angolaSeaZone);
    route.add(congoSeaZone);

    map = new IntegerMap<UnitType>();
    map.put(transport, 1);
    map.put(infantry, 2);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //move the other transport south, should
    //figure out that only 1 can move
    //and will choose that one
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);

    map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    map.put(transport, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);


  }

  public void testCanOverrunNeutralWithoutFunds()
  {
    assertEquals(35, british.getResources().getQuantity(ipcs));
    Change makePoor = ChangeFactory.changeResourcesChange(british, ipcs, -35);
    m_bridge.addChange(makePoor);
    assertEquals(0, british.getResources().getQuantity(ipcs));

    //try to take over South Africa, cant because we cant afford it

    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);

  }


  public void testAirViolateNeutrality()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testNeutralConquered()
  {
    //take over neutral
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 1);
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    assertTrue(DelegateFinder.battleDelegate(m_data).getBattleTracker().wasConquered(westAfrica));
    assertTrue(!DelegateFinder.battleDelegate(m_data).getBattleTracker().wasBlitzed(westAfrica));

  }

  public void testMoveTransportsTwice()
  {
    //move transports
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 2);
    map.put(transport, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //move again
    route = new Route();
    route.setStart(southAtlantic);
    route.add(angolaSeaZone);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

  }


  public void testCantMoveThroughConqueredNeutral()
  {
    //take over neutral
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //make sure we cant move through it by land
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);

    map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);

    //make sure we can still move units to the territory
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);


    //make sure air can though
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);
    route.add(westAfrica);
    route.add(equatorialAfrica);

    map = new IntegerMap<UnitType>();
    map.put(fighter, 3);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testCanBlitzThroughConqueredEnemy()
  {
    //take over empty enemy
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(infantry, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //make sure we can still blitz through it
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    route.add(algeria);

    map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

  }

  public void testAirCantLandInConquered()
  {
    //take over empty neutral
    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(armour, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //make sure the place cant use it to land
    //the only possibility would be newly conquered south africa
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(angolaSeaZone);
    route.add(southAfricaSeaZone);

    map = new IntegerMap<UnitType>();
    map.put(fighter, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testMoveAndTransportUnload()
  {
    //this was causing an exception
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(transport, 1);
    map.put(infantry, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    route = new Route();
    route.setStart(westAfricaSeaZone);
    route.add(westAfrica);

    map = new IntegerMap<UnitType>();
    map.put(infantry, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

  }

  public void testTakeOverAfterOverFlight()
  {
    //this was causing an exception
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(bomber, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    route = new Route();
    route.setStart(libya);
    route.add(algeria);

    //planes cannot leave a battle zone, but the territory was empty so no battle occured
    map = new IntegerMap<UnitType>();
    map.put(bomber, 1);
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

  }

  public void testBattleAdded()
  {
    //TODO if air make sure otnot alwasys battle
    //this was causing an exception
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(bomber, 1);
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testLargeMove()
  {
    //was causing an error
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(bomber, 6);
    map.put(fighter, 6);
    map.put(armour, 6);
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testAmphibiousAssaultAfterNavalBattle()
  {
    //move to take on brazil navy
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southBrazilSeaZone);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(transport, 2);
    map.put(infantry, 4);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //try to unload transports
    route = new Route();
    route.setStart(southBrazilSeaZone);
    route.add(brazil);

    map = new IntegerMap<UnitType>();
    map.put(infantry, 4);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    Battle inBrazil = DelegateFinder.battleDelegate(m_data).getBattleTracker().getPendingBattle(brazil, false);
    Battle inBrazilSea = DelegateFinder.battleDelegate(m_data).getBattleTracker().getPendingBattle(southBrazilSeaZone, false);

    assertNotNull(inBrazilSea);
    assertNotNull(inBrazil);
    assertEquals( DelegateFinder.battleDelegate(m_data).getBattleTracker().getDependentOn(inBrazil).iterator().next(), inBrazilSea);
  }

  public void testAirToWater()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastMediteranean);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 3);
    map.put(bomber, 3);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testNonCombatAttack()
  {
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(algeria);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(armour, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);

  }

  public void testNonCombatAttackNeutral()
  {
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(armour, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);

  }

  public void testNonCombatMoveToConquered()
  {
    //take over libya
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(armour, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    //go to non combat
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    //move more into libya
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    map = new IntegerMap<UnitType>();

    map.put(armour, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);


  }

  public void testAACantMoveToConquered()
  {
    m_bridge.setStepName("JapaneseCombatMove");
    m_bridge.setPlayerID(japanese);
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(congo);
    route.add(kenya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(armour, 2);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();


    assertTrue(tracker.wasBlitzed(kenya));
    assertTrue(tracker.wasConquered(kenya));

    map.clear();
    map.put(aaGun, 1);
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testBlitzConqueredNeutralInTwoSteps()
  {

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(infantry, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

    assertTrue(!tracker.wasBlitzed(westAfrica));
    assertTrue(tracker.wasConquered(westAfrica));

    map.clear();
    map.put(armour, 1);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    route = new Route();
    route.setStart(westAfrica);
    route.add(algeria);

    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertError(results);
  }

  public void testBlitzFactory()
  {
    //create a factory to be taken
    Collection<Unit> factCollection = factory.create(1, japanese);
    Change addFactory = ChangeFactory.addUnits(libya, factCollection);
    m_bridge.addChange(addFactory);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();

    map.put(infantry, 1);

    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);


    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

    assertTrue(tracker.wasBlitzed(libya));
    assertTrue(tracker.wasConquered(libya));

    Unit aFactory = (Unit) factCollection.iterator().next();

    assertEquals( aFactory.getOwner(), british);
  }

  public void testAirCanLandOnLand()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastMediteranean);
    route.add(blackSea);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 1);
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testAirDifferingRouts()
  {
    //move one air unit 3 spaces, and a second 2,
    //this was causing an exception when the validator tried to find if they
    //could both land

    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    route.add(angolaSeaZone);

    IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    map.put(fighter, 1);
    String results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);

    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    route.add(angolaSeaZone);

    map = new IntegerMap<UnitType>();
    map.put(fighter, 1);
    results = m_delegate.move( getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  public void testRoute()
  {
    Route route = m_data.getMap().getRoute(angola, russia);
    assertNotNull(route);
    assertEquals(route.getEnd(), russia);
  }

}
