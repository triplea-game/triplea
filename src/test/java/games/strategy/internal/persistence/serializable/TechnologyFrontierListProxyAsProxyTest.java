package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TechnologyFrontierList;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class TechnologyFrontierListProxyAsProxyTest extends AbstractProxyTestCase<TechnologyFrontierList> {
  public TechnologyFrontierListProxyAsProxyTest() {
    super(TechnologyFrontierList.class);
  }

  @Override
  protected Collection<TechnologyFrontierList> createPrincipals() {
    return Arrays.asList(newTechnologyFrontierList());
  }

  private static TechnologyFrontierList newTechnologyFrontierList() {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final TechnologyFrontierList technologyFrontierList = new TechnologyFrontierList(gameData);
    technologyFrontierList
        .addTechnologyFrontier(TestGameDataComponentFactory.newTechnologyFrontier(gameData, "technologyFrontier1"));
    technologyFrontierList
        .addTechnologyFrontier(TestGameDataComponentFactory.newTechnologyFrontier(gameData, "technologyFrontier2"));
    return technologyFrontierList;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.FAKE_TECH_ADVANCE)
        .add(EngineDataEqualityComparators.TECHNOLOGY_FRONTIER)
        .add(EngineDataEqualityComparators.TECHNOLOGY_FRONTIER_LIST)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(FakeTechAdvanceProxy.FACTORY)
        .add(TechnologyFrontierProxy.FACTORY)
        .add(TechnologyFrontierListProxy.FACTORY)
        .build();
  }
}
