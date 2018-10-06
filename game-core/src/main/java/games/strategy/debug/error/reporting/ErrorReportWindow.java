package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import swinglib.JFrameBuilder;
import swinglib.JPanelBuilder;

/**
 * This is primarily a layout class that shows a form for entering bug report information, and has buttons to
 * preview, cancel, or upload the bug report.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class ErrorReportWindow {
  private final Consumer<UserErrorReport> reportHandler;
  private final ErrorReportComponents errorReportComponents = new ErrorReportComponents();


  @VisibleForTesting
  JFrame buildWindow() {
    return buildWindow(null, null);
  }

  JFrame buildWindow(@Nullable final Component parent, @Nullable final LogRecord logRecord) {

    final JTextArea description = errorReportComponents.descriptionArea();
    final JTextArea additionalInfo = errorReportComponents.additionalInfo();

    final Supplier<UserErrorReport> guiReader = () -> UserErrorReport.builder()
        .logRecord(logRecord)
        .description(description.getText())
        .additionalInfo(additionalInfo.getText())
        .build();

    final JFrame frame = JFrameBuilder.builder()
        .title("Bug Report Upload")
        .locateRelativeTo(parent)
        .minSize(500, 400)
        .build();
    frame.add(JPanelBuilder.builder()
        .borderEmpty(10)
        .borderLayout()
        .addCenter(JPanelBuilder.builder()
            .verticalBoxLayout()
            .addLabel("Please describe the problem you encountered:", JPanelBuilder.TextAlignment.LEFT)
            .add(description)
            .addHtmlLabel(
                "Is there any additional information that would be useful to know:",
                JPanelBuilder.TextAlignment.LEFT)
            .add(additionalInfo)
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .borderEmpty(3)
            .addHorizontalStrut(30)
            .add(errorReportComponents.createSubmitButton(ErrorReportComponents.FormHandler.builder()
                .guiDataHandler(reportHandler)
                .guiReader(guiReader)
                .build()))
            .addHorizontalStrut(20)
            .add(errorReportComponents.createPreviewButton(ErrorReportComponents.FormHandler.builder()
                .guiDataHandler(data -> new PreviewWindow().build(frame, data).setVisible(true))
                .guiReader(guiReader)
                .build()))
            .addHorizontalStrut(100)
            .add(errorReportComponents.createCancelButton(frame::dispose))
            .addHorizontalStrut(30)
            .build())
        .build());
    return frame;
  }
}
