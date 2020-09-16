package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class OffensiveGeneralRetreat implements BattleStep {

  final BattleState battleState;

  final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    if (canAttackerRetreat()
        || canAttackerRetreatSeaPlanes()
        || (battleState.isAmphibious()
            && (canAttackerRetreatPartialAmphib() || canAttackerRetreatAmphibPlanes()))) {
      return List.of(battleState.getAttacker().getName() + ATTACKER_WITHDRAW);
    }
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.OFFENSIVE_GENERAL_RETREAT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    retreatUnits(bridge);
  }

  public void retreatUnits(final IDelegateBridge bridge) {
    if (battleState.isOver()) {
      return;
    }
    final RetreatData retreatData;

    if (battleState.isAmphibious()) {
      retreatData = getAmphibiousRetreatData();
      if (retreatData == null) {
        return;
      }

    } else if (canAttackerRetreat()) {
      retreatData =
          RetreatData.of(RetreatType.DEFAULT, battleState.getAttackerRetreatTerritories());
    } else {
      return;
    }

    battleActions.queryRetreat(false, retreatData.retreatType, bridge, retreatData.retreatSites);
  }

  private @Nullable RetreatData getAmphibiousRetreatData() {
    if (canAttackerRetreatPartialAmphib()) {
      return RetreatData.of(
          RetreatType.PARTIAL_AMPHIB, battleState.getAttackerRetreatTerritories());

    } else if (canAttackerRetreatAmphibPlanes()) {
      return RetreatData.of(RetreatType.PLANES, Set.of(battleState.getBattleSite()));

    } else {
      return null;
    }
  }

  @Value(staticConstructor = "of")
  private static class RetreatData {
    RetreatType retreatType;
    Collection<Territory> retreatSites;
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
    if (Properties.getPartialAmphibiousRetreat(battleState.getGameData())) {
      // Only include land units when checking for allow amphibious retreat
      final List<Unit> landUnits =
          CollectionUtils.getMatches(
              battleState.getUnits(BattleState.Side.OFFENSE), Matches.unitIsLand());
      for (final Unit unit : landUnits) {
        if (!unit.getWasAmphibious()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean canAttackerRetreatAmphibPlanes() {
    final GameData gameData = battleState.getGameData();
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }
}
