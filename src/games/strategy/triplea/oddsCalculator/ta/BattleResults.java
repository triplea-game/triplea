package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.triplea.delegate.MustFightBattle;

import java.io.Serializable;

public class BattleResults implements Serializable
{
	private int m_attackingUnitsLeft;
	private int m_defendingUnitsLeft;
	private int m_battleRoundsFought;
	private MustFightBattle m_battle = null;
	
	public BattleResults()
	{
	}
	
	public BattleResults(final MustFightBattle battle)
	{
		m_attackingUnitsLeft = battle.getRemainingAttackingUnits().size();
		m_battleRoundsFought = battle.getBattleRound();
		m_defendingUnitsLeft = battle.getRemainingDefendingUnits().size();
		m_battle = battle;
	}
	
	public MustFightBattle GetBattle()
	{
		return m_battle;
	}
	
	public int getAttackingUnitsLeft()
	{
		return m_attackingUnitsLeft;
	}
	
	public int getDefendingUnitsLeft()
	{
		return m_defendingUnitsLeft;
	}
	
	public int getBattleRoundsFought()
	{
		return m_battleRoundsFought;
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
