package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.DEFENDER_WITHDRAW;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public class DefensiveGeneralRetreat implements BattleStep {

  private static final long serialVersionUID = 7604976769071072256L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return (battleState.getDefendersRetreatTo() != null)
        ? List.of(new StepDetails(getName(), this))
        : List.of();
  }

  private String getName() {
    return battleState.getPlayer(DEFENSE).getName() + DEFENDER_WITHDRAW;
  }

  @Override
  public Order getOrder() {
    return Order.DEFENSIVE_GENERAL_RETREAT;
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

    final Retreater retreater;
    final Territory retreatTo = battleState.getDefendersRetreatTo();

    if (retreatTo != null) {
      retreater = new RetreaterGeneral(battleState, DEFENSE);
    } else {
      retreater = null;
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

      // We only want units that can move, be transported, or given bonus movement (no buildings
      // retreating)
      final Territory battleSite = battleState.getBattleSite();
      final Predicate<Unit> canMoveOrBeMoved =
          PredicateBuilder.of(Matches.unitCanMove())
              .or(
                  u ->
                      // Unit can be given bonus movement by another unit in this territory
                      Matches.unitCanBeGivenBonusMovementByFacilitiesInItsTerritory(
                                  battleSite, u.getOwner())
                              .test(u)
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
      retreater.getRetreatUnits().removeIf(canMoveOrBeMoved.negate());

      // Remove units that can't defensive retreat this round
      for (Unit unit : retreater.getRetreatUnits())
      {
        final int defensiveRetreatBattleRound = unit.getUnitAttachment().getDefensiveRetreatBattleRound();
        if (defensiveRetreatBattleRound != 0 && defensiveRetreatBattleRound < battleState.getStatus().getRound())
          retreater.getRetreatUnits().remove(unit);
      }

      if (!retreater.getRetreatUnits().isEmpty())
        retreat(bridge, retreater, retreatTo);
    }
  }

  private void retreat(
      final IDelegateBridge bridge, final Retreater retreater, final Territory retreatTo) {
    Collection<Unit> retreatUnits = retreater.getRetreatUnits();

    SoundUtils.playRetreatType(
        battleState.getPlayer(DEFENSE), retreatUnits, retreater.getRetreatType(), bridge);

    final Retreater.RetreatChanges retreatChanges = retreater.computeChanges(retreatTo);
    bridge.addChange(retreatChanges.getChange());

    retreatChanges
        .getHistoryText()
        .forEach(
            historyChild ->
                bridge
                    .getHistoryWriter()
                    .addChildToEvent(historyChild.getText(), historyChild.getUnits()));

    if (battleState.filterUnits(ALIVE, DEFENSE).isEmpty()) {
      battleActions.endBattle(IBattle.WhoWon.ATTACKER, bridge);
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
        battleState.getPlayer(DEFENSE).getName()
            + getShortBroadcastSuffix(retreater.getRetreatType());

    final String longMessage =
        battleState.getPlayer(DEFENSE).getName()
            + getLongBroadcastSuffix(retreater.getRetreatType(), retreatTo);

    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      bridge.sendMessage(
          IDisplay.NotifyRetreatMessage.builder()
              .shortMessage(shortMessage)
              .message(longMessage)
              .step(getName())
              .retreatingPlayerName(battleState.getPlayer(DEFENSE).getName())
              .build());
    } else {
      bridge
          .getDisplayChannelBroadcaster()
          .notifyRetreat(shortMessage, longMessage, getName(), battleState.getPlayer(DEFENSE));
    }
  }

  private String getShortBroadcastSuffix(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return " retreats";
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
      case PLANES:
        return " retreats planes";
    }
  }
}
