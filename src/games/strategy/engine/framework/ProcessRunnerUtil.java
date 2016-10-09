package games.strategy.engine.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.Memory;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.Version;

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
    populateBasicJavaArgs(commands, System.getProperty("java.class.path"));
  }

  public static void populateBasicJavaArgs(final List<String> commands, final long maxMemory) {
    populateBasicJavaArgs(commands, System.getProperty("java.class.path"), maxMemory);
  }

  public static void populateBasicJavaArgs(final List<String> commands, final String newClasspath) {
    // for whatever reason, .maxMemory() returns a value about 12% smaller than the real Xmx value, so we are going to
    // add 64m to that to
    // compensate
    // final long maxMemory = ((long) (Runtime.getRuntime().maxMemory() * 1.15) + 67108864);
    final long maxMemory = Memory.getMaxMemoryInBytes();
    System.out.println("Setting memory for new triplea process to: " + (maxMemory / (1024 * 1024)) + "m");
    populateBasicJavaArgs(commands, newClasspath, maxMemory);
  }

  public static void populateBasicJavaArgs(final List<String> commands, final String classpath, final long maxMemory) {
    final String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    commands.add(javaCommand);
    commands.add("-classpath");
    if (classpath != null && classpath.length() > 0) {
      commands.add(classpath);
    } else {
      commands.add(System.getProperty("java.class.path"));
    }
    commands.add("-Xmx" + maxMemory);
    // this should never ever go above 1000mb, because some users have errors because some JVM's can't handle
    // that much
    // commands.add("-Xmx896m");
    // preserve noddraw to fix 1742775
    final String[] preservedSystemProperties = {"sun.java2d.noddraw"};
    for (final String key : preservedSystemProperties) {
      if (System.getProperties().getProperty(key) != null) {
        final String value = System.getProperties().getProperty(key);
        if (value.matches("[a-zA-Z0-9.]+")) {
          commands.add("-D" + key + "=" + value);
        }
      }
    }
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final File icons = new File(ClientFileSystemHelper.getRootFolder(), "icons/triplea_icon.png");
      if (!icons.exists()) {
        throw new IllegalStateException("Icon file not found");
      }
      commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
    }
    final String version = System.getProperty(GameRunner.TRIPLEA_ENGINE_VERSION_BIN);
    if (version != null && version.length() > 0) {
      final Version testVersion;
      try {
        testVersion = new Version(version);
        commands.add("-D" + GameRunner.TRIPLEA_ENGINE_VERSION_BIN + "=" + testVersion.toString());
      } catch (final Exception e) {
        // nothing
      }
    }
    // since we are setting the xmx already, we need to
    // make sure this property is set so that triplea
    // doesn't restart
    commands.add("-D" + Memory.TRIPLEA_MEMORY_SET + "=" + Boolean.TRUE.toString());
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
      final Thread t = new Thread(() -> {
        try (Scanner scanner = new Scanner(s);) {
          while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
          }
        }
      }, "Process output gobbler");
      t.setDaemon(true);
      t.start();
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }
}
