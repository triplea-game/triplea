package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.Version;

public final class VersionProxyAsProxyTest extends AbstractProxyTestCase<Version> {
  public VersionProxyAsProxyTest() {
    super(Version.class);
  }

  @Override
  protected Version createPrincipal() {
    return new Version(1, 2, 3, 4);
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(VersionProxy.FACTORY);
  }
}
