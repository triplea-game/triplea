/*
 * NonFightingBattle.java
 *
 * Created on November 23, 2001, 11:53 AM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.Formatter;

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
	private boolean m_neutral;
	private GameData m_data;
	private TransportTracker m_transportTracker;

	//dependent units
	//maps unit -> Collection of units
	//if unit is lost in a battle we are dependent on
	//then we lose the corresponding collection of units
	private Map m_dependentUnits = new HashMap();
	
	public NonFightingBattle(Territory battleSite, PlayerID attacker, BattleTracker battleTracker, boolean neutral, GameData data, TransportTracker transportTracker) 
	{
		m_battleTracker = battleTracker;
		m_attacker = attacker;
		m_battleSite = battleSite;
		m_neutral = neutral;
		m_data = data;
		m_transportTracker = transportTracker;
	}
	
	public void fight(DelegateBridge bridge) 
	{
		if(!m_battleTracker.getDependentOn(this).isEmpty())
			throw new IllegalStateException("Must fight battles that this battle depends on first");
		
		//if any attacking non air units then win
		CompositeMatch attackingLand = new CompositeMatchAnd();
		attackingLand.add(Matches.alliedUnit(m_attacker, m_data));
		attackingLand.add(Matches.UnitIsLand);
		if( m_battleSite.getUnits().someMatch(attackingLand))
		{
			m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, m_data);	
			m_battleTracker.addToConquered(m_battleSite);
		}
		m_battleTracker.removeBattle(this);
	}
	
	public boolean isBombingRun() 
	{
		return false;
	}
	
	public void addAttack(Route route, Collection units) 
	{	
		Map addedTransporting = m_transportTracker.transporting(units);
		Iterator iter = addedTransporting.keySet().iterator();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			if(m_dependentUnits.get(unit) != null)
				((Collection) m_dependentUnits.get(unit)).addAll( (Collection) addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));	
		}
	}
	
	public Territory getTerritory() 
	{
		return m_battleSite;
	}
	
	public void unitsLost(Battle battle, Collection units, DelegateBridge bridge) 
	{
		Iterator iter = units.iterator();
		Collection lost = new ArrayList();
		while(iter.hasNext())
		{
			Unit unit = (Unit) iter.next();
			Collection dependent = (Collection) m_dependentUnits.get(unit);
			if(dependent != null)
				lost.addAll(dependent);
		}
		if(lost.size() != 0)
		{
			Change change = ChangeFactory.removeUnits(m_battleSite, lost);
			bridge.addChange(change);
			
			String transcriptText = Formatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
			bridge.getTranscript().write(transcriptText);
		}
	}
}