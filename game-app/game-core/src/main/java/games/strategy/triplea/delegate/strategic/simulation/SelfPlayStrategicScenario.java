package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.battle.simulation.SimulationDelegateBridge;
import games.strategy.triplea.delegate.scoring.SmallFrontScoringService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Chains every player's turn over one game so a whole game is a single episode.
 *
 * <p>Turns are chained in memory rather than through save files: {@link LoadedStrategicScenario}
 * binds a turn to a {@link GameData}, so the next turn is just another scenario built on the same
 * data. Nothing is serialised between turns.
 *
 * <p>Where a single-turn episode stops at COMPLETE, this one runs the player's end-turn step, then
 * hands over to the next player, running the game's end-round step whenever the turn order wraps.
 * That end-round step is what scores the position and ends the game, so {@code over()} here means
 * the game is over rather than the turn.
 */
public final class SelfPlayStrategicScenario implements StrategicScenario {
  private final GameData data;
  private final long seed;
  private final int maxActions;
  private final List<GamePlayer> turnOrder;
  private final int maxRounds;

  private LoadedStrategicScenario turn;
  private int turnIndex;
  private boolean gameOver;

  public SelfPlayStrategicScenario(
      final GameData data, final long seed, final int maxActions, final int maxRounds) {
    this.data = Objects.requireNonNull(data);
    this.seed = seed;
    this.maxActions = maxActions;
    this.maxRounds = maxRounds;
    this.turnOrder = turnOrder(data);
    if (turnOrder.isEmpty()) {
      throw new IllegalArgumentException("game sequence declares no player steps");
    }
    this.turnIndex = 0;
    this.turn = newTurn(turnOrder.get(0));
  }

  /** The players that own steps, in the order the sequence runs them, each appearing once. */
  private static List<GamePlayer> turnOrder(final GameData data) {
    final List<GamePlayer> order = new ArrayList<>();
    for (final GameStep step : data.getSequence().getSteps()) {
      final GamePlayer player = step.getPlayerId();
      if (player != null && !player.isNull() && !order.contains(player)) {
        order.add(player);
      }
    }
    return List.copyOf(order);
  }

  @Override
  public StrategicObservation observation() {
    return turn.observation();
  }

  @Override
  public Map<String, Integer> scores() {
    return turn.scores();
  }

  @Override
  public List<StrategicAction> legalActions() {
    return gameOver ? List.of() : turn.legalActions();
  }

  @Override
  public boolean isLegalAction(final StrategicAction action) {
    return !gameOver && turn.isLegalAction(action);
  }

  @Override
  public StrategicScenarioStep step(final StrategicAction action) {
    if (gameOver) {
      throw new IllegalStateException("game is already over");
    }
    final StrategicScenarioStep result = turn.step(action);
    if (!turn.observation().over()) {
      return result;
    }
    // The turn just finished, so hand the game on before the caller sees the next observation.
    final Map<String, String> info = new TreeMap<>(result.info());
    info.putAll(advanceTurn());
    return new StrategicScenarioStep(result.truncated(), info);
  }

  private Map<String, String> advanceTurn() {
    final Map<String, String> info = new LinkedHashMap<>();
    runStepsFor(turnOrder.get(turnIndex), GameStep::isEndTurnStepName);

    final boolean wrapped = turnIndex == turnOrder.size() - 1;
    if (wrapped) {
      runEndRound();
      info.put("endRoundRan", "true");
    }
    if (gameOver) {
      info.put("gameOver", "true");
      info.put("finalScores", scores().toString());
      return info;
    }

    turnIndex = (turnIndex + 1) % turnOrder.size();
    final GamePlayer next = turnOrder.get(turnIndex);
    turn = newTurn(next);
    info.put("nextPlayer", next.getName());
    info.put("round", Integer.toString(data.getSequence().getRound()));
    return info;
  }

  /**
   * Runs the end-round step, which is what scores the position and may end the game, then opens the
   * next round. The round counter is advanced here rather than by GameSequence#next because the
   * strategic environment drives steps directly instead of running the engine's own step loop.
   */
  private void runEndRound() {
    final GameStep endRound =
        data.getSequence().getSteps().stream()
            .filter(step -> step.getPlayerId() == null || step.getPlayerId().isNull())
            .filter(step -> step.getDelegate() instanceof EndRoundDelegate)
            .findFirst()
            .orElse(null);
    if (endRound != null) {
      final IDelegate delegate = endRound.getDelegate();
      delegate.setDelegateBridgeAndPlayer(bridgeFor(turnOrder.get(turnIndex)));
      delegate.start();
      delegate.end();
      if (delegate instanceof EndRoundDelegate endRoundDelegate && endRoundDelegate.isGameOver()) {
        gameOver = true;
        return;
      }
    }
    final int nextRound = data.getSequence().getRound() + 1;
    final GamePlayer opener = turnOrder.get(0);
    // setRoundAndStep logs an error for a step name it cannot find, so name a real one.
    firstStepOf(opener)
        .ifPresent(
            step -> data.getSequence().setRoundAndStep(nextRound, step.getDisplayName(), opener));
    // A map with no end-round delegate, or with auto termination off, still needs a stop.
    if (maxRounds > 0 && nextRound > maxRounds) {
      gameOver = true;
    }
  }

  private java.util.Optional<GameStep> firstStepOf(final GamePlayer player) {
    return data.getSequence().getSteps().stream()
        .filter(step -> player.equals(step.getPlayerId()))
        .findFirst();
  }

  private void runStepsFor(final GamePlayer player, final StepNameMatcher matcher) {
    for (final GameStep step : data.getSequence().getSteps()) {
      if (!player.equals(step.getPlayerId()) || !matcher.matches(step.getName())) {
        continue;
      }
      data.getSequence()
          .setRoundAndStep(data.getSequence().getRound(), step.getDisplayName(), player);
      final IDelegate delegate = step.getDelegate();
      delegate.setDelegateBridgeAndPlayer(bridgeFor(player));
      delegate.start();
      delegate.end();
    }
  }

  private LoadedStrategicScenario newTurn(final GamePlayer player) {
    return new LoadedStrategicScenario(data, player, seed, maxActions);
  }

  private SimulationDelegateBridge bridgeFor(final GamePlayer player) {
    return new SimulationDelegateBridge(data, player, null, seed);
  }

  public boolean isGameOver() {
    return gameOver;
  }

  public Map<String, Integer> finalScores() {
    return SmallFrontScoringService.score(data).entrySet().stream()
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(entry.getKey().getName(), entry.getValue()),
            LinkedHashMap::putAll);
  }

  @FunctionalInterface
  private interface StepNameMatcher {
    boolean matches(String stepName);
  }
}
