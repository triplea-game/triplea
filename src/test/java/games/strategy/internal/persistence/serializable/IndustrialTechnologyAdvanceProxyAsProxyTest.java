package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataComponentFactory;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;

public final class IndustrialTechnologyAdvanceProxyAsProxyTest
    extends AbstractProxyTestCase<IndustrialTechnologyAdvance> {
  public IndustrialTechnologyAdvanceProxyAsProxyTest() {
    super(IndustrialTechnologyAdvance.class);
  }

  @Override
  protected Collection<IndustrialTechnologyAdvance> createPrincipals() {
    return Arrays.asList(newIndustrialTechnologyAdvance());
  }

  private static IndustrialTechnologyAdvance newIndustrialTechnologyAdvance() {
    final IndustrialTechnologyAdvance industrialTechnologyAdvance =
        new IndustrialTechnologyAdvance(TestGameDataFactory.newValidGameData());
    TestGameDataComponentFactory.initializeAttachable(industrialTechnologyAdvance);
    return industrialTechnologyAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.INDUSTRIAL_TECHNOLOGY_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(IndustrialTechnologyAdvanceProxy.FACTORY)
        .build();
  }
}
