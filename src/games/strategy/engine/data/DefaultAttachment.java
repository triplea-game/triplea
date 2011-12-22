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
 * Attatchment.java
 * 
 * Created on November 8, 2001, 3:09 PM
 */
package games.strategy.engine.data;

import java.lang.reflect.Field;

/**
 * Contains some utility methods that subclasses can use to make writing attatchments easier
 * 
 * @author Sean Bridges
 */
public class DefaultAttachment implements IAttachment
{
	private GameData m_data;
	private Attachable m_attatchedTo;
	private String m_name;
	
	/**
	 * Throws an error if format is invalid.
	 */
	protected static int getInt(final String aString)
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(aString);
		} catch (final NumberFormatException nfe)
		{
			throw new IllegalArgumentException(aString + " is not a valid int value");
		}
		return val;
	}
	
	/**
	 * Throws an error if format is invalid. Must be either true or false ignoring case.
	 */
	protected static boolean getBool(final String aString)
	{
		if (aString.equalsIgnoreCase("true"))
			return true;
		else if (aString.equalsIgnoreCase("false"))
			return false;
		else
			throw new IllegalArgumentException(aString + " is not a valid boolean");
	}
	
	private Field getFieldIncludingFromSuperClasses(@SuppressWarnings("rawtypes") final Class c, final String name, final boolean justFromSuper)
	{
		Field rVal = null;
		try
		{
			if (!justFromSuper)
			{
				rVal = c.getDeclaredField(name);
				return rVal;
			}
		} catch (final Exception e)
		{
			// do nothing, go to finally
		} finally
		{
			try
			{
				rVal = c.getSuperclass().getDeclaredField(name);
			} catch (final Exception e2)
			{
				if (c.getSuperclass() == null)
					throw new IllegalStateException("No such Property: " + name);
				rVal = getFieldIncludingFromSuperClasses(c.getSuperclass(), name, true);
			}
		}
		return rVal;
	}
	
	public String getRawProperty(final String property)
	{
		String s = "";
		Field field = null;
		try
		{
			field = getClass().getDeclaredField("m_" + property);
		} catch (final Exception e)
		{
			try
			{
				field = getFieldIncludingFromSuperClasses(getClass(), "m_" + property, true);
			} catch (final Exception e2)
			{
				throw new IllegalStateException("No such Property: " + property);
			}
		}
		try
		{
			field.setAccessible(true);
			s += field.get(this);
		} catch (final Exception e)
		{
			throw new IllegalStateException("No such Property: " + property);
		}
		return s;
	}
	
	public void setData(final GameData data)
	{
		m_data = data;
	}
	
	protected GameData getData()
	{
		return m_data;
	}
	
	/**
	 * Called after the attatchment is created.
	 */
	public void validate(final GameData data) throws GameParseException
	{
	}
	
	public Attachable getAttatchedTo()
	{
		return m_attatchedTo;
	}
	
	public void setAttatchedTo(final Attachable attatchable)
	{
		m_attatchedTo = attatchable;
	}
	
	/** Creates new Attatchment */
	public DefaultAttachment()
	{
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public void setName(final String aString)
	{
		m_name = aString;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " attched to:" + m_attatchedTo + " with name:" + m_name;
	}
}
