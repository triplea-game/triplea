package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Interactive adapter over one battle restored from a TripleA save game. */
final class LoadedBattleScenario implements BattleScenario {
  private final GameData gameData;
  private final IBattle battle;
  private final BattleState battleState;
  private final long seed;
  private final BattleDecisionController decisionController = new BattleDecisionController();
  private final SimulationDelegateBridge bridge;

  LoadedBattleScenario(final GameData gameData, final IBattle battle, final long seed) {
    this.gameData = Objects.requireNonNull(gameData);
    this.battle = Objects.requireNonNull(battle);
    if (!(battle instanceof BattleState state)) {
      throw new IllegalArgumentException(
          "battle does not expose BattleState: " + battle.getClass());
    }
    battleState = state;
    this.seed = seed;
    initializeClientSettingsIfNeeded();
    bridge = new SimulationDelegateBridge(gameData, battle.getAttacker(), decisionController, seed);
    advanceUntilDecision();
  }

  @Override
  public BattleObservation observation() {
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      return BattleObservationFactory.create(battleState, seed, decisionController.observation());
    }
  }

  @Override
  public List<BattleAction> legalActions() {
    return decisionController.legalActions();
  }

  @Override
  public boolean isLegalAction(final BattleAction action) {
    return decisionController.isLegalAction(action);
  }

  @Override
  public BattleScenarioStep step(final BattleAction action) {
    final BattleDecisionType resolvedDecision = decisionController.observation().type();
    decisionController.submit(action);
    advanceUntilDecision();
    return new BattleScenarioStep(
        0,
        false,
        Map.of(
            "nextDecision", decisionController.observation().type().name(),
            "resolvedDecision", resolvedDecision.name()));
  }

  private void advanceUntilDecision() {
    if (battle.isOver()) {
      return;
    }
    try {
      battle.fight(bridge);
    } catch (final BattleDecisionRequiredException expected) {
      return;
    }
    if (!battle.isOver() && decisionController.observation().type() == BattleDecisionType.NONE) {
      throw new IllegalStateException(
          "battle execution returned without ending or requesting a decision");
    }
  }

  private static void initializeClientSettingsIfNeeded() {
    try {
      ClientSetting.useWebsocketNetwork.getValue();
    } catch (final IllegalStateException uninitialized) {
      ClientSetting.initialize();
    }
  }
}
