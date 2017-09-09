package games.strategy.internal.persistence.serializable;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.persistence.serializable.ProxyFactory;

/**
 * Provides support for incrementally building proxy factory collections typically required by subclasses of
 * {@code AbstractProxyTestCase}.
 */
final class TestProxyFactoryCollectionBuilder {
  private final Collection<ProxyFactory> proxyFactories = new ArrayList<>();

  private TestProxyFactoryCollectionBuilder() {}

  /**
   * Creates a new proxy factory collection builder that is pre-populated with all proxy factories required to serialize
   * instances of {@code GameData}.
   *
   * @return A new proxy factory collection builder.
   */
  static TestProxyFactoryCollectionBuilder forGameData() {
    return new TestProxyFactoryCollectionBuilder()
        .add(GameDataProxy.FACTORY)
        .add(PropertyBagMementoProxy.FACTORY)
        .add(TripleAProxy.FACTORY)
        .add(VersionProxy.FACTORY);
  }

  TestProxyFactoryCollectionBuilder add(final ProxyFactory proxyFactory) {
    proxyFactories.add(proxyFactory);
    return this;
  }

  TestProxyFactoryCollectionBuilder addAll(final Collection<ProxyFactory> proxyFactories) {
    this.proxyFactories.addAll(proxyFactories);
    return this;
  }

  Collection<ProxyFactory> build() {
    return new ArrayList<>(proxyFactories);
  }
}
