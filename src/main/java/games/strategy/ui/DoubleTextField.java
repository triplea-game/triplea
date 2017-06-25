package games.strategy.ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class DoubleTextField extends JTextField {
  private static final long serialVersionUID = -8698753345852617343L;
  private double max = Double.MAX_VALUE;
  private double min = Double.MIN_VALUE;
  private String terr = null;
  private final List<DoubleTextFieldChangeListener> listeners = new ArrayList<>();

  /** Creates new DoubleTextField. */
  public DoubleTextField() {
    super(10);
    initTextField();
  }

  public DoubleTextField(final double min) {
    this();
    setMin(min);
  }

  public DoubleTextField(final double min, final double max) {
    this();
    setMin(min);
    setMax(max);
  }

  private void initTextField() {
    setDocument(new DoubleDocument());
    setText(String.valueOf(min));
    addFocusListener(new LostFocus());
  }

  public double getValue() {
    return Double.parseDouble(getText());
  }

  private void checkValue() {
    if (getText().trim().equals("-")) {
      setText(String.valueOf(min));
    }
    try {
      Double.parseDouble(getText());
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

  public void setValue(final double value) {
    if (isGood(value)) {
      setText(String.valueOf(value));
    }
  }

  private void setMax(final double max) {
    if (max < min) {
      throw new IllegalArgumentException(
          "Max cant be less than min. Current Min: " + min + ", Current Max: " + this.max + ", New Max: " + max);
    }
    this.max = max;
    if (getValue() > this.max) {
      setText(String.valueOf(max));
    }
  }

  public void setTerr(final String terr) {
    this.terr = terr;
  }

  private void setMin(final double min) {
    if (min > max) {
      throw new IllegalArgumentException(
          "Min cant be greater than max. Current Max: " + max + ", Current Min: " + this.min + ", New Min: " + min);
    }
    this.min = min;
    if (getValue() < this.min) {
      setText(String.valueOf(min));
    }
  }

  public double getMax() {
    return max;
  }

  public String getTerr() {
    return terr;
  }

  public double getMin() {
    return min;
  }

  private boolean isGood(final double value) {
    return value <= max && value >= min;
  }

  /**
   * Make sure that no non numeric data is typed.
   */
  private class DoubleDocument extends PlainDocument {
    private static final long serialVersionUID = 64683753745223443L;

    @Override
    public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
      final String currentText = this.getText(0, getLength());
      final String beforeOffset = currentText.substring(0, offs);
      final String afterOffset = currentText.substring(offs, currentText.length());
      final String proposedResult = beforeOffset + str + afterOffset;
      // allow start of negative
      try {
        Double.parseDouble(proposedResult);
        super.insertString(offs, str, a);
        checkValue();
        notifyListeners();
      } catch (final NumberFormatException e) {
        // if an error dont insert
        // allow start of negative numbers
        if (offs == 0) {
          if (min < 0) {
            if (str.equals("-")) {
              super.insertString(offs, str, a);
            }
          }
        }
      }
    }

    @Override
    public void remove(final int offs, final int len) throws BadLocationException {
      super.remove(offs, len);
      // if its a valid number weve changed
      try {
        Double.parseDouble(DoubleTextField.this.getText());
        notifyListeners();
      } catch (final NumberFormatException e) {
        // ignore malformed input
      }
    }
  }

  public void addChangeListener(final DoubleTextFieldChangeListener listener) {
    listeners.add(listener);
  }

  public void removeChangeListener(final DoubleTextFieldChangeListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners() {
    for (final DoubleTextFieldChangeListener listener : listeners) {
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
