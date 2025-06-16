package games.strategy.engine.data.properties;

import java.io.Serializable;
import javax.swing.JComponent;

/**
 * An editable property.
 *
 * @param <T> The generic Type of the value being stored.
 */
public interface IEditableProperty<T> extends Serializable {
  /**
   * get the name of the property.
   *
   * @return the name
   */
  String getName();

  /**
   * Get the value of the property.
   *
   * @return the value
   */
  T getValue();

  /** Indicates the object is a valid object for setting as our value. */
  boolean validate(Object value);

  /**
   * Set the value of the property (programmatically), GUI would normally use the editor.
   *
   * @param value the new value
   */
  void setValue(T value);

  /** Returns the component used to edit this property. */
  JComponent getEditorComponent();

  /** Get the view (read only) component for this property. */
  JComponent getViewComponent();

  /** Description of what this property is, can be used for tooltip. */
  String getDescription();

  int getRowsNeeded();

  @SuppressWarnings("unchecked")
  default boolean setValueIfValid(final Object object) {
    if (validate(object)) {
      setValue((T) object);
      return true;
    }
    return false;
  }

  default void validateAndSet(final Object object) {
    if (!setValueIfValid(object)) {
      throw new IllegalArgumentException(
          "Invalid value " + object + " for class " + getClass().getCanonicalName());
    }
  }
}
