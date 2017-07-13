package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;

/**
 * A fixture for testing the basic aspects of classes that implement the {@link ProxyFactoryRegistry} interface.
 */
public abstract class AbstractProxyFactoryRegistryTestCase {
  private final ProxyFactory proxyFactory = ProxyFactory.newInstance(Object.class, Function.identity());

  protected AbstractProxyFactoryRegistryTestCase() {}

  /**
   * Creates the proxy factory registry to be tested.
   *
   * @param proxyFactories The collection of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return The proxy factory registry to be tested; never {@code null}.
   */
  protected abstract ProxyFactoryRegistry createProxyFactoryRegistry(Collection<ProxyFactory> proxyFactories);

  private ProxyFactoryRegistry newProxyFactoryRegistry(final ProxyFactory... proxyFactories) {
    return createProxyFactoryRegistry(Arrays.asList(proxyFactories));
  }

  @Test
  public void getProxyFactory_ShouldReturnProxyFactoryWhenPrincipalTypePresent() {
    final ProxyFactoryRegistry proxyFactoryRegistry = newProxyFactoryRegistry(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.of(proxyFactory)));
  }

  @Test
  public void getProxyFactory_ShouldReturnEmptyWhenPrincipalTypeAbsent() {
    final ProxyFactoryRegistry proxyFactoryRegistry = newProxyFactoryRegistry();

    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.empty()));
  }
}
