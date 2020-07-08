package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.LAND_PARATROOPS;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class LandParatroopers implements BattleStep {

  private static final long serialVersionUID = 3500647439487948115L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return new TransportsAndParatroopers().hasParatroopers() ? List.of(LAND_PARATROOPS) : List.of();
  }

  @Override
  public Order getOrder() {
    return Order.LAND_PARATROOPERS;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final TransportsAndParatroopers transportsAndParatroopers = new TransportsAndParatroopers();

    if (transportsAndParatroopers.hasParatroopers()) {
      battleActions.landParatroopers(
          bridge, transportsAndParatroopers.airTransports, transportsAndParatroopers.paratroopers);
    }
  }

  private class TransportsAndParatroopers {
    private Collection<Unit> airTransports = new ArrayList<>();
    private Collection<Unit> paratroopers = new ArrayList<>();

    private TransportsAndParatroopers() {
      if (battleState.getBattleRound() == 1
          && !battleState.getBattleSite().isWater()
          && TechAttachment.isAirTransportable(battleState.getAttacker())) {
        final Collection<Unit> airTransports =
            CollectionUtils.getMatches(
                battleState.getBattleSite().getUnits(), Matches.unitIsAirTransport());
        if (!airTransports.isEmpty()) {
          final Collection<Unit> paratroopers = battleState.getDependentUnits(airTransports);
          if (!paratroopers.isEmpty()) {
            this.airTransports = airTransports;
            this.paratroopers = paratroopers;
          }
        }
      }
    }

    private boolean hasParatroopers() {
      return !airTransports.isEmpty() && !paratroopers.isEmpty();
    }
  }
}
