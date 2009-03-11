package games.strategy.triplea.oddsCalculator.ta;

import java.io.Serializable;

import games.strategy.triplea.delegate.MustFightBattle;

public class BattleResults implements Serializable
{
    private final int m_attackingUnitsLeft;
    private final int m_defendingUnitsLeft;
    
    
    public BattleResults(MustFightBattle battle)
    {
        m_attackingUnitsLeft = battle.getRemainingAttackingUnits().size();
        m_defendingUnitsLeft = battle.getRemainingDefendingUnits().size();
    }

    

    public int getAttackingUnitsLeft()
    {
        return m_attackingUnitsLeft;
    }


    public int getDefendingUnitsLeft()
    {
        return m_defendingUnitsLeft;
    }
    
    public boolean attackerWon()
    {
        return !draw() && m_attackingUnitsLeft > 0;
    }
    
    public boolean defenderWon()
    {
        return !draw() && m_defendingUnitsLeft > 0;
    }
    
    public boolean draw()
    {
        return m_attackingUnitsLeft == 0 && m_defendingUnitsLeft == 0;
    }
    
}
