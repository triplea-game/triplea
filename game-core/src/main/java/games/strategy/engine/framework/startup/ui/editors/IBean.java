package games.strategy.engine.framework.startup.ui.editors;

import java.io.Serializable;

import javax.annotation.Nullable;

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
   * Get the help text which the editor will display, the text should be HTML.
   *
   * @return the help text
   */
  String getHelpText();

  /**
   * Returns {@code true} if this bean is the same type as {@code other}. Note that this doesn't necessarily mean type
   * in the sense of a Java class, but it may. This method is used to select a bean within a list of beans in the UI.
   */
  boolean isSameType(@Nullable IBean other);
}
