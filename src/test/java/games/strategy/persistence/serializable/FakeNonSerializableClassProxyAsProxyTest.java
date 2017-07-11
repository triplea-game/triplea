package games.strategy.persistence.serializable;

public final class FakeNonSerializableClassProxyAsProxyTest extends AbstractProxyTestCase<FakeNonSerializableClass> {
  public FakeNonSerializableClassProxyAsProxyTest() {
    super(FakeNonSerializableClass.class);
  }

  @Override
  protected FakeNonSerializableClass createPrincipal() {
    return new FakeNonSerializableClass(2112, "42");
  }

  @Override
  protected void registerProxyFactories(final ProxyFactoryRegistry proxyFactoryRegistry) {
    proxyFactoryRegistry.registerProxyFactory(FakeNonSerializableClassProxy.FACTORY);
  }
}
