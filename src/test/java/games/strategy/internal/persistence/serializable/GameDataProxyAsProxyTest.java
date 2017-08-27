package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

public final class GameDataProxyAsProxyTest extends AbstractProxyTestCase<GameData> {
  public GameDataProxyAsProxyTest() {
    super(GameData.class);
  }

  @Override
  protected Collection<GameData> createPrincipals() {
    return Arrays.asList(TestGameDataFactory.newValidGameData());
  }

  @Override
  protected Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData().build();
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData().build();
  }
}
