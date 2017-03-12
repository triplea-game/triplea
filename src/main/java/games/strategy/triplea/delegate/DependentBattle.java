package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

/**
 * Battle with possible dependencies
 * Includes MustFightBattle and NonFightingBattle
 */
abstract public class DependentBattle extends AbstractBattle {
  private static final long serialVersionUID = 9119442509652443015L;

  DependentBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final boolean isBombingRun, final BattleType battleType, final GameData data) {
    super(battleSite, attacker, battleTracker, isBombingRun, battleType, data);
  }

  abstract public Collection<Territory> getAttackingFrom();

  abstract public Map<Territory, Collection<Unit>> getAttackingFromMap();
}
