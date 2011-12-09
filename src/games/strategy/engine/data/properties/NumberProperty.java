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

import games.strategy.engine.framework.GameRunner;
import games.strategy.ui.IntTextField;
import games.strategy.ui.IntTextFieldChangeListener;

import java.io.File;

import javax.swing.JComponent;

public class NumberProperty extends AEditableProperty
{
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = 6826763550643504789L;
	private final int m_max;
	private final int m_min;
	private int m_value;
	
	public NumberProperty(final String name, final int max, final int min, final int def)
	{
		super(name);
		if (max < min)
			throw new IllegalThreadStateException("Max must be greater than min");
		if (def > max || def < min)
			throw new IllegalThreadStateException("Default value out of range");
		m_max = max;
		m_min = min;
		m_value = def;
	}
	
	public Integer getValue()
	{
		return m_value;
	}
	
	public void setValue(final Object value) throws ClassCastException
	{
		if (value instanceof String)
		{
			// warn developer which have run with the option cache when Number properties were stored as strings
			// todo (kg) remove at a later point
			throw new RuntimeException("Number properties are no longer stored as Strings. You should delete your option cache, located at "
						+ new File(GameRunner.getUserRootFolder(), "optionCache").toString());
		}
		else
		{
			m_value = (Integer) value;
		}
	}
	
	public JComponent getEditorComponent()
	{
		final IntTextField field = new IntTextField(m_min, m_max);
		field.setValue(m_value);
		field.addChangeListener(new IntTextFieldChangeListener()
		{
			public void changedValue(final IntTextField aField)
			{
				m_value = aField.getValue();
			}
		});
		return field;
	}
}
