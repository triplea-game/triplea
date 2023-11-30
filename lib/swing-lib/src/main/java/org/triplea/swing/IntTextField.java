package org.triplea.swing;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import lombok.Getter;

/**
 * Text field for entering int values. Ensures valid integers are entered, and can limit the range
 * of values user can enter.
 */
public class IntTextField extends JTextField {
  private static final long serialVersionUID = -7993942326354823887L;
  @Getter private int max = Integer.MAX_VALUE;
  @Getter private int min = Integer.MIN_VALUE;
  @Getter private String terr;
  private final List<IntTextFieldChangeListener> listeners = new ArrayList<>();

  public IntTextField() {
    super(3);
    initTextField();
  }

  public IntTextField(final int min, final int max) {
    this();
    setMin(min);
    setMax(max);
  }

  private void initTextField() {
    setDocument(new IntegerDocument());
    setText(String.valueOf(min));
    addFocusListener(new LostFocus());
  }

  public int getValue() {
    return Integer.parseInt(getText());
  }

  private void checkValue() {
    if (getText().trim().equals("-")) {
      setText(String.valueOf(min));
    }
    try {
      Integer.parseInt(getText());
    } catch (final NumberFormatException e) {
      setText(String.valueOf(min));
    }
    if (getValue() > max) {
      setText(String.valueOf(max));
    }
    if (getValue() < min) {
      setText(String.valueOf(min));
    }
  }

  public void setValue(final int value) {
    if (isGood(value)) {
      setText(String.valueOf(value));
    }
  }

  /**
   * Sets max length, number of characters of the text field, must be greater than the configured
   * min.
   */
  public void setMax(final int max) {
    if (max < min) {
      throw new IllegalArgumentException(
          "Max cant be less than min. Current Min: "
              + min
              + ", Current Max: "
              + this.max
              + ", New Max: "
              + max);
    }
    this.max = max;
    if (getValue() > this.max) {
      setText(String.valueOf(max));
    }
  }

  public void setTerr(final String terr) {
    this.terr = terr;
  }

  /** Updates the min length of the text field. Must be set to less than the max length. */
  public void setMin(final int min) {
    if (min > max) {
      throw new IllegalArgumentException(
          "Min cant be greater than max. Current Max: "
              + max
              + ", Current Min: "
              + this.min
              + ", New Min: "
              + min);
    }
    this.min = min;
    if (getValue() < this.min) {
      setText(String.valueOf(min));
    }
  }

  private boolean isGood(final int value) {
    return value <= max && value >= min;
  }

  /** Make sure that no non numeric data is typed. */
  private class IntegerDocument extends PlainDocument {
    private static final long serialVersionUID = 135871239193051281L;

    @Override
    public void insertString(final int offs, final String str, final AttributeSet a)
        throws BadLocationException {
      final String currentText = this.getText(0, getLength());
      final String beforeOffset = currentText.substring(0, offs);
      final String afterOffset = currentText.substring(offs);
      // allow start of negative
      try {
        final String proposedResult = beforeOffset + str + afterOffset;
        Integer.parseInt(proposedResult);
        super.insertString(offs, str, a);
        checkValue();
        notifyListeners();
      } catch (final NumberFormatException e) {
        // if an error dont insert
        // allow start of negative numbers
        if (offs == 0 && min < 0 && str.equals("-")) {
          super.insertString(0, str, a);
        }
      }
    }

    @Override
    public void remove(final int offs, final int len) throws BadLocationException {
      super.remove(offs, len);
      // if its a valid number we've changed
      try {
        Integer.parseInt(IntTextField.this.getText());
        notifyListeners();
      } catch (final NumberFormatException e) {
        // ignore malformed input
      }
    }
  }

  public void addChangeListener(final IntTextFieldChangeListener listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    for (final IntTextFieldChangeListener listener : listeners) {
      listener.changedValue(this);
    }
  }

  private class LostFocus extends FocusAdapter {
    @Override
    public void focusLost(final FocusEvent e) {
      // make sure the value is valid
      checkValue();
    }
  }
}
