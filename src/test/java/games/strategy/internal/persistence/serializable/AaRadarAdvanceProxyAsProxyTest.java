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
import games.strategy.triplea.delegate.AARadarAdvance;

public final class AaRadarAdvanceProxyAsProxyTest extends AbstractProxyTestCase<AARadarAdvance> {
  public AaRadarAdvanceProxyAsProxyTest() {
    super(AARadarAdvance.class);
  }

  @Override
  protected Collection<AARadarAdvance> createPrincipals() {
    return Arrays.asList(newAaRadarAdvance());
  }

  private static AARadarAdvance newAaRadarAdvance() {
    final AARadarAdvance aaRadarAdvance = new AARadarAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(aaRadarAdvance);
    return aaRadarAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.AA_RADAR_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(AaRadarAdvanceProxy.FACTORY)
        .build();
  }
}
