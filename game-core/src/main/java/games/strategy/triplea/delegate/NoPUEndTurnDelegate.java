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
@SuppressWarnings("checkstyle:AbbreviationAsWordInName") // rename upon next incompatible release
public class NoPUEndTurnDelegate extends EndTurnDelegate {
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
