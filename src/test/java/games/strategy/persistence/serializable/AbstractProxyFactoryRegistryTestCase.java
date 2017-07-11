package games.strategy.persistence.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * A fixture for testing the basic aspects of classes that implement the {@link ProxyFactoryRegistry} interface.
 */
public abstract class AbstractProxyFactoryRegistryTestCase {
  private final ProxyFactory proxyFactory = newProxyFactory();

  private ProxyFactoryRegistry proxyFactoryRegistry;

  protected AbstractProxyFactoryRegistryTestCase() {}

  private static ProxyFactory newProxyFactory() {
    return ProxyFactory.newInstance(Object.class, Function.identity());
  }

  /**
   * Creates the proxy factory registry to be tested.
   *
   * @return The proxy factory registry to be tested; never {@code null}.
   *
   * @throws Exception If the proxy factory registry cannot be created.
   */
  protected abstract ProxyFactoryRegistry createProxyFactoryRegistry() throws Exception;

  /**
   * Sets up the test fixture.
   *
   * <p>
   * Subclasses may override and must call the superclass implementation.
   * </p>
   *
   * @throws Exception If an error occurs.
   */
  @Before
  public void setUp() throws Exception {
    proxyFactoryRegistry = createProxyFactoryRegistry();
    assert proxyFactoryRegistry != null;
  }

  @Test
  public void getProxyFactory_ShouldReturnProxyFactoryWhenPrincipalTypePresent() {
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.of(proxyFactory)));
  }

  @Test
  public void getProxyFactory_ShouldReturnEmptyWhenPrincipalTypeAbsent() {
    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.empty()));
  }

  @Test
  public void getProxyFactories_ShouldReturnCopy() {
    final Collection<ProxyFactory> proxyFactories = proxyFactoryRegistry.getProxyFactories();
    final int expectedProxyFactoryCount = proxyFactories.size();

    proxyFactories.add(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactories(), hasSize(expectedProxyFactoryCount));
  }

  @Test
  public void getProxyFactories_ShouldReturnSnapshot() {
    final Collection<ProxyFactory> proxyFactories = proxyFactoryRegistry.getProxyFactories();
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactories(), is(not(proxyFactories)));
  }

  @Test
  public void registerProxyFactory_ShouldRegisterProxyFactoryWhenPrincipalTypeUnregistered() {
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.of(proxyFactory)));
  }

  @Test
  public void registerProxyFactory_ShouldThrowExceptionWhenPrincipalTypeRegistered() {
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);

    final ProxyFactory otherProxyFactory = newProxyFactory();
    catchException(() -> proxyFactoryRegistry.registerProxyFactory(otherProxyFactory));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @Test
  public void unregisterProxyFactory_ShouldUnregisterProxyFactoryWhenPrincipalTypeRegisteredWithSameInstance() {
    final int originalProxyFactoryCount = proxyFactoryRegistry.getProxyFactories().size();
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);
    assertThat(proxyFactoryRegistry.getProxyFactories(), hasSize(originalProxyFactoryCount + 1));

    proxyFactoryRegistry.unregisterProxyFactory(proxyFactory);

    assertThat(proxyFactoryRegistry.getProxyFactory(proxyFactory.getPrincipalType()), is(Optional.empty()));
    assertThat(proxyFactoryRegistry.getProxyFactories(), hasSize(originalProxyFactoryCount));
  }

  @Test
  public void unregisterProxyFactory_ShouldThrowExceptionWhenPrincipalTypeUnregistered() {
    catchException(() -> proxyFactoryRegistry.unregisterProxyFactory(proxyFactory));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }

  @Test
  public void unregisterProxyFactory_ShouldThrowExceptionWhenPrincipalTypeRegisteredWithDifferentInstance() {
    proxyFactoryRegistry.registerProxyFactory(proxyFactory);

    final ProxyFactory otherProxyFactory = newProxyFactory();
    catchException(() -> proxyFactoryRegistry.unregisterProxyFactory(otherProxyFactory));

    assertThat(caughtException(), is(instanceOf(IllegalArgumentException.class)));
  }
}
