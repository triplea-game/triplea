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
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;

public final class LongRangeAircraftAdvanceProxyAsProxyTest extends AbstractProxyTestCase<LongRangeAircraftAdvance> {
  public LongRangeAircraftAdvanceProxyAsProxyTest() {
    super(LongRangeAircraftAdvance.class);
  }

  @Override
  protected Collection<LongRangeAircraftAdvance> createPrincipals() {
    return Arrays.asList(
        TestGameDataComponentFactory.newLongRangeAircraftAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.LONG_RANGE_AIRCRAFT_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(LongRangeAircraftAdvanceProxy.FACTORY)
        .build();
  }
}
