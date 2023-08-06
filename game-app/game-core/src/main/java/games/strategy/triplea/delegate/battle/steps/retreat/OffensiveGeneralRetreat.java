package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public class OffensiveGeneralRetreat implements BattleStep {

  private static final long serialVersionUID = -9192684621899682418L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return isRetreatPossible() ? List.of(new StepDetails(getName(), this)) : List.of();
  }

  private boolean isRetreatPossible() {
    return canAttackerRetreat()
        || canAttackerRetreatSeaPlanes()
        || (battleState.getStatus().isAmphibious()
            && (canAttackerRetreatPartialAmphib() || canAttackerRetreatAmphibPlanes()));
  }

  private boolean canAttackerRetreat() {
    return RetreatChecks.canAttackerRetreat(
        battleState.filterUnits(ALIVE, DEFENSE),
        battleState.getGameData(),
        battleState::getAttackerRetreatTerritories,
        battleState.getStatus().isAmphibious());
  }

  private boolean canAttackerRetreatSeaPlanes() {
    return battleState.getBattleSite().isWater()
        && battleState.filterUnits(ALIVE, OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (!Properties.getPartialAmphibiousRetreat(battleState.getGameData().getProperties())) {
      return false;
    }
    // Only include land units when checking for allow amphibious retreat
    return battleState.filterUnits(ALIVE, OFFENSE).stream()
        .filter(Matches.unitIsLand())
        .anyMatch(Matches.unitWasNotAmphibious());
  }

  private boolean canAttackerRetreatAmphibPlanes() {
    final GameState gameData = battleState.getGameData();
    return (Properties.getWW2V2(gameData.getProperties())
            || Properties.getAttackerRetreatPlanes(gameData.getProperties())
            || Properties.getPartialAmphibiousRetreat(gameData.getProperties()))
        && battleState.filterUnits(ALIVE, OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private String getName() {
    return battleState.getPlayer(OFFENSE).getName() + ATTACKER_WITHDRAW;
  }

  @Override
  public Order getOrder() {
    return Order.OFFENSIVE_GENERAL_RETREAT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    retreatUnits(bridge);
  }

  @RemoveOnNextMajorRelease("This doesn't need to be public in the next major release")
  public void retreatUnits(final IDelegateBridge bridge) {
    if (battleState.getStatus().isOver()) {
      return;
    }

    Retreater retreater = null;

    if (battleState.getStatus().isAmphibious()) {
      retreater = getAmphibiousRetreater();
    } else if (canAttackerRetreat()) {
      retreater = new RetreaterGeneral(battleState);
    }

    if (retreater != null) {
      final String stepName = getName();
      // Only send the gotoBattleStep message if the step exists in the UI. It will not exist in the
      // case where normally retreat is not possible but only becomes possible when there are only
      // planes left.
      if (Optional.ofNullable(battleState.getStepStrings()).orElse(List.of()).contains(stepName)) {
        final var battleId = battleState.getBattleId();
        if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
          bridge.sendMessage(new IDisplay.GoToBattleStepMessage(battleId.toString(), stepName));
        } else {
          bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleId, stepName);
        }
      }

      final Collection<Unit> retreatUnits = retreater.getRetreatUnits();
      final Collection<Territory> possibleRetreatSites =
          retreater.getPossibleRetreatSites(retreatUnits);
      final Territory retreatTo =
          battleActions.queryRetreatTerritory(
              battleState,
              bridge,
              battleState.getPlayer(OFFENSE),
              possibleRetreatSites,
              getQueryText(retreater.getRetreatType()));

      if (retreatTo != null) {
        retreat(bridge, retreater, retreatTo);
      }
    }
  }

  private Retreater getAmphibiousRetreater() {
    if (canAttackerRetreatPartialAmphib()) {
      return new RetreaterPartialAmphibious(battleState);
    } else if (canAttackerRetreatAmphibPlanes()) {
      return new RetreaterAirAmphibious(battleState);
    }
    return null;
  }

  private String getQueryText(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return battleState.getPlayer(OFFENSE).getName() + " retreat?";
      case PARTIAL_AMPHIB:
        return battleState.getPlayer(OFFENSE).getName() + " retreat non-amphibious units?";
      case PLANES:
        return battleState.getPlayer(OFFENSE).getName() + " retreat planes?";
    }
  }

  private void retreat(
      final IDelegateBridge bridge, final Retreater retreater, final Territory retreatTo) {
    final Collection<Unit> retreatUnits = retreater.getRetreatUnits();

    SoundUtils.playRetreatType(
        battleState.getPlayer(OFFENSE), retreatUnits, retreater.getRetreatType(), bridge);

    final Retreater.RetreatChanges retreatChanges = retreater.computeChanges(retreatTo);
    bridge.addChange(retreatChanges.getChange());

    retreatChanges
        .getHistoryText()
        .forEach(
            historyChild ->
                bridge
                    .getHistoryWriter()
                    .addChildToEvent(historyChild.getText(), historyChild.getUnits()));

    if (battleState.filterUnits(ALIVE, OFFENSE).isEmpty()) {
      battleActions.endBattle(IBattle.WhoWon.DEFENDER, bridge);
    } else {
      if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
        bridge.sendMessage(
            new IDisplay.NotifyUnitsRetreatingMessage(battleState.getBattleId(), retreatUnits));
      } else {
        bridge
            .getDisplayChannelBroadcaster()
            .notifyRetreat(battleState.getBattleId(), retreatUnits);
      }
    }

    final String shortMessage =
        battleState.getPlayer(OFFENSE).getName()
            + getShortBroadcastSuffix(retreater.getRetreatType());

    final String longMessage =
        battleState.getPlayer(OFFENSE).getName()
            + getLongBroadcastSuffix(retreater.getRetreatType(), retreatTo);

    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      bridge.sendMessage(
          IDisplay.NotifyRetreatMessage.builder()
              .shortMessage(shortMessage)
              .message(longMessage)
              .step(getName())
              .retreatingPlayerName(battleState.getPlayer(OFFENSE).getName())
              .build());
    } else {
      bridge
          .getDisplayChannelBroadcaster()
          .notifyRetreat(shortMessage, longMessage, getName(), battleState.getPlayer(OFFENSE));
    }
  }

  private String getShortBroadcastSuffix(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return " retreats";
      case PARTIAL_AMPHIB:
        return " retreats non-amphibious units";
      case PLANES:
        return " retreats planes";
    }
  }

  private String getLongBroadcastSuffix(
      final MustFightBattle.RetreatType retreatType, final Territory retreatTo) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return " retreats all units to " + retreatTo.getName();
      case PARTIAL_AMPHIB:
        return " retreats non-amphibious units";
      case PLANES:
        return " retreats planes";
    }
  }
}
