package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class WrappedInvocationHandlerTest {
  private Object newProxy(final InvocationHandler handler) {
    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {}, handler);
  }

  @Nested
  final class VerifyEquals {
    @Test
    void shouldReturnTrueWhenOtherInstanceIsWrappedInvocationHandlerProxyWithEqualDelegate() {
      final Object proxy1 = newProxy(new WrappedInvocationHandler("test"));
      final Object proxy2 = newProxy(new WrappedInvocationHandler("test"));

      assertEquals(proxy1, proxy2);
    }

    @Test
    void shouldReturnFalseWhenOtherInstanceIsWrappedInvocationHandlerProxyWithUnequalDelegate() {
      final Object proxy1 = newProxy(new WrappedInvocationHandler("test1"));
      final Object proxy2 = newProxy(new WrappedInvocationHandler("test2"));

      assertNotEquals(proxy1, proxy2);
    }

    @Test
    void shouldReturnFalseWhenOtherInstanceIsProxyWithoutWrappedInvocationHandler() {
      final Object proxy1 = newProxy(new WrappedInvocationHandler("test"));
      final Object proxy2 = newProxy((proxy, method, args) -> null);

      assertNotEquals(proxy1, proxy2);
    }
  }

  @Test
  void hashCodeShouldReturnHashCodeOfDelegate() {
    final Object delegate = "test";
    final Object proxy = newProxy(new WrappedInvocationHandler(delegate));

    assertEquals(delegate.hashCode(), proxy.hashCode());
  }

  @Test
  void toStringShouldReturnToStringOfDelegate() {
    final Object delegate = "test";
    final Object proxy = newProxy(new WrappedInvocationHandler(delegate));

    assertEquals(delegate.toString(), proxy.toString());
  }
}
