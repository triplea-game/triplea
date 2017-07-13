package games.strategy.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

public final class FakeNonSerializableClassProxyAsProxyTest extends AbstractProxyTestCase<FakeNonSerializableClass> {
  public FakeNonSerializableClassProxyAsProxyTest() {
    super(FakeNonSerializableClass.class);
  }

  @Override
  protected FakeNonSerializableClass createPrincipal() {
    return new FakeNonSerializableClass(2112, "42");
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(FakeNonSerializableClassProxy.FACTORY);
  }
}
