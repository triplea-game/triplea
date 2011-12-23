package games.strategy.engine.framework.startup.ui.editors;

import java.io.Serializable;

/**
 * An interface specifying that this component is a bean that can provide an editor
 * Beans must have a default constructor
 * @author Klaus Groenbaek
 */
public interface IBean extends Serializable
{
	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------
	public String getDisplayName();

	/**
	 * Returns the editor for this IBean
	 * @return the editor
	 */
	public EditorPanel getEditor();

	/**
	 * Method to check of two beans are of the same type,
	 * this is used in the select and view to find an an bean in the list
	 * @param other the bean to check against
	 * @return true if they are the same type
	 */
	public boolean sameType(IBean other);

}
