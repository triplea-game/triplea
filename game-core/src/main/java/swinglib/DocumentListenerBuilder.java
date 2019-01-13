package swinglib;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;


/**
 * Helper to create a 'DocumentListener', which is a way to listen to text events on text fields
 * and text areas. An action listener on text fields only fires on a keypressed 'enter' event and
 * a key listener does not fire when text is copy-pasted into the field. The document listener
 * should reliably trigger when text is changed.
 */
public final class DocumentListenerBuilder {

  /**
   * Attaches a given (add/remove/changed) text change action to a {@code JTextComponent}.
   *
   * @param textComponent Will receive a new document listener
   * @param listenerAction The action to call.
   */
  public static void attachDocumentListener(
      final JTextComponent textComponent, final Runnable listenerAction) {
    textComponent.getDocument().addDocumentListener(
        new DocumentListener() {
          @Override
          public void insertUpdate(final DocumentEvent e) {
            listenerAction.run();
          }

          @Override
          public void removeUpdate(final DocumentEvent e) {
            listenerAction.run();
          }

          @Override
          public void changedUpdate(final DocumentEvent e) {
            listenerAction.run();
          }
        });
  }
}
