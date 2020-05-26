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

/**
 * Detect if there is a winner because there are no more units on the opposing side.
 *
 * <p>checkUndefendedTransports and checkForUnitsThatCanRollLeft will be called if the defender has
 * no more fighting units so that undefended units can be cleaned up.
 */
@Builder
public class NoMoreUnits {

  public enum Winner {
    ATTACKER,
    DEFENDER,
    DEFENDER_TURN_1,
    NONE
  }

  private @NonNull final GameData gameData;
  private @NonNull final GamePlayer attacker;
  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final Collection<Unit> defendingUnits;
  private @NonNull final Territory battleSite;
  private @NonNull final Integer round;
  private @NonNull final Runnable checkUndefendedTransports;
  private @NonNull final Runnable checkForUnitsThatCanRollLeft;

  public Winner check() {
    if (attackingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure())) {
      return noAttackingUnits();
    } else if (defendingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure())) {
      return noDefendingUnits();
    }
    return Winner.NONE;
  }

  private Winner noAttackingUnits() {
    final Winner result;
    if (!Properties.getTransportCasualtiesRestricted(gameData)) {
      result = Winner.DEFENDER;
    } else {
      if (round <= 1) {
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
