package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Objects;

/** Read-only adapter over one battle restored from a TripleA save game. */
final class LoadedBattleScenario implements BattleScenario {
  private final GameData gameData;
  private final BattleState battleState;
  private final long seed;

  LoadedBattleScenario(final GameData gameData, final BattleState battleState, final long seed) {
    this.gameData = Objects.requireNonNull(gameData);
    this.battleState = Objects.requireNonNull(battleState);
    this.seed = seed;
  }

  @Override
  public BattleObservation observation() {
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      return BattleObservationFactory.create(battleState, seed);
    }
  }

  @Override
  public List<BattleAction> legalActions() {
    return List.of();
  }

  @Override
  public BattleScenarioStep step(final BattleAction action) {
    throw new UnsupportedOperationException(
        "battle decision hooks are not installed yet; reset and observation are available");
  }
}
