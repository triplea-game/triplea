package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARD;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Create naval bombardment firing groups
 *
 * <p>All the units that can bombard are put into a firing group.
 *
 * <p>If there are multiple isSuicideOnHit unit types participating in the bombardment, then there
 * will be one firing group for each of the isSuicideOnHit unit types and one firing group for all
 * the other unit types.
 *
 * <p>See {@link FiringGroup} for why isSuicideOnHit needs to be separated by unit type.
 */
@Value(staticConstructor = "of")
public class FiringGroupSplitterBombard implements Function<BattleState, List<FiringGroup>> {

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    final Collection<Unit> enemyUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, OFFENSE.getOpposite()),
            Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(
                battleState.getPlayer(OFFENSE),
                battleState.getBattleSite(),
                battleState.getGameData()));

    return FiringGroup.groupBySuicideOnHit(
        NAVAL_BOMBARD, battleState.getBombardingUnits(), enemyUnits);
  }
}
