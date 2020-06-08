package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;

/** Exposes the battle state and allows updates to it */
public interface BattleState {

  Integer getRound();

  Territory getBattleSite();

  Collection<Unit> getAttackingUnits();

  Collection<Unit> getDefendingUnits();

  Collection<Unit> getBombardingUnits();
}
