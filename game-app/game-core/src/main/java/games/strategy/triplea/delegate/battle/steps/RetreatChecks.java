package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RetreatChecks {

  public static boolean canAttackerRetreat(
      final @Nonnull Collection<Unit> defendingUnits,
      final @Nonnull GameState gameData,
      final @Nonnull Supplier<Collection<Territory>> getAttackerRetreatTerritories,
      final @Nonnull Boolean isAmphibious) {
    if (isAmphibious) {
      return false;
    }
    if (onlyDefenselessTransportsLeft(defendingUnits, gameData)) {
      return false;
    }
    return !getAttackerRetreatTerritories.get().isEmpty();
  }

  public static boolean onlyDefenselessTransportsLeft(
      final @Nonnull Collection<Unit> units, final @Nonnull GameState gameData) {
    return Properties.getTransportCasualtiesRestricted(gameData.getProperties())
        && !units.isEmpty()
        && units.stream().allMatch(Matches.unitIsSeaTransportButNotCombatSeaTransport());
  }
}
