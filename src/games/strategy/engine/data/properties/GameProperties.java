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
/*
 * GameProperties.java
 * 
 * Created on January 15, 2002, 2:21 PM
 */
package games.strategy.engine.data.properties;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 * 
 * @author Sean Bridges
 */
public class GameProperties extends GameDataComponent
{
	private final Map<String, Object> m_constantProperties = new HashMap<String, Object>();
	// a set of IEditableProperties
	private final Map<String, IEditableProperty> m_editableProperties = new HashMap<String, IEditableProperty>();
	// This list is used to keep track of order properties were
	// added.
	private final List<String> m_ordering = new ArrayList<String>();
	
	/**
	 * Creates a new instance of Properties
	 * 
	 * @param data
	 *            game data
	 */
	public GameProperties(final GameData data)
	{
		super(data);
	}
	
	/**
	 * Setting a property to null has the effect of unbinding the key.
	 * package access to prevent outsiders from setting properties
	 * 
	 * @param key
	 *            key of property
	 * @param value
	 *            property
	 */
	// todo should only accept serializable, not object
	public void set(final String key, final Object value)
	{
		if (value == null)
		{
			m_constantProperties.remove(key);
			m_ordering.remove(key);
		}
		else
		{
			m_constantProperties.put(key, value);
			m_ordering.add(key);
		}
	}
	
	/**
	 * 
	 * @param key
	 *            referring key
	 * @return property with key or null if property is not contained in the list
	 *         (The object returned should not be modified, as modifications will not appear globally.)
	 */
	public Object get(final String key)
	{
		if (m_editableProperties.containsKey(key))
			return m_editableProperties.get(key).getValue();
		return m_constantProperties.get(key);
	}
	
	public boolean get(final String key, final boolean defaultValue)
	{
		final Object value = get(key);
		if (value == null)
			return defaultValue;
		return (Boolean) value;
	}
	
	public int get(final String key, final int defaultValue)
	{
		final Object value = get(key);
		if (value == null)
			return defaultValue;
		return (Integer) value;
	}
	
	public String get(final String key, final String defaultValue)
	{
		final Object value = get(key);
		if (value == null)
			return defaultValue;
		return (String) value;
	}
	
	public Object get(final String key, final Object defaultValue)
	{
		final Object value = get(key);
		if (value == null)
			return defaultValue;
		return value;
	}
	
	public void addEditableProperty(final IEditableProperty property)
	{
		// add to the editable properties
		m_editableProperties.put(property.getName(), property);
		m_ordering.add(property.getName());
	}
	
	/**
	 * Return list of editable properties in the order they were added.
	 * 
	 * @return a list of IEditableProperty
	 */
	public List<IEditableProperty> getEditableProperties()
	{
		final List<IEditableProperty> properties = new ArrayList<IEditableProperty>();
		for (String propertyName  : m_ordering) {
			if (m_editableProperties.containsKey(propertyName))
			{
				properties.add(m_editableProperties.get(propertyName));
			}
		}
		return properties;
	}
}
