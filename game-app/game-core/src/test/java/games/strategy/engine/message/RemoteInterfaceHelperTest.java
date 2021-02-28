package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class RemoteInterfaceHelperTest {
  @Test
  void testSimple() throws Exception {
    assertEquals("baz", RemoteInterfaceHelper.getMethod(5, FakeRemoteInterface.class).getName());
    assertEquals("qux", RemoteInterfaceHelper.getMethod(8, FakeRemoteInterface.class).getName());
    assertEquals(
        5,
        RemoteInterfaceHelper.getNumber(FakeRemoteInterface.class.getMethod("baz", Object.class)));
    assertEquals(
        8,
        RemoteInterfaceHelper.getNumber(FakeRemoteInterface.class.getMethod("qux", Object.class)));
  }

  @Test
  void testCorrectOverloadOrder() throws Exception {
    checkMethodMatches(
        "foo",
        new Class<?>[] {String.class, Object[].class},
        RemoteInterfaceHelper.getMethod(7, FakeRemoteInterface.class));
    checkMethodMatches(
        "bar",
        new Class<?>[] {char[].class},
        RemoteInterfaceHelper.getMethod(1, FakeRemoteInterface.class));
    checkMethodMatches(
        "bar",
        new Class<?>[] {boolean.class},
        RemoteInterfaceHelper.getMethod(2, FakeRemoteInterface.class));
    checkMethodMatches(
        "bar",
        new Class<?>[] {char.class},
        RemoteInterfaceHelper.getMethod(3, FakeRemoteInterface.class));

    assertEquals(0, RemoteInterfaceHelper.getNumber(FakeRemoteInterface.class.getMethod("bar")));
    assertEquals(
        4,
        RemoteInterfaceHelper.getNumber(FakeRemoteInterface.class.getMethod("bar", Object.class)));
  }

  private static void checkMethodMatches(
      final String name, final Class<?>[] parameterTypes, final Method method) {
    assertEquals(name, method.getName());
    assertArrayEquals(parameterTypes, method.getParameterTypes());
  }

  private interface FakeRemoteInterface {
    @RemoteActionCode(6)
    void foo(String arg1);

    @RemoteActionCode(7)
    void foo(String arg1, Object... arg2);

    @RemoteActionCode(0)
    void bar();

    @RemoteActionCode(1)
    void bar(char[] arg1);

    @RemoteActionCode(2)
    void bar(boolean arg1);

    @RemoteActionCode(3)
    void bar(char arg1);

    @RemoteActionCode(4)
    void bar(Object arg1);

    @RemoteActionCode(5)
    int baz(Object arg1);

    @RemoteActionCode(8)
    boolean qux(Object arg1);
  }
}
