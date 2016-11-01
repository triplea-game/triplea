package games.strategy.ui;

import games.strategy.util.ListenerList;
import javafx.scene.control.TextField;

/**
 * Text field for entering int values.
 * Ensures valid integers are entered, and can limit the range of
 * values user can enter.
 */
public class IntTextField extends TextField {
  private int m_max = Integer.MAX_VALUE;
  private int m_min = Integer.MIN_VALUE;
  private String m_terr = null;
  private final ListenerList<IntTextFieldChangeListener> m_listeners = new ListenerList<>();

  /** Creates new IntTextBox */
  public IntTextField() {
    setPrefColumnCount(3);
    initTextField();
  }

  public IntTextField(final int min) {
    this();
    setMin(min);
  }

  public IntTextField(final int min, final int max) {
    this();
    setMin(min);
    setMax(max);
  }

  public IntTextField(final int min, final int max, final int current) {
    this();
    setMin(min);
    setMax(max);
    setValue(current);
  }

  public IntTextField(final int min, final int max, final int current, final int columns) {
    setPrefColumnCount(columns);
    initTextField();
    setMin(min);
    setMax(max);
    setValue(current);
  }

  private void initTextField() {
    setText(String.valueOf(m_min));
    focusedProperty().addListener((c, o, n) -> {
      if (!isFocused()) {
        // make sure the value is valid
        checkValue();
      }
    });
  }

  public int getValue() {
    return Integer.parseInt(getText());
  }

  private void checkValue() {
    if (getText().trim().equals("-")) {
      setText(String.valueOf(m_min));
    }
    try {
      Integer.parseInt(getText());
    } catch (final NumberFormatException e) {
      setText(String.valueOf(m_min));
    }
    if (getValue() > m_max) {
      setText(String.valueOf(m_max));
    }
    if (getValue() < m_min) {
      setText(String.valueOf(m_min));
    }
  }

  public void setValue(final int value) {
    if (isGood(value)) {
      setText(String.valueOf(value));
    }
  }

  public void setMax(final int max) {
    if (max < m_min) {
      throw new IllegalArgumentException(
          "Max cant be less than min. Current Min: " + m_min + ", Current Max: " + m_max + ", New Max: " + max);
    }
    m_max = max;
    if (getValue() > m_max) {
      setText(String.valueOf(max));
    }
  }

  public void setTerr(final String terr) {
    m_terr = terr;
  }

  public void setMin(final int min) {
    if (min > m_max) {
      throw new IllegalArgumentException(
          "Min cant be greater than max. Current Max: " + m_max + ", Current Min: " + m_min + ", New Min: " + min);
    }
    m_min = min;
    if (getValue() < m_min) {
      setText(String.valueOf(min));
    }
  }

  public int getMax() {
    return m_max;
  }

  public String getTerr() {
    return m_terr;
  }

  public int getMin() {
    return m_min;
  }

  private boolean isGood(final int value) {
    return value <= m_max && value >= m_min;
  }

  public void addChangeListener(final IntTextFieldChangeListener listener) {
    m_listeners.add(listener);
  }

  public void removeChangeListener(final IntTextFieldChangeListener listener) {
    m_listeners.remove(listener);
  }
}
