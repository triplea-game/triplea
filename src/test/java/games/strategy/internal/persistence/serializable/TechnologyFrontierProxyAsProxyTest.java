package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newTechnologyFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class TechnologyFrontierProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<TechnologyFrontier> {
  public TechnologyFrontierProxyAsProxyTest() {
    super(TechnologyFrontier.class);
  }

  @Override
  protected Collection<TechnologyFrontier> createPrincipals() {
    return Arrays.asList(newTechnologyFrontier(getGameData(), "technologyFrontier"));
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(
        EngineDataEqualityComparators.FAKE_TECH_ADVANCE,
        EngineDataEqualityComparators.TECHNOLOGY_FRONTIER);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(
        FakeTechAdvanceProxy.FACTORY,
        TechnologyFrontierProxy.FACTORY);
  }
}
