package games.strategy.triplea.delegate;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.MapSupport;

/**
 * At the end of the turn collect NO income.
 */
@AutoSave(afterStepEnd = true)
@MapSupport
public class NoPUEndTurnDelegate extends EndTurnDelegate {
  @Override
  protected int getProduction(final Collection<Territory> territories) {
    return 0;
  }

  @Override
  protected void showEndTurnReport(final String endTurnReport) {
    // show nothing on purpose
  }

  /**
   * Default behavior for this delegate is that we do not collect PU/resource income from territories, but we do collect
   * and do any national
   * objectives and triggers.
   */
  @Override
  protected String doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge) {
    // TODO: add a step properties boolean for this (default = do this)
    return super.doNationalObjectivesAndOtherEndTurnEffects(bridge);
  }

  @Override
  protected String addOtherResources(final IDelegateBridge bridge) {
    return "";
  }
}
