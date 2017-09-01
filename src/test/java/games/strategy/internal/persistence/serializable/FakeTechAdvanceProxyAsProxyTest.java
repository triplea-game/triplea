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
import games.strategy.triplea.delegate.FakeTechAdvance;

public final class FakeTechAdvanceProxyAsProxyTest extends AbstractProxyTestCase<FakeTechAdvance> {
  public FakeTechAdvanceProxyAsProxyTest() {
    super(FakeTechAdvance.class);
  }

  @Override
  protected Collection<FakeTechAdvance> createPrincipals() {
    return Arrays.asList(
        TestGameDataComponentFactory.newFakeTechAdvance(TestGameDataFactory.newValidGameData(), "Tech Advance"));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.FAKE_TECH_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(FakeTechAdvanceProxy.FACTORY)
        .build();
  }
}
