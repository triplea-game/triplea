package games.strategy.debug;

import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
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

  private final JFrame windowReference = new JFrame("TripleA Error");
  private final JLabel errorMessage = new JLabel();
  private final AtomicBoolean isVisible = new AtomicBoolean(false);

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
        .borderEmpty(10)
        .addCenter(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .addHorizontalGlue()
            .add(JLabelBuilder.builder().errorIcon().build())
            .addHorizontalStrut(10)
            .add(errorMessage)
            .addHorizontalGlue()
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .borderEmpty(20, 0, 0, 0)
            .addHorizontalGlue()
            .add(JButtonBuilder.builder()
                .okTitle()
                .actionListener(this::hide)
                .selected(true)
                .build())
            .addHorizontalStrut(5)
            .add(JButtonBuilder.builder()
                .title("Show Details")
                .toolTip("Shows the error console window with full error details.")
                .actionListener(() -> {
                  hide();
                  ErrorConsole.showConsole();
                })
                .build())
            .addHorizontalGlue()
            .build())
        .build());
  }

  private void hide() {
    windowReference.setVisible(false);
    isVisible.set(false);
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
    if (INSTANCE.isVisible.compareAndSet(false, true)) {
      SwingUtilities.invokeLater(() -> {
        INSTANCE.errorMessage.setText(msg);
        INSTANCE.windowReference.pack();
        INSTANCE.windowReference.setLocationRelativeTo(null);
        INSTANCE.windowReference.setVisible(true);
      });
    }
  }
}
