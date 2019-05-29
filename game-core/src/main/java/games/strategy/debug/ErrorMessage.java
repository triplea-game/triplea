package games.strategy.debug;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.SwingComponents;

import com.google.common.base.Preconditions;

import games.strategy.debug.error.reporting.StackTraceReportView;
import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;

/**
 * Class for showing a modal error dialog to the user. The dialog has an 'ok' button to close it and a 'show details'
 * that will bring up the error console.
 * <p>
 * Note on threading: If we get an error while EDT thread is lock, we will not be able to create a new window.
 * If we do it tries to grab an EDT lock and we get into a deadlock situation. To avoid this we create the error
 * message window early and then show/hide it as needed.
 * </p>
 * <p>
 * Async behavior note: once the window is displayed, further error messages are ignored. The error message is intended
 * to be user friendly, clicking 'show details' would show full details of all error messages.
 * </p>
 */
@SuppressWarnings("ImmutableEnumChecker") // Enum singleton pattern
public enum ErrorMessage {
  INSTANCE;

  private static final String DEFAULT_LOGGER = "";
  private final JFrame windowReference = new JFrame("TripleA Error");
  private final JLabel errorMessage = JLabelBuilder.builder().errorIcon().iconTextGap(10).build();
  private final AtomicBoolean isVisible = new AtomicBoolean(false);
  private volatile boolean enableErrorPopup = false;

  private final JButton uploadButton = JButtonBuilder.builder()
      .title("Report To TripleA")
      .toolTip("Upload error report to TripleA support.")
      .build();

  ErrorMessage() {
    windowReference.setAlwaysOnTop(true);
    windowReference.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
    windowReference.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        hide();
      }
    });
    windowReference.add(JPanelBuilder.builder()
        .borderLayout()
        .border(10)
        .addCenter(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .addHorizontalGlue()
            .add(errorMessage)
            .addHorizontalGlue()
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .border(20, 0, 0, 0)
            .addHorizontalGlue()
            .add(JButtonBuilder.builder()
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
   * Set this to true on non-headless environments to actively notify user of errors via a pop-up message.
   */
  public static void initialize() {
    Preconditions.checkState(
        !GraphicsEnvironment.isHeadless(),
        "Error, must not enable error pop-up in a headless environment, there will be errors rendering "
            + "swing components. Check the call flow to this point and make sure we do not enable error reporting "
            + "unless we are in a non-headless environment");
    INSTANCE.enableErrorPopup = true;
    LogManager.getLogManager().getLogger(DEFAULT_LOGGER).addHandler(new ErrorMessageHandler());
  }

  public static void show(final LogRecord record) {
    if (INSTANCE.enableErrorPopup && INSTANCE.isVisible.compareAndSet(false, true)) {
      INSTANCE.setUploadRecord(record);

      SwingUtilities.invokeLater(() -> {
        INSTANCE.errorMessage.setText(new ErrorMessageFormatter().apply(record));
        INSTANCE.windowReference.pack();
        INSTANCE.windowReference.setLocationRelativeTo(null);
        INSTANCE.windowReference.setVisible(true);
      });
    }
  }

  private void setUploadRecord(final LogRecord record) {

    final ErrorUploadClient serviceClient = serviceClient();

    if (serviceClient == null) {
      // if no internet connection, do not show 'upload button' as it will not work anyways.
      INSTANCE.uploadButton.setVisible(false);
    } else {
      INSTANCE.uploadButton.setVisible(true);

      // replace button upload action to use the new log record object
      if (INSTANCE.uploadButton.getActionListeners().length > 0) {
        INSTANCE.uploadButton.removeActionListener(INSTANCE.uploadButton.getActionListeners()[0]);
      }
      INSTANCE.uploadButton.addActionListener(e -> {
        hide();
        if (serviceClient.canSubmitErrorReport()) {
          StackTraceReportView.showWindow(windowReference, serviceClient, record);
        } else {
          SwingComponents.showDialog(
              "Error Report Limit Reached",
              "You have reached a daily limit for error uploads. "
                  + "Please wait a day before submitting additional error reports.");
        }
      });
    }
  }

  private static ErrorUploadClient serviceClient() {
    return LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
        .fetchLobbyServerProperties()
        .map(LobbyServerProperties::getHttpsServerUri)
        .map(ErrorUploadClient::newClient)
        .orElse(null);
  }

  private void hide() {
    windowReference.setVisible(false);
    isVisible.set(false);
  }
}
