package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
class RetreaterAirAmphibious implements Retreater {

  private final BattleState battleState;

  @Override
  public Collection<Unit> getRetreatUnits() {
    return CollectionUtils.getMatches(
        battleState.getUnits(BattleState.Side.OFFENSE), Matches.unitIsAir());
  }

  @Override
  public Collection<Territory> getPossibleRetreatSites(final Collection<Unit> retreatUnits) {
    return List.of(battleState.getBattleSite());
  }

  @Override
  public String getQueryText() {
    return battleState.getAttacker().getName() + " retreat planes?";
  }

  @Override
  public MustFightBattle.RetreatType getRetreatType() {
    return MustFightBattle.RetreatType.PLANES;
  }

  @Override
  public Map<RetreatLocation, Collection<Unit>> splitRetreatUnits(
      final Collection<Unit> retreatUnits) {
    return Map.of(RetreatLocation.SAME_TERRITORY, retreatUnits);
  }

  @Override
  public Change extraRetreatChange(final Territory retreatTo, final Collection<Unit> retreatUnits) {
    return EMPTY_CHANGE;
  }

  @Override
  public String getShortBroadcastSuffix() {
    return " retreats planes";
  }

  @Override
  public String getLongBroadcastSuffix(final Territory retreatTo) {
    return getShortBroadcastSuffix();
  }
}
