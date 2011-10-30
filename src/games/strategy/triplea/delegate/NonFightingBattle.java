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
 * NonFightingBattle.java
 * 
 * Created on November 23, 2001, 11:53 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Iterator;

/**
 * Battle in which no fighting occurs. <b>
 * Example is a naval invasion into an empty country,
 * but the battle cannot be fought until a naval battle
 * occurs.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
@SuppressWarnings("serial")
public class NonFightingBattle extends AbstractBattle
{
	
	public NonFightingBattle(Territory battleSite, PlayerID attacker, BattleTracker battleTracker, boolean neutral, GameData data)
	{
		super(battleSite, attacker, battleTracker, data);
	}
	
	@Override
	public void fight(IDelegateBridge bridge)
	{
		if (!m_battleTracker.getDependentOn(this).isEmpty())
			throw new IllegalStateException("Must fight battles that this battle depends on first");
		
		// if any attacking non air units then win
		boolean someAttacking = hasAttackingUnits();
		if (someAttacking)
		{
			m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, null, null);
			m_battleTracker.addToConquered(m_battleSite);
		}
		end();
	}
	
	private void end()
	{
		m_battleTracker.removeBattle(this);
		m_isOver = true;
	}
	
	boolean hasAttackingUnits()
	{
		CompositeMatch<Unit> attackingLand = new CompositeMatchAnd<Unit>();
		attackingLand.add(Matches.alliedUnit(m_attacker, m_data));
		attackingLand.add(Matches.UnitIsLand);
		boolean someAttacking = m_battleSite.getUnits().someMatch(attackingLand);
		return someAttacking;
	}
	
	@Override
	public final boolean isBombingRun()
	{
		return false;
	}
	
	@Override
	public void removeAttack(Route route, Collection<Unit> units)
	{
		Iterator<Unit> dependents = m_dependentUnits.keySet().iterator();
		while (dependents.hasNext())
		{
			Unit dependence = dependents.next();
			Collection<Unit> dependent = m_dependentUnits.get(dependence);
			dependent.removeAll(units);
		}
	}
	
	@Override
	public int hashCode()
	{
		return m_battleSite.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		// 2 battles are equal if they are both the same type (bombing or not)
		// and occur on the same territory
		// equals in the sense that they should never occupy the same Set
		// if these conditions are met
		if (o == null || !(o instanceof IBattle))
			return false;
		
		IBattle other = (IBattle) o;
		return other.getTerritory().equals(this.m_battleSite) &&
					other.isBombingRun() == this.isBombingRun();
	}
	
	@Override
	public boolean isEmpty()
	{
		return !hasAttackingUnits();
	}
	
	@Override
	public void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge)
	{
		Collection<Unit> lost = getDependentUnits(units);
		lost = Match.getMatches(lost, Matches.unitIsInTerritory(m_battleSite));
		if (lost.size() != 0)
		{
			Change change = ChangeFactory.removeUnits(m_battleSite, lost);
			bridge.addChange(change);
			
			String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
			bridge.getHistoryWriter().startEvent(transcriptText);
		}
	}
}
