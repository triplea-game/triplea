package games.strategy.util;

import java.lang.ref.SoftReference;

import javax.swing.JEditorPane;

/**
 * For when your component contains images or data that is very very big, and you want it to be reclaimed as needed by
 * the GC.
 * Example, when a JEditorPane has rich HTML in it, with huge images.
 * @deprecated Class uses a very complicated SoftReference, it's buggy and very complex, avoid using this class, or
 * work to remove the soft reference.
 */
public class SoftJEditorPane {
  protected SoftReference<JEditorPane> component;
  protected final String text;

  public SoftJEditorPane(final String text) {
    this.text = text;
  }

  protected JEditorPane createComponent() {
    final JEditorPane pane = new JEditorPane();
    pane.setEditable(false);
    pane.setContentType("text/html");
    pane.setText(text);
    pane.setCaretPosition(0);
    return pane;
  }

  public synchronized JEditorPane getComponent() {
    if (component == null) {
      component = new SoftReference<>(createComponent());
    }

    JEditorPane editorPane = this.component.get();
    if (editorPane == null) {
      editorPane = createComponent();
      this.component = new SoftReference<>(editorPane);
    }
    return editorPane;
  }

  public String getText() {
    return text;
  }

  public void dispose() {
    if (component != null) {
      JEditorPane editorComponent = this.component.get();
      if (editorComponent != null) {
        editorComponent.setText("");
        editorComponent.removeAll();
        editorComponent = null;
      }
      this.component = null;
    }
  }
}
