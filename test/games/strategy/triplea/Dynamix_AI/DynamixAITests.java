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
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.baseAI.AIUtils;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;

/**
 * 
 * @author Stephen
 */
public class DynamixAITests extends TestCase
{
	private GameData m_data;
	@SuppressWarnings("unused")
	private Dynamix_AI m_ai;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("Great Lakes War", "Great Lakes War v1.4.xml");
		m_ai = new Dynamix_AI("Superior");
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	public void testCost()
	{
		UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
		PlayerID superior = m_data.getPlayerList().getPlayerID("Superior");
		
		assertEquals(3, AIUtils.getCost(infantry, superior, m_data));
	}
}
