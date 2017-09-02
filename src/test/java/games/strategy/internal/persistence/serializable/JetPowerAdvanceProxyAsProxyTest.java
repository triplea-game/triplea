package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.JetPowerAdvance;

public final class JetPowerAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<JetPowerAdvance> {
  public JetPowerAdvanceProxyAsProxyTest() {
    super(JetPowerAdvance.class);
  }

  @Override
  protected JetPowerAdvance newGameDataComponent(final GameData gameData) {
    final JetPowerAdvance jetPowerAdvance = new JetPowerAdvance(gameData);
    initializeAttachable(jetPowerAdvance);
    return jetPowerAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.JET_POWER_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(JetPowerAdvanceProxy.FACTORY);
  }
}
