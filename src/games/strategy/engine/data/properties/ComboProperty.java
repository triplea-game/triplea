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
public class ComboProperty<T> extends AEditableProperty
{
	private static final long serialVersionUID = -3098612299805630587L;
	private final List<T> m_possibleValues;
	private T m_value;
	
	/**
	 * 
	 * @param name
	 *            name of the property
	 * @param defaultValue
	 *            default string value
	 * @param possibleValues
	 *            collection of Strings
	 */
	public ComboProperty(final String name, final String description, final T defaultValue, final Collection<T> possibleValues)
	{
		this(name, description, defaultValue, possibleValues, false);
	}
	
	@SuppressWarnings("unchecked")
	public ComboProperty(final String name, final String description, final T defaultValue, final Collection<T> possibleValues, final boolean allowNone)
	{
		super(name, description);
		if (!allowNone && !possibleValues.contains(defaultValue) && defaultValue == null)
			throw new IllegalStateException("possible values does not contain default");
		else if (allowNone && !possibleValues.contains(defaultValue) && !possibleValues.isEmpty())
			m_value = possibleValues.iterator().next();
		else if (allowNone && !possibleValues.contains(defaultValue))
		{
			try
			{
				m_value = (T) "";
			} catch (final Exception e)
			{
				m_value = null;
			}
		}
		else
			m_value = defaultValue;
		m_possibleValues = new ArrayList<T>(possibleValues);
	}
	
	public Object getValue()
	{
		return m_value;
	}
	
	@SuppressWarnings("unchecked")
	public void setValue(final Object value) throws ClassCastException
	{
		m_value = (T) value;
	}
	
	public void setValueT(final T value)
	{
		m_value = value;
	}
	
	public JComponent getEditorComponent()
	{
		final JComboBox box = new JComboBox(new Vector<T>(m_possibleValues));
		box.setSelectedItem(m_value);
		box.addActionListener(new ActionListener()
		{
			@SuppressWarnings("unchecked")
			public void actionPerformed(final ActionEvent e)
			{
				m_value = (T) box.getSelectedItem();
			}
		});
		return box;
	}
}
