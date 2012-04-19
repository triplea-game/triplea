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
 * Attachment.java
 * 
 * Created on November 8, 2001, 3:09 PM
 */
package games.strategy.engine.data;

import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.util.PropertyUtil;

/**
 * Contains some utility methods that subclasses can use to make writing attachments easier
 * 
 * @author Sean Bridges
 */
public abstract class DefaultAttachment implements IAttachment
{
	private static final long serialVersionUID = -1985116207387301730L;
	@InternalDoNotExport
	private GameData m_data;
	@InternalDoNotExport
	private Attachable m_attachedTo;
	@InternalDoNotExport
	private String m_name;
	
	protected DefaultAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		setName(name);
		setData(gameData);
		setAttachedTo(attachable);
	}
	
	/**
	 * Called after ALL attachments are created.
	 */
	public abstract void validate(final GameData data) throws GameParseException;
	
	/**
	 * Throws an error if format is invalid.
	 */
	protected static int getInt(final String aString)
	{
		try
		{
			return Integer.parseInt(aString);
		} catch (final NumberFormatException nfe)
		{
			throw new IllegalArgumentException("Attachments: " + aString + " is not a valid int value");
		}
	}
	
	/*protected static char getChar(final String aString)
	{
		if (aString.equalsIgnoreCase(Constants.PROPERTY_DEFAULT))
			return Constants.VALUE_DEFAULT;
		else if (aString.equalsIgnoreCase(Constants.PROPERTY_TRUE))
			return Constants.VALUE_TRUE;
		else if (aString.equalsIgnoreCase(Constants.PROPERTY_FALSE))
			return Constants.VALUE_FALSE;
		else
			throw new IllegalArgumentException("Attachments: " + aString + " should equal "
						+ Constants.PROPERTY_DEFAULT + " or " + Constants.PROPERTY_TRUE + " or " + Constants.PROPERTY_FALSE);
	}*/

	/**
	 * Throws an error if format is invalid. Must be either true or false ignoring case.
	 */
	protected static boolean getBool(final String value)
	{
		if (value.equalsIgnoreCase(Constants.PROPERTY_TRUE))
			return true;
		else if (value.equalsIgnoreCase(Constants.PROPERTY_FALSE))
			return false;
		else
			throw new IllegalArgumentException("Attachments: " + value + " is not a valid boolean");
	}
	
	protected static IllegalArgumentException getSetterExceptionMessage(final DefaultAttachment failingObject, final String propertyName, final String givenValue, final String... allowedValues)
	{
		final StringBuilder rVal = new StringBuilder();
		rVal.append(failingObject.getClass().getName() + ": " + failingObject.getName() + ": property " + propertyName + " must be either ");
		rVal.append(allowedValues[0]);
		for (int i = 1; i < allowedValues.length; ++i)
			rVal.append(" or " + allowedValues[i]);
		return new IllegalArgumentException(rVal.toString() + " ([Not Allowed] Given: " + givenValue + ")");
	}
	
	protected String thisErrorMsg()
	{
		return "   for: " + this.toString();
	}
	
	public String getRawPropertyString(final String property)
	{
		return PropertyUtil.getPropertyFieldObject(property, this).toString();
	}
	
	public Object getRawPropertyObject(final String property)
	{
		return PropertyUtil.getPropertyFieldObject(property, this);
	}
	
	@InternalDoNotExport
	public void setData(final GameData data)
	{
		m_data = data;
	}
	
	protected GameData getData()
	{
		return m_data;
	}
	
	public Attachable getAttachedTo()
	{
		return m_attachedTo;
	}
	
	@InternalDoNotExport
	public void setAttachedTo(final Attachable attachable)
	{
		m_attachedTo = attachable;
	}
	
	public String getName()
	{
		return m_name;
	}
	
	@InternalDoNotExport
	public void setName(final String aString)
	{
		m_name = aString;
	}
	
	/**
	 * Any overriding method for toString needs to include at least the Class, m_attachedTo, and m_name.
	 * Or call super.toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " attached to:" + m_attachedTo + " with name:" + m_name;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DefaultAttachment other = (DefaultAttachment) obj;
		if (m_attachedTo == null)
		{
			if (other.m_attachedTo != null)
				return false;
		}
		else if (!m_attachedTo.toString().equals(other.m_attachedTo.toString()))
			return false;
		// else if (!m_attachedTo.equals(other.m_attachedTo)) // m_attachedTo does not override equals, so we should not test it
		// return false;
		if (m_name == null)
		{
			if (other.m_name != null)
				return false;
		}
		else if (!m_name.equals(other.m_name))
			return false;
		return this.toString().equals(other.toString());
	}
}
