package games.strategy.triplea.delegate.battle.steps.retreat;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.Collection;
import java.util.Map;

interface Retreater {

  enum RetreatLocation {
    SAME_TERRITORY,
    OTHER_TERRITORY;
  }

  Collection<Unit> getRetreatUnits();

  Collection<Territory> getPossibleRetreatSites(Collection<Unit> retreatUnits);

  String getQueryText();

  MustFightBattle.RetreatType getRetreatType();

  Map<RetreatLocation, Collection<Unit>> splitRetreatUnits(Collection<Unit> retreatUnits);

  Change extraRetreatChange(Territory retreatTo, Collection<Unit> retreatUnits);

  String getShortBroadcastSuffix();

  String getLongBroadcastSuffix(Territory retreatTo);
}
