package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;

public final class ImprovedShipyardsAdvanceProxyAsProxyTest extends AbstractProxyTestCase<ImprovedShipyardsAdvance> {
  public ImprovedShipyardsAdvanceProxyAsProxyTest() {
    super(ImprovedShipyardsAdvance.class);
  }

  @Override
  protected Collection<ImprovedShipyardsAdvance> createPrincipals() {
    return Arrays.asList(
        TestGameDataComponentFactory.newImprovedShipyardsAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.IMPROVED_SHIPYARDS_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(ImprovedShipyardsAdvanceProxy.FACTORY)
        .build();
  }
}
