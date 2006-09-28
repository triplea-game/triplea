package games.strategy.triplea.oddsCalculator.ta;

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
    
    public double getAverageAttackingUnits()
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
