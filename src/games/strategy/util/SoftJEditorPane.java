package games.strategy.util;

import java.lang.ref.SoftReference;

import javax.swing.JEditorPane;

/**
 * For when your component contains images or data that is very very big, and you want it to be reclaimed as needed by
 * the GC.
 * Example, when a JEditorPane has rich HTML in it, with huge images.
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
      component = new SoftReference<JEditorPane>(createComponent());
    }
    JEditorPane component = this.component.get();
    if (component == null) {
      component = createComponent();
      this.component = new SoftReference<JEditorPane>(component);
    }
    return component;
  }

  public String getText() {
    return text;
  }

  public void dispose() {
    if (component != null) {
      JEditorPane component = this.component.get();
      if (component != null) {
        component.setText("");
        component.removeAll();
        component = null;
      }
      this.component = null;
    }
  }
}
