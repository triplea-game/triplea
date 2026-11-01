package org.triplea.ai.benchmark.runner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Captures every territory worth recording as plain data: its owner, and its units by type split
 * into the seat's side ({@code u}) and everyone else ({@code e}). Land is always recorded, so
 * ownership can be tallied; water only when it holds units.
 */
final class TerritoryState {
  private TerritoryState() {}

  static Map<String, Object> capture(final GameData data, final List<GamePlayer> seat) {
    final Map<String, Object> territories = new TreeMap<>();
    for (final Territory territory : data.getMap().getTerritories()) {
      final Map<String, Integer> mine = new TreeMap<>();
      final Map<String, Integer> theirs = new TreeMap<>();
      for (final Unit unit : territory.getUnitCollection()) {
        (seat.contains(unit.getOwner()) ? mine : theirs)
            .merge(unit.getType().getName(), 1, Integer::sum);
      }
      if (territory.isWater() && mine.isEmpty() && theirs.isEmpty()) {
        continue;
      }
      final Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("o", territory.getOwner().getName());
      entry.put("s", seat.contains(territory.getOwner()));
      entry.put("u", mine);
      entry.put("e", theirs);
      territories.put(territory.getName(), entry);
    }
    return territories;
  }
}
