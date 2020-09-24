package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
  public RetreatChanges computeChanges(final Territory retreatTo) {
    final Collection<Unit> retreatingUnits = getRetreatUnits();

    final List<RetreatHistoryChild> historyChildren = new ArrayList<>();

    battleState.retreatUnits(BattleState.Side.OFFENSE, retreatingUnits);
    final String transcriptText = MyFormatter.unitsToText(retreatingUnits) + " retreated";
    historyChildren.add(RetreatHistoryChild.of(transcriptText, new ArrayList<>(retreatingUnits)));

    return RetreatChanges.of(EMPTY_CHANGE, historyChildren);
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
