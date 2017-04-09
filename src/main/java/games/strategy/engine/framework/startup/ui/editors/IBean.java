package games.strategy.engine.framework.startup.ui.editors;

import java.io.Serializable;

/**
 * An interface specifying that this component is a bean that can provide an editor
 * Beans must have a default constructor.
 */
public interface IBean extends Serializable {
  /**
   * Get the displayName of the bean.
   *
   * @return the display name
   */
  String getDisplayName();

  /**
   * Returns the editor for this IBean.
   *
   * @return the editor
   */
  EditorPanel getEditor();

  /**
   * Method to check of two beans are of the same type,
   * this is used in the select and view to find an an bean in the list.
   *
   * @param other
   *        the bean to check against
   * @return true if they are the same type
   */
  boolean sameType(IBean other);

  /**
   * Get the help text which the editor will display, the text should be HTML.
   *
   * @return the help text
   */
  String getHelpText();
}
