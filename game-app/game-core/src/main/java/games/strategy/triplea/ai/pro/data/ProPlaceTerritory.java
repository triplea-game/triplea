package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/** The result of an AI placement analysis for a single territory. */
@EqualsAndHashCode(of = "territory")
@Getter
@Setter
public class ProPlaceTerritory {
  private final @NotNull Territory territory;
  private final List<Unit> placeUnits = new ArrayList<>();
  private List<Unit> defendingUnits = new ArrayList<>();
  private ProBattleResult minBattleResult = new ProBattleResult();
  private double defenseValue = 0;
  private double strategicValue = 0;
  private boolean canHold = true;

  ProPlaceTerritory(final @NotNull Territory territory) {
    this.territory = territory;
  }

  @Override
  public String toString() {
    return territory.toString();
  }
}
