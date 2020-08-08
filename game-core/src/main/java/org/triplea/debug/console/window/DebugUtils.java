package org.triplea.debug.console.window;

import games.strategy.engine.framework.system.SystemProperties;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A collection of methods that provide information about the JVM state that may be useful for
 * debugging.
 */
public final class DebugUtils {
  private static final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

  private DebugUtils() {}

  /**
   * Returns a message containing the stack trace of each active thread, as well as a listing of
   * possibly-deadlocked threads.
   */
  static String getThreadDumps() {
    final StringBuilder result = new StringBuilder();
    result.append("THREAD DUMP\n");
    final ThreadInfo[] threadInfo =
        threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), Integer.MAX_VALUE);
    for (final ThreadInfo info : threadInfo) {
      if (info != null) {
        result
            .append("thread<")
            .append(info.getThreadId())
            .append(",")
            .append(info.getThreadName())
            .append(">\n")
            .append("state:")
            .append(info.getThreadState())
            .append("\n");
        if (info.getLockName() != null) {
          result
              .append("locked on:")
              .append(info.getLockName())
              .append(" locked owned by:<")
              .append(info.getLockOwnerId())
              .append(",")
              .append(info.getLockOwnerName())
              .append(">\n");
        }
        final StackTraceElement[] stackTrace = info.getStackTrace();
        for (final StackTraceElement element : stackTrace) {
          result.append("  ");
          result.append(element);
          result.append("\n");
        }
        result.append("\n");
      }
    }
    final long[] deadlocks = threadMxBean.findDeadlockedThreads();

    if (deadlocks != null) {
      result.append("DEADLOCKS!!");
      for (final long l : deadlocks) {
        result.append(l).append("\n");
      }
    }
    return result.toString();
  }

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

  static String getProperties() {
    final StringBuilder buf = new StringBuilder("SYSTEM PROPERTIES\n");
    final Properties props = SystemProperties.all();
    final List<String> keys = new ArrayList<>(props.stringPropertyNames());
    Collections.sort(keys);
    for (final String property : keys) {
      final String value = props.getProperty(property);
      buf.append(property).append("=").append(value).append("\n");
    }
    return buf.toString();
  }
}
