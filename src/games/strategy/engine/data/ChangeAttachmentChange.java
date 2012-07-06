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
package games.strategy.engine.data;

import games.strategy.util.PropertyUtil;

public class ChangeAttachmentChange extends Change
{
	private static final long serialVersionUID = -6447264150952218283L;
	private final Attachable m_attachedTo;
	private final String m_attachmentName;
	private final Object m_newValue;
	private final Object m_oldValue;
	private final String m_property;
	private boolean m_clearFirst = false;
	
	public Attachable getAttachedTo()
	{
		return m_attachedTo;
	}
	
	public String getAttachmentName()
	{
		return m_attachmentName;
	}
	
	ChangeAttachmentChange(final IAttachment attachment, final Object newValue, final String property)
	{
		if (attachment == null)
			throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
		m_attachedTo = attachment.getAttachedTo();
		m_clearFirst = false;
		m_attachmentName = attachment.getName();
		m_oldValue = PropertyUtil.getPropertyFieldObject(property, attachment);
		m_newValue = newValue;
		m_property = property;
		/*if (property.equals("rawProperty"))
		{
			final String[] s = ((String) newValue).split(":");
			m_oldValue = PropertyUtil.getRaw(s[0], attachment);
			m_newValue = s[1];
			m_property = s[0];
		}
		else
		{
			m_oldValue = PropertyUtil.get(property, attachment);
			m_newValue = newValue;
			m_property = property;
		}*/
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	ChangeAttachmentChange(final IAttachment attachment, final Object newValue, final String property, final boolean getRaw, final boolean resetFirst)
	{
		if (attachment == null)
			throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
		m_attachedTo = attachment.getAttachedTo();
		m_clearFirst = resetFirst;
		m_attachmentName = attachment.getName();
		m_oldValue = PropertyUtil.getPropertyFieldObject(property, attachment);
		m_newValue = newValue;
		m_property = property;
		/*if (getRaw)
		{
			m_oldValue = PropertyUtil.getRaw(property, attachment);
			m_newValue = newValue;
			m_property = property;
		}
		else
		{
			m_oldValue = PropertyUtil.get(property, attachment);
			m_newValue = newValue;
			m_property = property;
		}*/
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public ChangeAttachmentChange(final Attachable attachTo, final String attachmentName, final Object newValue, final Object oldValue, final String property, final boolean resetFirst)
	{
		m_attachmentName = attachmentName;
		m_attachedTo = attachTo;
		m_newValue = newValue;
		m_oldValue = oldValue;
		m_property = property;
		m_clearFirst = resetFirst;
	}
	
	@Override
	public void perform(final GameData data)
	{
		final IAttachment attachment = m_attachedTo.getAttachment(m_attachmentName);
		PropertyUtil.set(m_property, m_newValue, attachment, m_clearFirst);
	}
	
	@Override
	public Change invert()
	{
		return new ChangeAttachmentChange(m_attachedTo, m_attachmentName, m_oldValue, m_newValue, m_property, m_clearFirst);
	}
	
	@Override
	public String toString()
	{
		return "ChangAttachmentChange attached to:" + m_attachedTo + " name:" + m_attachmentName + " new value:" + m_newValue + " old value:" + m_oldValue;
	}
}
