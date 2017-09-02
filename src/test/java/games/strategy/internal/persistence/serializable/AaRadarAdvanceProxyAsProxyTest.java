package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.AARadarAdvance;

public final class AaRadarAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<AARadarAdvance> {
  public AaRadarAdvanceProxyAsProxyTest() {
    super(AARadarAdvance.class);
  }

  @Override
  protected AARadarAdvance newGameDataComponent(final GameData gameData) {
    final AARadarAdvance aaRadarAdvance = new AARadarAdvance(gameData);
    initializeAttachable(aaRadarAdvance);
    return aaRadarAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.AA_RADAR_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(AaRadarAdvanceProxy.FACTORY);
  }
}
