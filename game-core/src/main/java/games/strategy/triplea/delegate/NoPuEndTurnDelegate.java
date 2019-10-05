package games.strategy.triplea.delegate;

import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import java.util.Collection;

/** At the end of the turn collect NO income. */
@AutoSave(afterStepEnd = true)
public class NoPuEndTurnDelegate extends EndTurnDelegate {
  @Override
  protected int getProduction(final Collection<Territory> territories) {
    return 0;
  }

  @Override
  protected void showEndTurnReport(final String endTurnReport) {
    // show nothing on purpose
  }

  @Override
  protected String addOtherResources(final IDelegateBridge bridge) {
    return "";
  }
}
