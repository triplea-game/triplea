package org.triplea.debug.console.window;

import lombok.experimental.UtilityClass;

/**
 * A collection of methods that provide information about the JVM state that may be useful for
 * debugging.
 */
@UtilityClass
public final class DebugUtils {

  /** Returns a message containing information about current memory usage. */
  public static String getMemory() {
    System.gc();
    System.runFinalization();
    System.gc();
    final int mb = 1024 * 1024;
    final StringBuilder buf = new StringBuilder("Heap utilization statistics [MB]\r\n");
    final Runtime runtime = Runtime.getRuntime();
    buf.append("Used Memory: ")
        .append((runtime.totalMemory() - runtime.freeMemory()) / mb)
        .append("\r\n");
    buf.append("Free memory: ").append(runtime.freeMemory() / mb).append("\r\n");
    buf.append("Total memory: ").append(runtime.totalMemory() / mb).append("\r\n");
    buf.append("Max memory: ").append(runtime.maxMemory() / mb).append("\r\n");
    return buf.toString();
  }
}
