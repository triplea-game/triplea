package games.strategy.engine.data.properties;

import javax.swing.JComponent;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author unascribed
 * @version 1.0
 */
public interface IEditableProperty
{
	/**
	 * get the name of the property
	 * 
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Get the value of the property
	 * 
	 * @return the value
	 */
	public Object getValue();
	
	/**
	 * Set the value of the property (programmatically), GUI would normally use the editor
	 * 
	 * @param value
	 *            the new value
	 * @throws ClassCastException
	 *             if the type of value is wrong
	 */
	public void setValue(Object value) throws ClassCastException;
	
	/**
	 * 
	 * @return component used to edit this property
	 */
	public JComponent getEditorComponent();
	
	/**
	 * Get the view (read only) component for this property
	 * 
	 * @return
	 */
	public JComponent getViewComponent();
	
	/**
	 * Description of what this property is, can be used for tooltip.
	 * 
	 * @return
	 */
	public String getDescription();
	
	public int getRowsNeeded();
}
