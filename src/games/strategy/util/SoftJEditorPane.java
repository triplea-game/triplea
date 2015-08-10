package games.strategy.util;

import java.lang.ref.SoftReference;

import javax.swing.JEditorPane;

/**
 * For when your component contains images or data that is very very big, and you want it to be reclaimed as needed by the GC.
 * Example, when a JEditorPane has rich HTML in it, with huge images.
 *
 *
 */
public class SoftJEditorPane {
  protected SoftReference<JEditorPane> m_component;
  protected final String m_text;

  public SoftJEditorPane(final String text) {
    m_text = text;
  }

  protected JEditorPane createComponent() {
    final JEditorPane pane = new JEditorPane();
    pane.setEditable(false);
    pane.setContentType("text/html");
    pane.setText(m_text);
    pane.setCaretPosition(0);
    return pane;
  }

  public synchronized JEditorPane getComponent() {
    if (m_component == null) {
      m_component = new SoftReference<JEditorPane>(createComponent());
    }
    JEditorPane component = m_component.get();
    if (component == null) {
      component = createComponent();
      m_component = new SoftReference<JEditorPane>(component);
    }
    return component;
  }

  public String getText() {
    return m_text;
  }

  public void dispose() {
    if (m_component != null) {
      JEditorPane component = m_component.get();
      if (component != null) {
        component.setText("");
        component.removeAll();
        component = null;
      }
      m_component = null;
    }
  }

}
