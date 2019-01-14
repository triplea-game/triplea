package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import swinglib.JFrameBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;

/**
 * This is primarily a layout class that shows a form for entering bug report information, and has buttons to
 * preview, cancel, or upload the bug report.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class ErrorReportWindow {
  private final BiConsumer<JFrame, UserErrorReport> reportHandler;
  private final ErrorReportComponents errorReportComponents = new ErrorReportComponents();

  JFrame buildWindow(@Nullable final Component parent, @Nullable final LogRecord logRecord) {

    final JTextField titleField = errorReportComponents.titleField();
    final JTextArea description = errorReportComponents.descriptionArea();

    final Supplier<UserErrorReport> guiReader = () -> UserErrorReport.builder()
        .description(description.getText())
        .build();

    final JFrame frame = JFrameBuilder.builder()
        .title("Contact TripleA Support")
        .locateRelativeTo(parent)
        .minSize(500, 400)
        .build();
    frame.add(JPanelBuilder.builder()
        .borderEmpty(10)
        .borderLayout()
        .addNorth(JPanelBuilder.builder()
            .borderEmpty(3)
            .borderLayout()
            .addWest(JPanelBuilder.builder()
                .borderEmpty(3)
                .addLabel("Subject:")
                .build())
            .addCenter(titleField)
            .build())
        .addCenter(JPanelBuilder.builder()
            .borderLayout()
            .addNorth(JLabelBuilder.builder()
                .html("Please describe the problem and the events leading up to it:")
                .border(5)
                .build())
            .addCenter(description)
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .borderEmpty(3)
            .addHorizontalStrut(30)
            .add(errorReportComponents.newSubmitButton(frame, ErrorReportComponents.FormHandler.builder()
                .guiDataHandler(reportHandler)
                .guiReader(guiReader)
                .build()))
            .addHorizontalStrut(20)
            .add(errorReportComponents.newPreviewButton(frame, ErrorReportComponents.FormHandler.builder()
                .guiDataHandler((parentFrame, data) -> new PreviewWindow().build(parentFrame, data).setVisible(true))
                .guiReader(guiReader)
                .build()))
            .addHorizontalStrut(100)
            .add(errorReportComponents.newCancelButton(frame::dispose))
            .addHorizontalStrut(30)
            .build())
        .build());
    return frame;
  }
}
