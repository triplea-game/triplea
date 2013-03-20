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
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

@SuppressWarnings("unused")
public class Pact_of_Steel_2_Test extends TestCase
{
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		m_data = LoadGameUtil.loadGame("Pact of Steel 2 Test", "pact_of_steel_2_test.xml");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
		super.tearDown();
	}
	
	private ITestDelegateBridge getDelegateBridge(final PlayerID player)
	{
		return GameDataTestUtil.getDelegateBridge(player, m_data);
	}
	
	public void testDirectOwnershipTerritories()
	{
		final Territory Norway = m_data.getMap().getTerritory("Norway");
		final Territory Eastern_Europe = m_data.getMap().getTerritory("Eastern Europe");
		final Territory East_Balkans = m_data.getMap().getTerritory("East Balkans");
		final Territory Ukraine_S_S_R_ = m_data.getMap().getTerritory("Ukraine S.S.R.");
		final Territory Belorussia = m_data.getMap().getTerritory("Belorussia");
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		// this National Objective russia has to own at least 3 of the 5 territories by itself
		final RulesAttachment russian_easternEurope = RulesAttachment.get(russians, "objectiveAttachmentRussians1_EasternEurope");
		final Collection<Territory> terrs = new ArrayList<Territory>();
		terrs.add(Norway);
		terrs.add(Eastern_Europe);
		terrs.add(East_Balkans);
		terrs.add(Ukraine_S_S_R_);
		terrs.add(Belorussia);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 5);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 0);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
		assertFalse(russian_easternEurope.isSatisfied(null, bridge));
		Norway.setOwner(british);
		Eastern_Europe.setOwner(russians);
		East_Balkans.setOwner(russians);
		Ukraine_S_S_R_.setOwner(germans);
		Belorussia.setOwner(germans);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 1);
		assertFalse(russian_easternEurope.isSatisfied(null, bridge));
		Ukraine_S_S_R_.setOwner(british);
		Belorussia.setOwner(british);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 3);
		assertFalse(russian_easternEurope.isSatisfied(null, bridge));
		Norway.setOwner(russians);
		Ukraine_S_S_R_.setOwner(germans);
		Belorussia.setOwner(germans);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 3);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
		assertTrue(russian_easternEurope.isSatisfied(null, bridge));
		Ukraine_S_S_R_.setOwner(russians);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 1);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 4);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
		assertTrue(russian_easternEurope.isSatisfied(null, bridge));
		Belorussia.setOwner(russians);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 5);
		assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
		assertTrue(russian_easternEurope.isSatisfied(null, bridge));
	}
	
	public void testSupportAttachments()
	{
		// TODO
	}
	
	public void testNationalObjectiveUses()
	{
		// TODO
	}
	
	public void testBlockadeAndBlockadeZones()
	{
		// TODO
	}
	
	public void testTriggers()
	{
		// TODO
	}
	
	public void testConditions()
	{
		// TODO
	}
	
	public void testObjectives()
	{
		// TODO
	}
	
	public void testTechnologyFrontiers()
	{
		// TODO frontiers, renaming, generic, and new techs and adding of players to frontiers
	}
	
	public void testIsCombatTransport()
	{
		// TODO
	}
	
	public void testIsConstruction()
	{
		// TODO isConstruction, constructionType, constructionsPerTerrPerTypePerTurn, maxConstructionsPerTypePerTerr, "More Constructions with Factory", "More Constructions with Factory", "Unlimited Constructions"
	}
	
	public void testMaxPlacePerTerritory()
	{
		// TODO
	}
	
	public void testCapitalCapturePlayerOptions()
	{
		// TODO destroysPUs, retainCapitalNumber, retainCapitalProduceNumber
	}
	
	public void testUnitPlacementRestrictions()
	{
		// TODO
	}
	
	public void testRepairsUnits()
	{
		// TODO repairsUnits, "Two HitPoint Units Require Repair Facilities", "Battleships repair at beginning of round"
	}
	
	public void testProductionPerXTerritories()
	{
		// TODO
	}
	
	public void testGiveUnitControl()
	{
		// TODO giveUnitControl, changeUnitOwners, canBeGivenByTerritoryTo, "Give Units By Territory"
	}
	
	public void testDiceSides()
	{
		// TODO
	}
	
	public void testMaxBuiltPerPlayer()
	{
		// TODO
	}
	
	public void testDestroyedWhenCapturedBy()
	{
		// TODO "Units Can Be Destroyed Instead Of Captured", destroyedWhenCapturedBy
	}
	
	public void testIsInfrastructure()
	{
		// TODO
	}
	
	public void testCanBeDamaged()
	{
		// TODO
	}
	
	public void testIsSuicide()
	{
		// TODO isSuicide, "Suicide and Munition Casualties Restricted", "Defending Suicide and Munition Units Do Not Fire"
	}
	
	public void test()
	{
		// TODO
	}
	
	/***********************************************************
	************************************************************
	************************************************************
	***********************************************************/
	/*
	 * Add Utilities here
	 */
	private Collection<Unit> getUnits(final IntegerMap<UnitType> units, final PlayerID from)
	{
		final Iterator<UnitType> iter = units.keySet().iterator();
		final Collection<Unit> rVal = new ArrayList<Unit>(units.totalValues());
		while (iter.hasNext())
		{
			final UnitType type = iter.next();
			rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
		}
		return rVal;
	}
	
	/***********************************************************
	************************************************************
	************************************************************
	***********************************************************/
	/*
	 * Add assertions here
	 */
	public void assertValid(final String string)
	{
		assertNull(string, string);
	}
	
	public void assertError(final String string)
	{
		assertNotNull(string, string);
	}
}
