package games.strategy.engine.framework;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** To hold various static utility methods for running a java program. */
@Slf4j
final class ProcessRunnerUtil {
  private ProcessRunnerUtil() {}

  static void populateBasicJavaArgs(final List<String> commands) {
    final String javaCommand = Path.of(SystemProperties.getJavaHome(), "bin", "java").toString();
    commands.add(javaCommand);
    commands.add("-classpath");
    commands.add(SystemProperties.getJavaClassPath());

    ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(s -> s.toLowerCase().startsWith("-xmx"))
        .map(s -> s.substring(4))
        .findFirst()
        .ifPresent(max -> commands.add("-Xmx" + max));
    ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(s -> s.toLowerCase().startsWith("-xss"))
        .findFirst()
        .ifPresent(commands::add);
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final Path icons = ClientFileSystemHelper.getRootFolder().resolve("icons/triplea_icon.png");
      if (Files.exists(icons)) {
        commands.add("-Xdock:icon=" + icons.toAbsolutePath());
      }
    }
  }

  static void exec(final List<String> commands) {
    try {
      new ProcessBuilder(commands).inheritIO().start();
    } catch (final IOException e) {
      log.error("Failed to start new process", e);
    }
  }
}
