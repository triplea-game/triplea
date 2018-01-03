package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class RemoteInterfaceHelperTest {

  @Test
  public void testSimple() {
    assertEquals("compare", RemoteInterfaceHelper.getMethod(0, Comparator.class).getName());
    assertEquals("add", RemoteInterfaceHelper.getMethod(0, Collection.class).getName());
    assertEquals(0, RemoteInterfaceHelper.getNumber("add", new Class<?>[] {Object.class}, Collection.class));
    assertEquals(2, RemoteInterfaceHelper.getNumber("clear", new Class<?>[] {}, Collection.class));
  }


  @Test
  public void testCorrectOverloadOrder() {
    checkMethodMatches("printf", new Class<?>[] {Locale.class, String.class, Object[].class},
        RemoteInterfaceHelper.getMethod(26, PrintStream.class));
    checkMethodMatches("println", new Class<?>[] {char[].class},
        RemoteInterfaceHelper.getMethod(28, PrintStream.class));
    checkMethodMatches("println", new Class<?>[] {boolean.class},
        RemoteInterfaceHelper.getMethod(29, PrintStream.class));
    checkMethodMatches("println", new Class<?>[] {char.class}, RemoteInterfaceHelper.getMethod(30, PrintStream.class));

    assertEquals(27, RemoteInterfaceHelper.getNumber("println", new Class<?>[] {}, PrintStream.class));
    assertEquals(34, RemoteInterfaceHelper.getNumber("println", new Class<?>[] {Object.class}, PrintStream.class));
  }

  private static void checkMethodMatches(final String name, final Class<?>[] parameterTypes, final Method method) {
    assertEquals(name, method.getName());
    assertArrayEquals(parameterTypes, method.getParameterTypes());
  }
}
