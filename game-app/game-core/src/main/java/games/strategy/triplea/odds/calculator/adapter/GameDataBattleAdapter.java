package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.Force;
import java.util.Collection;

/**
 * The anti-corruption layer, living outside the engine-free calc core — the one seam that imports
 * {@code games.strategy.engine.data.*} (design §9, invariant 4). Bakes a fully self-contained
 * {@link BattleScenario} in, and maps survivor counts back to representative units for the UI out.
 */
public class GameDataBattleAdapter {

  /**
   * Bakes a self-contained {@link BattleScenario}; Phase 1 derives its {@code attackerOrder}/{@code
   * defenderOrder} from {@code options}' order-of-loss lists.
   */
  public BattleScenario toScenario(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> effects,
      final BattleOptions options) {
    throw new UnsupportedOperationException("phase 1");
  }

  /** Fungible by profile incl. damage, so any units matching the survivor counts will do. */
  public Collection<Unit> toRepresentativeUnits(final Force survivors, final GameData data) {
    throw new UnsupportedOperationException("phase 1");
  }
}
