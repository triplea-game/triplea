package games.strategy.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

import games.strategy.engine.framework.startup.launcher.ServerLauncher;

public class AccessLogFileHandler extends FileHandler {
  private static final String logFile;

  static {
    final File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
    if (!rootDir.exists()) {
      throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
    }
    final File logDir = new File(rootDir, "access_logs");
    if (!logDir.exists()) {
      logDir.mkdir();
    }
    logFile = new File(logDir, "access-log%g.txt").getAbsolutePath();
    System.out.print("logging to :" + logFile);
  }

  public AccessLogFileHandler() throws IOException, SecurityException {
    super(logFile, 20 * 1000 * 1000, 10, true);
    setFormatter(new TALogFormatter());
  }
}

