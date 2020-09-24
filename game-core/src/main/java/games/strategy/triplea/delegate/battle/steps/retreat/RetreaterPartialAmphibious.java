package games.strategy.triplea.delegate.battle.steps.retreat;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
  public RetreatChanges computeChanges(final Territory retreatTo) {
    final Collection<Unit> retreatUnits = getRetreatUnits();

    final CompositeChange change = new CompositeChange();
    final List<RetreatHistoryChild> historyChildren = new ArrayList<>();

    final Collection<Unit> airRetreating =
        CollectionUtils.getMatches(
            retreatUnits,
            Matches.unitIsAir().and(Matches.unitIsOwnedBy(battleState.getAttacker())));

    if (!airRetreating.isEmpty()) {
      battleState.retreatUnits(BattleState.Side.OFFENSE, airRetreating);
      final String transcriptText = MyFormatter.unitsToText(airRetreating) + " retreated";
      historyChildren.add(RetreatHistoryChild.of(transcriptText, new ArrayList<>(airRetreating)));
    }

    final Collection<Unit> nonAirRetreating = new HashSet<>(retreatUnits);
    nonAirRetreating.removeAll(airRetreating);
    nonAirRetreating.addAll(battleState.getDependentUnits(nonAirRetreating));

    if (!nonAirRetreating.isEmpty()) {
      battleState.retreatUnits(BattleState.Side.OFFENSE, nonAirRetreating);
      historyChildren.add(
          RetreatHistoryChild.of(
              MyFormatter.unitsToText(nonAirRetreating) + " retreated to " + retreatTo.getName(),
              new ArrayList<>(nonAirRetreating)));
      change.add(ChangeFactory.moveUnits(battleState.getBattleSite(), retreatTo, nonAirRetreating));
    }

    return RetreatChanges.of(change, historyChildren);
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
