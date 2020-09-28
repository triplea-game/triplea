package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.BOMBARD;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Create naval bombardment firing groups
 *
 * <p>The firing groups are separated by isSuicideOnHit
 */
@Value(staticConstructor = "of")
public class FiringGroupFilterBombard implements FiringGroupFilter {

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    final Collection<Unit> canFire = battleState.getBombardingUnits();
    final Collection<Unit> enemyUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, OFFENSE.getOpposite()),
            Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(
                battleState.getPlayer(OFFENSE),
                battleState.getBattleSite(),
                battleState.getGameData()));

    return FiringGroup.groupBySuicideOnHit(BOMBARD, canFire, enemyUnits);
  }
}
