package games.strategy.internal.persistence.serializable;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.TestEqualityComparatorCollectionBuilder;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.engine.framework.TestProxyFactoryCollectionBuilder;
import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;

/**
 * A fixture for testing the basic aspects of {@link GameDataComponent} proxy classes.
 *
 * @param <T> The type of the game data component to be proxied.
 */
public abstract class AbstractGameDataComponentProxyTestCase<T extends GameDataComponent>
    extends AbstractProxyTestCase<T> {
  private GameData gameData;

  protected AbstractGameDataComponentProxyTestCase(final Class<T> principalType) {
    super(principalType);
  }

  @Override
  protected final Collection<EqualityComparator> getEqualityComparators() {
    return TestEqualityComparatorCollectionBuilder.forGameData()
        .addAll(getAdditionalEqualityComparators())
        .build();
  }

  /**
   * Gets the collection of additional equality comparators required to compare two instances of the game data component
   * type for equality. Any equality comparators required to compare two instances of {@link GameData} do not need to be
   * included.
   *
   * <p>
   * This implementation returns an empty collection. Subclasses may override and are not required to call the
   * superclass implementation.
   * </p>
   *
   * @return The collection of additional equality comparators required to compare two instances of the game data
   *         component type for equality.
   */
  protected Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Collections.emptyList();
  }

  @Override
  protected final Collection<ProxyFactory> getProxyFactories() {
    return TestProxyFactoryCollectionBuilder.forGameData()
        .addAll(getAdditionalProxyFactories())
        .build();
  }

  /**
   * Gets the collection of additional proxy factories required for the game data component to be persisted. Any proxy
   * factories required to persist an instance of {@link GameData} do not need to be included.
   *
   * <p>
   * This implementation returns an empty collection. Subclasses may override and are not required to call the
   * superclass implementation.
   * </p>
   *
   * @return The collection of additional proxy factories required for the game data component to be persisted.
   */
  protected Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Collections.emptyList();
  }

  /**
   * Gets the fixture game data.
   *
   * @return The fixture game data.
   */
  protected final GameData getGameData() {
    assert gameData != null;
    return gameData;
  }

  /**
   * Subclasses may override and are required to call the superclass implementation first.
   */
  @Override
  protected void prepareDeserializedPrincipal(final T actual) {
    actual.setGameData(gameData);
  }

  /**
   * Subclasses may override and are required to call the superclass implementation first.
   */
  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();

    gameData = TestGameDataFactory.newValidGameData();
  }
}
