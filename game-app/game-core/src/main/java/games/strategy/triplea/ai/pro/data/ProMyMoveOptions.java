package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/** The result of an AI movement analysis for its own possible moves. */
@Getter
public class ProMyMoveOptions {
  private final Map<Territory, ProTerritory> territoryMap = new HashMap<>();
  private final Map<Unit, Set<Territory>> unitMoveMap = new HashMap<>();
  private final Map<Unit, Set<Territory>> transportMoveMap = new HashMap<>();
  private final Map<Unit, Set<Territory>> bombardMap = new HashMap<>();
  private final List<ProTransport> transportList = new ArrayList<>();
  private final Map<Unit, Set<Territory>> bomberMoveMap = new HashMap<>();

  ProMyMoveOptions() {}

  ProMyMoveOptions(final ProMyMoveOptions myMoveOptions, final ProData proData) {
    for (final Territory t : myMoveOptions.territoryMap.keySet()) {
      territoryMap.put(t, new ProTerritory(myMoveOptions.territoryMap.get(t), proData));
    }
    unitMoveMap.putAll(myMoveOptions.unitMoveMap);
    transportMoveMap.putAll(myMoveOptions.transportMoveMap);
    bombardMap.putAll(myMoveOptions.bombardMap);
    transportList.addAll(myMoveOptions.transportList);
    bomberMoveMap.putAll(myMoveOptions.bomberMoveMap);
  }
}
