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

import javax.swing.JComponent;

public abstract class AEditableProperty implements IEditableProperty, java.io.Serializable, Comparable<Object>
{
	private static final long serialVersionUID = -5005729898242568847L;
	private final String m_name;
	private final String m_description;
	
	public AEditableProperty(final String name, final String description)
	{
		m_name = name;
		m_description = description;
	}
	
	public int getRowsNeeded()
	{
		return 1;
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getDescription()
	{
		return m_description;
	}
	
	public JComponent getViewComponent()
	{
		final JComponent rVal = getEditorComponent();
		rVal.setEnabled(false);
		return rVal;
	}
	
	@Override
	public int hashCode()
	{
		return m_name.hashCode();
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (other instanceof AEditableProperty)
		{
			return ((AEditableProperty) other).m_name.equals(this.m_name);
		}
		return false;
	}
	
	public int compareTo(final Object other)
	{
		if (other instanceof AEditableProperty)
		{
			return this.m_name.compareTo(((AEditableProperty) other).getName());
		}
		return -1;
	}
	
	@Override
	public String toString()
	{
		return getName() + "=" + getValue().toString();
	}
}
