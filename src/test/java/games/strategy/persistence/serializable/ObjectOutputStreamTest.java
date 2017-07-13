package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class ObjectOutputStreamTest {
  private ObjectOutputStream oos;

  private final ProxyFactory proxyFactory = newProxyFactoryFor(Integer.class);

  private final ProxyFactoryRegistry proxyFactoryRegistry = ProxyFactoryRegistry.newInstance(proxyFactory);

  private static ProxyFactory newProxyFactoryFor(final Class<?> principalType) {
    final ProxyFactory proxyFactory = mock(ProxyFactory.class);
    doReturn(principalType).when(proxyFactory).getPrincipalType();
    return proxyFactory;
  }

  @Before
  public void setUp() throws Exception {
    oos = new ObjectOutputStream(new ByteArrayOutputStream(), proxyFactoryRegistry);
  }

  @After
  public void tearDown() throws Exception {
    oos.close();
  }

  @Test
  public void replaceObject_ShouldDelegateToProxyFactoryWhenProxyFactoryAvailable() throws Exception {
    assertTrue(proxyFactoryRegistry.getProxyFactory(Integer.class).isPresent());
    final Object obj = Integer.valueOf(42);

    oos.replaceObject(obj);

    verify(proxyFactory).newProxyFor(obj);
  }

  @Test
  public void replaceObject_ShouldReturnInputWhenProxyFactoryUnavailable() throws Exception {
    assertFalse(proxyFactoryRegistry.getProxyFactory(Double.class).isPresent());
    final Object obj = Double.valueOf(42.0);

    final Object replacedObj = oos.replaceObject(obj);

    assertThat(replacedObj, is(sameInstance(obj)));
  }

  @Test
  public void replaceObject_ShouldReturnNullWhenObjectIsNull() throws Exception {
    assertThat(oos.replaceObject(null), is(nullValue()));
  }
}
