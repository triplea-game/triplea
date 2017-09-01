package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class TechnologyFrontierProxyAsProxyTest extends AbstractProxyTestCase<TechnologyFrontier> {
  public TechnologyFrontierProxyAsProxyTest() {
    super(TechnologyFrontier.class);
  }

  @Override
  protected Collection<TechnologyFrontier> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newTechnologyFrontier(
        TestGameDataFactory.newValidGameData(),
        "technologyFrontier"));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.FAKE_TECH_ADVANCE)
        .add(EngineDataEqualityComparators.TECHNOLOGY_FRONTIER)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(FakeTechAdvanceProxy.FACTORY)
        .add(TechnologyFrontierProxy.FACTORY)
        .build();
  }
}
