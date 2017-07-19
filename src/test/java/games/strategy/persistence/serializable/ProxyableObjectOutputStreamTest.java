package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class ProxyableObjectOutputStreamTest {
  @Test
  public void replaceObject_ShouldDelegateToProxyRegistryWhenObjectIsNotNull() throws Exception {
    final Object obj = Integer.valueOf(42);
    final Object expectedReplacedObj = "42";
    final ProxyRegistry proxyRegistry = givenProxyRegistryFor(obj, expectedReplacedObj);
    try (final ProxyableObjectOutputStream oos = newProxyableObjectOutputStream(proxyRegistry)) {
      final Object actualReplacedObj = oos.replaceObject(obj);

      verify(proxyRegistry).getProxyFor(obj);
      assertThat(actualReplacedObj, is(expectedReplacedObj));
    }
  }

  private static ProxyRegistry givenProxyRegistryFor(final Object principal, final Object proxy) {
    final ProxyRegistry proxyRegistry = mock(ProxyRegistry.class);
    when(proxyRegistry.getProxyFor(principal)).thenReturn(proxy);
    return proxyRegistry;
  }

  private static ProxyableObjectOutputStream newProxyableObjectOutputStream(final ProxyRegistry proxyRegistry)
      throws Exception {
    return new ProxyableObjectOutputStream(new ByteArrayOutputStream(), proxyRegistry);
  }

  @Test
  public void replaceObject_ShouldReturnNullWhenObjectIsNull() throws Exception {
    try (final ProxyableObjectOutputStream oos = newProxyableObjectOutputStream(ProxyRegistry.newInstance())) {
      final Object replacedObj = oos.replaceObject(null);

      assertThat(replacedObj, is(nullValue()));
    }
  }
}
