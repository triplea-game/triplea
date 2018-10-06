package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.logging.LogRecord;

import javax.swing.JFrame;

/**
 * Used to create and show an error report window which users can use to add bug report information and upload
 * directly to TripleA servers.
 */
public class ReportWindowController {

  public static void showWindow(final Component parent) {
    showWindow(parent, null);
  }

  public static void showWindow(final Component parent, final LogRecord logRecord) {
    // TODO: wire up http client here to send data.
    final ErrorReportWindow window = new ErrorReportWindow(data -> {
    });
    final JFrame frame = window.buildWindow(parent, logRecord);
    frame.setVisible(true);
  }
}
