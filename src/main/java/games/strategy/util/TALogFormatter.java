package games.strategy.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TALogFormatter extends Formatter {
  private boolean showDates = false;

  public TALogFormatter() {}

  public void setShowDates(final boolean aVal) {
    showDates = aVal;
  }

  @Override
  public String format(final LogRecord record) {
    String shortName;
    if (record.getLoggerName() == null) {
      shortName = ".";
    } else if (record.getLoggerName().indexOf('.') == -1) {
      shortName = record.getLoggerName();
    } else {
      shortName = record.getLoggerName().substring(record.getLoggerName().lastIndexOf('.') + 1,
          record.getLoggerName().length());
    }
    final StringBuilder builder = new StringBuilder();
    if (showDates) {
      builder.append(new Date());
      builder.append(" ");
    }
    builder.append(record.getLevel());
    builder.append(" [");
    builder.append(Thread.currentThread().getName());
    builder.append("] ");
    builder.append(shortName);
    builder.append(" -> ");
    builder.append(record.getMessage());
    if (!builder.toString().endsWith("\r\n")) {
      if (builder.toString().endsWith("\n")) {
        builder.setLength(builder.toString().length() - 1);
      }
      builder.append("\r\n");
    }
    if (record.getThrown() != null) {
      final StringWriter writer = new StringWriter();
      final PrintWriter pw = new PrintWriter(writer);
      record.getThrown().printStackTrace(pw);
      pw.flush();
      builder.append(writer.getBuffer());
    }
    if (!builder.toString().endsWith("\r\n")) {
      if (builder.toString().endsWith("\n")) {
        builder.setLength(builder.toString().length() - 1);
      }
      builder.append("\r\n");
    }
    return builder.toString();
  }
}
