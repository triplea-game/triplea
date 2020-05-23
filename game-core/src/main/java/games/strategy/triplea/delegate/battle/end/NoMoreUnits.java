package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class NoMoreUnits {

  public enum Winner {
    ATTACKER,
    DEFENDER,
    DEFENDER_TURN_1,
    UNDETERMINED
  }

  private @NonNull final GameData gameData;
  private @NonNull final GamePlayer attacker;
  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final Collection<Unit> defendingUnits;
  private @NonNull final Territory battleSite;
  private final int round;
  private @NonNull final Runnable checkUndefendedTransports;
  private @NonNull final Runnable checkForUnitsThatCanRollLeft;

  public Winner check() {
    final Predicate<Unit> notInfrastructure = Matches.unitIsNotInfrastructure();
    if (CollectionUtils.getMatches(attackingUnits, notInfrastructure).isEmpty()) {
      return noAttackingUnits();
    } else if (CollectionUtils.getMatches(defendingUnits, notInfrastructure).isEmpty()) {
      return noDefendingUnits();
    }
    return Winner.UNDETERMINED;
  }

  private Winner noAttackingUnits() {
    final Winner result;
    if (!Properties.getTransportCasualtiesRestricted(gameData)) {
      result = Winner.DEFENDER;
    } else {
      // Get all allied transports in the territory
      final Predicate<Unit> matchAllied =
          Matches.unitIsTransport()
              .and(Matches.unitIsNotCombatTransport())
              .and(Matches.isUnitAllied(attacker, gameData));
      final List<Unit> alliedTransports =
          CollectionUtils.getMatches(battleSite.getUnits(), matchAllied);
      // If no transports, just end the battle
      if (alliedTransports.isEmpty()) {
        result = Winner.DEFENDER;
      } else if (round <= 1) {
        result = Winner.DEFENDER_TURN_1;
      } else {
        result = Winner.DEFENDER;
      }
    }
    return result;
  }

  private Winner noDefendingUnits() {
    if (Properties.getTransportCasualtiesRestricted(gameData)) {
      // If there are undefended attacking transports, determine if they automatically die
      checkUndefendedTransports.run();
    }
    checkForUnitsThatCanRollLeft.run();
    return Winner.ATTACKER;
  }
}
