package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.logging.LogRecord;

import javax.swing.JFrame;

/**
 * Used to create and show an error report window which users can use to add bug report information and upload
 * directly to TripleA servers.
 */
public final class ReportWindowController {

  private ReportWindowController() {}

  public static void showWindow(final Component parent) {
    showWindow(parent, null);
  }

  public static void showWindow(final Component parent, final LogRecord logRecord) {
    final JFrame frame = new ErrorReportWindow(ErrorReportConfiguration.newReportHandler())
        .buildWindow(parent, logRecord);
    frame.setVisible(true);
  }
}
