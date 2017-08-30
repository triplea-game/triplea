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
import games.strategy.triplea.delegate.HeavyBomberAdvance;

public final class HeavyBomberAdvanceProxyAsProxyTest extends AbstractProxyTestCase<HeavyBomberAdvance> {
  public HeavyBomberAdvanceProxyAsProxyTest() {
    super(HeavyBomberAdvance.class);
  }

  @Override
  protected Collection<HeavyBomberAdvance> createPrincipals() {
    return Arrays.asList(newHeavyBomberAdvance());
  }

  private static HeavyBomberAdvance newHeavyBomberAdvance() {
    final HeavyBomberAdvance heavyBomberAdvance = new HeavyBomberAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(heavyBomberAdvance);
    return heavyBomberAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.HEAVY_BOMBER_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(HeavyBomberAdvanceProxy.FACTORY)
        .build();
  }
}
