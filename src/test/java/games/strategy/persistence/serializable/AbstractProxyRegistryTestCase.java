package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * A fixture for testing the basic aspects of classes that implement the {@link ProxyRegistry} interface.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public abstract class AbstractProxyRegistryTestCase {
  protected AbstractProxyRegistryTestCase() {}

  /**
   * Creates the proxy registry to be tested.
   *
   * @param proxyFactories The collection of proxy factories to associate with the registry; must not be {@code null}.
   *
   * @return The proxy registry to be tested; never {@code null}.
   */
  protected abstract ProxyRegistry createProxyRegistry(Collection<ProxyFactory> proxyFactories);

  @Test
  public void getProxyFor_ShouldDelegateToProxyFactoryWhenProxyFactoryRegisteredForPrincipalType() {
    final Object principal = Integer.valueOf(42);
    final Object expectedProxy = "42";
    final ProxyFactory proxyFactory = givenProxyFactoryFor(principal, expectedProxy);
    final ProxyRegistry proxyRegistry = newProxyRegistry(proxyFactory);

    final Object actualProxy = proxyRegistry.getProxyFor(principal);

    verify(proxyFactory).newProxyFor(principal);
    assertThat(actualProxy, is(expectedProxy));
  }

  private static ProxyFactory givenProxyFactoryFor(final Object principal, final Object proxy) {
    final ProxyFactory proxyFactory = mock(ProxyFactory.class);
    doReturn(principal.getClass()).when(proxyFactory).getPrincipalType();
    doReturn(proxy).when(proxyFactory).newProxyFor(principal);
    return proxyFactory;
  }

  private ProxyRegistry newProxyRegistry(final ProxyFactory... proxyFactories) {
    return createProxyRegistry(Arrays.asList(proxyFactories));
  }

  @Test
  public void getProxyFor_ShouldReturnPrincipalWhenNoProxyFactoryRegisteredForPrincipalType() {
    final ProxyRegistry proxyRegistry = newProxyRegistry();
    final Object principal = Integer.valueOf(42);

    final Object proxy = proxyRegistry.getProxyFor(principal);

    assertThat(proxy, is(sameInstance(principal)));
  }
}
