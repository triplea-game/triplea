package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;

public final class ImprovedShipyardsAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ImprovedShipyardsAdvance> {
  public ImprovedShipyardsAdvanceProxyAsProxyTest() {
    super(ImprovedShipyardsAdvance.class);
  }

  @Override
  protected ImprovedShipyardsAdvance newGameDataComponent(final GameData gameData) {
    final ImprovedShipyardsAdvance improvedShipyardsAdvance = new ImprovedShipyardsAdvance(gameData);
    initializeAttachable(improvedShipyardsAdvance);
    return improvedShipyardsAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.IMPROVED_SHIPYARDS_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(ImprovedShipyardsAdvanceProxy.FACTORY);
  }
}
