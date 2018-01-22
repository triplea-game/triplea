package games.strategy.debug;

import java.awt.Dialog;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

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
public enum ErrorMessage {
  INSTANCE;

  private final JFrame windowReference;
  private final JLabel errorMessage;
  private final AtomicBoolean isVisible = new AtomicBoolean(false);

  ErrorMessage() {
    windowReference = new JFrame("TripleA Error");
    windowReference.setAlwaysOnTop(true);
    windowReference.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
    errorMessage = new JLabel("TripleA Error");
    windowReference.add(JPanelBuilder.builder()
        .borderLayout()
        .addCenter(
            errorMessage,
            JPanelBuilder.Padding.of(20))
        .addSouth(JPanelBuilder.builder()
            .flowLayout()
            .horizontalBoxLayout()
            .add(JButtonBuilder.builder()
                .title("Ok")
                .actionListener(() -> windowReference.setVisible(false))
                .selected(true)
                .build())
            .addHorizontalStrut(10)
            .add(JButtonBuilder.builder()
                .title("Show Details")
                .toolTip("Shows the error console window with full error details.")
                .actionListener(() -> {
                  windowReference.setVisible(false);
                  isVisible.set(false);
                  ErrorConsole.showConsole();
                })
                .build())
            .build())
        .build());
  }

  /**
   * Call this to make sure static swing components have been initialized. This will ensure that we initialized
   * the frame before an error occurs. This way when an error event does happen, we only need to pack and make
   * visible the frame.
   */
  public void init() {}


  /**
   * Displays the error dialog window with a given message. This is no-op if the window is already visible.
   */
  public static void show(final String msg) {
    if (!INSTANCE.isVisible.compareAndSet(false,true)) {
      SwingUtilities.invokeLater(() -> {
        INSTANCE.errorMessage.setText(msg);
        INSTANCE.windowReference.pack();
        INSTANCE.windowReference.setLocationRelativeTo(null);
        INSTANCE.windowReference.setVisible(true);
      });
    }
  }
}
