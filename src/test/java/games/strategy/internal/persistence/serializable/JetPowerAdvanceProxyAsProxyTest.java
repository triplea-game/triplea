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
import games.strategy.triplea.delegate.JetPowerAdvance;

public final class JetPowerAdvanceProxyAsProxyTest extends AbstractProxyTestCase<JetPowerAdvance> {
  public JetPowerAdvanceProxyAsProxyTest() {
    super(JetPowerAdvance.class);
  }

  @Override
  protected Collection<JetPowerAdvance> createPrincipals() {
    return Arrays.asList(TestGameDataComponentFactory.newJetPowerAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.JET_POWER_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(JetPowerAdvanceProxy.FACTORY)
        .build();
  }
}
