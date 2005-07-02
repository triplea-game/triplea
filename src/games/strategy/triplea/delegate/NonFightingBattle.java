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
 * NonFightingBattle.java
 *
 * Created on November 23, 2001, 11:53 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.*;

import java.util.*;

/**
 * Battle in which no fighting occurs.  <b>
 * Example is a naval invasion into an empty country,
 * but the battle cannot be fought until a naval battle
 * occurs.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class NonFightingBattle implements Battle
{

	private Territory m_battleSite;
	private PlayerID m_attacker;
	private BattleTracker m_battleTracker;
	private GameData m_data;
	private TransportTracker m_transportTracker;
	private boolean m_isOver = false; 

	//dependent units
	//maps unit -> Collection of units
	//if unit is lost in a battle we are dependent on
	//then we lose the corresponding collection of units
	private Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();

	public NonFightingBattle(Territory battleSite, PlayerID attacker, BattleTracker battleTracker, boolean neutral, GameData data, TransportTracker transportTracker)
	{
		m_battleTracker = battleTracker;
		m_attacker = attacker;
		m_battleSite = battleSite;
		m_data = data;
		m_transportTracker = transportTracker;
	}

	public void fight(IDelegateBridge bridge)
	{
		if(!m_battleTracker.getDependentOn(this).isEmpty())
			throw new IllegalStateException("Must fight battles that this battle depends on first");

		//if any attacking non air units then win
		boolean someAttacking = hasAttackingUnits();
		if( someAttacking)
		{
			m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, m_data, null);
			m_battleTracker.addToConquered(m_battleSite);
		}
		m_battleTracker.removeBattle(this);
		m_isOver = true;
	}
	
	public boolean isOver()
	{
	    return m_isOver;
	}

    private boolean hasAttackingUnits()
    {
        CompositeMatch<Unit> attackingLand = new CompositeMatchAnd<Unit>();
              attackingLand.add(Matches.alliedUnit(m_attacker, m_data));
              attackingLand.add(Matches.UnitIsLand);
              boolean someAttacking = m_battleSite.getUnits().someMatch(attackingLand);
        return someAttacking;
    }

	public boolean isBombingRun()
	{
		return false;
	}

    public void removeAttack(Route route, Collection<Unit> units)
    {
        Iterator<Unit> dependents = m_dependentUnits.keySet().iterator();
        while(dependents.hasNext())
        {
            Unit dependence = dependents.next();
            Collection<Unit> dependent = m_dependentUnits.get(dependence);
            dependent.removeAll(units);
        }
    }

    public boolean isEmpty()
    {
        return !hasAttackingUnits();
    }

	public void addAttack(Route route, Collection<Unit> units)
	{
		Map<Unit, Collection<Unit>> addedTransporting = m_transportTracker.transporting(units);
		Iterator<Unit> iter = addedTransporting.keySet().iterator();
		while(iter.hasNext())
		{
			Unit unit = iter.next();
			if(m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll( addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
	}

	public Territory getTerritory()
	{
		return m_battleSite;
	}

	public void unitsLost(Battle battle, Collection units, IDelegateBridge bridge)
	{
		
		Collection<Unit> lost = getDependentUnits(units);
		if(lost.size() != 0)
		{
			Change change = ChangeFactory.removeUnits(m_battleSite, lost);
			bridge.addChange(change);

			String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
            bridge.getHistoryWriter().startEvent(transcriptText);
		}
	}

    public Collection<Unit> getDependentUnits(Collection units)
    {
        Collection<Unit> rVal = new ArrayList<Unit>();
        
        Iterator iter = units.iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			Collection<Unit> dependent = m_dependentUnits.get(unit);
			if(dependent != null)
				rVal.addAll(dependent);
		}
        return rVal;
    }

    public int hashCode()
    {
      return m_battleSite.hashCode();
    }

    public boolean equals(Object o)
    {
      //2 battles are equal if they are both the same type (boming or not)
      //and occur on the same territory
      //equals in the sense that they should never occupy the same Set
      //if these conditions are met
      if (o == null || ! (o instanceof Battle))
        return false;

      Battle other = (Battle) o;
      return other.getTerritory().equals(this.m_battleSite) &&
          other.isBombingRun() == this.isBombingRun();
    }

    /**
     * Add bombarding unit. Doesn't make sense here so just do
     * nothing.
     */
    public void addBombardingUnit(Unit unit) {
	// nothing
    }

    /**
     * Return whether battle is amphibious.
     */
    public boolean isAmphibious() {
	return false;
    }
    
    public int getBattleRound()
    {
        return 0;
    }

    
}
