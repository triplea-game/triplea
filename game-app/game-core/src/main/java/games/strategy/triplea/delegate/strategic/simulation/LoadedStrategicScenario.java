package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.StackCapacityResolver;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.simulation.BattleAction;
import games.strategy.triplea.delegate.battle.simulation.BattleDecisionController;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.battle.simulation.BattleScenario;
import games.strategy.triplea.delegate.battle.simulation.BattleScenarioStep;
import games.strategy.triplea.delegate.battle.simulation.LoadedBattleScenario;
import games.strategy.triplea.delegate.battle.simulation.SimulationDelegateBridge;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementDelegate;
import games.strategy.triplea.delegate.scoring.SmallFrontScoringService;
import games.strategy.triplea.delegate.supply.SupplyDelegate;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import games.strategy.triplea.delegate.visibility.VisibilityService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Executes one player turn through normal delegates and nested battle decisions. */
public final class LoadedStrategicScenario implements StrategicScenario {
  private static final Comparator<Territory> TERRITORY_ORDER =
      Comparator.comparing(Territory::getName);

  private final GameData data;
  private final GamePlayer player;
  private final long seed;
  private final int maxActions;
  private final BattleDecisionController movementDecisionController =
      new BattleDecisionController();
  private final SimulationDelegateBridge bridge;
  private final Set<UUID> reinforcementUnitIds = new LinkedHashSet<>();

  private StrategicPhase phase;
  private @Nullable AbstractMoveDelegate activeMoveDelegate;
  private @Nullable BattleScenario activeBattle;
  private long battleSeedOffset;

  public LoadedStrategicScenario(
      final GameData data, final GamePlayer player, final long seed, final int maxActions) {
    this.data = Objects.requireNonNull(data);
    this.player = Objects.requireNonNull(player);
    this.seed = seed;
    this.maxActions = maxActions;
    bridge = new SimulationDelegateBridge(data, player, movementDecisionController, seed);
    initializeTurn();
  }

  @Override
  public StrategicObservation observation() {
    prepareBattleIfNeeded();
    final BattleObservation battleObservation =
        activeBattle == null ? null : activeBattle.observation();
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      return StrategicObservationFactory.create(data, player, seed, phase, battleObservation);
    }
  }

  @Override
  public Map<String, Integer> scores() {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      return SmallFrontScoringService.score(data).entrySet().stream()
          .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }
  }

  @Override
  public List<StrategicAction> legalActions() {
    prepareBattleIfNeeded();
    return switch (phase) {
      case REINFORCEMENT_ALLOCATION -> reinforcementActions();
      case COMBAT_MOVE, AIR_ASSIGNMENT, REDEPLOYMENT ->
          StrategicMoveCandidateGenerator.generate(
              data,
              player,
              phase,
              maxActions,
              // MoveValidator reads the moves already made; without them the generator would offer
              // moves that performMove then throws on.
              activeMoveDelegate == null ? List.of() : activeMoveDelegate.getMovesMade());
      case BATTLE -> battleActions();
      case COMPLETE -> List.of();
    };
  }

  @Override
  public StrategicScenarioStep step(final StrategicAction action) {
    return switch (action.type()) {
      case "end_phase" -> endPhase(action);
      case "allocate_reinforcement" -> allocateReinforcement(action);
      case "move", "air_assignment" -> executeMove(action);
      case "battle_decision" -> executeBattleDecision(action);
      default ->
          throw new IllegalArgumentException("unsupported strategic action: " + action.type());
    };
  }

  private void initializeTurn() {
    final Set<UUID> before = ownedUnitIds();
    findStep(step -> step.getDelegate() instanceof FixedReinforcementDelegate)
        .ifPresent(this::runAutomaticStep);
    final Set<UUID> after = ownedUnitIds();
    after.removeAll(before);
    reinforcementUnitIds.addAll(after);
    findStep(step -> step.getDelegate() instanceof SupplyDelegate)
        .ifPresent(this::runAutomaticStep);
    if (reinforcementUnitIds.isEmpty()) {
      enterCombatMove();
    } else {
      phase = StrategicPhase.REINFORCEMENT_ALLOCATION;
    }
  }

  private StrategicScenarioStep endPhase(final StrategicAction action) {
    final String expected = action.parameters().get("phase");
    if (expected == null || !phase.name().equals(expected)) {
      throw new IllegalArgumentException("end_phase does not match current phase " + phase);
    }
    return switch (phase) {
      case REINFORCEMENT_ALLOCATION -> {
        reinforcementUnitIds.clear();
        enterCombatMove();
        yield StrategicScenarioStep.completed(Map.of("nextPhase", phase.name()));
      }
      case COMBAT_MOVE -> {
        phase = StrategicPhase.AIR_ASSIGNMENT;
        yield StrategicScenarioStep.completed(Map.of("nextPhase", phase.name()));
      }
      case AIR_ASSIGNMENT -> {
        finishMoveDelegate();
        phase = StrategicPhase.BATTLE;
        setBattleStep();
        prepareBattleIfNeeded();
        yield StrategicScenarioStep.completed(Map.of("nextPhase", phase.name()));
      }
      case REDEPLOYMENT -> {
        finishMoveDelegate();
        phase = StrategicPhase.COMPLETE;
        yield StrategicScenarioStep.completed(Map.of("nextPhase", phase.name()));
      }
      case BATTLE -> throw new IllegalStateException("battle phase ends after all battles resolve");
      case COMPLETE -> throw new IllegalStateException("turn is already complete");
    };
  }

  private StrategicScenarioStep allocateReinforcement(final StrategicAction action) {
    requirePhase(StrategicPhase.REINFORCEMENT_ALLOCATION);
    final Territory origin = data.getMap().getTerritoryOrThrow(action.parameters().get("origin"));
    final Territory destination =
        data.getMap().getTerritoryOrThrow(action.parameters().get("destination"));
    final List<Unit> units = resolveUnits(action.parameters().get("unitIds"), origin);
    if (units.stream().anyMatch(unit -> !reinforcementUnitIds.contains(unit.getId()))) {
      throw new IllegalArgumentException("allocation includes a unit not delivered this turn");
    }
    if (!origin.equals(destination)) {
      data.performChange(ChangeFactory.moveUnits(origin, destination, units));
    }
    units.forEach(unit -> reinforcementUnitIds.remove(unit.getId()));
    if (reinforcementUnitIds.isEmpty()) {
      enterCombatMove();
    }
    return StrategicScenarioStep.completed(
        Map.of(
            "allocatedUnits", Integer.toString(units.size()),
            "destination", destination.getName(),
            "nextPhase", phase.name()));
  }

  private StrategicScenarioStep executeMove(final StrategicAction action) {
    if (phase != StrategicPhase.COMBAT_MOVE
        && phase != StrategicPhase.AIR_ASSIGNMENT
        && phase != StrategicPhase.REDEPLOYMENT) {
      throw new IllegalStateException("movement action is not valid during " + phase);
    }
    final AbstractMoveDelegate moveDelegate =
        Optional.ofNullable(activeMoveDelegate)
            .orElseThrow(() -> new IllegalStateException("no move delegate is active"));
    final Route route = resolveRoute(action.parameters().get("route"));
    final List<Unit> units = resolveUnits(action.parameters().get("unitIds"), route.getStart());
    final Optional<String> error = moveDelegate.performMove(new MoveDescription(units, route));
    final boolean uncertain = Boolean.parseBoolean(action.parameters().get("uncertain"));
    if (error.isPresent() && !uncertain) {
      throw new IllegalStateException("generated move failed validation: " + error.get());
    }
    final Map<String, String> info = new TreeMap<>();
    info.put("moveResolved", Boolean.toString(error.isEmpty()));
    info.put("origin", route.getStart().getName());
    info.put("destination", route.getEnd().getName());
    if (error.isPresent()) {
      info.put("blockedByHiddenState", "true");
    }
    return StrategicScenarioStep.completed(info);
  }

  private StrategicScenarioStep executeBattleDecision(final StrategicAction action) {
    requirePhase(StrategicPhase.BATTLE);
    prepareBattleIfNeeded();
    final BattleScenario battle =
        Optional.ofNullable(activeBattle)
            .orElseThrow(() -> new IllegalStateException("no battle decision is active"));
    final Map<String, String> parameters = new TreeMap<>(action.parameters());
    final String battleActionType = parameters.remove("battleActionType");
    parameters.remove("battleId");
    parameters.remove("battleTerritory");
    final BattleScenarioStep result = battle.step(new BattleAction(battleActionType, parameters));
    if (battle.observation().over()) {
      activeBattle = null;
      prepareBattleIfNeeded();
    }
    final Map<String, String> info = new TreeMap<>(result.info());
    info.put("nextPhase", phase.name());
    return new StrategicScenarioStep(result.truncated(), info);
  }

  private List<StrategicAction> reinforcementActions() {
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(player, data);
    final List<Territory> targets =
        SupplyNetworkResolver.getSuppliedTerritories(player, data).stream()
            .filter(visible::contains)
            .filter(territory -> isFriendly(territory, player))
            .filter(territory -> !territory.isWater())
            .sorted(TERRITORY_ORDER)
            .toList();
    final List<StrategicAction> actions = new ArrayList<>();
    for (final Map.Entry<ReinforcementKey, List<Unit>> entry : reinforcementGroups().entrySet()) {
      final Territory origin = data.getMap().getTerritoryOrThrow(entry.getKey().territory());
      for (final Territory target : targets) {
        if (origin.equals(target)
            || StackCapacityResolver.canFit(entry.getValue(), player, target, List.of())) {
          actions.add(
              new StrategicAction(
                  "allocate_reinforcement",
                  Map.of(
                      "origin", origin.getName(),
                      "destination", target.getName(),
                      "unitType", entry.getKey().unitType(),
                      "unitIds", unitIds(entry.getValue()))));
        }
      }
    }
    actions.add(
        new StrategicAction(
            "end_phase", Map.of("phase", StrategicPhase.REINFORCEMENT_ALLOCATION.name())));
    final List<StrategicAction> sorted =
        actions.stream()
            .distinct()
            .sorted(
                Comparator.comparing(StrategicAction::type)
                    .thenComparing(candidate -> candidate.parameters().toString()))
            .toList();
    if (sorted.size() > maxActions) {
      throw new StrategicActionSpaceOverflow(sorted.size(), maxActions);
    }
    return sorted;
  }

  private List<StrategicAction> battleActions() {
    prepareBattleIfNeeded();
    if (phase != StrategicPhase.BATTLE || activeBattle == null) {
      return legalActions();
    }
    final BattleObservation observation = activeBattle.observation();
    return activeBattle.legalActions().stream()
        .map(
            battleAction -> {
              final Map<String, String> parameters = new TreeMap<>(battleAction.parameters());
              parameters.put("battleActionType", battleAction.type());
              parameters.put("battleId", observation.battleId());
              // Not "territory": a retreat already carries one, naming its destination.
              parameters.put("battleTerritory", observation.territory());
              return new StrategicAction("battle_decision", parameters);
            })
        .toList();
  }

  private void prepareBattleIfNeeded() {
    while (phase == StrategicPhase.BATTLE) {
      if (activeBattle != null && !activeBattle.observation().over()) {
        return;
      }
      activeBattle = null;
      final Optional<IBattle> next =
          StrategicObservationFactory.pendingBattles(data).stream()
              .filter(BattleState.class::isInstance)
              .findFirst();
      if (next.isEmpty()) {
        enterRedeployment();
        return;
      }
      activeBattle = new LoadedBattleScenario(data, next.get(), seed + ++battleSeedOffset);
    }
  }

  private void enterCombatMove() {
    phase = StrategicPhase.COMBAT_MOVE;
    final Optional<GameStep> step = findStep(LoadedStrategicScenario::isCombatMoveStep);
    if (step.isEmpty()) {
      phase = StrategicPhase.BATTLE;
      setBattleStep();
      prepareBattleIfNeeded();
      return;
    }
    startMoveDelegate(step.get());
  }

  private void enterRedeployment() {
    phase = StrategicPhase.REDEPLOYMENT;
    final Optional<GameStep> step = findStep(LoadedStrategicScenario::isNonCombatMoveStep);
    if (step.isEmpty()) {
      phase = StrategicPhase.COMPLETE;
      return;
    }
    startMoveDelegate(step.get());
  }

  private void startMoveDelegate(final GameStep step) {
    setSequenceStep(step);
    final IDelegate delegate = step.getDelegate();
    if (!(delegate instanceof AbstractMoveDelegate moveDelegate)) {
      throw new IllegalArgumentException(
          "strategic move step uses unsupported delegate: " + delegate.getClass().getName());
    }
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    activeMoveDelegate = moveDelegate;
  }

  private void finishMoveDelegate() {
    if (activeMoveDelegate != null) {
      activeMoveDelegate.end();
      activeMoveDelegate = null;
    }
  }

  private void runAutomaticStep(final GameStep step) {
    setSequenceStep(step);
    final IDelegate delegate = step.getDelegate();
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    delegate.end();
  }

  private void setBattleStep() {
    findStep(
            step ->
                step.getDelegate() instanceof BattleDelegate
                    || GameStep.isBattleStepName(step.getName()))
        .ifPresent(this::setSequenceStep);
  }

  private void setSequenceStep(final GameStep step) {
    data.getSequence()
        .setRoundAndStep(data.getSequence().getRound(), step.getDisplayName(), player);
  }

  private Optional<GameStep> findStep(final Predicate<GameStep> predicate) {
    return data.getSequence().getSteps().stream()
        .filter(step -> player.equals(step.getPlayerId()))
        .filter(predicate)
        .findFirst();
  }

  private Map<ReinforcementKey, List<Unit>> reinforcementGroups() {
    final Map<ReinforcementKey, List<Unit>> groups = new TreeMap<>();
    for (final Territory territory : data.getMap().getTerritories()) {
      for (final Unit unit : territory.getUnitCollection().getUnits()) {
        if (reinforcementUnitIds.contains(unit.getId())) {
          groups
              .computeIfAbsent(
                  new ReinforcementKey(territory.getName(), unit.getType().getName()),
                  ignored -> new ArrayList<>())
              .add(unit);
        }
      }
    }
    groups
        .values()
        .forEach(units -> units.sort(Comparator.comparing(unit -> unit.getId().toString())));
    return groups;
  }

  private Set<UUID> ownedUnitIds() {
    final Set<UUID> result = new HashSet<>();
    for (final Territory territory : data.getMap().getTerritories()) {
      territory.getUnitCollection().getUnits().stream()
          .filter(unit -> unit.isOwnedBy(player))
          .map(Unit::getId)
          .forEach(result::add);
    }
    return result;
  }

  private Route resolveRoute(final String encodedRoute) {
    return StrategicActionResolver.resolveRoute(data, encodedRoute);
  }

  private List<Unit> resolveUnits(final String encodedUnitIds, final Territory origin) {
    return StrategicActionResolver.resolveUnits(encodedUnitIds, origin);
  }

  private boolean isFriendly(final Territory territory, final GamePlayer viewer) {
    final GamePlayer owner = territory.getOwner();
    return viewer.equals(owner)
        || (data.getRelationshipTracker().getRelationship(viewer, owner) != null
            && data.getRelationshipTracker().isAllied(viewer, owner));
  }

  private void requirePhase(final StrategicPhase expected) {
    if (phase != expected) {
      throw new IllegalStateException("expected phase " + expected + " but was " + phase);
    }
  }

  private static boolean isCombatMoveStep(final GameStep step) {
    return Boolean.parseBoolean(
            step.getProperties().getProperty(GameStep.PropertyKeys.COMBAT_MOVE, "false"))
        || (step.getName() != null && GameStep.isCombatMoveStepName(step.getName()));
  }

  private static boolean isNonCombatMoveStep(final GameStep step) {
    return step.isNonCombat()
        || Boolean.parseBoolean(
            step.getProperties().getProperty(GameStep.PropertyKeys.NON_COMBAT_MOVE, "false"));
  }

  private static String unitIds(final Collection<Unit> units) {
    return units.stream()
        .map(unit -> unit.getId().toString())
        .sorted()
        .collect(Collectors.joining(","));
  }

  private record ReinforcementKey(String territory, String unitType)
      implements Comparable<ReinforcementKey> {
    @Override
    public int compareTo(final ReinforcementKey other) {
      return Comparator.comparing(ReinforcementKey::territory)
          .thenComparing(ReinforcementKey::unitType)
          .compare(this, other);
    }
  }
}
