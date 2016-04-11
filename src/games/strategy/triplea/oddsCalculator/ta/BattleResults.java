package games.strategy.triplea.oddsCalculator.ta;

import java.io.Serializable;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

public class BattleResults extends GameDataComponent implements Serializable {
  private static final long serialVersionUID = 1381361441940258702L;

  private final int m_battleRoundsFought;
  final List<Unit> m_remainingAttackingUnits;
  final List<Unit> m_remainingDefendingUnits;
  private WhoWon m_whoWon;

  // FYI: do not save the battle in BattleResults. It is both too much memory overhead, and also causes problems with
  // BattleResults being
  // saved into BattleRecords
  /**
   * This battle must have been fought. If fight() was not run on this battle, then the WhoWon will not have been set
   * yet, which will give
   * an error with this constructor.
   */
  public BattleResults(final IBattle battle, final GameData data) {
    super(data);
    m_battleRoundsFought = battle.getBattleRound();
    m_remainingAttackingUnits = battle.getRemainingAttackingUnits();
    m_remainingDefendingUnits = battle.getRemainingDefendingUnits();
    m_whoWon = battle.getWhoWon();
    if (m_whoWon == WhoWon.NOTFINISHED) {
      throw new IllegalStateException("Battle not finished yet: " + battle);
    }
  }

  /**
   * This battle may or may not have been fought already. Use this for pre-setting the WhoWon flag.
   */
  public BattleResults(final IBattle battle, final WhoWon scriptedWhoWon, final GameData data) {
    super(data);
    m_battleRoundsFought = battle.getBattleRound();
    m_remainingAttackingUnits = battle.getRemainingAttackingUnits();
    m_remainingDefendingUnits = battle.getRemainingDefendingUnits();
    m_whoWon = scriptedWhoWon;
  }

  public void setWhoWon(final WhoWon whoWon) {
    m_whoWon = whoWon;
  }

  public List<Unit> getRemainingAttackingUnits() {
    return m_remainingAttackingUnits;
  }

  public List<Unit> getRemainingDefendingUnits() {
    return m_remainingDefendingUnits;
  }

  public List<Unit> getRemainingAttackingCombatUnits() {
    return Match.getMatches(m_remainingAttackingUnits, Matches.UnitIsNotInfrastructure);
  }

  public List<Unit> getRemainingDefendingCombatUnits() {
    return Match.getMatches(m_remainingDefendingUnits, Matches.UnitIsNotInfrastructure);
  }

  public int getAttackingCombatUnitsLeft() {
    return Match.countMatches(m_remainingAttackingUnits, Matches.UnitIsNotInfrastructure);
  }

  public int getDefendingCombatUnitsLeft() {
    return Match.countMatches(m_remainingDefendingUnits, Matches.UnitIsNotInfrastructure);
  }

  public int getBattleRoundsFought() {
    return m_battleRoundsFought;
  }

  // These could easily screw up an AI into thinking it has won when it really hasn't. Must make sure we only count
  // combat units that can
  // die.
  public boolean attackerWon() {
    return !draw() && m_whoWon == WhoWon.ATTACKER;
  }

  public boolean defenderWon() {
    // if noone is left, it is considered a draw, even if m_whoWon says defender.
    return !draw() && m_whoWon == WhoWon.DEFENDER;
  }

  public boolean draw() {
    return (m_whoWon != WhoWon.ATTACKER && m_whoWon != WhoWon.DEFENDER)
        || (getAttackingCombatUnitsLeft() == 0 && getDefendingCombatUnitsLeft() == 0);
  }
}
