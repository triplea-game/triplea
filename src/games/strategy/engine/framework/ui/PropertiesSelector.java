/**
 * Created on 12.03.2012
 */
package games.strategy.engine.framework.ui;

import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * Wrapper for properties selection window.
 * 
 * @author Frigoref
 * 
 */
public class PropertiesSelector
{
	
	/**
	 * 
	 * @param parent
	 *            parent component
	 * @param properties
	 *            properties that will get displayed
	 * @param buttonOptions
	 *            button options. They will be displayed in a row on the bottom
	 * @return pressed button
	 */
	static public Object getButton(final JComponent parent, final ArrayList<IEditableProperty> properties, final Object... buttonOptions)
	{
		final PropertiesUI panel = new PropertiesUI(properties, true);
		final JScrollPane scroll = new JScrollPane(panel);
		scroll.setBorder(null);
		scroll.getViewport().setBorder(null);
		
		final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
		pane.setOptions(buttonOptions);
		final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(parent), "Sound Options");
		window.setVisible(true);
		
		return pane.getValue();
	}
	
}
