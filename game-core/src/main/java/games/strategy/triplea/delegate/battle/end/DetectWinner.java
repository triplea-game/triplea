package games.strategy.triplea.delegate.battle.end;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;

/**
 * Detect if there is a winner because there are no more units on the opposing side.
 *
 * <p>checkUndefendedTransports and checkForUnitsThatCanRollLeft will be called if the defender has
 * no more fighting units so that undefended units can be cleaned up.
 */
@Builder
public class DetectWinner {

  public enum Winner {
    ATTACKER,
    DEFENDER,
    NOT_YET
  }

  private @NonNull final GameData gameData;
  private @NonNull final Collection<Unit> attackingUnits;
  private @NonNull final Collection<Unit> defendingUnits;
  private @NonNull final Runnable removeUndefendedTransports;
  private @NonNull final Runnable removeUnitsWithNoRollsLeft;

  public Winner detect() {
    if (attackingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure())) {
      return noAttackingUnits();
    } else if (defendingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure())) {
      return noDefendingUnits();
    }
    return Winner.NOT_YET;
  }

  private Winner noAttackingUnits() {
    return Winner.DEFENDER;
  }

  private Winner noDefendingUnits() {
    if (Properties.getTransportCasualtiesRestricted(gameData)) {
      // handle undefended transports
      removeUndefendedTransports.run();
    }
    removeUnitsWithNoRollsLeft.run();
    return Winner.ATTACKER;
  }
}
