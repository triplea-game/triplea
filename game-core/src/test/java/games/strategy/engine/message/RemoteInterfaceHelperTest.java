package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class RemoteInterfaceHelperTest {
  @Test
  void testSimple() {
    assertEquals("baz", RemoteInterfaceHelper.getMethod(5, FakeRemoteInterface.class).getName());
    assertEquals("qux", RemoteInterfaceHelper.getMethod(8, FakeRemoteInterface.class).getName());
    assertEquals(
        5,
        RemoteInterfaceHelper.getNumber(
            "baz", new Class<?>[] {Object.class}, FakeRemoteInterface.class));
    assertEquals(
        8,
        RemoteInterfaceHelper.getNumber(
            "qux", new Class<?>[] {Object.class}, FakeRemoteInterface.class));
  }

  @Test
  void testCorrectOverloadOrder() {
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

    assertEquals(
        0, RemoteInterfaceHelper.getNumber("bar", new Class<?>[] {}, FakeRemoteInterface.class));
    assertEquals(
        4,
        RemoteInterfaceHelper.getNumber(
            "bar", new Class<?>[] {Object.class}, FakeRemoteInterface.class));
  }

  private static void checkMethodMatches(
      final String name, final Class<?>[] parameterTypes, final Method method) {
    assertEquals(name, method.getName());
    assertArrayEquals(parameterTypes, method.getParameterTypes());
  }

  private interface FakeRemoteInterface {
    void foo(String arg1);

    void foo(String arg1, Object... arg2);

    void bar();

    void bar(char[] arg1);

    void bar(boolean arg1);

    void bar(char arg1);

    void bar(Object arg1);

    int baz(Object arg1);

    boolean qux(Object arg1);
  }
}
