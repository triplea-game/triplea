package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;

@UtilityClass
public class RetreatChecks {
  public static boolean canAttackerRetreatSubs(
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull Collection<Unit> defendingWaitingToDie,
      final @NonNull GameData gameData,
      final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories,
      final @NonNull Boolean isAmphibious) {
    if (defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return defendingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (canAttackerRetreat(
                defendingUnits, gameData, getAttackerRetreatTerritories, isAmphibious)
            || Properties.getSubmersibleSubs(gameData));
  }

  public static boolean canDefenderRetreatSubs(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull Collection<Unit> attackingWaitingToDie,
      final @NonNull GamePlayer defender,
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData,
      final @NonNull BiFunction<GamePlayer, Collection<Unit>, Collection<Territory>>
              getEmptyOrFriendlySeaNeighbors) {
    if (attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return attackingWaitingToDie.stream().noneMatch(Matches.unitIsDestroyer())
        && (getEmptyOrFriendlySeaNeighbors
                    .apply(
                        defender,
                        CollectionUtils.getMatches(defendingUnits, Matches.unitCanEvade()))
                    .size()
                != 0
            || Properties.getSubmersibleSubs(gameData));
  }

  public static boolean canAttackerRetreatPartialAmphib(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull GameData gameData,
      final @NonNull Boolean isAmphibious) {
    if (isAmphibious && Properties.getPartialAmphibiousRetreat(gameData)) {
      // Only include land units when checking for allow amphibious retreat
      final List<Unit> landUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsLand());
      for (final Unit unit : landUnits) {
        if (!unit.getWasAmphibious()) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean canAttackerRetreatPlanes(
      final @NonNull Collection<Unit> attackingUnits,
      final @NonNull GameData gameData,
      final @NonNull Boolean isAmphibious) {
    // TODO: BUG? Why must it be amphibious to retreat planes
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && isAmphibious
        && attackingUnits.stream().anyMatch(Matches.unitIsAir());
  }

  public static boolean canAttackerRetreat(
      final @NonNull Collection<Unit> defendingUnits,
      final @NonNull GameData gameData,
      final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories,
      final @NonNull Boolean isAmphibious) {
    if (onlyDefenselessDefendingTransportsLeft(defendingUnits, gameData)) {
      return false;
    }
    if (isAmphibious) {
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
