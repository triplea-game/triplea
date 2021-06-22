package games.strategy.triplea.delegate.battle.steps;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.battle.phase.BattlePhaseList;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BattlePhase - This needs to be expanded so that it actually implements the unit abilities for a
 * fight.
 */
@AllArgsConstructor
public class BattlePhaseStep implements BattleStep {

  private static final long serialVersionUID = -6567033620405540508L;

  @VisibleForTesting @Getter private String battlePhaseName;
  private int battlePhaseOrder;

  @VisibleForTesting @Getter
  private Collection<BattlePhaseList.UnitAbilityAndUnitTypes> unitAbilities;

  private BattleState.Side side;

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    // this is just a placeholder for now but it will be expanded in a later PR
  }

  @Override
  public List<String> getNames() {
    return null;
  }

  @Override
  public Order getOrder() {
    return null;
  }
}
