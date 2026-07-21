package games.strategy.triplea.ai.smallfront;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Immutable, JSON-friendly operational intent that remains stable for one player turn. */
public record OperationalTurnPlan(
    int schemaVersion,
    String planId,
    String playerName,
    int round,
    String commanderIntent,
    List<Objective> objectives,
    Set<String> protectedTerritories,
    int maximumReplans) {
  public static final int SCHEMA_VERSION = 1;

  public OperationalTurnPlan {
    if (schemaVersion != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported operational plan schema: " + schemaVersion);
    }
    if (round < 0) {
      throw new IllegalArgumentException("round must not be negative");
    }
    if (maximumReplans < 0) {
      throw new IllegalArgumentException("maximumReplans must not be negative");
    }
    planId = requireText(planId, "planId");
    playerName = requireText(playerName, "playerName");
    commanderIntent = requireText(commanderIntent, "commanderIntent");
    Objects.requireNonNull(objectives);
    Objects.requireNonNull(protectedTerritories);

    final List<Objective> orderedObjectives = new ArrayList<>(objectives);
    orderedObjectives.sort(
        Comparator.comparingInt(Objective::priority)
            .reversed()
            .thenComparing(Objective::objectiveId));
    validateObjectives(orderedObjectives);
    objectives = List.copyOf(orderedObjectives);
    protectedTerritories = Collections.unmodifiableSet(new TreeSet<>(protectedTerritories));
  }

  public Optional<Objective> primaryObjective() {
    return objectives.stream().findFirst();
  }

  public List<Objective> objectivesFor(final String territoryName) {
    return objectives.stream()
        .filter(objective -> objective.territoryName().equals(territoryName))
        .toList();
  }

  private static void validateObjectives(final List<Objective> objectives) {
    final Map<String, Objective> byId = new HashMap<>();
    objectives.forEach(
        objective -> {
          if (byId.put(objective.objectiveId(), objective) != null) {
            throw new IllegalArgumentException(
                "duplicate objective id: " + objective.objectiveId());
          }
        });
    objectives.forEach(
        objective ->
            objective
                .prerequisiteObjectiveIds()
                .forEach(
                    prerequisite -> {
                      if (!byId.containsKey(prerequisite)) {
                        throw new IllegalArgumentException(
                            "unknown prerequisite objective: " + prerequisite);
                      }
                    }));
    objectives.forEach(
        objective -> detectCycle(objective.objectiveId(), byId, new HashSet<>(), new HashSet<>()));
  }

  private static void detectCycle(
      final String objectiveId,
      final Map<String, Objective> byId,
      final Set<String> visiting,
      final Set<String> visited) {
    if (visited.contains(objectiveId)) {
      return;
    }
    if (!visiting.add(objectiveId)) {
      throw new IllegalArgumentException("objective prerequisite cycle includes: " + objectiveId);
    }
    byId.get(objectiveId)
        .prerequisiteObjectiveIds()
        .forEach(prerequisite -> detectCycle(prerequisite, byId, visiting, visited));
    visiting.remove(objectiveId);
    visited.add(objectiveId);
  }

  private static String requireText(final String value, final String fieldName) {
    Objects.requireNonNull(value);
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }

  /** One executable high-level mission with explicit priority and optional dependencies. */
  public record Objective(
      String objectiveId,
      OperationalObjectiveType type,
      String territoryName,
      int priority,
      Set<String> prerequisiteObjectiveIds) {
    public Objective {
      objectiveId = requireText(objectiveId, "objectiveId");
      Objects.requireNonNull(type);
      territoryName = requireText(territoryName, "territoryName");
      if (priority < 0 || priority > 100) {
        throw new IllegalArgumentException("priority must be between 0 and 100");
      }
      Objects.requireNonNull(prerequisiteObjectiveIds);
      prerequisiteObjectiveIds =
          Collections.unmodifiableSet(new TreeSet<>(prerequisiteObjectiveIds));
      if (prerequisiteObjectiveIds.contains(objectiveId)) {
        throw new IllegalArgumentException("objective cannot depend on itself");
      }
    }
  }
}
