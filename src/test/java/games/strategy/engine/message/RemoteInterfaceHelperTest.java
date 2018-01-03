package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

public class RemoteInterfaceHelperTest {

  @Test
  public void testSimple() {
    assertEquals("compare", RemoteInterfaceHelper.getMethod(0, Comparator.class).getName());
    assertEquals("add", RemoteInterfaceHelper.getMethod(0, Collection.class).getName());
    assertEquals(0, RemoteInterfaceHelper.getNumber("add", new Class<?>[] {Object.class}, Collection.class));
    assertEquals(2, RemoteInterfaceHelper.getNumber("clear", new Class<?>[] {}, Collection.class));
  }
}
