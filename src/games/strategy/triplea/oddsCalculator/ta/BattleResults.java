package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.io.Serializable;

public class BattleResults implements Serializable
{
	private static final long serialVersionUID = 1381361441940258702L;
	// private final int m_attackingUnitsLeft;
	// private final int m_defendingUnitsLeft;
	private final int m_attackingCombatUnitsLeft;
	private final int m_defendingCombatUnitsLeft;
	private final int m_battleRoundsFought;
	private final IBattle m_battle; // TODO: maybe this is too much memory overhead to keep this here?
	private WhoWon m_whoWon;
	
	/**
	 * This battle must have been fought. If fight() was not run on this battle, then the WhoWon will not have been set yet, which will give an error with this constructor.
	 * 
	 * @param battle
	 */
	public BattleResults(final IBattle battle)
	{
		// m_attackingUnitsLeft = battle.getRemainingAttackingUnits().size();
		m_attackingCombatUnitsLeft = Match.countMatches(battle.getRemainingAttackingUnits(), Matches.UnitIsDestructibleInCombatShort);
		// m_defendingUnitsLeft = battle.getRemainingDefendingUnits().size();
		m_defendingCombatUnitsLeft = Match.countMatches(battle.getRemainingDefendingUnits(), Matches.UnitIsDestructibleInCombatShort);
		m_battleRoundsFought = battle.getBattleRound();
		m_battle = battle;
		m_whoWon = battle.getWhoWon();
		if (m_whoWon == WhoWon.NOTFINISHED)
			throw new IllegalStateException("Battle not finished yet: " + m_battle);
	}
	
	/**
	 * This battle may or may not have been fought already. Use this for pre-setting the WhoWon flag.
	 * 
	 * @param battle
	 * @param scriptedWhoWon
	 */
	public BattleResults(final IBattle battle, final WhoWon scriptedWhoWon)
	{
		m_attackingCombatUnitsLeft = Match.countMatches(battle.getRemainingAttackingUnits(), Matches.UnitIsDestructibleInCombatShort);
		m_defendingCombatUnitsLeft = Match.countMatches(battle.getRemainingDefendingUnits(), Matches.UnitIsDestructibleInCombatShort);
		m_battleRoundsFought = battle.getBattleRound();
		m_battle = battle;
		m_whoWon = scriptedWhoWon;
	}
	
	public void setWhoWon(final WhoWon whoWon)
	{
		m_whoWon = whoWon;
	}
	
	public IBattle getBattle()
	{
		return m_battle;
	}
	
	/*public int getAttackingUnitsLeft()
	{
		return m_attackingUnitsLeft;
	}
	
	public int getDefendingUnitsLeft()
	{
		return m_defendingUnitsLeft;
	}*/

	public int getAttackingCombatUnitsLeft()
	{
		return m_attackingCombatUnitsLeft;
	}
	
	public int getDefendingCombatUnitsLeft()
	{
		return m_defendingCombatUnitsLeft;
	}
	
	public int getBattleRoundsFought()
	{
		return m_battleRoundsFought;
	}
	
	// These could easily screw up an AI into thinking it has won when it really hasn't. Must make sure we only count combat units that can die.
	public boolean attackerWon()
	{
		// return !draw() && m_attackingCombatUnitsLeft > 0;
		return !draw() && m_whoWon == WhoWon.ATTACKER;
	}
	
	public boolean defenderWon()
	{
		// if noone is left, it is considered a draw, even if m_whoWon says defender.
		// return !draw() && m_defendingCombatUnitsLeft > 0;
		return !draw() && m_whoWon == WhoWon.DEFENDER;
	}
	
	public boolean draw()
	{
		// technically the defender wins if there is noone left. However, most people using the battle calc consider that to be a "draw", so we should check and see if there is noone left.
		// return (m_attackingCombatUnitsLeft == 0 && m_defendingCombatUnitsLeft == 0) || (m_attackingCombatUnitsLeft > 0 && m_defendingCombatUnitsLeft > 0);
		return (m_attackingCombatUnitsLeft == 0 && m_defendingCombatUnitsLeft == 0) || (m_whoWon != WhoWon.ATTACKER && m_whoWon != WhoWon.DEFENDER);
	}
}
