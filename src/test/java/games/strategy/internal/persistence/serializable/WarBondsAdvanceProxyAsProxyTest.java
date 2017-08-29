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
import games.strategy.triplea.delegate.WarBondsAdvance;

public final class WarBondsAdvanceProxyAsProxyTest extends AbstractProxyTestCase<WarBondsAdvance> {
  public WarBondsAdvanceProxyAsProxyTest() {
    super(WarBondsAdvance.class);
  }

  @Override
  protected Collection<WarBondsAdvance> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newWarBondsAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.WAR_BONDS_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(WarBondsAdvanceProxy.FACTORY)
        .build();
  }
}
