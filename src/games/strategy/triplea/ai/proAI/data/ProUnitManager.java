package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProUnitManager {

  final Map<Unit, Set<Territory>> unitAttackMap = new HashMap<Unit, Set<Territory>>();
  final Map<Unit, Set<Territory>> transportAttackMap = new HashMap<Unit, Set<Territory>>();
  final Map<Unit, Set<Territory>> bombardMap = new HashMap<Unit, Set<Territory>>();
  final List<ProTransport> transportMapList = new ArrayList<ProTransport>();

  public ProUnitManager() {

  }

}
