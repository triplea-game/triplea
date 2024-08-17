package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.Normalizer;
import java.util.Locale;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * From http://www.orbital-computer.de/JComboBox. Originally released into the Public Domain
 * (http://creativecommons.org/licenses/publicdomain/).
 *
 * @param <E> The type of the combo box elements.
 */
final class AutoCompletion<E> extends PlainDocument {
  private static final long serialVersionUID = 3467837392266952714L;

  private final JComboBox<E> comboBox;
  private ComboBoxModel<E> model;
  private JTextComponent editor;
  // flag to indicate if setSelectedItem has been called
  // subsequent calls to remove/insertString should be ignored
  private boolean selecting = false;
  private boolean hitBackspace = false;
  private boolean hitBackspaceOnSelection;
  private final KeyListener editorKeyListener;
  private final FocusListener editorFocusListener;

  @SuppressWarnings("unchecked")
  private AutoCompletion(final JComboBox<E> comboBox) {
    this.comboBox = comboBox;
    model = comboBox.getModel();
    comboBox.addActionListener(
        e -> {
          if (!selecting) {
            highlightCompletedText(0);
          }
        });
    comboBox.addPropertyChangeListener(
        e -> {
          if (e.getPropertyName().equals("editor")) {
            configureEditor((ComboBoxEditor) e.getNewValue());
          }
          if (e.getPropertyName().equals("model")) {
            model = (ComboBoxModel<E>) e.getNewValue();
          }
        });
    editorKeyListener =
        new KeyAdapter() {
          @Override
          public void keyPressed(final KeyEvent e) {
            if ((e.getKeyCode() != KeyEvent.VK_ENTER)
                && (e.getKeyCode() != KeyEvent.VK_ESCAPE)
                && comboBox.isDisplayable()) {
              comboBox.setPopupVisible(true);
            }
            hitBackspace = false;
            switch (e.getKeyCode()) {
              case KeyEvent.VK_BACK_SPACE:
                hitBackspace = true;
                hitBackspaceOnSelection = editor.getSelectionStart() != editor.getSelectionEnd();
                break;
              case KeyEvent.VK_DELETE:
                e.consume();
                comboBox.getToolkit().beep();
                break;
              default:
                break;
            }
          }
        };
    // Highlight whole text when gaining focus
    editorFocusListener =
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            highlightCompletedText(0);
          }

          @Override
          public void focusLost(final FocusEvent e) {}
        };
    configureEditor(comboBox.getEditor());
    // Handle initially selected object
    final Object selected = comboBox.getSelectedItem();
    if (selected != null) {
      setText(selected.toString());
    }
    highlightCompletedText(0);
  }

  static <E> void enable(final JComboBox<E> comboBox) {
    // has to be editable
    comboBox.setEditable(true);
    // change the editor's document
    @SuppressWarnings("unused")
    final AutoCompletion<E> autoCompletion = new AutoCompletion<>(comboBox);
  }

  void configureEditor(final ComboBoxEditor newEditor) {
    if (editor != null) {
      editor.removeKeyListener(editorKeyListener);
      editor.removeFocusListener(editorFocusListener);
    }

    if (newEditor != null) {
      editor = (JTextComponent) newEditor.getEditorComponent();
      editor.addKeyListener(editorKeyListener);
      editor.addFocusListener(editorFocusListener);
      editor.setDocument(this);
    }
  }

  @Override
  public void remove(final int initialOffs, final int len) throws BadLocationException {
    int offs = initialOffs;
    // return immediately when selecting an item
    if (selecting) {
      return;
    }
    if (hitBackspace) {
      // user hit backspace => move the selection backwards
      // old item keeps being selected
      if (offs > 0) {
        if (hitBackspaceOnSelection) {
          offs--;
        }
      } else {
        // User hit backspace with the cursor positioned on the start => beep
        comboBox.getToolkit().beep(); // when available use:
        // UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
      }
      highlightCompletedText(offs);
    } else {
      super.remove(offs, len);
    }
  }

  @Override
  public void insertString(final int initialOffs, final String str, final AttributeSet a)
      throws BadLocationException {
    int offs = initialOffs;
    // return immediately when selecting an item
    if (selecting) {
      return;
    }
    // insert the string into the document
    super.insertString(offs, str, a);
    // lookup and select a matching item
    Object item = lookupItem(getText(0, getLength()));
    if (item != null) {
      setSelectedItem(item);
    } else {
      // keep old item selected if there is no match
      item = comboBox.getSelectedItem();
      // imitate no insert (later on offs will be incremented by str.length(): selection won't move
      // forward)
      offs = offs - str.length();
      // provide feedback to the user that his input has been received but can not be accepted
      UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
    }
    setText(item.toString());
    // select the completed part
    highlightCompletedText(offs + str.length());
  }

  private void setText(final String text) {
    try {
      // remove all text and insert the completed string
      super.remove(0, getLength());
      super.insertString(0, text, null);
    } catch (final BadLocationException e) {
      throw new RuntimeException(e.toString());
    }
  }

  private void highlightCompletedText(final int start) {
    editor.setCaretPosition(getLength());
    editor.moveCaretPosition(start);
  }

  private void setSelectedItem(final Object item) {
    selecting = true;
    model.setSelectedItem(item);
    selecting = false;
  }

  private Object lookupItem(final String pattern) {
    final Object selectedItem = model.getSelectedItem();
    // only search for a different item if the currently selected does not match
    if (selectedItem != null && startsWith(selectedItem.toString(), pattern)) {
      return selectedItem;
    }
    // iterate over all items
    for (int i = 0, n = model.getSize(); i < n; i++) {
      final Object currentItem = model.getElementAt(i);
      // current item starts with the pattern?
      if (currentItem != null && startsWith(currentItem.toString(), pattern)) {
        return currentItem;
      }
    }
    // no item starts with the pattern => return null
    return null;
  }

  @VisibleForTesting
  static boolean startsWith(final String str1, final String str2) {
    return normalize(str1).startsWith(normalize(str2));
  }

  private static String normalize(final String str) {
    return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
  }
}
