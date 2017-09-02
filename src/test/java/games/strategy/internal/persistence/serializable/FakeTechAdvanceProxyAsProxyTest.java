package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newFakeTechAdvance;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.FakeTechAdvance;

public final class FakeTechAdvanceProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<FakeTechAdvance> {
  public FakeTechAdvanceProxyAsProxyTest() {
    super(FakeTechAdvance.class);
  }

  @Override
  protected FakeTechAdvance newGameDataComponent(final GameData gameData) {
    return newFakeTechAdvance(gameData, "Tech Advance");
  }

  @Override
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Arrays.asList(EngineDataEqualityComparators.FAKE_TECH_ADVANCE);
  }

  @Override
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Arrays.asList(FakeTechAdvanceProxy.FACTORY);
  }
}
