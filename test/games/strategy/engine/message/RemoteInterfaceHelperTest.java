package games.strategy.engine.message;

import java.util.Collection;
import java.util.Comparator;

import junit.framework.TestCase;

public class RemoteInterfaceHelperTest extends TestCase { 
  public void testSimple() {
    assertEquals("compare", RemoteInterfaceHelper.getMethodInfo(0, Comparator.class).getFirst());
    assertEquals("add", RemoteInterfaceHelper.getMethodInfo(0, Collection.class).getFirst());
    assertEquals(0, RemoteInterfaceHelper.getNumber("add", new Class[] {Object.class}, Collection.class));
    assertEquals(2, RemoteInterfaceHelper.getNumber("clear", new Class[] {}, Collection.class));
  }
}
