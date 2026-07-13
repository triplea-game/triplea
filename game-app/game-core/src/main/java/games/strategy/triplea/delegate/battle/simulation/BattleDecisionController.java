package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Owns the single player decision that may block a simulated battle execution stack. */
final class BattleDecisionController {
  private static final String SELECT_CASUALTIES = "select_casualties";
  private static final String CONTINUE = "continue";
  private static final String RETREAT = "retreat";
  private static final String SUBMERGE = "submerge";

  private PendingDecision pendingDecision;
  private BattleAction submittedAction;

  BattleDecisionObservation observation() {
    return pendingDecision == null
        ? BattleDecisionObservation.none()
        : pendingDecision.observation();
  }

  List<BattleAction> legalActions() {
    return pendingDecision == null ? List.of() : pendingDecision.legalActions();
  }

  boolean isLegalAction(final BattleAction action) {
    return pendingDecision != null && pendingDecision.isLegal(action);
  }

  void submit(final BattleAction action) {
    Objects.requireNonNull(action);
    if (!isLegalAction(action)) {
      throw new IllegalArgumentException(
          "action is not legal for the pending battle decision: " + action);
    }
    if (submittedAction != null) {
      throw new IllegalStateException("a battle decision action is already queued");
    }
    submittedAction = action;
  }

  CasualtyDetails requestCasualties(
      final Collection<Unit> selectFrom,
      final int count,
      final String message,
      final GamePlayer hitPlayer,
      final CasualtyList defaultCasualties,
      final boolean allowMultipleHitsPerUnit) {
    final CasualtyDecision decision =
        new CasualtyDecision(
            selectFrom, count, message, hitPlayer, defaultCasualties, allowMultipleHitsPerUnit);
    return resolveOrPause(decision, decision::resolve);
  }

  Optional<Territory> requestRetreat(
      final GamePlayer player,
      final boolean submerge,
      final Collection<Territory> possibleTerritories,
      final String message) {
    final RetreatDecision decision =
        new RetreatDecision(player, submerge, possibleTerritories, message);
    return resolveOrPause(decision, decision::resolve);
  }

  private <T> T resolveOrPause(
      final PendingDecision currentDecision, final Function<BattleAction, T> resolver) {
    if (submittedAction == null) {
      pendingDecision = currentDecision;
      throw new BattleDecisionRequiredException();
    }
    final BattleAction action = submittedAction;
    submittedAction = null;
    pendingDecision = null;
    return resolver.apply(action);
  }

  private interface PendingDecision {
    BattleDecisionObservation observation();

    List<BattleAction> legalActions();

    boolean isLegal(BattleAction action);
  }

  private static final class CasualtyDecision implements PendingDecision {
    private final List<Unit> candidates;
    private final Map<String, Unit> candidatesById;
    private final int requiredHits;
    private final String message;
    private final GamePlayer player;
    private final CasualtyList defaultCasualties;
    private final boolean allowMultipleHitsPerUnit;

    private CasualtyDecision(
        final Collection<Unit> selectFrom,
        final int requiredHits,
        final String message,
        final GamePlayer player,
        final CasualtyList defaultCasualties,
        final boolean allowMultipleHitsPerUnit) {
      candidates = selectFrom.stream().sorted(Comparator.comparing(Unit::getId)).toList();
      candidatesById =
          candidates.stream()
              .collect(Collectors.toMap(unit -> unit.getId().toString(), unit -> unit));
      this.requiredHits = requiredHits;
      this.message = Objects.requireNonNullElse(message, "");
      this.player = Objects.requireNonNull(player);
      this.defaultCasualties = Objects.requireNonNull(defaultCasualties);
      this.allowMultipleHitsPerUnit = allowMultipleHitsPerUnit;
    }

    @Override
    public BattleDecisionObservation observation() {
      return new BattleDecisionObservation(
          BattleDecisionType.SELECT_CASUALTIES,
          player.getName(),
          message,
          requiredHits,
          allowMultipleHitsPerUnit,
          candidates.stream()
              .map(
                  unit ->
                      new BattleDecisionUnitObservation(
                          unit.getId().toString(),
                          unit.getOwner().getName(),
                          unit.getType().getName(),
                          unit.getHits(),
                          unit.getUnitAttachment().getHitPoints(),
                          unit.getAlreadyMoved()))
              .toList(),
          List.of(),
          unitIds(defaultCasualties.getKilled()),
          unitIds(defaultCasualties.getDamaged()));
    }

    @Override
    public List<BattleAction> legalActions() {
      return List.of(
          new BattleAction(
              SELECT_CASUALTIES,
              Map.of(
                  "allowMultipleHitsPerUnit",
                  Boolean.toString(allowMultipleHitsPerUnit),
                  "candidateUnitIds",
                  String.join(",", candidatesById.keySet().stream().sorted().toList()),
                  "defaultDamagedUnitIds",
                  String.join(",", unitIds(defaultCasualties.getDamaged())),
                  "defaultKilledUnitIds",
                  String.join(",", unitIds(defaultCasualties.getKilled())),
                  "requiredHits",
                  Integer.toString(requiredHits))));
    }

    @Override
    public boolean isLegal(final BattleAction action) {
      try {
        resolve(action);
        return true;
      } catch (final IllegalArgumentException e) {
        return false;
      }
    }

    private CasualtyDetails resolve(final BattleAction action) {
      if (!SELECT_CASUALTIES.equals(action.type())) {
        throw new IllegalArgumentException("expected select_casualties action");
      }
      final List<String> killedIds = parseIds(action.parameters().get("killedUnitIds"));
      final List<String> damagedIds = parseIds(action.parameters().get("damagedUnitIds"));
      if (new HashSet<>(killedIds).size() != killedIds.size()) {
        throw new IllegalArgumentException("killedUnitIds must not contain duplicates");
      }
      if (!allowMultipleHitsPerUnit && !damagedIds.isEmpty()) {
        throw new IllegalArgumentException(
            "damagedUnitIds are not allowed for this casualty decision");
      }

      final List<Unit> killed = resolveUnits(killedIds, candidatesById);
      final List<Unit> damaged = resolveUnits(damagedIds, candidatesById);
      final Map<Unit, Long> damageFrequency =
          damaged.stream()
              .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
      for (final Map.Entry<Unit, Long> entry : damageFrequency.entrySet()) {
        final Unit unit = entry.getKey();
        final int maximumDamageBeforeDeath =
            Math.max(0, unit.getUnitAttachment().getHitPoints() - unit.getHits() - 1);
        if (entry.getValue() > maximumDamageBeforeDeath) {
          throw new IllegalArgumentException(
              "too many non-lethal hits assigned to unit " + unit.getId());
        }
      }
      if (killed.size() + damaged.size() != requiredHits) {
        throw new IllegalArgumentException(
            "casualty action must assign exactly " + requiredHits + " hits");
      }
      return new CasualtyDetails(killed, damaged, false);
    }
  }

  private static final class RetreatDecision implements PendingDecision {
    private final GamePlayer player;
    private final boolean submerge;
    private final String message;
    private final List<Territory> territories;

    private RetreatDecision(
        final GamePlayer player,
        final boolean submerge,
        final Collection<Territory> possibleTerritories,
        final String message) {
      this.player = Objects.requireNonNull(player);
      this.submerge = submerge;
      this.message = Objects.requireNonNullElse(message, "");
      territories =
          possibleTerritories.stream()
              .distinct()
              .sorted(Comparator.comparing(Territory::getName))
              .toList();
    }

    @Override
    public BattleDecisionObservation observation() {
      return new BattleDecisionObservation(
          submerge ? BattleDecisionType.SUBMERGE : BattleDecisionType.RETREAT,
          player.getName(),
          message,
          0,
          false,
          List.of(),
          territories.stream().map(Territory::getName).toList(),
          List.of(),
          List.of());
    }

    @Override
    public List<BattleAction> legalActions() {
      final List<BattleAction> actions = new ArrayList<>();
      actions.add(new BattleAction(CONTINUE, Map.of()));
      final String actionType = submerge ? SUBMERGE : RETREAT;
      territories.forEach(
          territory ->
              actions.add(new BattleAction(actionType, Map.of("territory", territory.getName()))));
      return List.copyOf(actions);
    }

    @Override
    public boolean isLegal(final BattleAction action) {
      return legalActions().contains(action);
    }

    private Optional<Territory> resolve(final BattleAction action) {
      if (CONTINUE.equals(action.type()) && action.parameters().isEmpty()) {
        return Optional.empty();
      }
      final String expectedType = submerge ? SUBMERGE : RETREAT;
      if (!expectedType.equals(action.type())) {
        throw new IllegalArgumentException("unexpected retreat action type: " + action.type());
      }
      final String territoryName = action.parameters().get("territory");
      return territories.stream()
          .filter(territory -> territory.getName().equals(territoryName))
          .findFirst()
          .or(
              () -> {
                throw new IllegalArgumentException(
                    "territory is not a legal retreat destination: " + territoryName);
              });
    }
  }

  private static List<String> unitIds(final Collection<Unit> units) {
    return units.stream().map(unit -> unit.getId().toString()).toList();
  }

  private static List<String> parseIds(final String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(id -> !id.isEmpty()).toList();
  }

  private static List<Unit> resolveUnits(
      final List<String> ids, final Map<String, Unit> candidatesById) {
    final List<Unit> units = new ArrayList<>();
    for (final String id : ids) {
      try {
        UUID.fromString(id);
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException("unit ID is not a UUID: " + id, e);
      }
      final Unit unit = candidatesById.get(id);
      if (unit == null) {
        throw new IllegalArgumentException("unit is not a casualty candidate: " + id);
      }
      units.add(unit);
    }
    return units;
  }
}
