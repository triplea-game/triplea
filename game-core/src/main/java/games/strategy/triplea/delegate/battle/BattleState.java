package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  int getBattleRound();

  Territory getBattleSite();

  Collection<Unit> getAttackingUnits();

  Collection<Unit> getDefendingUnits();

  Collection<Unit> getOffensiveAa();

  Collection<Unit> getDefendingAa();

  Collection<Unit> getBombardingUnits();

  GamePlayer getAttacker();

  GamePlayer getDefender();
}
