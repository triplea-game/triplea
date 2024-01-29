package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.IBattle.WhoWon;
import java.util.Collection;
import lombok.Getter;

/** The results of an in-progress or complete battle. */
public class BattleResults extends GameDataComponent {
  private static final long serialVersionUID = 1L;

  @Getter private final int battleRoundsFought;
  @Getter private final Collection<Unit> remainingAttackingUnits;
  @Getter private final Collection<Unit> remainingDefendingUnits;
  private final WhoWon whoWon;

  // FYI: do not save the battle in BattleResults. It is both too much memory overhead, and also
  // causes problems with
  // BattleResults being saved into BattleRecords
  /**
   * This battle must have been fought. If fight() was not run on this battle, then the WhoWon will
   * not have been set yet, which will give an error with this constructor.
   */
  public BattleResults(final IBattle battle, final GameData data) {
    super(data);
    battleRoundsFought = battle.getBattleRound();
    remainingAttackingUnits = battle.getRemainingAttackingUnits();
    remainingDefendingUnits = battle.getRemainingDefendingUnits();
    whoWon = battle.getWhoWon();
    if (whoWon == WhoWon.NOT_FINISHED) {
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
    whoWon = scriptedWhoWon;
  }

  // These could easily screw up an AI into thinking it has won when it really hasn't. Must make
  // sure we only count
  // combat units that can die.
  public boolean attackerWon() {
    return !draw() && whoWon == WhoWon.ATTACKER;
  }

  public boolean defenderWon() {
    // if no one is left, it is considered a draw, even if whoWon says defender.
    return !draw() && whoWon == WhoWon.DEFENDER;
  }

  public boolean draw() {
    return (whoWon != WhoWon.ATTACKER && whoWon != WhoWon.DEFENDER)
        || (getRemainingAttackingUnits().isEmpty() && getRemainingDefendingUnits().isEmpty());
  }
}
