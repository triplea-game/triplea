package games.strategy.engine.persistence.serializable;

import games.strategy.internal.persistence.serializable.PropertyBagMementoProxy;
import games.strategy.internal.persistence.serializable.VersionProxy;
import games.strategy.persistence.serializable.ProxyRegistry;

/**
 * Factory for creating instances of {@link ProxyRegistry} configured with the requisite proxy factories for use in
 * various persistence scenarios.
 */
public final class ProxyRegistryFactory {
  private ProxyRegistryFactory() {}

  /**
   * Creates a new proxy registry that has been configured with all proxy factories required to serialize a game data
   * memento.
   *
   * @return A new proxy registry; never {@code null}.
   */
  public static ProxyRegistry newGameDataMementoProxyRegistry() {
    return ProxyRegistry.newInstance(
        PropertyBagMementoProxy.FACTORY,
        VersionProxy.FACTORY);
  }
}
