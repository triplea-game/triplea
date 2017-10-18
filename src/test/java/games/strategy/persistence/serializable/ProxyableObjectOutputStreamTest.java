package games.strategy.persistence.serializable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import games.strategy.io.IoUtils;

@ExtendWith(MockitoExtension.class)
public final class ProxyableObjectOutputStreamTest {
  @Test
  public void replaceObject_ShouldDelegateToProxyRegistryWhenObjectIsNotNull() throws Exception {
    final Object obj = Integer.valueOf(42);
    final Object expectedReplacedObj = "42";
    final ProxyRegistry proxyRegistry = givenProxyRegistryFor(obj, expectedReplacedObj);
    IoUtils.writeToMemory(os -> {
      try (ProxyableObjectOutputStream oos = new ProxyableObjectOutputStream(os, proxyRegistry)) {
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
      try (ProxyableObjectOutputStream oos = new ProxyableObjectOutputStream(os, ProxyRegistry.newInstance())) {
        final Object replacedObj = oos.replaceObject(null);

        assertThat(replacedObj, is(nullValue()));
      }
    });
  }
}
