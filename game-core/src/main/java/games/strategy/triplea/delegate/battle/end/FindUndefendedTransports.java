package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Check for unescorted transports and return them and enemy units with them
 */
@Builder
public class FindUndefendedTransports {

  private @NonNull final GamePlayer player;
  private @NonNull final Boolean isAttacker;
  private @NonNull final Boolean hasRetreatTerritories;
  private @NonNull final Collection<Unit> friendlyUnits;
  private @NonNull final GameData gameData;
  private @NonNull final Territory battleSite;

  @Value(staticConstructor = "of")
  public static class Result {
    Collection<Unit> enemyUnits;
    Collection<Unit> transports;
  }

  private final Result emptyResult = Result.of(List.of(), List.of());

  public Result find() {
    if (isRetreatPossible()) {
      return emptyResult;
    }
    // Get all allied transports in the territory
    final List<Unit> alliedTransports = getAlliedTransports();
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return emptyResult;
    }
    // Get all ALLIED, sea & air units in the territory (that are NOT submerged)
    final Collection<Unit> alliedUnits = getAlliedUnits();
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      // Get all the ENEMY sea and air units (that can attack) in the territory
      final Collection<Unit> enemyUnits = getEnemyUnits();
      if (!enemyUnits.isEmpty()) {
        return Result.of(enemyUnits, alliedTransports);
      }
    }
    return emptyResult;
  }

  private boolean isRetreatPossible() {
    // if we are the attacker, we can retreat instead of dying
    return isAttacker && (hasRetreatTerritories || friendlyUnits.stream().anyMatch(Matches.unitIsAir()));
  }

  private Collection<Unit> getEnemyUnits() {
    final Predicate<Unit> enemyUnitsMatch =
        Matches.unitIsNotLand()
            .and(Matches.unitIsSubmerged().negate())
            .and(Matches.unitCanAttack(player));
    return CollectionUtils.getMatches(battleSite.getUnits(), enemyUnitsMatch);
  }

  private Collection<Unit> getAlliedUnits() {
    final Predicate<Unit> alliedUnitsMatch =
        Matches.isUnitAllied(player, gameData)
            .and(Matches.unitIsNotLand())
            .and(Matches.unitIsSubmerged().negate());
    return CollectionUtils.getMatches(battleSite.getUnits(), alliedUnitsMatch);
  }

  private List<Unit> getAlliedTransports() {
    final Predicate<Unit> matchAllied =
        Matches.unitIsTransport()
            .and(Matches.unitIsNotCombatTransport())
            .and(Matches.isUnitAllied(player, gameData))
            .and(Matches.unitIsSea());
    return CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
  }
}
