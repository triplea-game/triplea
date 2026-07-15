package games.strategy.triplea.delegate.visibility;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Stable strategic-agent snapshot filtered for one player's current visibility. */
public record VisibilityObservation(
    int schemaVersion,
    int currentRound,
    String viewer,
    int visionRadius,
    List<TerritoryState> territories) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public VisibilityObservation {
    Objects.requireNonNull(viewer);
    territories = List.copyOf(territories);
  }

  public record TerritoryState(
      String territory,
      boolean water,
      boolean visible,
      @Nullable String owner,
      List<String> neighbors,
      List<UnitGroup> units) {
    public TerritoryState {
      Objects.requireNonNull(territory);
      neighbors = List.copyOf(neighbors);
      units = List.copyOf(units);
    }
  }

  public record UnitGroup(String owner, String unitType, int count) {
    public UnitGroup {
      Objects.requireNonNull(owner);
      Objects.requireNonNull(unitType);
      if (count < 1) {
        throw new IllegalArgumentException("Unit-group count must be positive");
      }
    }
  }
}
