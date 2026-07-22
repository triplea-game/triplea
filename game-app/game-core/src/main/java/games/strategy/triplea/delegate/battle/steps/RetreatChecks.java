package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import org.triplea.java.PredicateBuilder;

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

  public static boolean canDefenderRetreat(
      final @Nonnull Collection<Unit> attackingUnits,
      final @Nonnull Collection<Unit> defendingUnits,
      final @Nonnull GameState gameData,
      final @Nonnull Supplier<Collection<Territory>> getDefenderRetreatTerritories,
      final @Nonnull Supplier<Territory> getBattleSite,
      final int battleRound) {
    if (onlyDefenselessTransportsLeft(attackingUnits, gameData)) {
      return false;
    }
    // We only want units that can move, be transported, or given bonus movement (no buildings
    // retreating)
    final Territory battleSite = getBattleSite.get();

    final Predicate<Unit> canMoveOrBeMoved =
        PredicateBuilder.of(Matches.unitCanMove())
            .or(
                u ->
                    // Unit can be given bonus movement by another unit in this territory
                    (u.getOwner() != null
                            && Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(
                                    battleSite, u.getOwner())
                                .test(u))
                        // Unit is already being transported
                        // TODO: Check if transporting unit has movement left for sea transports
                        || Matches.unitIsBeingTransported().test(u)
                        // Unit can be loaded onto an available transport in this
                        // territory
                        || (Matches.unitCanBeTransported().test(u)
                            && battleSite.anyUnitsMatch(Matches.unitCanTransport())))
            // cannot move aa units
            .and(Matches.unitCanMoveDuringCombatMove())
            .build();
    defendingUnits.removeIf(canMoveOrBeMoved.negate());

    // Remove units that can't defensive retreat
    defendingUnits.removeIf(Matches.unitCanDefensiveRetreat().negate());

    if (defendingUnits.isEmpty()) {
      return false;
    }
    return !getDefenderRetreatTerritories.get().isEmpty();
  }

  public static boolean getDefenderRetreatBeforeBattle(
      final @Nonnull Supplier<Integer> getDefenderRetreatRound) {
    return getDefenderRetreatRound.get() == 0;
  }

  public static boolean onlyDefenselessTransportsLeft(
      final @Nonnull Collection<Unit> units, final @Nonnull GameState gameData) {
    return Properties.getTransportCasualtiesRestricted(gameData.getProperties())
        && !units.isEmpty()
        && units.stream().allMatch(Matches.unitIsSeaTransportButNotCombatSeaTransport());
  }
}
