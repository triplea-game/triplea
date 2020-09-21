package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
class RetreaterPartialAmphibious implements Retreater {

  private final BattleState battleState;

  @Override
  public Collection<Unit> getRetreatUnits() {
    return CollectionUtils.getMatches(
        battleState.getUnits(BattleState.Side.OFFENSE), Matches.unitWasNotAmphibious());
  }

  @Override
  public Collection<Territory> getPossibleRetreatSites(final Collection<Unit> retreatUnits) {
    final Collection<Territory> allRetreatTerritories = battleState.getAttackerRetreatTerritories();
    return retreatUnits.stream().anyMatch(Matches.unitIsLand())
        ? CollectionUtils.getMatches(allRetreatTerritories, Matches.territoryIsLand())
        : new ArrayList<>(allRetreatTerritories);
  }

  @Override
  public String getQueryText() {
    return battleState.getAttacker().getName() + " retreat non-amphibious units?";
  }

  @Override
  public MustFightBattle.RetreatType getRetreatType() {
    return MustFightBattle.RetreatType.PARTIAL_AMPHIB;
  }

  @Override
  public Map<RetreatLocation, Collection<Unit>> splitRetreatUnits(
      final Collection<Unit> retreatUnits) {
    final Collection<Unit> airRetreating =
        CollectionUtils.getMatches(retreatUnits, Matches.unitIsAir());

    final Collection<Unit> nonAirRetreating = new ArrayList<>(retreatUnits);
    nonAirRetreating.removeAll(airRetreating);
    nonAirRetreating.addAll(battleState.getDependentUnits(nonAirRetreating));

    return Map.of(
        RetreatLocation.SAME_TERRITORY, airRetreating,
        RetreatLocation.OTHER_TERRITORY, nonAirRetreating);
  }

  @Override
  public Change extraRetreatChange(final Territory retreatTo, final Collection<Unit> retreatUnits) {
    return EMPTY_CHANGE;
  }

  @Override
  public String getShortBroadcastSuffix() {
    return " retreats non-amphibious units";
  }

  @Override
  public String getLongBroadcastSuffix(final Territory retreatTo) {
    return getShortBroadcastSuffix();
  }
}
