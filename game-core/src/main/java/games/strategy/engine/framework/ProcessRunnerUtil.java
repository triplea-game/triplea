package games.strategy.engine.framework;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import lombok.extern.java.Log;

/** To hold various static utility methods for running a java program. */
@Log
final class ProcessRunnerUtil {
  private ProcessRunnerUtil() {}

  static void populateBasicJavaArgs(final List<String> commands) {
    final String javaCommand =
        SystemProperties.getJavaHome() + File.separator + "bin" + File.separator + "java";
    commands.add(javaCommand);
    commands.add("-classpath");
    commands.add(SystemProperties.getJavaClassPath());

    final Optional<String> maxMemory =
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(s -> s.toLowerCase().startsWith("-xmx"))
            .map(s -> s.substring(4))
            .findFirst();
    maxMemory.ifPresent(max -> commands.add("-Xmx" + max));
    final Optional<String> maxStackSize =
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(s -> s.toLowerCase().startsWith("-xss"))
            .findFirst();
    maxStackSize.ifPresent(commands::add);
    if (SystemProperties.isMac()) {
      commands.add("-Dapple.laf.useScreenMenuBar=true");
      commands.add("-Xdock:name=\"TripleA\"");
      final File icons = new File(ClientFileSystemHelper.getRootFolder(), "icons/triplea_icon.png");
      if (icons.exists()) {
        commands.add("-Xdock:icon=" + icons.getAbsolutePath());
      }
    }
  }
}
