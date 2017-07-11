package games.strategy.internal.persistence.serializable;

import com.google.common.collect.ImmutableMap;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactoryRegistry;
import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoProxyAsProxyTest extends AbstractProxyTestCase<PropertyBagMemento> {
  public PropertyBagMementoProxyAsProxyTest() {
    super(PropertyBagMemento.class);
  }

  @Override
  protected PropertyBagMemento createPrincipal() {
    return new PropertyBagMemento("schema-id", ImmutableMap.<String, Object>of(
        "property1", 42L,
        "property2", "2112"));
  }

  @Override
  protected void registerProxyFactories(final ProxyFactoryRegistry proxyFactoryRegistry) {
    proxyFactoryRegistry.registerProxyFactory(PropertyBagMementoProxy.FACTORY);
  }
}
