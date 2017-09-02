package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

public final class MechanizedInfantryAdvanceProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<MechanizedInfantryAdvance> {
  public MechanizedInfantryAdvanceProxyAsProxyTest() {
    super(MechanizedInfantryAdvance.class);
  }

  @Override
  protected MechanizedInfantryAdvance newGameDataComponent(final GameData gameData) {
    final MechanizedInfantryAdvance mechanizedInfantryAdvance = new MechanizedInfantryAdvance(gameData);
    initializeAttachable(mechanizedInfantryAdvance);
    return mechanizedInfantryAdvance;
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.MECHANIZED_INFANTRY_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(MechanizedInfantryAdvanceProxy.FACTORY);
  }
}
