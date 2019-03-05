package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;

/**
 * Some utility methods for dealing with collections.
 */
public final class Util {

  private Util() {}

  // TODO: move to a test utility
  @VisibleForTesting
  public static String newUniqueTimestamp() {
    final long time = System.currentTimeMillis();
    while (time == System.currentTimeMillis()) {
      Interruptibles.sleep(1);
    }
    return "" + System.currentTimeMillis();
  }
}
