package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.delegate.dataObjects.BattleRecords.BattleResultDescription;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.util.IntegerMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A sort of scripted battle made for blitzed/conquered territories without a fight.
 * TODO: expand to cover all possible scripting battle needs.
 * 
 * @author veqryn
 * 
 */
public class FinishedBattle extends AbstractBattle
{
	private static final long serialVersionUID = -5852495231826940879L;
	
	public FinishedBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final boolean isBombingRun, final BattleType battleType, final GameData data,
				final BattleResultDescription battleResultDescription, final WhoWon whoWon, final Collection<Unit> attackingUnits)
	{
		super(battleSite, attacker, battleTracker, isBombingRun, battleType, data);
		m_battleResultDescription = battleResultDescription;
		m_whoWon = whoWon;
		m_attackingUnits.addAll(attackingUnits);
	}
	
	public void setDefendingUnits(final List<Unit> defendingUnits)
	{
		m_defendingUnits = defendingUnits;
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty();
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		if (!m_headless)
			m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription, new BattleResults(this), 0);
		m_battleTracker.removeBattle(this);
		m_isOver = true;
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
		m_attackingUnits.addAll(units);
		return ChangeFactory.EMPTY_CHANGE;
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
		m_attackingUnits.removeAll(units);
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge)
	{
		final Collection<Unit> lost = getDependentUnits(units);
		if (lost.size() != 0)
		{
			m_attackingUnits.removeAll(lost);
			/* TODO: these units are no longer in this territory, most probably.  Plus they may have already been removed by another "real" battle class.
			final String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
			bridge.getHistoryWriter().startEvent(transcriptText);
			final Change change = ChangeFactory.removeUnits(m_battleSite, lost);
			bridge.addChange(change);*/
			if (m_attackingUnits.isEmpty())
			{
				final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
				final int tuvLostAttacker = BattleCalculator.getTUV(lost, m_attacker, costs, m_data);
				m_attackerLostTUV += tuvLostAttacker;
				if (!m_headless)
					m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, BattleRecords.BattleResultDescription.LOST,
								new BattleResults(this), 0);
				m_battleTracker.removeBattle(this);
			}
		}
	}
	
}
