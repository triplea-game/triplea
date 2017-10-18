package games.strategy.internal.persistence.serializable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.TripleA;

public final class TripleAProxyAsProxyTest extends AbstractProxyTestCase<TripleA> {
  public TripleAProxyAsProxyTest() {
    super(TripleA.class);
  }

  @Override
  protected void assertPrincipalEquals(final TripleA expected, final TripleA actual) {
    assertTrue(true, "no persistent state; all non-null instances are considered equal");
  }

  @Override
  protected Collection<TripleA> createPrincipals() {
    return Arrays.asList(new TripleA());
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(TripleAProxy.FACTORY);
  }
}
