package games.strategy.engine.data.properties;

import javax.swing.JComponent;

/**
 * An editable property.
 */
public interface IEditableProperty {
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
  Object getValue();

  /**
   * @param value
   * @return is the object a valid object for setting as our value.
   */
  boolean validate(Object value);

  /**
   * Set the value of the property (programmatically), GUI would normally use the editor.
   *
   * @param value
   *        the new value
   * @throws ClassCastException
   *         if the type of value is wrong
   */
  void setValue(Object value) throws ClassCastException;

  /**
   * @return component used to edit this property.
   */
  JComponent getEditorComponent();

  /**
   * Get the view (read only) component for this property.
   */
  JComponent getViewComponent();

  /**
   * Description of what this property is, can be used for tooltip.
   */
  String getDescription();

  int getRowsNeeded();
}
