package games.strategy.triplea.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.Test;

public class WrappedInvocationHandlerTest {

  @Test
  public void testEquals() {
    final String s1 = "test";
    final WrappedInvocationHandler handler = new WrappedInvocationHandler(s1);
    final Object o1 = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {}, handler);
    assertEquals(o1.hashCode(), s1.hashCode());
    final String s2 = "test";
    final WrappedInvocationHandler handler2 = new WrappedInvocationHandler(s2);
    final Object o2 = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {}, handler2);
    assertEquals(o1, o2);
  }
}
