package games.strategy.debug.error.reporting;

import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingComponents;

class ReportPreviewSwingView implements Consumer<ErrorReport> {
  @Override
  public void accept(final ErrorReport errorReport) {
    JOptionPane.showMessageDialog(
        null, SwingComponents.newJScrollPane(
            JTextAreaBuilder.builder()
                .columns(45)
                .rows(12)
                .readOnly()
                .text(errorReport.getTitle() + "\n\n" + errorReport.getBody())
                .build()),
        "Preview - The Following Data Will Be Uploaded",
        JOptionPane.INFORMATION_MESSAGE);
  }
}
