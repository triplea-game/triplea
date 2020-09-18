package games.strategy.triplea.delegate.battle.steps.retreat;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.triplea.sound.SoundUtils;

@UtilityClass
public class EvaderRetreat {

  public static void retreatUnits(
      final BattleState battleState,
      final BattleActions battleActions,
      final BattleState.Side side,
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final Collection<Territory> possibleRetreatSites,
      final Collection<Unit> units,
      final String step) {
    final boolean canDefendingSubsSubmergeOrRetreat =
        side == BattleState.Side.DEFENSE
            && Properties.getSubmarinesDefendingMaySubmergeOrRetreat(battleState.getGameData());
    final boolean submerge = Properties.getSubmersibleSubs(battleState.getGameData());

    final String text = retreatingPlayer.getName() + " retreat subs?";

    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), step);
    final Territory retreatTo =
        battleActions.queryRetreatTerritory(
            battleState,
            bridge,
            retreatingPlayer,
            possibleRetreatSites,
            (canDefendingSubsSubmergeOrRetreat || submerge),
            text);
    if (retreatTo == null) {
      return;
    }
    playSound(battleState, bridge, retreatingPlayer, units);
    if (battleState.getBattleSite().equals(retreatTo)) {
      submergeEvaders(battleState, battleActions, units, side, bridge);
      broadcastRetreat(bridge, retreatingPlayer, step, " submerges subs");
    } else {
      retreatEvaders(battleState, battleActions, units, retreatTo, side, bridge);
      broadcastRetreat(
          bridge, retreatingPlayer, step, " retreats", " retreats subs to " + retreatTo.getName());
    }
  }

  private static void playSound(
      final BattleState battleState,
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final Collection<Unit> units) {
    if (battleState.isHeadless()) {
      return;
    }
    SoundUtils.playRetreatType(retreatingPlayer, units, MustFightBattle.RetreatType.SUBS, bridge);
  }

  public static void submergeEvaders(
      final BattleState battleState,
      final BattleActions battleActions,
      final Collection<Unit> submerging,
      final BattleState.Side side,
      final IDelegateBridge bridge) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : submerging) {
      change.add(ChangeFactory.unitPropertyChange(u, true, Unit.SUBMERGED));
    }
    bridge.addChange(change);
    battleState.retreatUnits(side, submerging);

    addHistoryRetreat(bridge, submerging, " submerged");
    notifyRetreat(battleState, battleActions, submerging, side, bridge);
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
      battleActions.endBattle(side.getOpposite().won(), bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreating);
    }
  }

  private static void broadcastRetreat(
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final String step,
      final String messageShortSuffix) {
    broadcastRetreat(bridge, retreatingPlayer, step, messageShortSuffix, messageShortSuffix);
  }

  private static void broadcastRetreat(
      final IDelegateBridge bridge,
      final GamePlayer retreatingPlayer,
      final String step,
      final String messageShortSuffix,
      final String messageLongSuffix) {
    final String messageShort = retreatingPlayer.getName() + messageShortSuffix;
    final String messageLong = retreatingPlayer.getName() + messageLongSuffix;
    bridge
        .getDisplayChannelBroadcaster()
        .notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
  }

  private static void retreatEvaders(
      final BattleState battleState,
      final BattleActions battleActions,
      final Collection<Unit> retreating,
      final Territory to,
      final BattleState.Side side,
      final IDelegateBridge bridge) {

    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(battleState.getBattleSite(), to, retreating));
    bridge.addChange(change);
    battleState.retreatUnits(side, retreating);

    addHistoryRetreat(bridge, retreating, " retreated to " + to.getName());
    notifyRetreat(battleState, battleActions, retreating, side, bridge);
  }
}
