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
 * StrategicBombingRaidBattle.java
 *
 * Created on November 29, 2001, 2:21 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle implements Battle
{
	private Territory m_battleSite;
	private List m_units = new ArrayList();
	private PlayerID m_defender;
	private PlayerID m_attacker;
	private GameData m_data;
	private BattleTracker m_tracker;
	
	/** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker) 
	{
		m_battleSite = territory;
		m_data = data;
		m_attacker = attacker;
		m_defender = defender;
		m_tracker = tracker;
    }

	public void addAttack(Route route, Collection units) 
	{
		if(!Match.allMatch(units, Matches.UnitIsStrategicBomber))
			throw new IllegalArgumentException("Non bombers added to strategic bombing raid:" + units);
		
		m_units.addAll(units);
		
		//TODO, add dependencies in case of land attack in same territory
	}
	
	public void fight(DelegateBridge bridge) 
	{
		//sort according to least movement
		MoveDelegate moveDelegate = DelegateFinder.moveDelegate(m_data);
		moveDelegate.sortAccordingToMovementLeft(m_units, false);
		
		CompositeMatch hasAA = new CompositeMatchAnd();
		hasAA.add(Matches.UnitIsAA);
		hasAA.add(Matches.enemyUnit(m_attacker, m_data));
		if(m_battleSite.getUnits().someMatch(hasAA))
			fireAA(bridge);
		
		conductRaid(bridge);
		
		m_tracker.removeBattle(this);
	}

	private void fireAA(DelegateBridge bridge)
	{
		int hits = BattleCalculator.getAAHits(m_units, bridge);
		if(hits == 0)
			bridge.sendMessage(new StringMessage("No AA hits in " + m_battleSite.getName(), false));
		else
			removeAAHits(bridge, hits);	
	}
	
	private void removeAAHits(DelegateBridge bridge, int hits)
	{	
		String text = hits + " hits from AA fire";
		Collection casualties = BattleCalculator.selectCasualties(m_attacker, m_units,hits,bridge, text, m_data);
		m_units.removeAll(casualties);
		Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
		bridge.addChange(remove);
		
		String transcriptText = Formatter.unitsToText(casualties) + " lost in bombing raid in " + m_battleSite.getName();
		bridge.getTranscript().write(transcriptText);
	}

	private void conductRaid(DelegateBridge bridge)
	{	
		int rollCount = BattleCalculator.getRolls(m_units, m_attacker, false); 
		int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount);
		int cost = 0;
		
		for(int i = 0; i < dice.length; i++)
		{
			cost += 1 + dice[i];
		}
		bridge.sendMessage(new StringMessage("Bombing raid costs " + cost + " IPCS", false));
		
		//get resources 
		Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
		int have = m_defender.getResources().getQuantity(ipcs);
		int toRemove = Math.min(cost, have);
		Change change = ChangeFactory.changeResourcesChange(m_defender, ipcs, -toRemove);
		bridge.addChange(change);
		
		String transcriptText = "Bombing raid by " + m_attacker.getName() + " costs " + cost + " ipcs for " +  m_defender.getName();
		bridge.getTranscript().write(transcriptText);	
	}
	
	public boolean isBombingRun() 
	{
		return true;
	}
	
	public void unitsLost(Battle battle, Collection units, DelegateBridge bridge) 
	{
		//should never happen
		throw new IllegalStateException("say what, why you telling me that");
	}
	
	public Territory getTerritory() 
	{
		return m_battleSite;
	}
}
