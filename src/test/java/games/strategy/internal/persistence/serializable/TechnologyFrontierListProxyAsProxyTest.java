package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newTechnologyFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TechnologyFrontierList;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class TechnologyFrontierListProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<TechnologyFrontierList> {
  public TechnologyFrontierListProxyAsProxyTest() {
    super(TechnologyFrontierList.class);
  }

  @Override
  protected TechnologyFrontierList newGameDataComponent(final GameData gameData) {
    final TechnologyFrontierList technologyFrontierList = new TechnologyFrontierList(gameData);
    technologyFrontierList.addTechnologyFrontier(newTechnologyFrontier(gameData, "technologyFrontier1"));
    technologyFrontierList.addTechnologyFrontier(newTechnologyFrontier(gameData, "technologyFrontier2"));
    return technologyFrontierList;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        EngineDataEqualityComparators.FAKE_TECH_ADVANCE,
        EngineDataEqualityComparators.TECHNOLOGY_FRONTIER,
        EngineDataEqualityComparators.TECHNOLOGY_FRONTIER_LIST);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        FakeTechAdvanceProxy.FACTORY,
        TechnologyFrontierProxy.FACTORY,
        TechnologyFrontierListProxy.FACTORY);
  }
}
