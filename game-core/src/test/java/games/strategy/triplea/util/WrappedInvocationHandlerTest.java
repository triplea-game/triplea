package games.strategy.triplea.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

final class WrappedInvocationHandlerTest {
  private Object newProxy(final InvocationHandler handler) {
    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {}, handler);
  }

  @Test
  void equals_ShouldReturnTrueWhenOtherInstanceIsWrappedInvocationHandlerProxyWithEqualDelegate() {
    final Object proxy1 = newProxy(new WrappedInvocationHandler("test"));
    final Object proxy2 = newProxy(new WrappedInvocationHandler("test"));

    assertEquals(proxy1, proxy2);
  }

  @Test
  void equals_ShouldReturnFalseWhenOtherInstanceIsWrappedInvocationHandlerProxyWithUnequalDelegate() {
    final Object proxy1 = newProxy(new WrappedInvocationHandler("test1"));
    final Object proxy2 = newProxy(new WrappedInvocationHandler("test2"));

    assertNotEquals(proxy1, proxy2);
  }

  @Test
  void equals_ShouldReturnFalseWhenOtherInstanceIsProxyWithoutWrappedInvocationHandler() {
    final Object proxy1 = newProxy(new WrappedInvocationHandler("test"));
    final Object proxy2 = newProxy((proxy, method, args) -> null);

    assertNotEquals(proxy1, proxy2);
  }

  @Test
  void hashCode_ShouldReturnHashCodeOfDelegate() {
    final Object delegate = "test";
    final Object proxy = newProxy(new WrappedInvocationHandler(delegate));

    assertEquals(delegate.hashCode(), proxy.hashCode());
  }

  @Test
  void toString_ShouldReturnToStringOfDelegate() {
    final Object delegate = "test";
    final Object proxy = newProxy(new WrappedInvocationHandler(delegate));

    assertEquals(delegate.toString(), proxy.toString());
  }
}
