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
package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.Dynamix_AI.DOddsCalculator;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.xml.LoadGameUtil;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * 
 * @author Stephen
 */
public class DOddsCalculatorTest extends TestCase
{
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("Great Lakes War Test", "Great Lakes War v1.4 test.xml");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	public void testBattleCalculator()
	{
		final PlayerID superior = m_data.getPlayerList().getPlayerID("Superior");
		final PlayerID huron = m_data.getPlayerList().getPlayerID("Huron");
		final Territory cIsland = m_data.getMap().getTerritory("C");
		final UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
		final UnitType artillery = m_data.getUnitTypeList().getUnitType("artillery");
		final UnitType fighter = m_data.getUnitTypeList().getUnitType("fighter");
		final List<Unit> attacking = new ArrayList<Unit>();
		final List<Unit> defending = new ArrayList<Unit>();
		for (int i = 0; i < 30; i++)
		{
			attacking.add(infantry.create(superior));
			attacking.add(artillery.create(superior));
			attacking.add(fighter.create(superior));
		}
		for (int i = 0; i < 60; i++)
		{
			defending.add(infantry.create(huron));
		}
		DOddsCalculator.SetGameData(m_data);
		final AggregateResults results = DUtils.GetBattleResults(attacking, defending, cIsland, m_data, 150, true);
		System.out.print("Time Taken To Calculate: " + results.getTime() + "\r\n");
		assertEquals(1.0D, results.getAttackerWinPercent());
		assertEquals(0.0D, results.getAverageDefendingUnitsLeft());
		assertEquals(0.0D, results.getDefenderWinPercent());
	}
}
