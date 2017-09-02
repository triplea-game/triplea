package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;

public final class IndustrialTechnologyAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<IndustrialTechnologyAdvance> {
  public IndustrialTechnologyAdvanceProxyAsProxyTest() {
    super(IndustrialTechnologyAdvance.class);
  }

  @Override
  protected IndustrialTechnologyAdvance newGameDataComponent(final GameData gameData) {
    final IndustrialTechnologyAdvance industrialTechnologyAdvance = new IndustrialTechnologyAdvance(gameData);
    initializeAttachable(industrialTechnologyAdvance);
    return industrialTechnologyAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.INDUSTRIAL_TECHNOLOGY_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(IndustrialTechnologyAdvanceProxy.FACTORY);
  }
}
