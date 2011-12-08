/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.data.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * A String property that uses a list for selecting the value
 */
public class ListProperty extends AEditableProperty
{
	private final List<String> m_possibleValues;
	private String m_value;
	
	/**
	 * 
	 * @param name
	 *            name of the property
	 * @param defaultValue
	 *            default string value
	 * @param possibleValues
	 *            collection of Strings
	 */
	public ListProperty(final String name, final String defaultValue, final Collection<String> possibleValues)
	{
		super(name);
		if (!possibleValues.contains(defaultValue))
			throw new IllegalStateException("possible values does not contain default");
		m_possibleValues = new ArrayList<String>(possibleValues);
		m_value = defaultValue;
	}
	
	public Object getValue()
	{
		return m_value;
	}

	public void setValue(Object value) throws ClassCastException
	{
		m_value = (String) value;
	}

	public JComponent getEditorComponent()
	{
		final JComboBox box = new JComboBox(new Vector<String>(m_possibleValues));
		box.setSelectedItem(m_value);
		box.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_value = (String) box.getSelectedItem();
			}
		});
		return box;
	}
}
