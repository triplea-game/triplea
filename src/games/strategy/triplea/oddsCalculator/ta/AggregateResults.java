package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AggregateResults implements Serializable
{
	private static final long serialVersionUID = -556699626060414738L;
	public List<BattleResults> m_results;
	private long m_time;
	
	public AggregateResults(final int expectedCount)
	{
		m_results = new ArrayList<BattleResults>(expectedCount);
	}
	
	public void addResult(final BattleResults result)
	{
		m_results.add(result);
	}
	
	public double getAttackerWinPercent()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			if (result.attackerWon())
				count++;
		}
		return count / m_results.size();
	}
	
	public BattleResults GetBattleResultsClosestToAverage()
	{
		float closestBattleDif = Integer.MAX_VALUE;
		BattleResults closestBattle = null;
		for (final BattleResults results : m_results)
		{
			float dif = DUtils.MNN((float) (results.getAttackingCombatUnitsLeft() - getAverageAttackingUnitsLeft()));
			dif += DUtils.MNN((float) (results.getDefendingCombatUnitsLeft() - getAverageDefendingUnitsLeft()));
			if (dif < closestBattleDif)
			{
				closestBattleDif = dif;
				closestBattle = results;
			}
		}
		return closestBattle;
	}
	
	public List<Unit> GetAverageAttackingUnitsRemaining()
	{
		return GetBattleResultsClosestToAverage().getRemainingAttackingUnits();
	}
	
	public List<Unit> GetAverageDefendingUnitsRemaining()
	{
		return GetBattleResultsClosestToAverage().getRemainingDefendingUnits();
	}
	
	public double getAverageAttackingUnitsLeft()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getAttackingCombatUnitsLeft();
		}
		return count / m_results.size();
	}
	
	public double getAverageDefendingUnitsLeft()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getDefendingCombatUnitsLeft();
		}
		return count / m_results.size();
	}
	
	public double getDefenderWinPercent()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			if (result.defenderWon())
				count++;
		}
		return count / m_results.size();
	}
	
	public double getAverageBattleRoundsFought()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getBattleRoundsFought();
		}
		if (m_results.isEmpty() || count == 0)
			return 1.0F; // If this is a 'fake' aggregate result, return 1.0
		return count / m_results.size();
	}
	
	public double getDrawPercent()
	{
		double count = 0;
		for (final BattleResults result : m_results)
		{
			if (result.draw())
				count++;
		}
		return count / m_results.size();
	}
	
	public int getRollCount()
	{
		return m_results.size();
	}
	
	public long getTime()
	{
		return m_time;
	}
	
	public void setTime(final long time)
	{
		m_time = time;
	}
}
