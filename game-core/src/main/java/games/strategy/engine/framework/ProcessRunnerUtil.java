package games.strategy.engine.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.Util;

/**
 * To hold various static utility methods for running a java program.
 */
public class ProcessRunnerUtil {

  public static void runClass(final Class<?> mainClass) {
    final List<String> commands = new ArrayList<>();
    populateBasicJavaArgs(commands);
    commands.add(mainClass.getName());
    exec(commands);
  }

  public static void populateBasicJavaArgs(final List<String> commands) {
    populateBasicJavaArgs(commands, SystemProperties.getJavaClassPath());
  }

  public static void populateBasicJavaArgs(final List<String> commands, final long maxMemory) {
    populateBasicJavaArgs(commands, SystemProperties.getJavaClassPath(), Optional.of(String.valueOf(maxMemory)));
  }

  static void populateBasicJavaArgs(final List<String> commands, final String newClasspath) {
    populateBasicJavaArgs(commands, newClasspath, ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(s -> s.toLowerCase().startsWith("-xmx")).map(s -> s.substring(4)).findFirst());
  }

  private static void populateBasicJavaArgs(final List<String> commands, final String classpath,
      final Optional<String> maxMemory) {
    final String javaCommand = SystemProperties.getJavaHome() + File.separator + "bin" + File.separator + "java";
    commands.add(javaCommand);
    commands.add("-classpath");
    if ((classpath != null) && (classpath.length() > 0)) {
      commands.add(classpath);
    } else {
      commands.add(SystemProperties.getJavaClassPath());
    }
    if (maxMemory.isPresent()) {
      System.out.println("Setting memory for new triplea process to: " + maxMemory.get());
      commands.add("-Xmx" + maxMemory.get());
    }
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final File icons = new File(ClientFileSystemHelper.getRootFolder(), "icons/triplea_icon.png");
      if (icons.exists()) {
        commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
      }
    }
  }

  public static void exec(final List<String> commands) {
    // System.out.println("Commands: " + commands);
    final ProcessBuilder builder = new ProcessBuilder(commands);
    // merge the streams, so we only have to start one reader thread
    builder.redirectErrorStream(true);
    try {
      final Process p = builder.start();
      final InputStream s = p.getInputStream();
      // we need to read the input stream to prevent possible
      // deadlocks
      Util.createDaemonThread(() -> {
        try (Scanner scanner = new Scanner(s, Charset.defaultCharset().name())) {
          while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
          }
        }
      }, "Process output gobbler").start();
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to start new process", e);
    }
  }
}
