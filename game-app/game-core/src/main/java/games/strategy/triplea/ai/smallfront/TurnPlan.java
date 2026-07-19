package games.strategy.triplea.ai.smallfront;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A machine-readable operational intent that remains stable across the phases of one player turn. */
public record TurnPlan(
    String id,
    String playerName,
    String commanderIntent,
    List<Objective> objectives,
    List<FormationAssignment> assignments) {

  public TurnPlan {
    id = requireText(id, "id");
    playerName = requireText(playerName, "playerName");
    commanderIntent = Objects.requireNonNull(commanderIntent);
    objectives = List.copyOf(objectives);
    assignments = List.copyOf(assignments);

    final Set<String> objectiveIds =
        objectives.stream().map(Objective::id).collect(Collectors.toCollection(LinkedHashSet::new));
    if (objectiveIds.size() != objectives.size()) {
      throw new IllegalArgumentException("Turn plan objective ids must be unique");
    }
    for (final FormationAssignment assignment : assignments) {
      if (!objectiveIds.contains(assignment.objectiveId())) {
        throw new IllegalArgumentException(
            "Formation assignment references missing objective: " + assignment.objectiveId());
      }
    }
  }

  public static TurnPlan standPat(final String playerName) {
    return new TurnPlan(
        playerName + "-stand-pat", playerName, "Preserve the current position.", List.of(), List.of());
  }

  public Optional<Objective> primaryObjective() {
    return objectives.stream().max((left, right) -> Integer.compare(left.priority(), right.priority()));
  }

  private static String requireText(final String value, final String field) {
    Objects.requireNonNull(value);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  public record Objective(
      String id, ObjectiveType type, Set<String> targetTerritories, int priority) {

    public Objective {
      id = requireText(id, "objective id");
      type = Objects.requireNonNull(type);
      targetTerritories =
          Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(targetTerritories)));
      if (targetTerritories.isEmpty()) {
        throw new IllegalArgumentException("Objective must have at least one target territory");
      }
      if (priority < 0 || priority > 100) {
        throw new IllegalArgumentException("Objective priority must be between 0 and 100");
      }
    }
  }

  public record FormationAssignment(String id, Set<String> unitIds, String objectiveId) {

    public FormationAssignment {
      id = requireText(id, "formation assignment id");
      unitIds = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(unitIds)));
      objectiveId = requireText(objectiveId, "formation objective id");
    }
  }

  public enum ObjectiveType {
    CAPTURE,
    GAIN_AIR_SUPERIORITY,
    HOLD,
    REDEPLOY
  }
}