package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;

public final class IncreasedFactoryProductionAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<IncreasedFactoryProductionAdvance> {
  public IncreasedFactoryProductionAdvanceProxyAsProxyTest() {
    super(IncreasedFactoryProductionAdvance.class);
  }

  @Override
  protected IncreasedFactoryProductionAdvance newGameDataComponent(final GameData gameData) {
    final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance =
        new IncreasedFactoryProductionAdvance(gameData);
    initializeAttachable(increasedFactoryProductionAdvance);
    return increasedFactoryProductionAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.INCREASED_FACTORY_PRODUCTION_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(IncreasedFactoryProductionAdvanceProxy.FACTORY);
  }
}
