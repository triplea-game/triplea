package games.strategy.engine.framework;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

/** To hold various static utility methods for running a java program. */
@Slf4j
@UtilityClass
class ProcessRunnerUtil {

  public static List<String> createBasicJavaArgs() {
    @NonNls List<String> commands = new ArrayList<>();

    String javaCommand = Path.of(SystemProperties.getJavaHome(), "bin", "java").toString();
    commands.add(javaCommand);
    commands.add("-classpath");
    commands.add(SystemProperties.getJavaClassPath());

    Optional<String> maxMemory =
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("-xmx"))
            .map(s -> s.substring(4))
            .findFirst();
    maxMemory.ifPresent(max -> commands.add("-Xmx" + max));
    Optional<String> maxStackSize =
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("-xss"))
            .findFirst();
    maxStackSize.ifPresent(commands::add);
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final Path icons = ClientFileSystemHelper.getRootFolder().resolve("icons/triplea_icon.png");
      if (Files.exists(icons)) {
        commands.add("-Xdock:icon=" + icons.toAbsolutePath());
      }
    }
    return commands;
  }

  static void exec(final List<String> commands) {
    try {
      new ProcessBuilder(commands).inheritIO().start();
    } catch (final IOException e) {
      log.error("Failed to start new process", e);
    }
  }
}
