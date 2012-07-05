package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.List;

public class BattleResults extends GameDataComponent implements Serializable
{
	private static final long serialVersionUID = 1381361441940258702L;
	// private final int m_attackingUnitsLeft;
	// private final int m_defendingUnitsLeft;
	// private final int m_attackingCombatUnitsLeft;
	// private final int m_defendingCombatUnitsLeft;
	private final int m_battleRoundsFought;
	final List<Unit> m_remainingAttackingUnits;
	final List<Unit> m_remainingDefendingUnits;
	private WhoWon m_whoWon;
	
	// FYI: do not save the battle in BattleResults. It is both too much memory overhead, and also causes problems with BattleResults being saved into BattleRecords
	
	/**
	 * This battle must have been fought. If fight() was not run on this battle, then the WhoWon will not have been set yet, which will give an error with this constructor.
	 * 
	 * @param battle
	 */
	public BattleResults(final IBattle battle, final GameData data)
	{
		super(data);
		// m_attackingUnitsLeft = battle.getRemainingAttackingUnits().size();
		// m_attackingCombatUnitsLeft = Match.countMatches(battle.getRemainingAttackingUnits(), Matches.UnitIsDestructibleInCombatShort);
		// m_defendingUnitsLeft = battle.getRemainingDefendingUnits().size();
		// m_defendingCombatUnitsLeft = Match.countMatches(battle.getRemainingDefendingUnits(), Matches.UnitIsDestructibleInCombatShort);
		m_battleRoundsFought = battle.getBattleRound();
		m_remainingAttackingUnits = battle.getRemainingAttackingUnits();
		m_remainingDefendingUnits = battle.getRemainingDefendingUnits();
		m_whoWon = battle.getWhoWon();
		if (m_whoWon == WhoWon.NOTFINISHED)
			throw new IllegalStateException("Battle not finished yet: " + battle);
	}
	
	/**
	 * This battle may or may not have been fought already. Use this for pre-setting the WhoWon flag.
	 * 
	 * @param battle
	 * @param scriptedWhoWon
	 */
	public BattleResults(final IBattle battle, final WhoWon scriptedWhoWon, final GameData data)
	{
		super(data);
		// m_attackingCombatUnitsLeft = Match.countMatches(battle.getRemainingAttackingUnits(), Matches.UnitIsDestructibleInCombatShort);
		// m_defendingCombatUnitsLeft = Match.countMatches(battle.getRemainingDefendingUnits(), Matches.UnitIsDestructibleInCombatShort);
		m_battleRoundsFought = battle.getBattleRound();
		m_remainingAttackingUnits = battle.getRemainingAttackingUnits();
		m_remainingDefendingUnits = battle.getRemainingDefendingUnits();
		m_whoWon = scriptedWhoWon;
	}
	
	public void setWhoWon(final WhoWon whoWon)
	{
		m_whoWon = whoWon;
	}
	
	/*public int getAttackingUnitsLeft()
	{
		return m_attackingUnitsLeft;
	}
	
	public int getDefendingUnitsLeft()
	{
		return m_defendingUnitsLeft;
	}*/

	public List<Unit> getRemainingAttackingUnits()
	{
		return m_remainingAttackingUnits;
	}
	
	public List<Unit> getRemainingDefendingUnits()
	{
		return m_remainingDefendingUnits;
	}
	
	public List<Unit> getRemainingAttackingCombatUnits()
	{
		return Match.getMatches(m_remainingAttackingUnits, Matches.UnitIsNotInfrastructure);
	}
	
	public List<Unit> getRemainingDefendingCombatUnits()
	{
		return Match.getMatches(m_remainingDefendingUnits, Matches.UnitIsNotInfrastructure);
	}
	
	public int getAttackingCombatUnitsLeft()
	{
		return Match.countMatches(m_remainingAttackingUnits, Matches.UnitIsNotInfrastructure);
	}
	
	public int getDefendingCombatUnitsLeft()
	{
		return Match.countMatches(m_remainingDefendingUnits, Matches.UnitIsNotInfrastructure);
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
		return (m_whoWon != WhoWon.ATTACKER && m_whoWon != WhoWon.DEFENDER) || (getAttackingCombatUnitsLeft() == 0 && getDefendingCombatUnitsLeft() == 0);
	}
}
