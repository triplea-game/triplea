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
 * ParserTest.java
 *
 * Created on October 12, 2001, 1:32 PM
 */

package games.strategy.engine.xml;

import junit.framework.*;
import java.io.*;
import java.net.URL;
import java.util.*;


import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ParserTest extends TestCase 
{
	private GameData gameData;
		
	public ParserTest(String string)
	{
		super(string);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(ParserTest.class);
		
		return suite;
	}
	
	public void setUp() throws Exception
	{
		//get the xml file
		URL url = this.getClass().getResource("GameExample.xml");
		//System.out.println(url);
		InputStream input= url.openStream();
		gameData = (new GameParser()).parse(input);
	}
	
	public void testCanCreateData()
	{
		assertNotNull(gameData);
	}
	
	public void testTerritoriesCreated()
	{
		GameMap map = gameData.getMap();
		Collection territories = map.getTerritories();
		assertEquals(territories.size(), 3);
	}
	
	public void testWater()
	{
		Territory atl = gameData.getMap().getTerritory("atlantic");
		assertEquals(atl.isWater(), true);
		Territory can = gameData.getMap().getTerritory("canada");
		assertEquals(can.isWater(), false);
		
		
	}
	
	public void testTerritoriesConnected()
	{
		GameMap map = gameData.getMap();
		assertEquals(1, map.getDistance( map.getTerritory("canada"), map.getTerritory("us")));
	}
	
	public void testResourcesAdded()
	{
		ResourceList resources = gameData.getResourceList();
		assertEquals(resources.size(), 2);
	}
	
	public void testUnitTypesAdded()
	{
		UnitTypeList units = gameData.getUnitTypeList();
		assertEquals(units.size(), 1);
	}
	
	public void testPlayersAdded()
	{
		PlayerList players = gameData.getPlayerList();
		assertEquals(players.size(), 3);
	}
	
	public void testAllianceMade()
	{
		PlayerList players = gameData.getPlayerList();
		PlayerID castro = players.getPlayerID("castro");
		PlayerID chretian = players.getPlayerID("chretian");
		
		AllianceTracker alliances = gameData.getAllianceTracker();
		assertEquals(true, alliances.isAllied(castro, chretian));
	}
	
	public void testDelegatesCreated()
	{
		DelegateList delegates = gameData.getDelegateList();
		assertEquals(delegates.size(), 2);
	}

	public void testStepsCreated()
	{
		 gameData.getSequence();
		
	}
	
	public void testProductionFrontiersCreated()
	{
		assertEquals(gameData.getProductionFrontierList().size(), 2);
	}
	
	public void testProductionRulesCreated()
	{
		assertEquals(gameData.getProductionRuleList().size(), 3);
	}
	
	public void testPlayerProduction()
	{
		ProductionFrontier cf = gameData.getProductionFrontierList().getProductionFrontier("canProd");
		PlayerID can = gameData.getPlayerList().getPlayerID("chretian");
		assertEquals(can.getProductionFrontier(), cf);
	}
	
	public void testAttatchments()
	{
		TestAttatchment att = (TestAttatchment) gameData.getResourceList().getResource("gold").getAttatchment("resourceAttatchment");
		assertEquals(att.getValue(), "gold");
		
		att = (TestAttatchment) gameData.getUnitTypeList().getUnitType("inf").getAttatchment("infAttatchment");
		assertEquals(att.getValue(), "inf");
		
		att = (TestAttatchment) gameData.getMap().getTerritory("us").getAttatchment("territoryAttatchment");
		assertEquals(att.getValue(), "us of a");
		
		att = (TestAttatchment) gameData.getPlayerList().getPlayerID("chretian").getAttatchment("playerAttatchment");
		assertEquals(att.getValue(), "liberal");

	}
	
	public void testOwnerInitialze()
	{
		Territory can = gameData.getMap().getTerritory("canada");
		assertNotNull("couldnt find country", can);
		assertNotNull( "owner null", can.getOwner());
		assertEquals(can.getOwner().getName(), "chretian");
		
		Territory us = gameData.getMap().getTerritory("us");
		assertEquals(us.getOwner().getName(), "bush");
	}
	
	public void testUnitsHeldInitialized()
	{
		PlayerID bush = gameData.getPlayerList().getPlayerID("bush");
		assertEquals(bush.getUnits().getUnitCount(), 20);
	}
	
	public void testUnitsPlacedInitialized()
	{
		Territory terr = gameData.getMap().getTerritory("canada");
		assertEquals(terr.getUnits().getUnitCount(), 5);
	}
	
	public void testResourcesGiven()
	{
		PlayerID chretian = gameData.getPlayerList().getPlayerID("chretian");
		Resource resource = gameData.getResourceList().getResource("silver");
		assertEquals(200, chretian.getResources().getQuantity(resource));
	}
}


