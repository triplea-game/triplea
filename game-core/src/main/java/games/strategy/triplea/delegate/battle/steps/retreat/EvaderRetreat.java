package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.triplea.sound.SoundUtils;

/** Utility class for handling retreating or submerging of `canEvade` units */
@UtilityClass
public class EvaderRetreat {

  @Builder(toBuilder = true)
  public static class Parameters {
    final @NonNull BattleState battleState;
    final @NonNull BattleActions battleActions;
    final @NonNull BattleState.Side side;
    final @NonNull IDelegateBridge bridge;
    final @NonNull Collection<Unit> units;
  }

  public static void retreatUnits(
      final Parameters parameters,
      final Collection<Territory> possibleRetreatSites,
      final String step) {
    final GamePlayer retreatingPlayer =
        parameters.side == DEFENSE
            ? parameters.battleState.getDefender()
            : parameters.battleState.getAttacker();
    final String text = retreatingPlayer.getName() + " retreat subs?";

    parameters
        .bridge
        .getDisplayChannelBroadcaster()
        .gotoBattleStep(parameters.battleState.getBattleId(), step);

    final boolean isAttemptingSubmerge =
        possibleRetreatSites.size() == 1
            && possibleRetreatSites.contains(parameters.battleState.getBattleSite());
    final Territory retreatTo =
        isAttemptingSubmerge
            ? parameters.battleActions.querySubmergeTerritory(
                parameters.battleState,
                parameters.bridge,
                retreatingPlayer,
                possibleRetreatSites,
                text)
            : parameters.battleActions.queryRetreatTerritory(
                parameters.battleState,
                parameters.bridge,
                retreatingPlayer,
                possibleRetreatSites,
                text);
    if (retreatTo != null) {
      retreatUnits(parameters, step, retreatTo);
    }
  }

  private static void retreatUnits(
      final Parameters parameters, final String step, final Territory retreatTo) {
    final GamePlayer retreatingPlayer =
        parameters.side == DEFENSE
            ? parameters.battleState.getDefender()
            : parameters.battleState.getAttacker();

    SoundUtils.playRetreatType(
        retreatingPlayer, parameters.units, MustFightBattle.RetreatType.SUBS, parameters.bridge);

    final String shortMessage;
    final String longMessage;
    if (parameters.battleState.getBattleSite().equals(retreatTo)) {
      submergeEvaders(parameters);
      longMessage = shortMessage = retreatingPlayer.getName() + " submerges subs";
    } else {
      retreatEvaders(parameters, retreatTo);
      shortMessage = retreatingPlayer.getName() + " retreats";
      longMessage = retreatingPlayer.getName() + " retreats subs to " + retreatTo.getName();
    }

    parameters
        .bridge
        .getDisplayChannelBroadcaster()
        .notifyRetreat(shortMessage, longMessage, step, retreatingPlayer);
  }

  public static void submergeEvaders(final Parameters parameters) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : parameters.units) {
      change.add(ChangeFactory.unitPropertyChange(u, true, Unit.SUBMERGED));
    }
    parameters.bridge.addChange(change);
    parameters.battleState.retreatUnits(parameters.side, parameters.units);

    addHistoryRetreat(parameters.bridge, parameters.units, " submerged");
    notifyRetreat(
        parameters.battleState,
        parameters.battleActions,
        parameters.units,
        parameters.side,
        parameters.bridge);
  }

  private static void addHistoryRetreat(
      final IDelegateBridge bridge, final Collection<Unit> units, final String suffix) {
    final String transcriptText = MyFormatter.unitsToText(units) + suffix;
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private static void notifyRetreat(
      final BattleState battleState,
      final BattleActions battleActions,
      final Collection<Unit> retreating,
      final BattleState.Side side,
      final IDelegateBridge bridge) {
    if (battleState.getUnits(side).isEmpty()) {
      battleActions.endBattle(side.getOpposite().getWhoWon(), bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreating);
    }
  }

  private static void retreatEvaders(final Parameters parameters, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    change.add(
        ChangeFactory.moveUnits(
            parameters.battleState.getBattleSite(), retreatTo, parameters.units));
    parameters.bridge.addChange(change);
    parameters.battleState.retreatUnits(parameters.side, parameters.units);

    addHistoryRetreat(parameters.bridge, parameters.units, " retreated to " + retreatTo.getName());
    notifyRetreat(
        parameters.battleState,
        parameters.battleActions,
        parameters.units,
        parameters.side,
        parameters.bridge);
  }
}
