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
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

public final class MechanizedInfantryAdvanceProxyAsProxyTest extends AbstractProxyTestCase<MechanizedInfantryAdvance> {
  public MechanizedInfantryAdvanceProxyAsProxyTest() {
    super(MechanizedInfantryAdvance.class);
  }

  @Override
  protected Collection<MechanizedInfantryAdvance> createPrincipals() {
    return Arrays.asList(
        TestGameDataComponentFactory.newMechanizedInfantryAdvance(TestGameDataFactory.newValidGameData()));
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .add(EngineDataEqualityComparators.MECHANIZED_INFANTRY_ADVANCE)
        .build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .add(MechanizedInfantryAdvanceProxy.FACTORY)
        .build();
  }
}
