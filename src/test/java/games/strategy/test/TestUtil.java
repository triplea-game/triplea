package games.strategy.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import com.google.common.io.Files;

import games.strategy.ui.SwingAction;

/**
 * A Utility class for test classes.
 */
public final class TestUtil {
  private TestUtil() {}

  /** Create and returns a simple delete on exit temp file with contents equal to the String parameter. */
  public static File createTempFile(final String contents) {
    final File file;
    try {
      file = File.createTempFile("testFile", ".tmp");
      file.deleteOnExit();
      Files.asCharSink(file, StandardCharsets.UTF_8).write(contents);
      return file;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A server socket has a time to live after it is closed in which it is still
   * bound to its port. For testing, we need to use a new port each time
   * to prevent socket already bound errors
   */
  public static int getUniquePort() {
    // store/get from SystemProperties
    // to get around junit reloading
    final String key = "triplea.test.port";
    String prop = System.getProperties().getProperty(key);
    if (prop == null) {
      // start off with something fairly random, between 12000 - 14000
      prop = Integer.toString(12000 + (int) (Math.random() % 2000));
    }
    int val = Integer.parseInt(prop);
    val++;
    if (val > 15000) {
      val = 12000;
    }
    System.getProperties().put(key, "" + val);
    return val;
  }

  /**
   * Blocks until all Swing event thread actions have completed.
   *
   * <p>
   * Task is accomplished by adding a do-nothing event with SwingUtilities
   * to the event thread and then blocking until the do-nothing event is done.
   * </p>
   */
  public static void waitForSwingThreads() {
    // add a no-op action to the end of the swing event queue, and then wait for it
    SwingAction.invokeAndWait(() -> {
    });
  }

  public static Class<?>[] getClassArrayFrom(final Class<?>... classes) {
    return classes;
  }

  /**
   * Indicates the specified objects are equal.
   *
   * <p>
   * This method uses the equality comparators in the specified registry to determine if the objects are equal. If an
   * equality comparator is not available for a specific type, {@link Object#equals(Object)} will be used instead.
   * </p>
   *
   * <p>
   * This method correctly handles circular references between objects involved in the equality test.
   * </p>
   *
   * @param equalityComparatorRegistry The registry containing the equality comparators to use during the equality test.
   * @param o1 The first object to compare.
   * @param o2 The second object to compare.
   *
   * @return {@code true} if the specified objects are equal; otherwise {@code false}.
   */
  public static boolean equals(
      final EqualityComparatorRegistry equalityComparatorRegistry,
      final @Nullable Object o1,
      final @Nullable Object o2) {
    checkNotNull(equalityComparatorRegistry);

    return new EqualsPredicate(equalityComparatorRegistry).test(o1, o2);
  }
}
