package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.DEFENDER_WITHDRAW;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import games.strategy.triplea.settings.ClientSetting;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;

@AllArgsConstructor
public class DefenderFightOrRetreat implements BattleStep {

  @Serial private static final long serialVersionUID = 8701119241839999078L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return isRetreatPossible() ? List.of(new StepDetails(getName(), this)) : List.of();
  }

  private boolean isRetreatPossible() {
    return canDefenderRetreat();
  }

  private boolean canDefenderRetreat() {
    return RetreatChecks.canDefenderRetreat(
        battleState.filterUnits(ALIVE, OFFENSE),
        battleState.filterUnits(ALIVE, DEFENSE),
        battleState.getGameData(),
        battleState::getDefenderRetreatTerritories,
        battleState::getBattleSite,
        battleState.getStatus().getRound());
  }

  private String getName() {
    return battleState.getPlayer(DEFENSE).getName() + DEFENDER_WITHDRAW;
  }

  @Override
  public Order getOrder() {
    return Order.DEFENDER_FIGHT_OR_RETREAT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    retreatUnits(bridge);
  }

  @RemoveOnNextMajorRelease("This doesn't need to be public in the next major release")
  public void retreatUnits(final IDelegateBridge bridge) {
    if (battleState.getStatus().isOver() || !canDefenderRetreat()) {
      return;
    }

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

    final Collection<Territory> possibleRetreatSites = battleState.getDefenderRetreatTerritories();

    battleActions
        .queryRetreatTerritory(
            battleState,
            bridge,
            battleState.getPlayer(DEFENSE),
            possibleRetreatSites,
            getQueryText(MustFightBattle.RetreatType.DEFAULT))
        .ifPresent(retreatTo -> battleState.setDefendersRetreatTo(retreatTo));
  }

  private String getQueryText(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return battleState.getPlayer(DEFENSE).getName() + " retreat?";
      case PLANES:
        return battleState.getPlayer(DEFENSE).getName() + " retreat planes?";
    }
  }
}
