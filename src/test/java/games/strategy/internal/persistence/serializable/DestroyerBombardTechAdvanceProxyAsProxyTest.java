package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

public final class DestroyerBombardTechAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<DestroyerBombardTechAdvance> {
  public DestroyerBombardTechAdvanceProxyAsProxyTest() {
    super(DestroyerBombardTechAdvance.class);
  }

  @Override
  protected DestroyerBombardTechAdvance newGameDataComponent(final GameData gameData) {
    final DestroyerBombardTechAdvance destroyerBombardTechAdvance = new DestroyerBombardTechAdvance(gameData);
    initializeAttachable(destroyerBombardTechAdvance);
    return destroyerBombardTechAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.DESTROYER_BOMBARD_TECH_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(DestroyerBombardTechAdvanceProxy.FACTORY);
  }
}
