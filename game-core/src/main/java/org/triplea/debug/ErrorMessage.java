package org.triplea.debug;

import com.google.common.base.Preconditions;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.triplea.debug.error.reporting.UploadDecisionModule;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Class for showing a modal error dialog to the user. The dialog has an 'ok' button to close it and
 * a 'show details' that will bring up the error console.
 *
 * <p>Note on threading: If we get an error while EDT thread is lock, we will not be able to create
 * a new window. If we do it tries to grab an EDT lock and we get into a deadlock situation. To
 * avoid this we create the error message window early and then show/hide it as needed.
 *
 * <p>Async behavior note: once the window is displayed, further error messages are ignored. The
 * error message is intended to be user friendly, clicking 'show details' would show full details of
 * all error messages.
 */
@SuppressWarnings("ImmutableEnumChecker") // Enum singleton pattern
public enum ErrorMessage {
  INSTANCE;

  private static final String DEFAULT_LOGGER = "";
  private final JFrame windowReference = new JFrame("TripleA Error");
  private final JEditorPaneWithClickableLinks errorMessage = new JEditorPaneWithClickableLinks("");
  private final AtomicBoolean isVisible = new AtomicBoolean(false);
  private volatile boolean enableErrorPopup = false;

  private final JButton uploadButton =
      new JButtonBuilder()
          .title("Report To TripleA")
          .toolTip("Upload error report to TripleA support.")
          .build();

  ErrorMessage() {
    windowReference.setAlwaysOnTop(true);
    windowReference.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
    windowReference.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            hide();
          }
        });
    errorMessage.setBorder(new EmptyBorder(5, 20, 5, 10));
    windowReference.add(
        new JPanelBuilder()
            .border(10)
            .borderLayout()
            .addCenter(
                new JPanelBuilder()
                    .boxLayoutHorizontal()
                    .addHorizontalGlue()
                    .add(errorMessage)
                    .addHorizontalGlue()
                    .build())
            .addSouth(
                new JPanelBuilder()
                    .border(20, 0, 0, 0)
                    .boxLayoutHorizontal()
                    .addHorizontalGlue()
                    .add(
                        new JButtonBuilder()
                            .okTitle()
                            .actionListener(this::hide)
                            .selected(true)
                            .build())
                    .addHorizontalStrut(5)
                    .add(uploadButton)
                    .addHorizontalGlue()
                    .build())
            .build());
  }

  /**
   * Set this to true on non-headless environments to actively notify user of errors via a pop-up
   * message.
   */
  public static void initialize() {
    Preconditions.checkState(
        !GraphicsEnvironment.isHeadless(),
        "Error, must not enable error pop-up in a headless environment, there will "
            + "be errors rendering swing components. Check the call flow to this point and make "
            + "sure we do not enable error reporting unless we are in a non-headless environment");
    INSTANCE.enableErrorPopup = true;
    LogManager.getLogManager().getLogger(DEFAULT_LOGGER).addHandler(new ErrorMessageHandler());
  }

  /**
   * Updates the text of the error message dialog and sets the error message dialog to visible.
   * Note, we hide and reveal the error message dialog instead of creating and disposing it to avoid
   * swing component creation threading issues.
   */
  public static void show(final LogRecord record) {
    if (INSTANCE.enableErrorPopup && INSTANCE.isVisible.compareAndSet(false, true)) {
      INSTANCE.setUploadRecord(record);

      SwingUtilities.invokeLater(
          () -> {
            INSTANCE.errorMessage.setText(new ErrorMessageFormatter().apply(record));
            INSTANCE.windowReference.pack();
            INSTANCE.windowReference.setLocationRelativeTo(null);
            INSTANCE.windowReference.setVisible(true);
          });
    }
  }

  private void setUploadRecord(final LogRecord logRecord) {
    new LiveServersFetcher()
        .lobbyUriForCurrentVersion()
        .ifPresentOrElse(
            uri -> activateUploadErrorReportButton(uri, logRecord),
            // if no internet connection, do not show 'upload button' as it will not work anyways.
            () -> INSTANCE.uploadButton.setVisible(false));
  }

  private void activateUploadErrorReportButton(final URI lobbyUri, final LogRecord logRecord) {
    // Set upload button to be visible only if the logging is greater than a 'warning'.
    // Warning logging should tell a user how to correct the error and it should be something
    // that is 'normal' (but maybe not happy, like no internet) and something they can fix
    // (or perhaps something that we simply can never fix). Hence, for warning we want to inform
    // the user that the problem happened, how to fix it, and let them create a bug report manually.
    INSTANCE.uploadButton.setVisible(logRecord.getLevel().intValue() > Level.WARNING.intValue());

    final ErrorReportClient errorReportClient = ErrorReportClient.newClient(lobbyUri);

    // replace button upload action to use the new log record object
    if (INSTANCE.uploadButton.getActionListeners().length > 0) {
      INSTANCE.uploadButton.removeActionListener(INSTANCE.uploadButton.getActionListeners()[0]);
    }
    INSTANCE.uploadButton.addActionListener(
        e -> {
          hide();
          UploadDecisionModule.processUploadDecision(windowReference, errorReportClient, logRecord);
        });
  }

  private void hide() {
    windowReference.setVisible(false);
    isVisible.set(false);
  }
}
