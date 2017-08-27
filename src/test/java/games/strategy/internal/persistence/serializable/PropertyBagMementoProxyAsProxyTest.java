package games.strategy.internal.persistence.serializable;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;

import games.strategy.persistence.serializable.AbstractProxyTestCase;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoProxyAsProxyTest extends AbstractProxyTestCase<PropertyBagMemento> {
  public PropertyBagMementoProxyAsProxyTest() {
    super(PropertyBagMemento.class);
  }

  @Override
  protected Collection<PropertyBagMemento> createPrincipals() {
    return Arrays.asList(new PropertyBagMemento("schema-id", ImmutableMap.<String, Object>of(
        "property1", 42L,
        "property2", "2112")));
  }

  @Override
  protected Collection<ProxyFactory> getProxyFactories() {
    return Arrays.asList(PropertyBagMementoProxy.FACTORY);
  }
}
