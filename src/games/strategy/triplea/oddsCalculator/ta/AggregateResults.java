package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AggregateResults implements Serializable
{
	private static final long serialVersionUID = -556699626060414738L;
	private final List<BattleResults> m_results; // can be empty!
	private long m_time;
	
	public AggregateResults(final int expectedCount)
	{
		m_results = new ArrayList<BattleResults>(expectedCount);
	}
	
	public void addResult(final BattleResults result)
	{
		m_results.add(result);
	}
	
	public void addResults(final Collection<BattleResults> results)
	{
		m_results.addAll(results);
	}
	
	public List<BattleResults> getResults()
	{
		return m_results;
	}
	
	/**
	 * This could be null if we have zero results!
	 */
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
		return closestBattle; // can be null!
	}
	
	public List<Unit> GetAverageAttackingUnitsRemaining()
	{
		final BattleResults results = GetBattleResultsClosestToAverage(); // can be null!
		return results == null ? new ArrayList<Unit>() : results.getRemainingAttackingUnits();
	}
	
	public List<Unit> GetAverageDefendingUnitsRemaining()
	{
		final BattleResults results = GetBattleResultsClosestToAverage(); // can be null!
		return results == null ? new ArrayList<Unit>() : results.getRemainingDefendingUnits();
	}
	
	public double getAverageAttackingUnitsLeft()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getAttackingCombatUnitsLeft();
		}
		return count / m_results.size();
	}
	
	/**
	 * First is Attacker, Second is Defender
	 */
	public Tuple<Double, Double> getAverageTUVofUnitsLeftOver(final IntegerMap<UnitType> attackerCostsForTUV, final IntegerMap<UnitType> defenderCostsForTUV)
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return new Tuple<Double, Double>(0.0, 0.0);
		}
		double attackerTUV = 0;
		double defenderTUV = 0;
		for (final BattleResults result : m_results)
		{
			attackerTUV += BattleCalculator.getTUV(result.getRemainingAttackingUnits(), attackerCostsForTUV);
			defenderTUV += BattleCalculator.getTUV(result.getRemainingDefendingUnits(), defenderCostsForTUV);
		}
		return new Tuple<Double, Double>(attackerTUV / m_results.size(), defenderTUV / m_results.size());
	}
	
	public double getAverageTUVswing(final PlayerID attacker, final Collection<Unit> attackers, final PlayerID defender, final Collection<Unit> defenders, final GameData data)
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		final IntegerMap<UnitType> attackerCostsForTUV = BattleCalculator.getCostsForTUV(attacker, data);
		final IntegerMap<UnitType> defenderCostsForTUV = BattleCalculator.getCostsForTUV(defender, data);
		final int attackerTotalTUV = BattleCalculator.getTUV(attackers, attackerCostsForTUV);
		final int defenderTotalTUV = BattleCalculator.getTUV(defenders, defenderCostsForTUV);
		// could we possibly cause a bug by comparing UnitType's from one game data, to a different game data's UnitTypes?
		final Tuple<Double, Double> average = getAverageTUVofUnitsLeftOver(attackerCostsForTUV, defenderCostsForTUV);
		final double attackerLost = attackerTotalTUV - average.getFirst();
		final double defenderLost = defenderTotalTUV - average.getSecond();
		return defenderLost - attackerLost;
	}
	
	public double getAverageAttackingUnitsLeftWhenAttackerWon()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		double total = 0;
		for (final BattleResults result : m_results)
		{
			if (result.attackerWon())
			{
				count += result.getAttackingCombatUnitsLeft();
				total += 1;
			}
		}
		if (total <= 0)
			return 0;
		return count / total;
	}
	
	public double getAverageDefendingUnitsLeft()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getDefendingCombatUnitsLeft();
		}
		return count / m_results.size();
	}
	
	public double getAverageDefendingUnitsLeftWhenDefenderWon()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		double total = 0;
		for (final BattleResults result : m_results)
		{
			if (result.defenderWon())
			{
				count += result.getDefendingCombatUnitsLeft();
				total += 1;
			}
		}
		if (total <= 0)
			return 0;
		return count / total;
	}
	
	public double getAttackerWinPercent()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		for (final BattleResults result : m_results)
		{
			if (result.attackerWon())
				count++;
		}
		return count / m_results.size();
	}
	
	public double getDefenderWinPercent()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
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
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
		double count = 0;
		for (final BattleResults result : m_results)
		{
			count += result.getBattleRoundsFought();
		}
		if (count == 0)
			return 1.0; // If this is a 'fake' aggregate result, return 1.0
		return count / m_results.size();
	}
	
	public double getDrawPercent()
	{
		if (m_results.isEmpty()) // can be empty!
		{
			return 0.0;
		}
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
