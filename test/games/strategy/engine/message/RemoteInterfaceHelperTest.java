package games.strategy.engine.message;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Comparator;

import org.junit.Test;

public class RemoteInterfaceHelperTest {

  @Test
  public void testSimple() {
    assertEquals("compare", RemoteInterfaceHelper.getMethodInfo(0, Comparator.class).getFirst());
    assertEquals("add", RemoteInterfaceHelper.getMethodInfo(0, Collection.class).getFirst());
    assertEquals(0, RemoteInterfaceHelper.getNumber("add", new Class[] {Object.class}, Collection.class));
    assertEquals(2, RemoteInterfaceHelper.getNumber("clear", new Class[] {}, Collection.class));
  }
}
