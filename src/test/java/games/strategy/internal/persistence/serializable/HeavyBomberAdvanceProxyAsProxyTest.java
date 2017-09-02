package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.HeavyBomberAdvance;

public final class HeavyBomberAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<HeavyBomberAdvance> {
  public HeavyBomberAdvanceProxyAsProxyTest() {
    super(HeavyBomberAdvance.class);
  }

  @Override
  protected HeavyBomberAdvance newGameDataComponent(final GameData gameData) {
    final HeavyBomberAdvance heavyBomberAdvance = new HeavyBomberAdvance(gameData);
    initializeAttachable(heavyBomberAdvance);
    return heavyBomberAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.HEAVY_BOMBER_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(HeavyBomberAdvanceProxy.FACTORY);
  }
}
