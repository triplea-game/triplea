package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

/**
 * Battle with possible dependencies
 * Includes MustFightBattle and NonFightingBattle.
 */
public abstract class DependentBattle extends AbstractBattle {
  private static final long serialVersionUID = 9119442509652443015L;

  DependentBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final GameData data) {
    super(battleSite, attacker, battleTracker, false, BattleType.NORMAL, data);
  }

  /**
   * Return attacking from Collection.
   */
  public abstract Collection<Territory> getAttackingFrom();

  /**
   * Return attacking from Map.
   */
  public abstract Map<Territory, Collection<Unit>> getAttackingFromMap();

  /**
   * @return territories where there are amphibious attacks.
   */
  public abstract Collection<Territory> getAmphibiousAttackTerritories();
}
