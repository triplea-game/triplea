package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.RocketsAdvance;

public final class RocketsAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<RocketsAdvance> {
  public RocketsAdvanceProxyAsProxyTest() {
    super(RocketsAdvance.class);
  }

  @Override
  protected RocketsAdvance newGameDataComponent(final GameData gameData) {
    final RocketsAdvance rocketsAdvance = new RocketsAdvance(gameData);
    initializeAttachable(rocketsAdvance);
    return rocketsAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.ROCKETS_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(RocketsAdvanceProxy.FACTORY);
  }
}
