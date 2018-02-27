package games.strategy.triplea.ai.proAI.data;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(of = "territory")
@Getter
@Setter
public class ProPlaceTerritory {
  private final Territory territory;
  private final List<Unit> placeUnits = new ArrayList<>();
  private List<Unit> defendingUnits = new ArrayList<>();
  private ProBattleResult minBattleResult = new ProBattleResult();
  private double defenseValue = 0;
  private double strategicValue = 0;
  private boolean canHold = true;

  ProPlaceTerritory(final Territory territory) {
    this.territory = territory;
  }

  @Override
  public String toString() {
    return territory.toString();
  }
}
