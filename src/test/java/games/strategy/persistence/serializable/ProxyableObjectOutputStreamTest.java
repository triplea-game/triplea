package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.io.IoUtils;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class ProxyableObjectOutputStreamTest {
  @Test
  public void replaceObject_ShouldDelegateToProxyRegistryWhenObjectIsNotNull() throws Exception {
    final Object obj = Integer.valueOf(42);
    final Object expectedReplacedObj = "42";
    final ProxyRegistry proxyRegistry = givenProxyRegistryFor(obj, expectedReplacedObj);
    IoUtils.writeToMemory(os -> {
      try (final ProxyableObjectOutputStream oos = new ProxyableObjectOutputStream(os, proxyRegistry)) {
        final Object actualReplacedObj = oos.replaceObject(obj);

        verify(proxyRegistry).getProxyFor(obj);
        assertThat(actualReplacedObj, is(expectedReplacedObj));
      }
    });
  }

  private static ProxyRegistry givenProxyRegistryFor(final Object principal, final Object proxy) {
    final ProxyRegistry proxyRegistry = mock(ProxyRegistry.class);
    when(proxyRegistry.getProxyFor(principal)).thenReturn(proxy);
    return proxyRegistry;
  }

  @Test
  public void replaceObject_ShouldReturnNullWhenObjectIsNull() throws Exception {
    IoUtils.writeToMemory(os -> {
      try (final ProxyableObjectOutputStream oos = new ProxyableObjectOutputStream(os, ProxyRegistry.newInstance())) {
        final Object replacedObj = oos.replaceObject(null);

        assertThat(replacedObj, is(nullValue()));
      }
    });
  }
}
