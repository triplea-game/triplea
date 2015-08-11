package games.strategy.util;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * A simple logger that prints to System.out.
 * wtf? Why do I need to write this. Why cant ConsoleHandler
 * be set up to write to something other than System.err? I
 * am so close to switching to log4j
 */
public class SystemOutHandler extends StreamHandler {
  public SystemOutHandler() {
    super(System.out, new SimpleFormatter());
    setFormatter(new TALogFormatter());
  }

  @Override
  public void publish(final LogRecord record) {
    super.publish(record);
    flush();
  }
}
