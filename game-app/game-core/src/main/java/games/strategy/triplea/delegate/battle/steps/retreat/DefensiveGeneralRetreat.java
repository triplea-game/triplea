package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.DEFENDER_WITHDRAW;

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
public class DefensiveGeneralRetreat implements BattleStep {

    private static final long serialVersionUID = 7604976769071072256L;

    protected final BattleState battleState;

    protected final BattleActions battleActions;

    @Override
    public List<StepDetails> getAllStepDetails() {
        return isRetreatPossible() ? List.of(new StepDetails(getName(), this)) : List.of();
    }

    private boolean isRetreatPossible() {
        return Properties.getGeneralDefendersCanRetreat(battleState.getGameData().getProperties())
                && (canDefenderRetreat() || canDefenderRetreatSeaPlanes());
    }

    private boolean canDefenderRetreat() {
        return RetreatChecks.canDefenderRetreat(
                battleState.filterUnits(ALIVE, OFFENSE),
                battleState.getGameData(),
                battleState::getDefenderRetreatTerritories);
    }

    private boolean canDefenderRetreatSeaPlanes() {
        return battleState.getBattleSite().isWater()
                && battleState.filterUnits(ALIVE, DEFENSE).stream().anyMatch(Matches.unitIsAir());
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

        if (canDefenderRetreat()) {
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

            final Collection<Territory> possibleRetreatSites =
                    battleState.getDefenderRetreatTerritories();

            battleActions.queryRetreatTerritory(
                    battleState,
                    bridge,
                    battleState.getPlayer(DEFENSE),
                    possibleRetreatSites,
                    getQueryText(retreater.getRetreatType()))
                    .ifPresent(retreatTo -> retreat(bridge, retreater, retreatTo));
        }
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

    private void retreat(
            final IDelegateBridge bridge, final Retreater retreater, final Territory retreatTo) {
        final Collection<Unit> retreatUnits = retreater.getRetreatUnits();

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