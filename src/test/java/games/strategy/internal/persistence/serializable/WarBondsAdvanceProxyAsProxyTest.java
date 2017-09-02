package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.WarBondsAdvance;

public final class WarBondsAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<WarBondsAdvance> {
  public WarBondsAdvanceProxyAsProxyTest() {
    super(WarBondsAdvance.class);
  }

  @Override
  protected WarBondsAdvance newGameDataComponent(final GameData gameData) {
    final WarBondsAdvance warBondsAdvance = new WarBondsAdvance(gameData);
    initializeAttachable(warBondsAdvance);
    return warBondsAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.WAR_BONDS_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(WarBondsAdvanceProxy.FACTORY);
  }
}
