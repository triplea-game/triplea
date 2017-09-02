package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.ParatroopersAdvance;

public final class ParatroopersAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<ParatroopersAdvance> {
  public ParatroopersAdvanceProxyAsProxyTest() {
    super(ParatroopersAdvance.class);
  }

  @Override
  protected ParatroopersAdvance newGameDataComponent(final GameData gameData) {
    final ParatroopersAdvance paratroopersAdvance = new ParatroopersAdvance(gameData);
    initializeAttachable(paratroopersAdvance);
    return paratroopersAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.PARATROOPERS_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(ParatroopersAdvanceProxy.FACTORY);
  }
}
