package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.MustFightBattle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AggregateResults implements Serializable
{
    public List<BattleResults> m_results;
    private long m_time;

    public AggregateResults(int expectedCount)
    {
        m_results = new ArrayList<BattleResults>(expectedCount);
    }

    public void addResult(BattleResults result)
    {
        m_results.add(result);
    }

    public double getAttackerWinPercent()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            if(result.attackerWon())
                count++;
        }

        return  count / m_results.size() ;
    }
    public BattleResults GetBattleResultsClosestToAverage()
    {
        double closestBattleDif = Integer.MAX_VALUE;
        BattleResults closestBattle = null;
        for (BattleResults results : m_results)
        {
            double dif = (Math.max(results.getAttackingUnitsLeft(), getAverageAttackingUnitsLeft()) - Math.min(results.getAttackingUnitsLeft(), getAverageAttackingUnitsLeft()));
            dif += (Math.max(results.getDefendingUnitsLeft(), getAverageDefendingUnitsLeft()) - Math.min(results.getDefendingUnitsLeft(), getAverageDefendingUnitsLeft()));
            if (dif < closestBattleDif)
            {
                closestBattleDif = dif;
                closestBattle = results;
            }
        }
        return closestBattle;
    }
    public MustFightBattle GetBattleClosestToAverage()
    {
        BattleResults battleR = GetBattleResultsClosestToAverage();
        if(battleR == null)
            return null;
        return battleR.GetBattle();
    }
    public List<Unit> GetAverageAttackingUnitsRemaining()
    {
        MustFightBattle battle = GetBattleClosestToAverage();
        if(battle == null)
            return null;
        return battle.getRemainingAttackingUnits();
    }
    public List<Unit> GetAverageDefendingUnitsRemaining()
    {
        MustFightBattle battle = GetBattleClosestToAverage();
        if(battle == null)
            return null;
        return battle.getRemainingDefendingUnits();
    }

    public double getAverageAttackingUnitsLeft()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            count += result.getAttackingUnitsLeft();
        }

        return  count / m_results.size() ;

    }

    public double getAverageDefendingUnitsLeft()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            count += result.getDefendingUnitsLeft();
        }

        return  count / m_results.size() ;

    }



    public double getDefenderWinPercent()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            if(result.defenderWon())
                count++;
        }

        return  count / m_results.size() ;
    }

    public double getAverageBattleRoundsFought()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            count += result.getBattleRoundsFought();
        }

        return  count / m_results.size() ;
    }

    public double getDrawPercent()
    {
        double count = 0;
        for(BattleResults result : m_results)
        {
            if(result.draw())
                count++;
        }

        return  count / m_results.size() ;
    }

    public int getRollCount()
    {
        return m_results.size();
    }

    public long getTime()
    {
        return m_time;
    }

    public void setTime(long time)
    {
        m_time = time;
    }



}
