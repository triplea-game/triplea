package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the encoded parameters of a movement {@link StrategicAction} back into engine objects.
 *
 * <p>Shared so that the simulation and a live AI decode an action the same way: an action generated
 * by {@link StrategicMoveCandidateGenerator} has to mean one thing regardless of who executes it.
 */
public final class StrategicActionResolver {
  private StrategicActionResolver() {}

  public static Route resolveRoute(final GameData data, final String encodedRoute) {
    if (encodedRoute == null || encodedRoute.isBlank()) {
      throw new IllegalArgumentException("route must not be blank");
    }
    final List<Territory> territories =
        List.of(encodedRoute.split(">", -1)).stream()
            .map(data.getMap()::getTerritoryOrThrow)
            .toList();
    if (territories.size() < 2) {
      throw new IllegalArgumentException("route must contain at least two territories");
    }
    return new Route(territories);
  }

  public static List<Unit> resolveUnits(final String encodedUnitIds, final Territory origin) {
    if (encodedUnitIds == null || encodedUnitIds.isBlank()) {
      throw new IllegalArgumentException("unitIds must not be blank");
    }
    final Map<UUID, Unit> available = new HashMap<>();
    origin.getUnitCollection().getUnits().forEach(unit -> available.put(unit.getId(), unit));
    final List<Unit> result = new ArrayList<>();
    for (final String encodedId : encodedUnitIds.split(",", -1)) {
      final UUID id = UUID.fromString(encodedId);
      final Unit unit = available.get(id);
      if (unit == null) {
        throw new IllegalArgumentException(
            "unit " + id + " is not present in origin " + origin.getName());
      }
      result.add(unit);
    }
    return result;
  }
}
