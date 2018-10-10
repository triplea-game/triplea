package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.logging.LogRecord;

import javax.swing.JFrame;

/**
 * Used to create and show an error report window which users can use to add bug report information and upload
 * directly to TripleA servers.
 */
public final class ReportWindowController {

  private ReportWindowController() {}

  // TODO: replace no-op report handler with 'new ReportUploadProcess()'
  private static final BiConsumer<JFrame, UserErrorReport> reportHandler = (frame, report) -> {
  };

  public static void showWindow(final Component parent) {
    showWindow(parent, null);
  }

  public static void showWindow(final Component parent, final LogRecord logRecord) {
    final JFrame frame = new ErrorReportWindow(reportHandler)
        .buildWindow(parent, logRecord);
    frame.setVisible(true);
  }
}
