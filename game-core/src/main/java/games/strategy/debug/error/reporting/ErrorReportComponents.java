package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.google.common.annotations.VisibleForTesting;

import lombok.Builder;
import swinglib.DialogBuilder;
import swinglib.JButtonBuilder;
import swinglib.JTextAreaBuilder;
import swinglib.JTextFieldBuilder;


class ErrorReportComponents {

  @VisibleForTesting
  enum Names {
    DESCRIPTION, TITLE, UPLOAD_BUTTON, PREVIEW_BUTTON
  }

  @Builder
  static class FormHandler {
    private final BiConsumer<JFrame, UserErrorReport> guiDataHandler;
    private final Supplier<UserErrorReport> guiReader;
  }

  JTextArea descriptionArea() {
    return JTextAreaBuilder.builder()
        .componentName(Names.DESCRIPTION.toString())
        .columns(10)
        .rows(2)
        .build();
  }

  JTextField titleField() {
    return JTextFieldBuilder.builder()
        .componentName(Names.TITLE.toString())
        .columns(10)
        .maxLength(40)
        .build();
  }


  JButton createSubmitButton(final JFrame frame, final FormHandler config) {
    return JButtonBuilder.builder()
        .title("Upload")
        .toolTip("Upload error report to TripleA server")
        .componentName(Names.UPLOAD_BUTTON.toString())
        .actionListener(button -> createSendConfirmationDialog(
            button,
            () -> config.guiDataHandler.accept(frame, config.guiReader.get())))
        .biggerFont()
        .build();
  }

  private static Runnable createSendConfirmationDialog(
      final Component parent,
      final Runnable confirmAction) {

    return () -> DialogBuilder.builder()
        .parent(parent)
        .title("Confirm Upload")
        .confirmationQuestion("Please confirm you are ready to begin uploading the error report.")
        .confirmAction(confirmAction)
        .showDialog();
  }

  JButton createCancelButton(final Runnable closeAction) {
    return JButtonBuilder.builder()
        .title("Cancel")
        .actionListener(closeAction)
        .build();
  }

  JButton createPreviewButton(final JFrame frame, final FormHandler config) {
    return JButtonBuilder.builder()
        .title("Preview")
        .toolTip("Preview the full error report that will be uploaded")
        .actionListener(() -> config.guiDataHandler.accept(frame, config.guiReader.get()))
        .componentName(Names.PREVIEW_BUTTON.toString())
        .build();
  }
}
