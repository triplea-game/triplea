package games.strategy.triplea.delegate.battle.casualty.power.model;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.java.Postconditions;

@Builder(toBuilder = true)
public class UnitGroup {
  @Getter @Nonnull private final UnitTypeByPlayer unitTypeByPlayer;
  @Getter @Nonnull private final Integer strength;
  @Getter @Nonnull private final Integer diceRolls;
  @Getter @Nonnull private Integer unitCount;

  void incrementCount() {
    unitCount++;
    Postconditions.assertState(unitCount > 0);
  }

  void decrementCount() {
    unitCount--;
    Postconditions.assertState(unitCount >= 0);
  }

  boolean isEmpty() {
    return unitCount == 0;
  }
}
