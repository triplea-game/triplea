package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public class OffensiveGeneralRetreat implements BattleStep {

  private static final long serialVersionUID = -9192684621899682418L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return isRetreatPossible() ? List.of(getName()) : List.of();
  }

  private boolean isRetreatPossible() {
    return canAttackerRetreat()
        || canAttackerRetreatSeaPlanes()
        || (battleState.isAmphibious()
            && (canAttackerRetreatPartialAmphib() || canAttackerRetreatAmphibPlanes()));
  }

  private boolean canAttackerRetreat() {
    return RetreatChecks.canAttackerRetreat(
        battleState.getUnits(BattleState.Side.DEFENSE),
        battleState.getGameData(),
        battleState::getAttackerRetreatTerritories,
        battleState.isAmphibious());
  }

  private boolean canAttackerRetreatSeaPlanes() {
    return battleState.getBattleSite().isWater()
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (!Properties.getPartialAmphibiousRetreat(battleState.getGameData())) {
      return false;
    }
    // Only include land units when checking for allow amphibious retreat
    return battleState.getUnits(BattleState.Side.OFFENSE).stream()
        .filter(Matches.unitIsLand())
        .anyMatch(Matches.unitWasNotAmphibious());
  }

  private boolean canAttackerRetreatAmphibPlanes() {
    final GameData gameData = battleState.getGameData();
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private String getName() {
    return battleState.getAttacker().getName() + ATTACKER_WITHDRAW;
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
    if (battleState.isOver()) {
      return;
    }

    final Retreater retreater;

    if (battleState.isAmphibious()) {
      if (canAttackerRetreatPartialAmphib()) {
        retreater = new RetreaterPartialAmphibious(battleState);
      } else if (canAttackerRetreatAmphibPlanes()) {
        retreater = new RetreaterAirAmphibious(battleState);
      } else {
        return;
      }
    } else if (canAttackerRetreat()) {
      retreater = new RetreaterGeneral(battleState);
    } else {
      return;
    }

    retreat(bridge, retreater);
  }

  private void retreat(final IDelegateBridge bridge, final Retreater retreater) {
    final Collection<Unit> retreatUnits = retreater.getRetreatUnits();
    final Collection<Territory> possibleRetreatSites =
        retreater.getPossibleRetreatSites(retreatUnits);
    final String text = retreater.getQueryText();

    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), getName());
    final Territory retreatTo =
        battleActions.queryRetreatTerritory(
            battleState, bridge, battleState.getAttacker(), possibleRetreatSites, text);
    if (retreatTo == null) {
      return;
    }
    if (!battleState.isHeadless()) {
      SoundUtils.playRetreatType(
          battleState.getAttacker(), retreatUnits, retreater.getRetreatType(), bridge);
    }

    final CompositeChange change = new CompositeChange();

    final Change extraRetreatChange = retreater.extraRetreatChange(retreatTo, retreatUnits);
    if (!extraRetreatChange.isEmpty()) {
      change.add(extraRetreatChange);
    }

    final Map<Retreater.RetreatLocation, Collection<Unit>> retreatingUnitMap =
        retreater.splitRetreatUnits(retreatUnits);

    if (retreatingUnitMap.containsKey(Retreater.RetreatLocation.SAME_TERRITORY)) {
      final Collection<Unit> sameTerritoryRetreatingUnits =
          retreatingUnitMap.get(Retreater.RetreatLocation.SAME_TERRITORY);
      if (!sameTerritoryRetreatingUnits.isEmpty()) {
        battleState.retreatUnits(BattleState.Side.OFFENSE, sameTerritoryRetreatingUnits);
        addRetreatToHistory(bridge, sameTerritoryRetreatingUnits);
      }
    }

    if (retreatingUnitMap.containsKey(Retreater.RetreatLocation.OTHER_TERRITORY)) {
      final Collection<Unit> otherTerritoryRetreatingUnits =
          retreatingUnitMap.get(Retreater.RetreatLocation.OTHER_TERRITORY);
      if (!otherTerritoryRetreatingUnits.isEmpty()) {
        battleState.retreatUnits(BattleState.Side.OFFENSE, otherTerritoryRetreatingUnits);
        addTerritoryRetreatToHistory(retreatTo, bridge, otherTerritoryRetreatingUnits);
        change.add(
            ChangeFactory.moveUnits(
                battleState.getBattleSite(), retreatTo, otherTerritoryRetreatingUnits));
      }
    }

    bridge.addChange(change);

    if (battleState.getUnits(BattleState.Side.OFFENSE).isEmpty()) {
      battleActions.endBattle(IBattle.WhoWon.DEFENDER, bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreatUnits);
    }

    broadcastRetreat(
        bridge, retreater.getShortBroadcastSuffix(), retreater.getLongBroadcastSuffix(retreatTo));
  }

  private void addTerritoryRetreatToHistory(
      final Territory to, final IDelegateBridge bridge, final Collection<Unit> units) {
    final String transcriptText = MyFormatter.unitsToText(units) + " retreated to " + to.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private void addRetreatToHistory(final IDelegateBridge bridge, final Collection<Unit> units) {
    final String transcriptText = MyFormatter.unitsToText(units) + " retreated";
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private void broadcastRetreat(
      final IDelegateBridge bridge,
      final String messageShortSuffix,
      final String messageLongSuffix) {
    final String messageShort = battleState.getAttacker().getName() + messageShortSuffix;
    final String messageLong = battleState.getAttacker().getName() + messageLongSuffix;
    bridge
        .getDisplayChannelBroadcaster()
        .notifyRetreat(messageShort, messageLong, getName(), battleState.getAttacker());
  }
}
