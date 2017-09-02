package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.SuperSubsAdvance;

public final class SuperSubsAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<SuperSubsAdvance> {
  public SuperSubsAdvanceProxyAsProxyTest() {
    super(SuperSubsAdvance.class);
  }

  @Override
  protected SuperSubsAdvance newGameDataComponent(final GameData gameData) {
    final SuperSubsAdvance superSubsAdvance = new SuperSubsAdvance(gameData);
    initializeAttachable(superSubsAdvance);
    return superSubsAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.SUPER_SUBS_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(SuperSubsAdvanceProxy.FACTORY);
  }
}
