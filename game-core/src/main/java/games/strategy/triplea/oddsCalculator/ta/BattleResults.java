package games.strategy.triplea.oddsCalculator.ta;

import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;
import lombok.Getter;

public class BattleResults extends GameDataComponent {
  private static final long serialVersionUID = 1381361441940258702L;

  @Getter
  private final int battleRoundsFought;
  @Getter
  final List<Unit> remainingAttackingUnits;
  @Getter
  final List<Unit> remainingDefendingUnits;
  private final WhoWon m_whoWon;

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
    battleRoundsFought = battle.getBattleRound();
    remainingAttackingUnits = battle.getRemainingAttackingUnits();
    remainingDefendingUnits = battle.getRemainingDefendingUnits();
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
    battleRoundsFought = battle.getBattleRound();
    remainingAttackingUnits = battle.getRemainingAttackingUnits();
    remainingDefendingUnits = battle.getRemainingDefendingUnits();
    m_whoWon = scriptedWhoWon;
  }


  public int getAttackingCombatUnitsLeft() {
    return CollectionUtils.countMatches(remainingAttackingUnits, Matches.unitIsNotInfrastructure());
  }

  public int getDefendingCombatUnitsLeft() {
    return CollectionUtils.countMatches(remainingDefendingUnits, Matches.unitIsNotInfrastructure());
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
