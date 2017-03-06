package games.strategy.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import games.strategy.engine.framework.startup.launcher.ServerLauncher;

public class AccessLogFileHandler extends FileHandler {
  private static final String logFile = getLogFilePath();

  private static String getLogFilePath() {
    final File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
    if (!rootDir.exists()) {
      throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
    }
    final File logDir = new File(rootDir, "access_logs");
    if (!logDir.exists()) {
      logDir.mkdir();
    }
    System.out.print("logging to :" + logFile);
    return new File(logDir, "access-log%g.txt").getAbsolutePath();
  }


  public AccessLogFileHandler() throws IOException, SecurityException {
    super(logFile, 20 * 1000 * 1000, 10, true);
    setFormatter(new TALogFormatter());
  }
}


class AccessLogFormat extends Formatter {
  @Override
  public String format(final LogRecord record) {
    return record.getMessage() + "\n";
  }
}
