package games.strategy.triplea.delegate.battle.steps.retreat;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.Collection;
import java.util.List;
import lombok.Value;

interface Retreater {

  @Value(staticConstructor = "of")
  class RetreatChanges {
    Change change;
    List<RetreatHistoryChild> historyText;
  }

  @Value(staticConstructor = "of")
  class RetreatHistoryChild {
    String text;
    Collection<Unit> units;
  }

  Collection<Unit> getRetreatUnits();

  Collection<Territory> getPossibleRetreatSites(Collection<Unit> retreatUnits);

  MustFightBattle.RetreatType getRetreatType();

  RetreatChanges computeChanges(Territory retreatTo);
}
