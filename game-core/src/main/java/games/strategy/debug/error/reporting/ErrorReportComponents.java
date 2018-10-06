package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JTextArea;

import com.google.common.annotations.VisibleForTesting;

import lombok.Builder;
import swinglib.ConfirmationDialogBuilder;
import swinglib.JButtonBuilder;
import swinglib.JTextAreaBuilder;


class ErrorReportComponents {

  @VisibleForTesting
  enum Names {
    ERROR_DESCRIPTION, ADDITIONAL_INFO_NAME, UPLOAD_BUTTON, PREVIEW_BUTTON
  }

  @Builder
  static class FormHandler {
    private final Consumer<UserErrorReport> guiDataHandler;
    private final Supplier<UserErrorReport> guiReader;
  }

  JTextArea descriptionArea() {
    return JTextAreaBuilder.builder()
        .componentName(Names.ERROR_DESCRIPTION.toString())
        .columns(10)
        .rows(2)
        .build();
  }

  JTextArea additionalInfo() {
    return JTextAreaBuilder.builder()
        .componentName(Names.ADDITIONAL_INFO_NAME.toString())
        .columns(10)
        .rows(2)
        .build();
  }


  JButton createSubmitButton(final FormHandler config) {
    return JButtonBuilder.builder()
        .title("Upload")
        .toolTip("Upload error report to TripleA server")
        .componentName(Names.UPLOAD_BUTTON.toString())
        .actionListener(button -> createSendConfirmationDialog(
            button,
            () -> config.guiDataHandler.accept(config.guiReader.get())))
        .biggerFont()
        .build();
  }

  private Runnable createSendConfirmationDialog(
      final Component parent,
      final Runnable confirmAction) {

    return ConfirmationDialogBuilder.builder()
        .title("Confirm Upload")
        .message("Please confirm you are ready to begin uploading the error report.")
        .confirmAction(confirmAction)
        .parent(parent)
        .build();
  }

  JButton createCancelButton(final Runnable closeAction) {
    return JButtonBuilder.builder()
        .title("Cancel")
        .actionListener(closeAction)
        .build();
  }

  JButton createPreviewButton(final FormHandler config) {
    return JButtonBuilder.builder()
        .title("Preview")
        .toolTip("Preview the full error report that will be uploaded")
        .actionListener(() -> config.guiDataHandler.accept(config.guiReader.get()))
        .componentName(Names.PREVIEW_BUTTON.toString())
        .build();
  }
}
