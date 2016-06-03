package games.strategy.debug;

import java.awt.Frame;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner2;

/**
 * Moved out of Console class, so that we don't need swing.
 */
public class DebugUtils {
  private static final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

  public static String getThreadDumps() {
    final StringBuilder result = new StringBuilder();
    result.append("THREAD DUMP\n");
    final ThreadInfo[] threadInfo = threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), Integer.MAX_VALUE);
    for (final ThreadInfo info : threadInfo) {
      if (info != null) {
        result.append("thread<").append(info.getThreadId()).append(",").append(info.getThreadName()).append(">\n").append("state:")
            .append(info.getThreadState()).append("\n");
        if (info.getLockName() != null) {
          result.append("locked on:").append(info.getLockName()).append(" locked owned by:<").append(info.getLockOwnerId())
              .append(",").append(info.getLockOwnerName()).append(">\n");
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
    long[] deadlocks;
    try {
      // invoke a 1.6 method if available
      final Method m = threadMxBean.getClass().getMethod("findDeadlockedThreads");
      final Object o = m.invoke(threadMxBean);
      deadlocks = (long[]) o;
    } catch (final Throwable t) {
      // fall back to 1.5
      deadlocks = threadMxBean.findMonitorDeadlockedThreads();
    }
    if (deadlocks != null) {
      result.append("DEADLOCKS!!");
      for (final long l : deadlocks) {
        result.append(l).append("\n");
      }
    }
    return result.toString();
  }

  public static String getMemory() {
    System.gc();
    System.runFinalization();
    System.gc();
    final int mb = 1024 * 1024;
    final StringBuilder buf = new StringBuilder("Heap utilization statistics [MB]\r\n");
    final Runtime runtime = Runtime.getRuntime();
    buf.append("Used Memory: ").append((runtime.totalMemory() - runtime.freeMemory()) / mb).append("\r\n");
    buf.append("Free memory: ").append(runtime.freeMemory() / mb).append("\r\n");
    buf.append("Total memory: ").append(runtime.totalMemory() / mb).append("\r\n");
    buf.append("Max memory: ").append(runtime.maxMemory() / mb).append("\r\n");
    final int currentMaxSetting = GameRunner2.getMaxMemoryFromSystemIniFileInMB(GameRunner2.getSystemIni());
    if (currentMaxSetting > 0) {
      buf.append("Max Memory user setting within 22% of: ").append(currentMaxSetting).append("\r\n");
    }
    return buf.toString();
  }
  public static String getProperties() {
    final StringBuilder buf = new StringBuilder("SYSTEM PROPERTIES\n");
    final Properties props = System.getProperties();
    final List<String> keys = new ArrayList<>(props.stringPropertyNames());
    Collections.sort(keys);
    for (String property : keys) {
      final String value = props.getProperty(property);
      buf.append(property).append(" ").append(value).append("\n");
    }
    return buf.toString();
  }

  public static String getDebugReportHeadless() {
    final StringBuilder result = new StringBuilder(500);
    result.append(getThreadDumps());
    result.append(getProperties());
    result.append(getMemory());
    result.append("ENGINE VERSION: ").append(ClientContext.engineVersion()).append("\n");
    return result.toString();
  }

  public static String getDebugReportWithFramesAndWindows() {
    final StringBuilder result = new StringBuilder(500);
    result.append("CONSOLE_OUTPUT:\n");
    result.append(ErrorConsole.getConsole().getText());
    result.append("\n");
    result.append(getThreadDumps());
    result.append(getProperties());
    result.append(getMemory());
    result.append(getOpenAppWindows());
    result.append("ENGINE VERSION: ").append(ClientContext.engineVersion().getVersion()).append("\n");
    return result.toString();
  }

  private static String getOpenAppWindows() {
    final StringBuilder builder = new StringBuilder("WINDOWS\n");
    for (final Frame f : Frame.getFrames()) {
      if (f.isVisible()) {
        builder.append("window:").append("class ").append(f.getClass()).append(" size ").append(f.getSize()).append(" title ")
            .append(f.getTitle()).append("\n");
      }
    }
    return builder.toString();
  }
}
