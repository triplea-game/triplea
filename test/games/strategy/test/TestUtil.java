package games.strategy.test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;

public class TestUtil {
  /**
   * A server socket has a time to live after it is closed in which it is still
   * bound to its port. For testing, we need to use a new port each time
   * to prevent socket already bound errors
   */
  public static int getUniquePort() {
    // store/get from SystemProperties
    // to get around junit reloading
    final String KEY = "triplea.test.port";
    String prop = System.getProperties().getProperty(KEY);
    if (prop == null) {
      // start off with something fairly random, between 12000 - 14000
      prop = Integer.toString(12000 + (int) (Math.random() % 2000));
    }
    int val = Integer.parseInt(prop);
    val++;
    if (val > 15000) {
      val = 12000;
    }
    System.getProperties().put(KEY, "" + val);
    return val;
  }

  /**
   * Blocks until all Swing event thread actions have completed.
   *
   * Task is accomplished by adding a do-nothing event with SwingUtilities
   * to the event thread and then blocking until the do-nothing event is done.
   */
  public static void waitForSwingThreads() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          // do nothing event, should be at the end of the event queue.
        }
      });
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void assertVisibleAndEnabled(Component... components) {
    for (Component component : components) {
      assertThat(component.isVisible(), is(true));
      assertThat(component.isEnabled(), is(true));
    }
  }
}
