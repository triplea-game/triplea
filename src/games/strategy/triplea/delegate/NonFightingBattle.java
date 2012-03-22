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
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Battle in which no fighting occurs. <b>
 * Example is a naval invasion into an empty country,
 * but the battle cannot be fought until a naval battle
 * occurs.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class NonFightingBattle extends AbstractBattle
{
	private static final long serialVersionUID = -1699534010648145123L;
	
	public NonFightingBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final GameData data)
	{
		super(battleSite, attacker, battleTracker, false, BattleType.NORMAL, data);
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		final Map<Unit, Collection<Unit>> addedTransporting = new TransportTracker().transporting(units);
		for (final Unit unit : addedTransporting.keySet())
		{
			if (m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		if (!m_battleTracker.getDependentOn(this).isEmpty())
			throw new IllegalStateException("Must fight battles that this battle depends on first");
		// if any attacking non air units then win
		final boolean someAttacking = hasAttackingUnits();
		if (someAttacking)
		{
			m_battleResultDescription = BattleRecords.BattleResultDescription.BLITZED;
			m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, null, null);
			m_battleTracker.addToConquered(m_battleSite);
		}
		else
		{
			m_battleResultDescription = BattleRecords.BattleResultDescription.LOST;
		}
		m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription, new BattleResults(this), 0);
		end();
	}
	
	private void end()
	{
		m_battleTracker.removeBattle(this);
		m_isOver = true;
	}
	
	boolean hasAttackingUnits()
	{
		final CompositeMatch<Unit> attackingLand = new CompositeMatchAnd<Unit>();
		attackingLand.add(Matches.alliedUnit(m_attacker, m_data));
		attackingLand.add(Matches.UnitIsLand);
		final boolean someAttacking = m_battleSite.getUnits().someMatch(attackingLand);
		return someAttacking;
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
	{
		final Iterator<Unit> dependents = m_dependentUnits.keySet().iterator();
		while (dependents.hasNext())
		{
			final Unit dependence = dependents.next();
			final Collection<Unit> dependent = m_dependentUnits.get(dependence);
			dependent.removeAll(units);
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return !hasAttackingUnits();
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge)
	{
		Collection<Unit> lost = getDependentUnits(units);
		lost = Match.getMatches(lost, Matches.unitIsInTerritory(m_battleSite));
		if (lost.size() != 0)
		{
			final String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
			bridge.getHistoryWriter().startEvent(transcriptText);
			final Change change = ChangeFactory.removeUnits(m_battleSite, lost);
			bridge.addChange(change);
		}
	}
}
