package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;

public final class LongRangeAircraftAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<LongRangeAircraftAdvance> {
  public LongRangeAircraftAdvanceProxyAsProxyTest() {
    super(LongRangeAircraftAdvance.class);
  }

  @Override
  protected LongRangeAircraftAdvance newGameDataComponent(final GameData gameData) {
    final LongRangeAircraftAdvance longRangeAircraftAdvance = new LongRangeAircraftAdvance(gameData);
    initializeAttachable(longRangeAircraftAdvance);
    return longRangeAircraftAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.LONG_RANGE_AIRCRAFT_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(LongRangeAircraftAdvanceProxy.FACTORY);
  }
}
