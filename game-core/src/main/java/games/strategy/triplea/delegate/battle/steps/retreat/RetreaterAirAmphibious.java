package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitsStatus.ALIVE;

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
    return CollectionUtils.getMatches(battleState.getUnits(ALIVE, OFFENSE), Matches.unitIsAir());
  }

  @Override
  public Collection<Territory> getPossibleRetreatSites(final Collection<Unit> retreatUnits) {
    return List.of(battleState.getBattleSite());
  }

  @Override
  public MustFightBattle.RetreatType getRetreatType() {
    return MustFightBattle.RetreatType.PLANES;
  }

  @Override
  public RetreatChanges computeChanges(final Territory retreatTo) {
    final Collection<Unit> retreatingUnits = getRetreatUnits();

    final List<RetreatHistoryChild> historyChildren = new ArrayList<>();

    battleState.retreatUnits(OFFENSE, retreatingUnits);
    final String transcriptText = MyFormatter.unitsToText(retreatingUnits) + " retreated";
    historyChildren.add(RetreatHistoryChild.of(transcriptText, new ArrayList<>(retreatingUnits)));

    return RetreatChanges.of(EMPTY_CHANGE, historyChildren);
  }
}
