package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.triplea.delegate.battle.casualty.power.model.UnitTypeByPlayer;
import java.util.Collection;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.java.collections.CollectionUtils;

/**
 * Assuming we have multiple unit types all with the same effective combat power, this function will
 * decide which one is the best to choose for a casualty.
 */
@Builder
public class OolTieBreaker implements Function<Collection<UnitTypeByPlayer>, UnitTypeByPlayer> {
  @Nonnull private final CasualtyOrderOfLosses.Parameters parameters;

  @Override
  public UnitTypeByPlayer apply(final Collection<UnitTypeByPlayer> unitTypes) {
    return CollectionUtils.findMin(unitTypes, u -> parameters.getCosts().getInt(u.getUnitType()))
        .iterator()
        .next();
  }
}
