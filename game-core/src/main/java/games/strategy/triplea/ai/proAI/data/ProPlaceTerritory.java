package games.strategy.triplea.ai.proAI.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProPlaceTerritory {
  private final Territory territory;
  private List<Unit> defendingUnits;
  private ProBattleResult minBattleResult;
  private double defenseValue;
  private double strategicValue;
  private final List<Unit> placeUnits;
  private boolean canHold;

  ProPlaceTerritory(final Territory territory) {
    this.territory = territory;
    defendingUnits = new ArrayList<>();
    minBattleResult = new ProBattleResult();
    defenseValue = 0;
    strategicValue = 0;
    placeUnits = new ArrayList<>();
    canHold = true;
  }

  @Override
  public String toString() {
    return territory.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof ProPlaceTerritory) {
      return ((ProPlaceTerritory) o).getTerritory().equals(territory);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(territory);
  }
}
