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

  private Collection getUnits(IntegerMap units, Territory from)
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


  public void testNotEnoughUnits()
  {
    IntegerMap map = new IntegerMap();

    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    MoveMessage msg = new MoveMessage( armour.create(10, british), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
  }

  public void testCantMoveEnemy()
  {
    IntegerMap map = new IntegerMap();
    map.put(infantry, 1);
    Route route = new Route();
    route.setStart(algeria);
    route.add(libya);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());
  }

  public void testSimpleMove()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16, egypt.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }


  public void testSimpleMoveLength2()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    route.add(kenya);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, kenya.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16, egypt.getUnits().size());
    assertEquals(2, kenya.getUnits().size());
  }

  public void testCanReturnToCarrier()
  {
    IntegerMap map = new IntegerMap();
    map.put(fighter, 3);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid( (StringMessage) m_delegate.sendMessage(msg));

  }

  public void testLandOnCarrier()
  {
    IntegerMap map = new IntegerMap();
    map.put(fighter, 2);
    Route route = new Route();
    route.setStart(egypt);
    //extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, redSea.getUnits().size());
  }

  public void testCantLandWithNoCarrier()
  {
    IntegerMap map = new IntegerMap();
    map.put(fighter, 2);
    Route route = new Route();
    route.setStart(egypt);
    //extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(redSea);
    //no carriers
    route.add(mozambiqueSeaZone);


    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }


  public void testNotEnoughCarrierCapacity()
  {
    IntegerMap map = new IntegerMap();
    map.put(fighter, 5);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }

  public void testLandMoveToWaterWithNoTransports()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(eastMediteranean);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());
  }


  public void testSeaMove()
  {
    IntegerMap map = new IntegerMap();
    map.put(carrier, 2);
    Route route = new Route();
    route.setStart(redSea);
    //exast movement to force landing
    route.add(mozambiqueSeaZone);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(4, redSea.getUnits().size());
    assertEquals(0, mozambiqueSeaZone.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(2, redSea.getUnits().size());
    assertEquals(2, mozambiqueSeaZone.getUnits().size());

  }

  public void testSeaCantMoveToLand()
  {
    IntegerMap map = new IntegerMap();
    map.put(carrier, 2);
    Route route = new Route();
    route.setStart(redSea);
    //exast movement to force landing
    route.add(egypt);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());


  }
  public void testLandMoveToWaterWithTransportsFull()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 1);
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    //exast movement to force landing
    route.add(congoSeaZone);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(4,equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(4,equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());

  }


  public void testAirCanFlyOverWater()
  {
    IntegerMap map = new IntegerMap();
    map.put(bomber, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(redSea);
    route.add(syria);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertValid( (StringMessage) m_delegate.sendMessage(msg));
  }

  public void testLandMoveToWaterWithTransportsEmpty()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    //exast movement to force landing
    route.add(redSea);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());

    assertEquals(18,egypt.getUnits().size());
    assertEquals(4,redSea.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16,egypt.getUnits().size());
    assertEquals(6,redSea.getUnits().size());

  }

  public void testBlitzWithArmour()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
    assertEquals(libya.getOwner(), japanese);

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16, egypt.getUnits().size());
    assertEquals(3, algeria.getUnits().size());
    assertEquals(libya.getOwner(), british);

  }

  public void testCantBlitzNuetral()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
  }


  public void testOverrunNeutral()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(0, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), PlayerID.NULL_PLAYERID);
    assertEquals(35, british.getResources().getQuantity(ipcs));

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(2, equatorialAfrica.getUnits().size());
    assertEquals(2, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), british);
    assertEquals(32, british.getResources().getQuantity(ipcs));
  }

  public void testAirCanOverFlyEnemy()
  {
    IntegerMap map = new IntegerMap();
    map.put(bomber, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);
    route.add(equatorialAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertValid( (StringMessage) m_delegate.sendMessage(msg));
  }

  public void testOverrunNeutralMustStop()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    map = new IntegerMap();
    map.put(armour, 2);
    route = new Route();
    route.setStart(westAfrica);
    route.add(equatorialAfrica);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError( (StringMessage) m_delegate.sendMessage(msg));

  }


  public void testmultipleMovesExceedingMovementLimit()
  {
    IntegerMap map = new IntegerMap();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(eastAfrica);
    route.add(kenya);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(2, eastAfrica.getUnits().size());
    assertEquals(0, kenya.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(0, eastAfrica.getUnits().size());
    assertEquals(2, kenya.getUnits().size());

    route = new Route();
    route.setStart(kenya);
    route.add(egypt);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());

    assertError( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
  }

  public void testMovingUnitsWithMostMovement()
  {
    //move 2 tanks to equatorial africa
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(equatorialAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, equatorialAfrica.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, equatorialAfrica.getUnits().size());

    //now move 2 tanks out of equatorial africa to east africa
    //only the tanks with movement 2 can make it,
    //this makes sure that the correct units are moving
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);
    route.add(eastAfrica);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertEquals(6, equatorialAfrica.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());

    assertValid( (StringMessage) m_delegate.sendMessage(msg));

    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }





  public void testTransportsMustStayWithUnits()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route,route.getEnd().getUnits().getUnits());
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    map = new IntegerMap();
    map.put(transport, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertError((StringMessage)m_delegate.sendMessage(msg));
  }

  public void testUnitsStayWithTransports()
  {
    IntegerMap map = new IntegerMap();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    map = new IntegerMap();
    map.put(armour, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);

    assertError((StringMessage)m_delegate.sendMessage(msg));
  }

  public void testUnload()
  {
    IntegerMap map = new IntegerMap();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testUnloadedCantMove()
  {
    IntegerMap map = new IntegerMap();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));


    map = new IntegerMap();
    //only 2 originially, would have to move the 2 we just unloaded
    //as well
    map.put(infantry, 4);
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);

    //units were unloaded, shouldnt be able to move any more
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testUnloadingTransportsCantMove()
  {
    IntegerMap map = new IntegerMap();
    map.put(infantry, 4);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    map = new IntegerMap();
    map.put(transport, 2);
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);

    //the transports unloaded so they cant move
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));


  }

  public void testTransportsCanSplit()
  {
    //move 1 armour to red sea
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);

    IntegerMap map = new IntegerMap();
    map.put(armour, 1);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //move two infantry to red sea
    route = new Route();
    route.setStart(eastAfrica);
    route.add(redSea);

    map = new IntegerMap();
    map.put(infantry, 2);

    msg = new MoveMessage( getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //try to move 1 transport to indian ocean with 1 tank
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);

    map = new IntegerMap();
    map.put(armour, 1);
    map.put(transport, 1);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //move the other transport to west compass
    route = new Route();
    route.setStart(redSea);
    route.add(westCompass);

    map = new IntegerMap();
    map.put(infantry, 2);
    map.put(transport, 1);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testUseTransportsWithLowestMovement()
  {
    //move transport south
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);

    IntegerMap map = new IntegerMap();
    map.put(transport, 1);
    map.put(infantry, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //move transport back
    route = new Route();
    route.setStart(angolaSeaZone);
    route.add(congoSeaZone);

    map = new IntegerMap();
    map.put(transport, 1);
    map.put(infantry, 2);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //move the other transport south, should
    //figure out that only 1 can move
    //and will choose that one
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);

    map = new IntegerMap();
    map.put(infantry, 2);
    map.put(transport, 1);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));


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

    IntegerMap map = new IntegerMap();
    map.put(armour, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));

  }


  public void testAirViolateNeutrality()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);

    IntegerMap map = new IntegerMap();
    map.put(fighter, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testNeutralConquered()
  {
    //take over neutral
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap map = new IntegerMap();
    map.put(armour, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
    assertTrue(DelegateFinder.battleDelegate(m_data).getBattleTracker().wasConquered(westAfrica));
    assertTrue(!DelegateFinder.battleDelegate(m_data).getBattleTracker().wasBlitzed(westAfrica));

  }

  public void testMoveTransportsTwice()
  {
    //move transports
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);

    IntegerMap map = new IntegerMap();
    map.put(infantry, 2);
    map.put(transport, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //move again
    route = new Route();
    route.setStart(southAtlantic);
    route.add(angolaSeaZone);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

  }


  public void testCantMoveThroughConqueredNeutral()
  {
    //take over neutral
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap map = new IntegerMap();
    map.put(armour, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //make sure we cant move through it by land
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);

    map = new IntegerMap();
    map.put(armour, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));

    //make sure we can still move units to the territory
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    map = new IntegerMap();
    map.put(armour, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));


    //make sure air can though
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);
    route.add(westAfrica);
    route.add(equatorialAfrica);

    map = new IntegerMap();
    map.put(fighter, 3);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testCanBlitzThroughConqueredEnemy()
  {
    //take over empty enemy
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap map = new IntegerMap();
    map.put(infantry, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //make sure we can still blitz through it
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    route.add(algeria);

    map = new IntegerMap();
    map.put(armour, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

  }

  public void testAirCantLandInConquered()
  {
    //take over empty neutral
    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);

    IntegerMap map = new IntegerMap();
    map.put(armour, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //make sure the place cant use it to land
    //the only possibility would be newly conquered south africa
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(angolaSeaZone);
    route.add(southAfricaSeaZone);

    map = new IntegerMap();
    map.put(fighter, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testMoveAndTransportUnload()
  {
    //this was causing an exception
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);

    IntegerMap map = new IntegerMap();
    map.put(transport, 1);
    map.put(infantry, 2);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    route = new Route();
    route.setStart(westAfricaSeaZone);
    route.add(westAfrica);

    map = new IntegerMap();
    map.put(infantry, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

  }

  public void testTakeOverAfterOverFlight()
  {
    //this was causing an exception
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);

    IntegerMap map = new IntegerMap();
    map.put(bomber, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    route = new Route();
    route.setStart(libya);
    route.add(algeria);

    //planes cannot leave a battle zone, but the territory was empty so no battle occured
    map = new IntegerMap();
    map.put(bomber, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

  }

  public void testBattleAdded()
  {
    //TODO if air make sure otnot alwasys battle
    //this was causing an exception
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);

    IntegerMap map = new IntegerMap();
    map.put(bomber, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testLargeMove()
  {
    //was causing an error
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);

    IntegerMap map = new IntegerMap();
    map.put(bomber, 6);
    map.put(fighter, 6);
    map.put(armour, 6);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testAmphibiousAssaultAfterNavalBattle()
  {
    //move to take on brazil navy
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southBrazilSeaZone);

    IntegerMap map = new IntegerMap();
    map.put(transport, 2);
    map.put(infantry, 4);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //try to unload transports
    route = new Route();
    route.setStart(southBrazilSeaZone);
    route.add(brazil);

    map = new IntegerMap();
    map.put(infantry, 4);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

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

    IntegerMap map = new IntegerMap();
    map.put(fighter, 3);
    map.put(bomber, 3);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testNonCombatAttack()
  {
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(algeria);

    IntegerMap map = new IntegerMap();

    map.put(armour, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));

  }

  public void testNonCombatAttackNeutral()
  {
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap map = new IntegerMap();

    map.put(armour, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));

  }

  public void testNonCombatMoveToConquered()
  {
    //take over libya
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap map = new IntegerMap();

    map.put(armour, 1);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    //go to non combat
    m_bridge.setStepName("BritishNonCombatMove");
    m_delegate.start(m_bridge, m_data);

    //move more into libya
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    map = new IntegerMap();

    map.put(armour, 1);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));


  }

  public void testAACantMoveToConquered()
  {
    m_bridge.setStepName("JapaneseCombatMove");
    m_bridge.setPlayerID(japanese);
    m_delegate.start(m_bridge, m_data);

    Route route = new Route();
    route.setStart(congo);
    route.add(kenya);

    IntegerMap map = new IntegerMap();

    map.put(armour, 2);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();


    assertTrue(tracker.wasBlitzed(kenya));
    assertTrue(tracker.wasConquered(kenya));

    map.clear();
    map.put(aaGun, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testBlitzConqueredNeutralInTwoSteps()
  {

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);

    IntegerMap map = new IntegerMap();

    map.put(infantry, 1);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

    assertTrue(!tracker.wasBlitzed(westAfrica));
    assertTrue(tracker.wasConquered(westAfrica));

    map.clear();
    map.put(armour, 1);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    route = new Route();
    route.setStart(westAfrica);
    route.add(algeria);

    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertError((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testBlitzFactory()
  {
    //create a factory to be taken
    Collection factCollection = factory.create(1, japanese);
    Change addFactory = ChangeFactory.addUnits(libya, factCollection);
    m_bridge.addChange(addFactory);

    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);

    IntegerMap map = new IntegerMap();

    map.put(infantry, 1);

    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));


    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

    assertTrue(tracker.wasBlitzed(libya));
    assertTrue(tracker.wasConquered(libya));

    Unit factory = (Unit) factCollection.iterator().next();

    assertEquals( factory.getOwner(), british);
  }

  public void testAirCanLandOnLand()
  {
    Route route = new Route();
    route.setStart(egypt);
    route.add(eastMediteranean);
    route.add(blackSea);

    IntegerMap map = new IntegerMap();
    map.put(fighter, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
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

    IntegerMap map = new IntegerMap();
    map.put(fighter, 1);
    MoveMessage msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));

    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    route.add(angolaSeaZone);

    map = new IntegerMap();
    map.put(fighter, 1);
    msg = new MoveMessage( getUnits(map, route.getStart()), route);
    assertValid((StringMessage) m_delegate.sendMessage(msg));
  }

  public void testRoute()
  {
    Route route = m_data.getMap().getRoute(angola, russia);
    assertNotNull(route);
    assertEquals(route.getEnd(), russia);
  }

}
