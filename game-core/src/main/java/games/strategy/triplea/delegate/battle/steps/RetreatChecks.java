package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RetreatChecks {

  public static boolean canAttackerRetreat(
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData,
      final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories,
      final @NonNull Boolean isAmphibious) {
    if (isAmphibious) {
      return false;
    }
    if (onlyDefenselessDefendingTransportsLeft(defendingUnits, gameData)) {
      return false;
    }
    return !getAttackerRetreatTerritories.get().isEmpty();
  }

  public static boolean onlyDefenselessDefendingTransportsLeft(
      final @NonNull Collection<Unit> defendingUnits, final @NonNull GameData gameData) {
    return Properties.getTransportCasualtiesRestricted(gameData)
        && !defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsTransportButNotCombatTransport());
  }
}
