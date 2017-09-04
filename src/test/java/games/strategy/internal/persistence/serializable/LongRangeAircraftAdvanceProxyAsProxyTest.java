package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.LongRangeAircraftAdvance;

public final class LongRangeAircraftAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<LongRangeAircraftAdvance> {
  public LongRangeAircraftAdvanceProxyAsProxyTest() {
    super(
        LongRangeAircraftAdvance.class,
        LongRangeAircraftAdvance::new,
        EngineDataEqualityComparators.LONG_RANGE_AIRCRAFT_ADVANCE,
        LongRangeAircraftAdvanceProxy.FACTORY);
  }
}
