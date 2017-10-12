package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.internal.persistence.serializable.FakeTechAdvanceProxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * Provides support for incrementally building proxy factory collections typically required by tests that serialize
 * engine data.
 */
public final class TestProxyFactoryCollectionBuilder {
  private final Collection<ProxyFactory> proxyFactories = new ArrayList<>();

  private TestProxyFactoryCollectionBuilder() {}

  /**
   * Creates a new proxy factory collection builder that is pre-populated with all proxy factories required to serialize
   * instances of {@code GameData}.
   *
   * @return A new proxy factory collection builder.
   */
  public static TestProxyFactoryCollectionBuilder forGameData() {
    return new TestProxyFactoryCollectionBuilder()
        .addAll(ProxyRegistries.getGameDataMementoProxyFactories())
        .add(FakeTechAdvanceProxy.FACTORY);
  }

  /**
   * Adds the specified proxy factory to the collection under construction.
   *
   * @param proxyFactory The proxy factory to add.
   *
   * @return A reference to this builder.
   */
  public TestProxyFactoryCollectionBuilder add(final ProxyFactory proxyFactory) {
    checkNotNull(proxyFactory);

    proxyFactories.add(proxyFactory);
    return this;
  }

  /**
   * Adds the specified collection of proxy factories to the collection under construction.
   *
   * @param proxyFactories The collection of proxy factories to add.
   *
   * @return A reference to this builder.
   */
  public TestProxyFactoryCollectionBuilder addAll(final Collection<ProxyFactory> proxyFactories) {
    checkNotNull(proxyFactories);

    this.proxyFactories.addAll(proxyFactories);
    return this;
  }

  public Collection<ProxyFactory> build() {
    return new ArrayList<>(proxyFactories);
  }
}
