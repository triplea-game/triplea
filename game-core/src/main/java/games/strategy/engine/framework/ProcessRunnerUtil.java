package games.strategy.engine.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import lombok.extern.java.Log;

/**
 * To hold various static utility methods for running a java program.
 */
@Log
public class ProcessRunnerUtil {

  static void populateBasicJavaArgs(final List<String> commands) {
    final String javaCommand = SystemProperties.getJavaHome() + File.separator + "bin" + File.separator + "java";
    commands.add(javaCommand);
    commands.add("-classpath");
    commands.add(SystemProperties.getJavaClassPath());

    final Optional<String> maxMemory = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(s -> s.toLowerCase().startsWith("-xmx"))
        .map(s -> s.substring(4))
        .findFirst();
    maxMemory.ifPresent(max -> commands.add("-Xmx" + max));
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final File icons = new File(ClientFileSystemHelper.getRootFolder(), "icons/triplea_icon.png");
      if (icons.exists()) {
        commands.add("-Xdock:icon=" + icons.getAbsolutePath());
      }
    }
  }

  public static void exec(final List<String> commands) {
    final ProcessBuilder builder = new ProcessBuilder(commands);
    // merge the streams, so we only have to start one reader thread
    builder.redirectErrorStream(true);
    try {
      final Process p = builder.start();
      final InputStream s = p.getInputStream();
      // we need to read the input stream to prevent possible
      // deadlocks
      startDaemonThread(() -> {
        try (Scanner scanner = new Scanner(s, Charset.defaultCharset().name())) {
          while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
          }
        }
      }, "Process output gobbler");
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to start new process", e);
    }
  }

  private static void startDaemonThread(final Runnable runnable, final String name) {
    final Thread thread = new Thread(runnable, name);
    thread.setDaemon(true);
    thread.start();
  }
}
