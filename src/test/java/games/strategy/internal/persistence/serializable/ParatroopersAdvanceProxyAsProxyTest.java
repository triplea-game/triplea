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
import games.strategy.triplea.delegate.ParatroopersAdvance;

public final class ParatroopersAdvanceProxyAsProxyTest extends AbstractProxyTestCase<ParatroopersAdvance> {
  public ParatroopersAdvanceProxyAsProxyTest() {
    super(ParatroopersAdvance.class);
  }

  @Override
  protected Collection<ParatroopersAdvance> createPrincipals() {
    return Arrays.asList(newParatroopersAdvance());
  }

  private static ParatroopersAdvance newParatroopersAdvance() {
    final ParatroopersAdvance paratroopersAdvance = new ParatroopersAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(paratroopersAdvance);
    return paratroopersAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.PARATROOPERS_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(ParatroopersAdvanceProxy.FACTORY)
        .build();
  }
}
