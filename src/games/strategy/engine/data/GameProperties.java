/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * GameProperties.java
 *
 * Created on January 15, 2002, 2:21 PM
 */

package games.strategy.engine.data;

import java.util.*;
import games.strategy.engine.data.properties.IEditableProperty;

/**
 * Properties of the current game. <br>
 * Maps string -> Object <br>
 * Set through changeFactory.setProperty.
 *
 * @author  Sean Bridges
 */
public class GameProperties extends GameDataComponent
{

    private HashMap m_constantProperties = new HashMap();
    //a set of IEditableProperties
    //dont serialize the editable data
    private HashMap m_editableProperties = new HashMap();

    // This list is used to keep track of order properties were
    // added.
    private List m_ordering = new ArrayList();

    /** Creates a new instance of Properties */
    public GameProperties(GameData data)
    {
	super(data);
    }

    /**
     * Setting a property to null has the effect of
     * unbinding the key.
     */
    void set(String key, Object value)
    {
	if(value == null)
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
     * Could potentially return null. <br>
     * The object returned should not be modified, as modifications
     * will not appear globally.
     */
    public Object get(String key)
    {
	if(m_editableProperties.containsKey(key))
	    return  ((IEditableProperty) m_editableProperties.get(key)).getValue();
	return m_constantProperties.get(key);
    }

    public boolean get(String key, boolean defaultValue)
    {
	Object value = get(key);
	if(value == null)
	    return defaultValue;
	return ((Boolean) value).booleanValue();
    }
  
    public Object get(String key, Object defaultValue)
    {
	Object value = get(key);
	if(value == null)
	    return defaultValue;
	return defaultValue;
    }
  
    public void addEditableProperty(IEditableProperty property)
    {
	//add to the editable properties
	m_editableProperties.put(property.getName(), property);
	m_ordering.add(property.getName());
    }

    /**
     * Return list of editable properties in the order they
     * were added.
     * @return a list of IEditableProperty
     */
    public List getEditableProperties()
    {
	List properties = new ArrayList();
	Iterator orderIter = m_ordering.iterator();
	Set editablePropertyNames = m_editableProperties.keySet();
	while (orderIter.hasNext())
	{
	    Object propertyName = orderIter.next();
	    if (m_editableProperties.containsKey(propertyName))
	    {
		properties.add(m_editableProperties.get(propertyName));
	    }
	}
	return properties;
    }

}
