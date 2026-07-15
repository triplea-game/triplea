package games.strategy.triplea.delegate.battle.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/** Stateful lifecycle implementation shared by concrete TripleA scenario providers. */
public final class StatefulBattleEnvironment implements BattleEnvironment {
  private static final Comparator<BattleAction> ACTION_ORDER =
      Comparator.comparing(BattleAction::type)
          .thenComparing(StatefulBattleEnvironment::canonicalParameters);

  private final BattleScenarioLoader scenarioLoader;
  private final BattleRewardFunction rewardFunction;
  private final List<BattleTransition> transitions = new ArrayList<>();
  private BattleScenario activeScenario;
  private BattleResetRequest activeResetRequest;
  private BattleObservation initialObservation;
  private long episodeId;
  private long stepId;
  private double cumulativeReward;
  private boolean finished = true;
  private boolean truncated;

  public StatefulBattleEnvironment(final BattleScenarioLoader scenarioLoader) {
    this(scenarioLoader, BattleRewardFunction.standard());
  }

  public StatefulBattleEnvironment(
      final BattleScenarioLoader scenarioLoader, final BattleRewardFunction rewardFunction) {
    this.scenarioLoader = Objects.requireNonNull(scenarioLoader);
    this.rewardFunction = Objects.requireNonNull(rewardFunction);
  }

  @Override
  public synchronized BattleObservation reset(final BattleResetRequest request) {
    activeResetRequest = Objects.requireNonNull(request);
    activeScenario = Objects.requireNonNull(scenarioLoader.load(request));
    episodeId++;
    stepId = 0;
    cumulativeReward = 0;
    truncated = false;
    transitions.clear();
    initialObservation = Objects.requireNonNull(activeScenario.observation());
    finished = initialObservation.over();
    return initialObservation;
  }

  @Override
  public synchronized List<BattleAction> legalActions() {
    final BattleScenario scenario = requireScenario();
    if (finished) {
      return List.of();
    }
    return scenario.legalActions().stream()
        .map(Objects::requireNonNull)
        .sorted(ACTION_ORDER)
        .toList();
  }

  @Override
  public synchronized BattleStepResult step(final BattleAction action) {
    Objects.requireNonNull(action);
    final BattleScenario scenario = requireScenario();
    if (finished) {
      throw new IllegalStateException("battle episode is already finished");
    }

    final BattleObservation before = Objects.requireNonNull(scenario.observation());
    final List<BattleAction> legalActions = legalActions();
    if (!scenario.isLegalAction(action)) {
      throw new IllegalArgumentException(
          "action is not legal in the current battle state: " + action + "; mask: " + legalActions);
    }

    final BattleScenarioStep scenarioStep = Objects.requireNonNull(scenario.step(action));
    stepId++;
    final BattleObservation observation = Objects.requireNonNull(scenario.observation());
    final boolean terminated = observation.over();
    truncated = scenarioStep.truncated();
    finished = terminated || truncated;

    final Map<String, String> info = new TreeMap<>(scenarioStep.info());
    info.put("episodeId", Long.toString(episodeId));
    info.put("stepId", Long.toString(stepId));
    info.put("actionType", action.type());

    final double reward = rewardFunction.reward(before, observation, scenarioStep);
    final BattleStepResult result =
        new BattleStepResult(observation, reward, terminated, truncated, info);
    cumulativeReward += reward;
    transitions.add(new BattleTransition(stepId, before, legalActions, action, result));
    return result;
  }

  @Override
  public synchronized BattleEpisodeLog episodeLog() {
    requireScenario();
    return new BattleEpisodeLog(
        BattleEpisodeLog.CURRENT_LOG_SCHEMA_VERSION,
        activeResetRequest,
        initialObservation,
        transitions,
        cumulativeReward,
        initialObservation.over()
            || (!transitions.isEmpty() && transitions.getLast().result().terminated()),
        truncated);
  }

  @Override
  public BattleReplayResult replay(final BattleEpisodeLog expectedEpisode) {
    Objects.requireNonNull(expectedEpisode);
    final StatefulBattleEnvironment replayEnvironment =
        new StatefulBattleEnvironment(scenarioLoader, rewardFunction);
    final BattleObservation replayInitial = replayEnvironment.reset(expectedEpisode.resetRequest());
    if (!replayInitial.equals(expectedEpisode.initialObservation())) {
      return mismatch(replayEnvironment, 0, "initial observation differs");
    }

    int verifiedTransitions = 0;
    for (final BattleTransition expectedTransition : expectedEpisode.transitions()) {
      final List<BattleAction> actualLegalActions = replayEnvironment.legalActions();
      if (!actualLegalActions.equals(expectedTransition.legalActions())) {
        return mismatch(
            replayEnvironment,
            verifiedTransitions,
            "legal actions differ at step " + expectedTransition.stepId());
      }
      if (!actualLegalActions.contains(expectedTransition.action())
          && !replayEnvironment.activeScenario.isLegalAction(expectedTransition.action())) {
        return mismatch(
            replayEnvironment,
            verifiedTransitions,
            "recorded action is no longer legal at step " + expectedTransition.stepId());
      }

      final BattleStepResult actualResult = replayEnvironment.step(expectedTransition.action());
      final String resultMismatch = compareResults(expectedTransition.result(), actualResult);
      if (!resultMismatch.isEmpty()) {
        return mismatch(
            replayEnvironment,
            verifiedTransitions,
            resultMismatch + " at step " + expectedTransition.stepId());
      }
      verifiedTransitions++;
    }

    final BattleEpisodeLog actualEpisode = replayEnvironment.episodeLog();
    if (Double.doubleToLongBits(actualEpisode.cumulativeReward())
        != Double.doubleToLongBits(expectedEpisode.cumulativeReward())) {
      return new BattleReplayResult(
          false, verifiedTransitions, "cumulative reward differs", actualEpisode);
    }
    if (actualEpisode.terminated() != expectedEpisode.terminated()
        || actualEpisode.truncated() != expectedEpisode.truncated()) {
      return new BattleReplayResult(
          false, verifiedTransitions, "terminal state differs", actualEpisode);
    }
    if (!actualEpisode.finalObservation().equals(expectedEpisode.finalObservation())) {
      return new BattleReplayResult(
          false, verifiedTransitions, "final observation differs", actualEpisode);
    }
    return new BattleReplayResult(true, verifiedTransitions, "", actualEpisode);
  }

  @Override
  public BattleBatchResult batch(final BattleBatchRequest request) {
    Objects.requireNonNull(request);
    final long startedAt = System.nanoTime();
    if (request.episodes().isEmpty()) {
      return BattleBatchResult.from(List.of(), 0, System.nanoTime() - startedAt, usedMemoryBytes());
    }

    final int workerCount = Math.min(request.parallelism(), request.episodes().size());
    if (workerCount == 1) {
      final List<BattleReplayResult> results =
          request.episodes().stream().map(this::replay).toList();
      return BattleBatchResult.from(
          results, workerCount, System.nanoTime() - startedAt, usedMemoryBytes());
    }

    final ExecutorService executor = Executors.newFixedThreadPool(workerCount);
    try {
      final List<Callable<BattleReplayResult>> tasks =
          request.episodes().stream()
              .map(episode -> (Callable<BattleReplayResult>) () -> replay(episode))
              .toList();
      final List<Future<BattleReplayResult>> futures = executor.invokeAll(tasks);
      final List<BattleReplayResult> results = new ArrayList<>(futures.size());
      for (final Future<BattleReplayResult> future : futures) {
        results.add(future.get());
      }
      return BattleBatchResult.from(
          results, workerCount, System.nanoTime() - startedAt, usedMemoryBytes());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("battle replay batch was interrupted", e);
    } catch (final ExecutionException e) {
      throw new IllegalStateException("battle replay batch failed", e.getCause());
    } finally {
      executor.shutdownNow();
    }
  }

  private BattleScenario requireScenario() {
    if (activeScenario == null) {
      throw new IllegalStateException("reset must be called before using the battle environment");
    }
    return activeScenario;
  }

  private static BattleReplayResult mismatch(
      final StatefulBattleEnvironment environment,
      final int verifiedTransitions,
      final String message) {
    return new BattleReplayResult(false, verifiedTransitions, message, environment.episodeLog());
  }

  private static String compareResults(
      final BattleStepResult expected, final BattleStepResult actual) {
    if (!expected.observation().equals(actual.observation())) {
      return "observation differs";
    }
    if (Double.doubleToLongBits(expected.reward()) != Double.doubleToLongBits(actual.reward())) {
      return "reward differs";
    }
    if (expected.terminated() != actual.terminated()
        || expected.truncated() != actual.truncated()) {
      return "termination flags differ";
    }
    if (!deterministicInfo(expected.info()).equals(deterministicInfo(actual.info()))) {
      return "transition info differs";
    }
    return "";
  }

  private static Map<String, String> deterministicInfo(final Map<String, String> info) {
    final Map<String, String> deterministic = new TreeMap<>(info);
    deterministic.remove("episodeId");
    return deterministic;
  }

  private static long usedMemoryBytes() {
    final Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private static String canonicalParameters(final BattleAction action) {
    return action.parameters().entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\u0000"));
  }
}
